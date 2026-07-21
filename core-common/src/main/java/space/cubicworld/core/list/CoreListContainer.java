package space.cubicworld.core.list;

import com.electronwill.nightconfig.core.Config;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.database.CoreList;
import space.cubicworld.core.database.CorePLRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.http.CoreHttpServer;
import space.cubicworld.core.json.CoreJsonObjectMapper;
import space.cubicworld.core.util.ImmutablePair;
import space.cubicworld.core.util.PlayerUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CoreListContainer {
    @Data
    public static class RelationMessage {
        private final Long id;
        private final String name;
        private final String list;
        private final CorePLRelation.Value value;
    }

    static class MaybeFluxSink<T> {
        private final List<T> queue = new ArrayList<>();
        private volatile FluxSink<T> sink = null;

        public void next(T el) {
            if (sink != null) {
                sink.next(el);
                return;
            }
            synchronized (this) {
                if (sink != null) {
                    sink.next(el);
                    return;
                }
                queue.add(el);
            }
        }

        public boolean isCancelled() {
            return sink != null && sink.isCancelled();
        }

        public void place(FluxSink<T> sink) {
            if (this.sink != null) {
                throw new IllegalStateException("Sink is already set!");
            }
            synchronized (this) {
                if (this.sink != null) {
                    throw new IllegalStateException("Sink is already set!");
                }
                this.sink = sink;
                for (T el: queue) this.sink.next(el);
                queue.clear();
            }
        }
    }

    private final Map<String, CoreListData> listName2Obj = new HashMap<>();
    // might not be the best solution for integers given that they are somewhat sequential,
    // but in the case where they are not sequential it would be disastrous.
    private final Map<Integer, CoreListData> listId2Obj = new HashMap<>();

    private final Map<UUID, Set<Integer>> playerListAcceptedRelationsCache = new ConcurrentHashMap<>();

    // First we update lastId and only then a relation message shall be sent.
    // It will only send newly created request, it won't send events about previous ones.
    private volatile long lastId = 0L;
    private final List<MaybeFluxSink<RelationMessage>> sinks = new CopyOnWriteArrayList<>();


    private final CorePlugin plugin;

    public CoreListContainer(CorePlugin plugin) {
        this.plugin = plugin;
        Map<String, Object> lists = plugin.getConfig().<Config>get("lists").valueMap();
        plugin.getDatabase().synchronize(lists.keySet()).block();
        for (CoreList list : plugin.getDatabase().fetchLists().collectList().block()) {
            CoreListData data = new CoreListData(plugin, list.getName(), list.getId());
            this.listName2Obj.put(list.getName(), data);
            this.listId2Obj.put(list.getId(), data);
        }
        plugin.getHttpServer().with(server -> server.route(routes -> routes
                .get("/lists/pending/{last}", (inbound, outbound) ->
                        Mono.just(inbound.param("last"))
                                .map(Long::parseLong)
                                .flatMapMany(param -> {
                                    long upto;
                                    MaybeFluxSink<RelationMessage> sinkWrapper = new MaybeFluxSink<>();
                                    synchronized (CoreListContainer.this) {
                                        if (lastId < param) {
                                            return CoreHttpServer.badRequest(
                                                    outbound,
                                                    "Last param is too big, lastId = " + lastId
                                            );
                                        }
                                        // `this` is Mutex for lastId, lastId is updated before sending relation message hence
                                        // we will receive excessive messages, but we will not forget messages.
                                        sinks.add(sinkWrapper);
                                        upto = lastId;
                                    }
                                    return outbound.sendWebsocket((wsInbound, wsOutbound) -> {
                                        wsInbound.receiveCloseStatus().subscribe(_ -> sinks.remove(sinkWrapper));
                                        return wsOutbound.sendByteArray(
                                                Flux.<RelationMessage>concat(
                                                        plugin.getDatabase()
                                                                .fetchAllPendingPLRelations(param, upto)
                                                                .flatMap(this::relationToMessage), // handles messages from last (exclusive) to upto (inclusive)
                                                        Flux.create(sinkWrapper::place) // handles messages from upto (exclusive)
                                                                .filter(val -> val.getId() > upto) // ensures that all messages are from upto (exclusive)
                                                )
                                                        .map(CoreJsonObjectMapper::writeBytes)
                                        );
                                    });
                                })
                                .onErrorResume(_ -> CoreHttpServer.badRequest(
                                        outbound,
                                        "Last param is not an integer or cannot be represented as a signed 64-bit integer"
                                ))
                                .then()
                )
                .put("/list/{name}/{id}/accept", (request, response) ->
                        restSetRelation(request, response, CorePLRelation.Value.ACCEPTED)
                )
                .put("/list/{name}/{id}/decline", (request, response) ->
                        restSetRelation(request, response, CorePLRelation.Value.DECLINED)
                )
        ));
    }

    private Mono<Void> restSetRelation(HttpServerRequest request, HttpServerResponse response, CorePLRelation.Value value) {
        // TODO: handle errors
        int list = this.listName2Obj.get(request.param("name")).getId();
        String idString = request.param("id");
        Mono<UUID> uuidMono;
        if (PlayerUtils.isUuid(idString)) {
            uuidMono = Mono.just(UUID.fromString(idString));
        } else { // name
            uuidMono = plugin.getDatabase().fetchPlayer(idString).map(CorePlayer::getId);
        }
        return uuidMono
                .flatMap(uuid -> updateRelation(uuid, list, value))
                .then(response.status(HttpResponseStatus.OK).send());
    }

    private Mono<RelationMessage> relationToMessage(CorePLRelation rel) {
        return rel.getPlayer()
                .map(player -> new ImmutablePair<>(rel, player.getName()))
                .map(pair -> new RelationMessage(
                        pair.getFirst().getId(),
                        pair.getSecond(),
                        listId2Obj.get(pair.getFirst().getListId()).getName(),
                        rel.getValue()
                ));
    }

    public Mono<Void> updateRelation(UUID player, int list, CorePLRelation.Value value) {
        Set<Integer> listsSet = playerListAcceptedRelationsCache.get(player);
        if (listsSet != null) {
            if (value == CorePLRelation.Value.ACCEPTED) listsSet.add(list);
            else listsSet.remove(list);
        }
        return plugin.getDatabase()
                .fetchPLRelation(player, list)
                .flatMap(relation -> {
                    if (relation.getValue() == value) {
                        return Mono.empty();
                    }
                    relation.setValue(value);
                    return plugin.getDatabase().update(relation)
                            .then(relation.getId() != null ?
                                    Mono.just(relation) :
                                    plugin.getDatabase()
                                            .fetchPLRelation(player, list)
                                            .<CorePLRelation>map(it -> (CorePLRelation)it)
                            );
                })
                .flatMap(this::relationToMessage)
                .doOnNext(message -> {
                    Iterator<MaybeFluxSink<RelationMessage>> currentSinks;
                    synchronized (CoreListContainer.this) {
                        lastId = Long.max(lastId, message.getId());
                        currentSinks = sinks.iterator();
                    }
                    for (Iterator<MaybeFluxSink<RelationMessage>> it = currentSinks; it.hasNext(); ) {
                        it.next().next(message);
                    }
                })
                .then();
    }

    public Mono<Void> cachePlayer(UUID uuid) {
        return plugin
                .getDatabase()
                .fetchPLAcceptedRelations(uuid)
                .map(CorePLRelation::getListId)
                .collect(HashSet<Integer>::new, HashSet::add)
                .doOnNext(set -> {
                    Set<Integer> currentlyStored = playerListAcceptedRelationsCache.putIfAbsent(uuid, set);
                    if (currentlyStored != null) currentlyStored.addAll(set);
                })
                .then();
    }

    public void uncachePlayer(UUID uuid) {
        playerListAcceptedRelationsCache.remove(uuid);
    }

    public Set<Integer> getPlayerCache(UUID uuid) {
        return playerListAcceptedRelationsCache.get(uuid);
    }

    public CoreListData getList(String name) {
        return listName2Obj.get(name);
    }

    public CoreListData getList(int id) {
        return listId2Obj.get(id);
    }

    public Map<String, CoreListData> getLists() {
        return Collections.unmodifiableMap(listName2Obj);
    }
}