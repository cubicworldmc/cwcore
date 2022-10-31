package space.cubicworld.core.database.migration;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.database.CoreDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class Migration {

    public List<InputStream> loadMigrationScripts(ClassLoader classLoader) {
        List<InputStream> result = new ArrayList<>();
        int current = 0;
        while (true) {
            InputStream loaded = classLoader.getResourceAsStream("migrations/" + (current++) + ".sql");
            if (loaded == null) break;
            result.add(loaded);
        }
        return result;
    }

    public Mono<Void> executeMigrationScripts(ClassLoader classLoader, CoreDatabase database) {
        List<InputStream> migrationScripts = loadMigrationScripts(classLoader);
        return Mono.usingWhen(
                database.getConnection(),
                connection -> Mono.fromDirect(connection.beginTransaction())
                        .thenMany(connection
                                .createStatement("""
                                        CREATE TABLE IF NOT EXISTS __migrations(
                                            current INT NOT NULL DEFAULT 0
                                        );
                                        """
                                ).execute()
                        )
                        .thenMany(connection.createStatement("SELECT current FROM __migrations").execute())
                        .flatMap(result -> result.map((row, metadata) -> row.get(0, Integer.class)))
                        .singleOrEmpty()
                        .switchIfEmpty(Mono
                                .fromDirect(connection
                                        .createStatement("INSERT INTO __migrations(current) VALUES(0)")
                                        .execute()
                                )
                                .then(Mono.just(0))
                        )
                        .flatMap(migration -> Mono
                                .fromDirect(connection
                                        .createStatement("UPDATE __migrations SET current = ?")
                                        .bind(0, migrationScripts.size())
                                        .execute()
                                )
                                .then(Mono.just(migration))
                        )
                        .map(migration -> migrationScripts.stream().skip(migration))
                        .flatMapMany(inputStreams -> {
                            Iterator<InputStream> inputStreamIterator = inputStreams.iterator();
                            List<Statement> statements = new ArrayList<>();
                            Exception exception = null;
                            while (inputStreamIterator.hasNext()) {
                                try (InputStream inputStream = inputStreamIterator.next()) {
                                    String sqlFileContent = new String(inputStream.readAllBytes());
                                    String[] sqlStatements = sqlFileContent.split(";");
                                    for (String sqlStatement : sqlStatements) {
                                        if (sqlStatement.isBlank()) continue;
                                        statements.add(connection.createStatement(sqlStatement));
                                    }
                                } catch (Exception e) {
                                    exception = e;
                                    break;
                                }
                            }
                            inputStreamIterator.forEachRemaining(is -> {
                                try (is) {
                                } catch (Exception e) { /* IGNORED */ }
                            });
                            return exception == null ? Flux.fromIterable(statements).flatMap(Statement::execute) : Flux.error(exception);
                        })
                        .thenEmpty(connection.commitTransaction()),
                Connection::close
        );
    }

}
