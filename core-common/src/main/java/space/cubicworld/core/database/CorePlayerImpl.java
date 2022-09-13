package space.cubicworld.core.database;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.cubicworld.core.color.CoreColor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Getter(onMethod_ = @Synchronized("lock"))
@Setter(onMethod_ = @Synchronized("lock"))
class CorePlayerImpl implements CorePlayer {

    private final Object lock = new Object[0];

    private final CoreDatabaseImpl database;

    @NotNull
    @Getter
    private final UUID id;
    @NotNull
    private String name;
    private int reputation;
    @Nullable
    private CoreColor globalColor = CoreColor.empty();
    private Integer selectedTeamId;
    private int inactiveBoosts;

    @Override
    public @Nullable TextColor getResolvedGlobalColor() {
        synchronized (lock) {
            return database.getResolver().resolve(this, globalColor);
        }
    }

    @Override
    public void setName(@NotNull String name) {
        synchronized (lock) {
            database.getPlayerCache().changeSecondaryKey(this.name, name);
            this.name = name;
        }
    }

    @Override
    public void setReputation(int reputation) {
        int offset;
        synchronized (lock) {
            offset = reputation - this.reputation;
            this.reputation = reputation;
        }
    }

    public void setRawReputation(int reputation) {
        synchronized (lock) {
            this.reputation = reputation;
        }
    }

    @Override
    public @Nullable CoreTeam getSelectedTeam() {
        synchronized (lock) {
            return selectedTeamId == null ?
                    null :
                    database.fetchTeam(selectedTeamId)
                            .orElseGet(() -> {
                                setSelectedTeamId(null);
                                return null;
                            });
        }
    }

    @Override
    public void setSelectedTeam(@Nullable CoreTeam team) {
        synchronized (lock) {
            this.selectedTeamId = (team == null ? null : team.getId());
        }
    }

    @Override
    public @NotNull List<CoreTeam> getRelations(CorePTRelation.Value value, int count) {
        return database
                .getRelationCache()
                .fetchTeams(value, id, count)
                .stream()
                .map(id -> database.fetchTeam(id).orElseThrow())
                .collect(Collectors.toList());
    }

    @Override
    public int getRelationsCount(CorePTRelation.Value value) {
        return database
                .getRelationCache()
                .fetchTeamsSize(value, id);
    }

    @Override
    public List<CoreBoost> getBoosts() {
        return database.fetchPlayerBoosts(id);
    }

    @Override
    public void incrementInactiveBoosts() {
        synchronized (lock) {
            inactiveBoosts++;
        }
    }

    @Override
    public void decrementInactiveBoosts() {
        synchronized (lock) {
            inactiveBoosts--;
        }
    }
}
