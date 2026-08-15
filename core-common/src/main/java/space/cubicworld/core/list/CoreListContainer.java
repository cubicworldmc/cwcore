package space.cubicworld.core.list;

import com.electronwill.nightconfig.core.Config;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.tcp.TcpServer;
import space.cubicworld.core.CorePlugin;
import space.cubicworld.core.database.CoreList;
import space.cubicworld.core.database.CorePLRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.util.PlayerUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CoreListContainer {
    private final Map<String, CoreListData> listName2Obj = new HashMap<>();
    // might not be the best solution for integers given that they are somewhat sequential,
    // but in the case where they are not sequential it would be disastrous.
    private final Map<Integer, CoreListData> listId2Obj = new HashMap<>();

    private final Map<UUID, Set<Integer>> playerListAcceptedRelationsCache = new ConcurrentHashMap<>();

    private final CorePlugin plugin;

    private static final String ENCRYPTION_ALGORITHM = "ChaCha20-Poly1305";
    private static final int NONCE_LEN = 12;
    private static final int TCP_MESSAGE_BYTES_SIZE_LIMIT = 1<<12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @SneakyThrows
    private byte[] encrypt(byte[] bytes) {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        byte[] nonce = new byte[NONCE_LEN];
        SECURE_RANDOM.nextBytes(nonce);
        IvParameterSpec iv = new IvParameterSpec(nonce);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encBytes = cipher.doFinal(bytes);
        int len = nonce.length+encBytes.length;
        return ByteBuffer.allocate(4 + encBytes.length + NONCE_LEN)
                .put((byte) (len&0xff))
                .put((byte) ((len>>8)&0xff))
                .put((byte) ((len>>16)&0xff))
                .put((byte) ((len>>24)&0xff))
                .put(nonce)
                .put(encBytes)
                .array();
    }

    @SneakyThrows
    private String decrypt(byte[] bytes, int len) {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        byte[] nonce = new byte[NONCE_LEN];
        byte[] encBytes = new byte[len - NONCE_LEN];
        bb.get(nonce);
        bb.get(encBytes);
        IvParameterSpec iv = new IvParameterSpec(nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return new String(cipher.doFinal(encBytes), StandardCharsets.UTF_8);
    }

    private boolean verifyTime(TCPMessage message) {
        long l = Long.parseLong(message.getTime());
        return l + tcpMessageTimeout >= System.currentTimeMillis();
    }

    @Data
    @Builder
    private static class TCPMessage {
        public enum C2SKind {
            QUERY,
            ACCEPT,
            DECLINE,
            MAKE_PENDING,
            ;
        }
        private final String time;
        private final String kind;
        private final String id;
        private final String list;

        public static TCPMessage from(String str) {
            String[] split = str.split(" ");
            if (split.length!=4)return null;
            return new TCPMessage(
                    split[0],
                    split[1],
                    split[2],
                    split[3]
            );
        }

        public TCPMessage derive(String kind) {
            return new TCPMessage(
                    String.valueOf(System.currentTimeMillis()),
                    kind,
                    this.id,
                    this.list
            );
        }

        public String toMessageString() {
            return "%s %s %s %s".formatted(time, kind, id, list);
        }

        public C2SKind enumC2SKind() {
            return C2SKind.valueOf(kind);
        }
    }

    private final SecretKey secretKey;
    private final int tcpMessageTimeout;

    public CoreListContainer(CorePlugin plugin) {
        this.plugin = plugin;
        Map<String, Object> lists = plugin.getConfig().<Config>get("lists").valueMap();
        plugin.getDatabase().synchronize(lists.keySet()).block();
        for (CoreList list : plugin.getDatabase().fetchLists().collectList().block()) {
            CoreListData data = new CoreListData(plugin, list.getName(), list.getId());
            this.listName2Obj.put(list.getName(), data);
            this.listId2Obj.put(list.getId(), data);
        }
        try {
            secretKey = plugin.getBootstrap().loadKey(plugin.getConfig().get("lists-tcp.key"), ENCRYPTION_ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        tcpMessageTimeout = plugin.getConfig().get("lists-tcp.message-timeout");
        TcpServer.create()
                .host(plugin.getConfig().get("lists-tcp.host"))
                .port(plugin.getConfig().get("lists-tcp.port"))
                .handle(this::tcpHandle)
                .bind()
                .block();
    }

    private Mono<? extends byte[]> tcpHandleMessage(byte[] arr, int len, FluxSink<?> sink){
        // NOTE: arr must stay within the invocation!
        if (len > TCP_MESSAGE_BYTES_SIZE_LIMIT) {
            sink.complete();
            return Mono.empty();
        }
        String tcpRawMessage = decrypt(arr,len);
        TCPMessage message = TCPMessage.from(tcpRawMessage);
        if (!verifyTime(message)) {
            sink.complete();
            return Mono.empty();
        }
        Function<String, byte[]> derivedMessageBytes = (kind) -> encrypt(
                message.derive(kind).toMessageString().getBytes(StandardCharsets.UTF_8)
        );
        TCPMessage.C2SKind kind = message.enumC2SKind();
        CoreListData listData = this.listName2Obj.get(message.getList());
        if (listData==null){
            plugin.getLogger().warn("TCPMessage contained a non existing list: {}", message.getList());
            return Mono.just(derivedMessageBytes.apply("ERR_LIST"));
        }
        int list = listData.getId();
        Mono<UUID> uuidMono;
        if (PlayerUtils.isUuid(message.getId())) {
            uuidMono = Mono.just(UUID.fromString(message.getId()));
        } else { // name
            uuidMono = plugin.getDatabase().fetchPlayer(message.getId()).map(CorePlayer::getId);
        }
        return uuidMono.flatMap(playerUuid ->
                switch (kind) {
                    case TCPMessage.C2SKind.ACCEPT ->
                            updateRelation(playerUuid, list, CorePLRelation.Value.ACCEPTED)
                                    .then(Mono.just(derivedMessageBytes.apply("OK_ACCEPTED")));
                    case TCPMessage.C2SKind.DECLINE ->
                            updateRelation(playerUuid, list, CorePLRelation.Value.DECLINED)
                                    .then(Mono.just(derivedMessageBytes.apply("OK_DECLINED")));
                    case TCPMessage.C2SKind.MAKE_PENDING ->
                            updateRelation(playerUuid, list, CorePLRelation.Value.PENDING)
                                    .then(Mono.just(derivedMessageBytes.apply("OK_MAKE_PENDING")));
                    case TCPMessage.C2SKind.QUERY ->
                            plugin.getDatabase().fetchPLRelation(playerUuid, list)
                                    .map(it -> derivedMessageBytes.apply(it.getValue().name()));
                }
        ).switchIfEmpty(Mono.fromCallable(() -> derivedMessageBytes.apply("ERR_PLAYER")));
    }

    @Data
    @AllArgsConstructor
    class TCPBufState {
        public byte[] buf;
        public int cur;
        public int len;
    }

    private Publisher<Void> tcpHandle(NettyInbound inbound, NettyOutbound outbound) {
        return outbound.sendByteArray(Flux.create(sink -> {
            TCPBufState state = new TCPBufState(new byte[TCP_MESSAGE_BYTES_SIZE_LIMIT], 0, -1);
            inbound.receive()
                    .flatMap(buf -> {
                        synchronized (state) {
                            while (true) {
                                if (state.len == state.cur) {
                                    tcpHandleMessage(state.buf,state.len,sink).subscribe(sink::next);
                                    state.cur=0;
                                    state.len=-1;
                                }
                                if (buf.readableBytes()==0)break;
                                if (state.len == -1) {
                                    state.buf[state.cur++] = buf.readByte();
                                    if (state.cur == 4) {
                                        state.len = state.buf[0] |
                                                (((int) (state.buf[1])) << 8) |
                                                (((int) (state.buf[2])) << 16) |
                                                (((int) (state.buf[3])) << 24)
                                        ;
                                        if (state.len<=0){
                                            sink.complete();
                                            return Mono.empty();
                                        }
                                        state.cur = 0;
                                    }
                                }
                                else {
                                    state.buf[state.cur++] = buf.readByte();
                                }
                            }
                        }

                        return Mono.empty();
                    })
                    .doOnError(err -> sink.complete())
                    .subscribe();
        }));

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
                    return plugin.getDatabase().update(relation);
                });
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