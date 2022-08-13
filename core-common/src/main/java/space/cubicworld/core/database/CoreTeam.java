package space.cubicworld.core.database;

import com.google.common.base.Suppliers;
import lombok.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreTeam {

    @Data
    @Setter(onMethod_ = @Synchronized)
    @Getter(onMethod_ = @Synchronized)
    static class Fetched {
        private String name;
        private String description;
        private boolean verified;
        private boolean hide;
        private UUID owner;

        public static Fetched fromResultSet(ResultSet resultSet, int start) throws SQLException {
            Fetched result = new Fetched();
            result.setName(resultSet.getString(start++));
            result.setDescription(resultSet.getString(start++));
            result.setVerified(resultSet.getBoolean(start++));
            result.setHide(resultSet.getBoolean(start++));
            result.setOwner(resultSet.getObject(start, UUID.class));
            return result;
        }
    }

    public static CoreTeam fromResultSet(CoreDatabase database, ResultSet resultSet, int start) throws SQLException {
        if (resultSet.next()) {
            CoreTeam result = new CoreTeam(resultSet.getInt(start++), database, false);
            result.fetched = Fetched.fromResultSet(resultSet, start);
            return result;
        }
        return null;
    }

    private final Object updateLock = new Object();

    private final int id;
    private final CoreDatabase database;
    private boolean newObject;

    @Getter(AccessLevel.PACKAGE)
    private final Supplier<CoreDatabaseIterator<CorePlayer>> memberships;
    @Getter(AccessLevel.PACKAGE)
    private final Supplier<CoreDatabaseIterator<CorePlayer>> invited;

    public CoreTeam(int id, CoreDatabase database) {
        this(id, database, true);
    }

    private CoreTeam(int id, CoreDatabase database, boolean newObject) {
        this.id = id;
        this.database = database;
        this.newObject = newObject;
        memberships = Suppliers.memoize(() ->
                fetchPlayerIterator(CorePlayerTeamRelation.Relation.MEMBERSHIP, """
                        SELECT * FROM team_player_relations WHERE team_id = ? AND relation = "MEMBERSHIP"
                        """, id
                )
        );
        invited = Suppliers.memoize(() ->
                fetchPlayerIterator(CorePlayerTeamRelation.Relation.INVITE, """
                        SELECT * FROM team_invitations WHERE team_id = ? AND relation = "INVITE"
                        """, id
                )
        );
    }

    @SneakyThrows
    private CoreDatabaseIterator<CorePlayer> fetchPlayerIterator(
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
                    UUID uuid = resultSet1.getObject(start, UUID.class);
                    CorePlayerTeamRelation relationObject = new CorePlayerTeamRelation(uuid, id, database1);
                    relationObject.setRelation(relation);
                    database1.cache(relationObject);
                    return new CorePlayer(uuid, database1);
                },
                database
        );
    }

    private Fetched fetched;

    @SneakyThrows
    public void close() {
        memberships.get().close();
        invited.get().close();
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
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM teams WHERE id = ?")
            ) {
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();
                fetched = resultSet.next() ?
                        Fetched.fromResultSet(resultSet, 2) : new Fetched();
                resultSet.close();
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        validateFetch();
        return fetched.getName();
    }

    public String getDescription() {
        validateFetch();
        return fetched.getDescription();
    }

    public boolean isVerified() {
        validateFetch();
        return fetched.isVerified();
    }

    public boolean isHide() {
        validateFetch();
        return fetched.isHide();
    }

    public CorePlayer getOwner() {
        validateFetch();
        return database.fetchPlayerByUuid(fetched.getOwner());
    }

    public void setName(String name) {
        validateFetch();
        fetched.setName(name);
    }

    public void setDescription(String description) {
        validateFetch();
        fetched.setDescription(description);
    }

    public void setVerified(boolean verified) {
        validateFetch();
        fetched.setVerified(verified);
    }

    public void setHide(boolean hide) {
        validateFetch();
        fetched.setHide(hide);
    }

    public void setOwner(CorePlayer player) {
        validateFetch();
        fetched.setOwner(player.getUuid());
    }

    public void setOwnerRaw(UUID uuid) {
        validateFetch();
        fetched.setOwner(uuid);
    }

    public void addInvited(CorePlayer player) {
        player.addInvitation(this);
    }

    public void addInvitedRaw(UUID uuid) {
        addInvited(database.fetchPlayerByUuid(uuid));
    }

    public void addMembership(CorePlayer player) {
        player.addMembership(this);
    }

    public void addMembershipRaw(UUID uuid) {
        addMembership(database.fetchPlayerByUuid(uuid));
    }

    public Iterator<CorePlayer> getInvitedIterator() {
        return invited.get().newIterator();
    }

    public Iterator<CorePlayer> getMembershipsIterator() {
        return memberships.get().newIterator();
    }

    public void update() {
        if (fetched != null) {
            updateUnsafe();
            return;
        }
        synchronized (this) {
            if (fetched != null) updateUnsafe();
        }
    }

    public boolean isActuallyExists() {
        validateFetch();
        return fetched.getName() != null;
    }

    @SneakyThrows
    public void remove() {
        synchronized (updateLock) {
            if (!newObject) {
                try (Connection connection = database.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM teams WHERE id = ?")
                ) {
                    statement.setObject(1, id);
                    statement.executeUpdate();
                }
            }
        }
        database.forceUnCache(this);
    }

    @SneakyThrows
    private void updateUnsafe() {
        synchronized (updateLock) {
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(newObject ?
                         "INSERT INTO teams (name, description, verified, hide, owner_uuid, id) VALUES (?, ?, ?, ?, ?, ?)" :
                         "UPDATE teams SET name = ?, description = ?, verified = ?, hide = ?, owner_uuid = ? WHERE id = ?"
                 )) {
                statement.setString(1, fetched.getName());
                statement.setString(2, fetched.getDescription());
                statement.setBoolean(3, fetched.isVerified());
                statement.setBoolean(4, fetched.isHide());
                statement.setObject(5, fetched.getOwner());
                statement.setInt(6, id);
                statement.executeUpdate();
                newObject = false;
            }
        }
    }

}
