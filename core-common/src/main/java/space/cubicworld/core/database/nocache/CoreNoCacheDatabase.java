package space.cubicworld.core.database.nocache;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.*;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.CoreResolver;
import space.cubicworld.core.CoreStatic;
import space.cubicworld.core.color.CoreColor;
import space.cubicworld.core.database.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;

public class CoreNoCacheDatabase implements CoreDatabase {

    private final ConnectionFactory factory;
    private final ConnectionPool pool;

    @Getter
    private final CoreResolver resolver;

    public CoreNoCacheDatabase(
            String mysqlHost,
            String mysqlUsername,
            String mysqlPassword,
            String mysqlDatabase,
            boolean mysqlSsl,
            ClassLoader classLoader,
            CoreResolver resolver
    ) throws IOException {
        String[] host = mysqlHost.split(":");
        factory = ConnectionFactories.get(
                ConnectionFactoryOptions.builder()
                        .option(ConnectionFactoryOptions.DRIVER, "pool")
                        .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
                        .option(ConnectionFactoryOptions.USER, mysqlUsername)
                        .option(ConnectionFactoryOptions.PASSWORD, mysqlPassword)
                        .option(ConnectionFactoryOptions.HOST, host.length == 0 ? "localhost" : host[0])
                        .option(ConnectionFactoryOptions.PORT, host.length == 1 ? 3306 : Integer.parseInt(host[1]))
                        .option(ConnectionFactoryOptions.DATABASE, mysqlDatabase)
                        .option(ConnectionFactoryOptions.SSL, mysqlSsl)
                        .build()
        );
        pool = new ConnectionPool(ConnectionPoolConfiguration.builder(factory).build());
        this.resolver = resolver;
        try (InputStream setupSqlIs = classLoader.getResourceAsStream("setup.sql")) {
            String setupSqlString = new String(setupSqlIs.readAllBytes());
            String[] setupSqlStatements = setupSqlString.split(";");
            Flux.usingWhen(
                    getConnection(),
                    connection -> Mono.from(connection.beginTransaction())
                            .thenMany(Flux.fromArray(setupSqlStatements)
                                    .map(connection::createStatement)
                                    .flatMap(Statement::execute)
                            )
                            .thenMany(connection.commitTransaction()),
                    Connection::close
            ).collectList().block();
        }
    }

    @Override
    public void close() throws Exception {
        pool.dispose();
    }

    @Override
    public Mono<? extends Connection> getConnection() {
        return Mono.fromDirect(factory.create());
    }

