package space.cubicworld.core.database;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoreDatabase {

    Connection getConnection();

    Optional<CorePlayer> fetchPlayer(UUID id);

    Optional<CorePlayer> fetchPlayer(String name);

    List<CorePlayer> fetchPlayers(String sql, Object... objects);

    Optional<CoreTeam> fetchTeam(int id);

    Optional<CoreTeam> fetchTeam(String name);

    List<CoreTeam> fetchTeams(String sql, Object... objects);

    Optional<CorePTRelation> fetchPTRelation(UUID player, int team);

    Optional<CoreBoost> fetchBoost(long id);

    List<CoreBoost> fetchPlayerBoosts(UUID player);

    List<CoreBoost> fetchTeamBoosts(int team);

    CoreTeam newTeam(String name, UUID owner);

    CorePlayer newPlayer(UUID id, String name);

    CoreBoost newBoost(UUID player);

    void update(CorePlayer player);

    void update(CoreTeam team);

    void update(CorePTRelation relation);

    void update(CoreBoost boost);

    void remove(CorePlayer player);

    void remove(CoreTeam team);

    void remove(CoreBoost boost);

}
