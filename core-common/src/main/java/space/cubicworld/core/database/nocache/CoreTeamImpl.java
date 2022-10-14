package space.cubicworld.core.database.nocache;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.database.CoreBoost;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;

import java.util.UUID;

@Data
@Builder
class CoreTeamImpl implements CoreTeam {

    private final CoreNoCacheDatabase database;
    private final int id;

    private int updated;

    private @NotNull String name;
    private @Nullable String description;
    private @NotNull UUID ownerId;
    private boolean hide;
    private boolean verified;

    public void setName(String name) {
        updated = updated | 0x1;
        this.name = name;
    }

    public void setDescription(String description) {
        updated = updated | 0x2;
        this.description = description;
    }

    public void setOwnerId(UUID ownerId) {
        updated = updated | 0x4;
        this.ownerId = ownerId;
    }

    public void setHide(boolean hide) {
        updated = updated | 0x8;
        this.hide = hide;
    }

    public void setVerified(boolean verified) {
        updated = updated | 0xf;
        this.verified = verified;
    }

    @Override
    public Mono<? extends CorePlayer> getOwner() {
        return database.fetchPlayer(ownerId);
    }

    @Override
    public void setOwner(@NotNull CorePlayer owner) {
        ownerId = owner.getId();
    }

    @Override
    public Flux<? extends CorePlayer> getRelations(CorePTRelation.Value value, long count) {
        return database.fetchTeamPTRelations(id, value, count);
    }

    @Override
    public Flux<? extends CorePlayer> getRelations(CorePTRelation.Value value, long count, long skip) {
        return database.fetchTeamPTRelations(id, value, count, skip);
    }

    @Override
    public Mono<Long> getRelationsCount(CorePTRelation.Value value) {
        return database.fetchTeamPTRelationsCount(id, value);
    }

    @Override
    public Flux<? extends CoreBoost> getBoosts() {
        return database.fetchTeamBoosts(id);
    }

    @Override
    public Mono<Long> getReputation() {
        return database.fetchTeamReputation(id);
    }

    @Override
    public Mono<Long> getUpgradeLevel() {
        return database.fetchTeamBoostsCount(id);
    }

    @Override
    public Mono<Long> getMaxMembers() {
        return getUpgradeLevel().flatMap(level -> database.getResolver().getTeamLimit(level));
    }

}
