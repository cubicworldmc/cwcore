package space.cubicworld.core.database.migration;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.database.CoreDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Migration {

    @SneakyThrows
    public int getMigrationsCount(ClassLoader classLoader) {
        int current = 0;
        while (true) {
            InputStream loaded = classLoader.getResourceAsStream("migrations/" + current + ".sql");
            if (loaded == null) break;
            loaded.close();
            current++;
        }
        return current;
    }

    public Mono<Void> executeMigrationScripts(ClassLoader classLoader, CoreDatabase database) {
        int migrationsCount = getMigrationsCount(classLoader);
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
                                        .bind(0, migrationsCount)
                                        .execute()
                                )
                                .then(Mono.just(migration))
                        )
                        .flatMapMany(migration -> {
                            List<Statement> statements = new ArrayList<>();
                            int current = migration;
                            while (true) {
                                try (InputStream inputStream = classLoader.getResourceAsStream("migrations/" + (current++) + ".sql")) {
                                    if (inputStream == null) break;
                                    String sqlFileContent = new String(inputStream.readAllBytes());
                                    String[] sqlStatements = sqlFileContent.split(";");
                                    for (String sqlStatement : sqlStatements) {
                                        if (sqlStatement.isBlank()) continue;
                                        statements.add(connection.createStatement(sqlStatement));
                                    }
                                } catch (Exception e) {
                                    return Flux.error(e);
                                }
                            }
                            return Flux.fromIterable(statements).flatMap(Statement::execute);
                        })
                        .thenEmpty(connection.commitTransaction()),
                Connection::close
        );
    }

}
