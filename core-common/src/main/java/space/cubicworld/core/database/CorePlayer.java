package space.cubicworld.core.database;

import com.google.common.base.Suppliers;
import lombok.*;
import net.kyori.adventure.text.format.TextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CorePlayer {

    @Data
    @Getter(onMethod_ = @Synchronized)
    @Setter(onMethod_ = @Synchronized)
    static class Fetched {
        private String name;
        private int reputation;
        private TextColor globalColor;
        private Integer selectedTeamId;

        public static Fetched fromResultSet(ResultSet resultSet, int start) throws SQLException {
            Fetched result = new Fetched();
            result.setName(resultSet.getString(start++));
            result.setReputation(resultSet.getInt(start++));
            int color = resultSet.getInt(start++);
            result.setGlobalColor(color == -1 ? null : TextColor.color(color));
            result.setSelectedTeamId(resultSet.getObject(start, Integer.class));
            return result;
        }

    }

    public static CorePlayer fromResultSet(CoreDatabase database, ResultSet resultSet, int start) throws SQLException {
        if (resultSet.next()) {
            CorePlayer result = new CorePlayer(UUID.fromString(resultSet.getString(start++)), database, false);
            result.fetched = Fetched.fromResultSet(resultSet, start);
            return result;
        }
        return null;
    }

    private final Object updateLock = new Object();

    private final UUID uuid;
    private final CoreDatabase database;
    private boolean newObject;

    private final Supplier<CoreDatabaseIterator<CoreTeam>> memberships;
    private final Supplier<CoreDatabaseIterator<CoreTeam>> invites;

    public CorePlayer(UUID uuid, CoreDatabase database) {
        this(uuid, database, true);
    }

    private CorePlayer(UUID uuid, CoreDatabase database, boolean newObject) {
        this.uuid = uuid;
        this.database = database;
        this.newObject = newObject;
        memberships = Suppliers.memoize(() ->
                fetchTeamIterator(CorePlayerTeamRelation.Relation.MEMBERSHIP, """
                        SELECT * FROM team_player_relations WHERE player_uuid = ? AND relation = "MEMBERSHIP"
                        """, uuid.toString()
                )
        );
        invites = Suppliers.memoize(() ->
                fetchTeamIterator(CorePlayerTeamRelation.Relation.INVITE, """
                        SELECT * FROM team_player_relations WHERE player_uuid = ? AND relation = "INVITE"
                        """, uuid.toString()
                )
        );
    }

    @SneakyThrows
    private CoreDatabaseIterator<CoreTeam> fetchTeamIterator(
            CorePlayerTeamRelation.Relation relation, String sql, Object... objects) {
        Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        int counter = 0;
        for (Object obj : objects) {
            statement.setObject(++counter, obj);
        }
        ResultSet resultSet = statement.executeQuery();
        return new CoreDatabaseIterator.WithConnection<>(
                connection,
                statement,
                resultSet,
                (database1, resultSet1, start) -> {
                    int teamId = resultSet1.getInt(start + 1);
                    CorePlayerTeamRelation relationObject = new CorePlayerTeamRelation(
                            uuid, teamId, database1
                    );
                    relationObject.setRelation(relation);
                    database1.cache(relationObject);
                    return new CoreTeam(teamId, database1);
                },
                database
        );
    }

    private Fetched fetched;

    public UUID getUuid() {
        return uuid;
    }

    @SneakyThrows
    public void close() {
        memberships.get().close();
        invites.get().close();
    }

    void setFetched(Fetched fetched) {
        if (this.fetched != null) return;
        synchronized (this) {
            if (this.fetched != null) return;
            this.fetched = fetched;
        }
    }

    Fetched getFetched() {
        return fetched;
    }

    @SneakyThrows
    public void validateFetch() {
        if (fetched != null) return;
        synchronized (this) {
            if (fetched != null) return;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();
                synchronized (updateLock) {
                    newObject = !resultSet.next();
                    fetched = newObject ?
                            new Fetched() : Fetched.fromResultSet(resultSet, 2) ;
                }
                resultSet.close();
            }
        }
    }

    public String getName() {
        validateFetch();
        return fetched.getName();
    }

    public int getReputation() {
        validateFetch();
        return fetched.getReputation();
    }

    public TextColor getGlobalColor() {
        validateFetch();
        return fetched.getGlobalColor();
    }

    public CoreTeam getSelectedTeam() {
        validateFetch();
        return fetched.selectedTeamId == null ? null : database.fetchTeamById(fetched.selectedTeamId);
    }

    public void setName(String name) {
        validateFetch();
        fetched.setName(name);
    }

    public void setReputation(int reputation) {
        validateFetch();
        fetched.setReputation(reputation);
    }

    public void setGlobalColor(TextColor color) {
        validateFetch();
        fetched.setGlobalColor(color);
    }

    public void setSelectedTeam(CoreTeam team) {
        validateFetch();
        fetched.setSelectedTeamId(team == null ? null : team.getId());
    }

    public void setSelectedTeamRaw(Integer id) {
        validateFetch();
        fetched.setSelectedTeamId(id);
    }

    @SneakyThrows
    public void addInvitation(CoreTeam team) {
        CorePlayerTeamRelation.update(database, CorePlayerTeamRelation.Relation.INVITE, uuid, team.getId());
        invites.get().addObject(team);
        team.getInvited().get().addObject(this);
    }

    public void addInvitationRaw(int teamId) {
        addInvitation(database.fetchTeamById(teamId));
    }

    @SneakyThrows
    public void addMembership(CoreTeam team) {
        CorePlayerTeamRelation.update(database, CorePlayerTeamRelation.Relation.MEMBERSHIP, uuid, team.getId());
        memberships.get().addObject(team);
        team.getMemberships().get().addObject(this);
    }

    public void addMembershipRaw(int teamId) {
        addMembership(database.fetchTeamById(teamId));
    }

    public void update() throws SQLException {
        if (fetched != null) {
            updateUnsafe();
            return;
        }
        synchronized (this) {
            if (fetched != null) updateUnsafe();
        }
    }

    @SneakyThrows
    public void remove() {
        synchronized (updateLock) {
            if (!newObject) {
                try (Connection connection = database.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM teams WHERE uuid = ?")
                ) {
                    statement.setString(1, uuid.toString());
                    statement.executeUpdate();
                }
            }
        }
        database.forceUnCache(this);
    }

    public Iterator<CoreTeam> getInvitationsIterator() {
        return invites.get().newIterator();
    }

    public Iterator<CoreTeam> getMembershipsIterator() {
        return memberships.get().newIterator();
    }

    public boolean isActuallyExists() {
        validateFetch();
        return fetched.getName() != null;
    }

    private void updateUnsafe() throws SQLException {
        synchronized (updateLock) {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(newObject ?
                         "INSERT INTO players (name, reputation, global_color, selected_team_id, uuid) VALUES (?, ?, ?, ?, ?)" :
                         "UPDATE players SET name = ?, reputation = ?, global_color = ?, selected_team_id = ? WHERE uuid = ?"
                 )
            ) {
                statement.setString(1, fetched.getName());
                statement.setInt(2, fetched.getReputation());
                TextColor globalColor = fetched.getGlobalColor();
                statement.setInt(3, globalColor == null ? -1 : globalColor.value());
                statement.setObject(4, fetched.selectedTeamId);
                statement.setString(5, uuid.toString());
                statement.executeUpdate();
                newObject = false;
            }
        }
    }

}