    @Override
    public Mono<? extends CorePlayer> fetchPlayer(UUID id) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parsePlayers(
                        Flux.from(connection
                                .createStatement("SELECT * FROM players WHERE uuid = ?")
                                .bind(0, id.toString())
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CorePlayer> fetchPlayer(String name) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parsePlayers(
                        Flux.from(connection
                                .createStatement("SELECT * FROM players WHERE name = ?")
                                .bind(0, name)
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    private Flux<? extends CorePlayer> parsePlayers(Flux<? extends Result> flux) {
        return flux.flatMap(
                result -> result.map((row, metadata) -> CorePlayerImpl.builder()
                        .database(this)
                        .id(UUID.fromString(row.get(0, String.class)))
                        .name(row.get(1, String.class))
                        .reputation(row.get(2, Integer.class))
                        .globalColor(CoreColor.fromInteger(row.get(3, Integer.class)))
                        .selectedTeamId(row.get(4, Integer.class))
                        .inactiveBoosts(row.get(5, Integer.class))
                        .build()
                )
        );
    }

    @Override
    public Mono<? extends CoreTeam> fetchTeam(int id) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("SELECT * FROM teams WHERE id = ?")
                                .bind(0, id)
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreTeam> fetchTeam(String name) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("SELECT * FROM teams WHERE name = ?")
                                .bind(0, name)
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CoreTeam> fetchTeamsByVerified(boolean verified, long count, long skip) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("SELECT * FROM teams WHERE verified = ? LIMIT ?, ?")
                                .bind(0, verified)
                                .bind(1, skip)
                                .bind(2, count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchTeamsCountByVerified(boolean verified) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("SELECT COUNT(*) FROM teams WHERE verified = ?")
                                .bind(0, verified)
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Long.class)))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    private Flux<? extends CoreTeam> parseTeams(Flux<? extends Result> flux) {
        return flux.flatMap(
                result -> result.map((row, metadata) -> CoreTeamImpl.builder()
                        .database(this)
                        .id(row.get(0, Integer.class))
                        .name(row.get(1, String.class))
                        .description(row.get(2, String.class))
                        .verified(row.get(3, Boolean.class))
                        .hide(row.get(4, Boolean.class))
                        .ownerId(UUID.fromString(row.get(5, String.class)))
                        .build()
                )
        );
    }

    @Override
    public Mono<? extends CorePTRelation> fetchPTRelation(UUID player, int team) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux.from(connection
                                .createStatement("SELECT relation FROM team_player_relations WHERE player_uuid = ? AND team_id = ?")
                                .bind(0, player.toString())
                                .bind(1, team)
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> CorePTRelationImpl
                                .builder()
                                .database(this)
                                .teamId(team)
                                .playerId(player)
                                .value(CorePTRelation.Value.valueOf(row.get(0, String.class)))
                                .build()
                        ))
                        .singleOrEmpty()
                        .switchIfEmpty(Mono.defer(() -> Mono.just(CorePTRelationImpl
                                .builder()
                                .database(this)
                                .teamId(team)
                                .playerId(player)
                                .value(CorePTRelation.Value.NONE)
                                .build()
                        ))),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreBoost> fetchBoost(long id) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parseBoosts(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT pb.id, pb.player_uuid, pb.ends, tb.team_id FROM player_boosts pb
                                        LEFT JOIN team_boosts tb ON tb.boost_id = ?id
                                        WHERE pb.id = ?id
                                        """)
                                .bind("id", id)
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchTeamReputation(int id) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("""
                                        SELECT IFNULL(SUM(p.reputation), 0) FROM team_player_relations r
                                        INNER JOIN players p ON p.uuid = r.player_uuid AND r.relation = "MEMBERSHIP" AND r.team_id = ?
                                        """)
                                .bind(0, id)
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, BigDecimal.class).longValueExact()))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchTeamPTRelationsCount(int team, CorePTRelation.Value value) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("""
                                        SELECT COUNT(*) FROM team_player_relations r
                                        WHERE r.team_id = ? AND r.relation = ?
                                        """)
                                .bind(0, team)
                                .bind(1, value.toString())
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Long.class)))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchPlayerPTRelationsCount(UUID player, CorePTRelation.Value value) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("""
                                        SELECT COUNT(*) FROM team_player_relations r
                                        WHERE r.player_uuid = ? AND r.relation = ?
                                        """)
                                .bind(0, player.toString())
                                .bind(1, value.toString())
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Long.class)))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CorePlayer> fetchTeamPTRelations(int team, CorePTRelation.Value value, long count) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parsePlayers(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT p.* FROM players p
                                        INNER JOIN team_player_relations r ON r.player_uuid = p.uuid AND r.team_id = ? AND r.relation = ?
                                        LIMIT ?
                                        """)
                                .bind(0, team)
                                .bind(1, value.toString())
                                .bind(2, count == -1 ? Integer.MAX_VALUE : count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CorePlayer> fetchTeamPTRelations(int team, CorePTRelation.Value value, long count, long skip) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parsePlayers(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT p.* FROM players p
                                        INNER JOIN team_player_relations r ON r.player_uuid = p.uuid AND r.team_id = ? AND r.relation = ?
                                        LIMIT ?, ?
                                        """)
                                .bind(0, team)
                                .bind(1, value.toString())
                                .bind(2, skip)
                                .bind(3, count == -1 ? Integer.MAX_VALUE : count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CoreTeam> fetchPlayerPTRelations(UUID player, CorePTRelation.Value value, long count) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT t.* FROM teams t
                                        INNER JOIN team_player_relations r ON r.player_uuid = ? AND r.team_id = t.id AND r.relation = ?
                                        LIMIT ?
                                        """)
                                .bind(0, player.toString())
                                .bind(1, value.toString())
                                .bind(2, count == -1 ? Integer.MAX_VALUE : count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CoreTeam> fetchPlayerPTRelations(UUID player, CorePTRelation.Value value, long count, long skip) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT t.* FROM teams t
                                        INNER JOIN team_player_relations r ON r.player_uuid = ? AND r.team_id = t.id AND r.relation = ?
                                        LIMIT ?, ?
                                        """)
                                .bind(0, player.toString())
                                .bind(1, value.toString())
                                .bind(2, skip)
                                .bind(3, count == -1 ? Integer.MAX_VALUE : count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CoreBoost> fetchPlayerBoosts(UUID player, long count, long skip) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseBoosts(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT pb.id, pb.player_uuid, pb.ends, tb.team_id FROM player_boosts pb
                                        LEFT JOIN team_boosts tb ON tb.boost_id = pb.id
                                        WHERE pb.player_uuid = ?
                                        LIMIT ?, ?
                                        """)
                                .bind(0, player.toString())
                                .bind(1, skip)
                                .bind(2, count)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreBoost> fetchLastPlayerBoost(UUID player) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parseBoosts(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT pb.*, tb.team_id FROM player_boosts pb
                                        LEFT JOIN team_boosts tb ON tb.boost_id = pb.id
                                        WHERE player_uuid = ?
                                        ORDER BY ends DESC
                                        LIMIT 1
                                        """)
                                .bind(0, player.toString())
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreTeam> fetchPlayerNotVerifiedOwnedTeam(UUID player) {
        return Mono.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT t.* FROM teams t
                                        INNER JOIN team_player_relations r ON r.team_id = t.id AND r.player_uuid = ?owner AND r.relation = "MEMBERSHIP"
                                        WHERE t.owner_uuid = ?owner AND t.verified = false
                                        """)
                                .bind("owner", player.toString())
                                .execute()
                        )
                ).singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CoreBoost> fetchTeamBoosts(int team) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseBoosts(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT pb.id, pb.player_uuid, pb.ends, tb.team_id FROM player_boosts pb
                                        LEFT JOIN team_boosts tb ON tb.boost_id = pb.id AND tb.team_id = ?
                                        """)
                                .bind(0, team)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    private Flux<? extends CoreBoost> parseBoosts(Flux<? extends Result> flux) {
        return flux.flatMap(
                result -> result.map((row, metadata) -> CoreBoostImpl.builder()
                        .database(this)
                        .id(row.get(0, Long.class))
                        .playerId(UUID.fromString(row.get(1, String.class)))
                        .end(row.get(2, Long.class))
                        .teamId(row.get(3, Integer.class))
                        .build()
                )
        );
    }

    @Override
    public Flux<? extends CoreTeam> fetchTeamReputationTop(int page) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parseTeams(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT t.* FROM team_player_relations r
                                        INNER JOIN players p ON p.uuid = r.player_uuid AND r.relation = "MEMBERSHIP"
                                        INNER JOIN teams t ON t.id = r.team_id
                                        GROUP BY r.team_id
                                        ORDER BY SUM(DISTINCT p.reputation) DESC
                                        LIMIT ?,?
                                        """)
                                .bind(0, page * CoreStatic.TOP_SIZE)
                                .bind(1, CoreStatic.TOP_SIZE)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Flux<? extends CorePlayer> fetchPlayerReputationTop(int page) {
        return Flux.usingWhen(
                getConnection(),
                connection -> parsePlayers(
                        Flux.from(connection
                                .createStatement("""
                                        SELECT p.* FROM players p
                                        ORDER BY p.reputation DESC
                                        LIMIT ?,?
                                        """)
                                .bind(0, page * CoreStatic.TOP_SIZE)
                                .bind(1, CoreStatic.TOP_SIZE)
                                .execute()
                        )
                ),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchPlayerBoostsCount(UUID player) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("""
                                        SELECT COUNT(*) FROM player_boosts pb
                                        WHERE pb.player_uuid = ? AND pb.ends > ?
                                        """)
                                .bind(0, player.toString())
                                .bind(1, System.currentTimeMillis())
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Long.class)))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<Long> fetchTeamBoostsCount(int team) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("""
                                        SELECT COUNT(*) FROM team_boosts tb
                                        INNER JOIN player_boosts pb ON pb.id = tb.boost_id AND pb.ends > ?
                                        WHERE tb.team_id = ?
                                        """)
                                .bind(0, System.currentTimeMillis())
                                .bind(1, team)
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Long.class)))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreTeam> newTeam(String name, UUID owner) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("INSERT INTO teams (name, owner_uuid) VALUES (?, ?)")
                                .bind(0, name)
                                .bind(1, owner.toString())
                                .returnGeneratedValues("id")
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> CoreTeamImpl.builder()
                                .database(this)
                                .id(row.get(0, Integer.class))
                                .name(name)
                                .ownerId(owner)
                                .build()
                        ))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CorePlayer> newPlayer(UUID id, String name) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("INSERT INTO players (uuid, name) VALUES (?, ?)")
                                .bind(0, id.toString())
                                .bind(1, name)
                                .execute()
                        )
                        .singleOrEmpty()
                        .map(it -> CorePlayerImpl.builder()
                                .database(this)
                                .id(id)
                                .name(name)
                                .build()
                        ),
                Connection::close
        );
    }

    @Override
    public Mono<? extends CoreBoost> newBoost(UUID player) {
        long ends = System.currentTimeMillis() + CoreStatic.BOOST_EXTEND_UNIX;
        return Mono.usingWhen(
                getConnection(),
                connection -> Flux
                        .from(connection
                                .createStatement("INSERT INTO player_boosts (player_uuid, ends) VALUES (?, ?)")
                                .bind(0, player.toString())
                                .bind(1, ends)
                                .returnGeneratedValues("id")
                                .execute()
                        )
                        .flatMap(result -> result.map((row, metadata) -> CoreBoostImpl.builder()
                                .database(this)
                                .id(row.get(0, Long.class))
                                .playerId(player)
                                .end(ends)
                                .build()
                        ))
                        .singleOrEmpty(),
                Connection::close
        );
    }

    @Override
    public Mono<Void> update(CorePlayer player) {
        return Mono.usingWhen(
                getConnection(),
                connection -> {
                    Statement statement = connection
                            .createStatement("""
                                    UPDATE players SET
                                    name = ?, reputation = ?, global_color = ?, selected_team_id = ?, boosts = ?
                                    WHERE uuid = ?
                                    """)
                            .bind(0, player.getName())
                            .bind(1, player.getReputation())
                            .bind(2, player.getGlobalColor().toInteger())
                            .bind(4, player.getInactiveBoosts())
                            .bind(5, player.getId().toString());
                    if (player.getSelectedTeamId() == null) statement.bindNull(3, Integer.class);
                    else statement.bind(3, player.getSelectedTeamId());
                    return Mono.fromDirect(statement.execute()).then();
                },
                Connection::close
        );
    }

    @Override
    public Mono<Void> update(CoreTeam team) {
        return Mono.usingWhen(
                getConnection(),
                connection -> {
                    Statement statement = connection
                            .createStatement("""
                                    UPDATE teams SET
                                    name = ?, description = ?, verified = ?, hide = ?, owner_uuid = ?
                                    WHERE id = ?
                                    """)
                            .bind(0, team.getName())
                            .bind(2, team.isVerified())
                            .bind(3, team.isHide())
                            .bind(4, team.getOwnerId().toString())
                            .bind(5, team.getId());
                    if (team.getDescription() == null) statement.bindNull(1, String.class);
                    else statement.bind(1, team.getDescription());
                    return Mono.fromDirect(statement.execute()).then();
                },
                Connection::close
        );
    }

    @Override
    public Mono<Void> update(CorePTRelation relation) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Mono.fromDirect(
                        (relation.getValue() == CorePTRelation.Value.NONE ?
                                connection.createStatement("DELETE FROM team_player_relations WHERE player_uuid = ? AND team_id = ?")
                                        .bind(0, relation.getPlayerId().toString())
                                        .bind(1, relation.getTeamId()) :
                                connection.createStatement("""
                                                INSERT INTO team_player_relations (player_uuid, team_id, relation) VALUES (?, ?, ?relation)
                                                ON DUPLICATE KEY UPDATE relation = ?relation
                                                """)
                                        .bind(0, relation.getPlayerId().toString())
                                        .bind(1, relation.getTeamId())
                                        .bind("relation", relation.getValue().toString())
                        ).execute()
                ).then(),
                Connection::close
        );
    }

    @Override
    public Mono<Void> update(CoreBoost boost) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Mono.from(connection.beginTransaction())
                        .then(Mono.from(connection
                                .createStatement("UPDATE player_boosts SET ends = ? WHERE id = ?")
                                .bind(0, boost.getEnd())
                                .bind(1, boost.getId())
                                .execute()
                        ))
                        .then(Mono.from(
                                (boost.getTeamId() == null ?
                                        connection
                                                .createStatement("DELETE FROM team_boosts WHERE boost_id = ?")
                                                .bind(0, boost.getId()) :
                                        connection
                                                .createStatement(
                                                        "INSERT INTO team_boosts (boost_id, team_id) VALUES (?, ?ti) ON DUPLICATE KEY UPDATE team_id = ?ti")
                                                .bind(0, boost.getId())
                                                .bind("ti", boost.getTeamId())
                                ).execute()
                        ))
                        .then(Mono.from(connection.commitTransaction()))
                        .then(),
                Connection::close
        );
    }

    @Override
    public Mono<Void> remove(CorePlayer player) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Mono
                        .fromDirect(connection
                                .createStatement("DELETE FROM players WHERE uuid = ?")
                                .bind(0, player.getId().toString())
                                .execute()
                        )
                        .then(),
                Connection::close
        );
    }

    @Override
    public Mono<Void> remove(CoreTeam team) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Mono
                        .fromDirect(connection
                                .createStatement("DELETE FROM teams WHERE id = ?")
                                .bind(0, team.getId())
                                .execute()
                        )
                        .then(),
                Connection::close
        );
    }

    @Override
    public Mono<Void> remove(CoreBoost boost) {
        return Mono.usingWhen(
                getConnection(),
                connection -> Mono
                        .fromDirect(connection
                                .createStatement("DELETE FROM player_boosts WHERE id = ?")
                                .bind(0, boost.getId())
                                .execute()
                        )
                        .then(),
                Connection::close
        );
    }
}
