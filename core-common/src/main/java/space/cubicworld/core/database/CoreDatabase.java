package space.cubicworld.core.database;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CoreDatabase extends AutoCloseable {

    Mono<? extends Connection> getConnection();

    Mono<? extends CorePlayer> fetchPlayer(UUID id);

    Mono<? extends CorePlayer> fetchPlayer(String name);

    Mono<? extends CoreTeam> fetchTeam(int id);

    Mono<? extends CoreTeam> fetchTeam(String name);

    Flux<? extends CoreTeam> fetchTeamsByVerified(boolean verified, long count, long skip);

    Mono<Long> fetchTeamsCountByVerified(boolean verified);

    Mono<? extends CorePTRelation> fetchPTRelation(UUID player, int team);

    Mono<? extends CoreBoost> fetchBoost(long id);

    Mono<Long> fetchTeamReputation(int id);

    Mono<Long> fetchTeamPTRelationsCount(int team, CorePTRelation.Value value);

    Mono<Long> fetchPlayerPTRelationsCount(UUID player, CorePTRelation.Value value);

    Flux<? extends CorePlayer> fetchTeamPTRelations(int team, CorePTRelation.Value value, long count);

    Flux<? extends CorePlayer> fetchTeamPTRelations(int team, CorePTRelation.Value value, long count, long skip);

    Flux<? extends CoreTeam> fetchPlayerPTRelations(UUID player, CorePTRelation.Value value, long count);

    Flux<? extends CoreTeam> fetchPlayerPTRelations(UUID player, CorePTRelation.Value value, long count, long skip);

    Flux<? extends CoreBoost> fetchPlayerBoosts(UUID player, long count, long skip);

    Mono<? extends CoreBoost> fetchLastPlayerBoost(UUID player);

    Mono<? extends CoreTeam> fetchPlayerNotVerifiedOwnedTeam(UUID player);

    Mono<Long> fetchPlayerBoostsCount(UUID player);

    Flux<? extends CoreBoost> fetchTeamBoosts(int team);

    Mono<Long> fetchTeamBoostsCount(int team);

    Flux<? extends CoreTeam> fetchTeamReputationTop(int page);

    Flux<? extends CorePlayer> fetchPlayerReputationTop(int page);

    Mono<? extends CoreTeam> newTeam(String name, UUID owner);

    Mono<? extends CorePlayer> newPlayer(UUID id, String name);

    Mono<? extends CoreBoost> newBoost(UUID player);

    Mono<Void> update(CorePlayer player);

    Mono<Void> update(CoreTeam team);

    Mono<Void> update(CorePTRelation relation);

    Mono<Void> update(CoreBoost boost);

    Mono<Void> remove(CorePlayer player);

    Mono<Void> remove(CoreTeam team);

    Mono<Void> remove(CoreBoost boost);

}
