package space.cubicworld.core.database.nocache;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.*;
import space.cubicworld.core.json.CoreLightPlayer;
import space.cubicworld.core.json.CoreLightPlayerImpl;

import java.util.Optional;
import java.util.UUID;

@Data
@Builder
class CorePlayerImpl implements CorePlayer {

    private final CoreNoCacheDatabase database;
    private final UUID id;
    private String name;
    private int reputation;
    private CoreColor globalColor;
    private @Nullable Integer selectedTeamId;
    private int inactiveBoosts;
    private @Nullable Long discordId;

    @Override
    public Mono<? extends CoreTeam> getSelectedTeam() {
        return selectedTeamId == null ? Mono.empty() : database.fetchTeam(selectedTeamId);
    }

    @Override
    public void setSelectedTeam(@Nullable CoreTeam team) {
        selectedTeamId = team == null ? null : team.getId();
    }

    @Override
    public Flux<? extends CoreTeam> getRelations(CorePTRelation.Value value, long count) {
        return database.fetchPlayerPTRelations(id, value, count);
    }

    @Override
    public Flux<? extends CoreTeam> getRelations(CorePTRelation.Value value, long count, long skip) {
        return database.fetchPlayerPTRelations(id, value, count, skip);
    }

    @Override
    public Mono<Long> getRelationsCount(CorePTRelation.Value value) {
        return database.fetchPlayerPTRelationsCount(id, value);
    }

    @Override
    public Flux<? extends CoreBoost> getBoosts(long count, long skip) {
        return database.fetchPlayerBoosts(id, count, skip);
    }

    @Override
    public Mono<Long> getBoostsCount() {
        return database.fetchPlayerBoostsCount(id);
    }

    @Override
    public Mono<? extends CoreLightPlayer> asLight() {
        return database.getResolver().resolve(this, globalColor)
                .map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(resolvedColor -> getSelectedTeam()
                        .map(CoreTeam::getName)
                        .map(Optional::of).defaultIfEmpty(Optional.empty())
                        .map(team -> new CoreLightPlayerData(id, name, resolvedColor.orElse(null), team.orElse(null)))
                );
    }
}
