package space.cubicworld.core.model;

import lombok.*;

import java.sql.SQLException;
import java.util.UUID;

@Data
@Getter(onMethod_ = @Synchronized)
@Setter(onMethod_ = @Synchronized)
@Builder
public class CoreTeam {

    @Data
    @Builder
    public static class CoreTeamCreator {
        @NonNull
        private final String name;
        private final String description;
        @NonNull
        private final UUID owner;
    }

    @Data
    @Builder
    public static class CoreTeamApplication {
        private final String name;
        private final UUID owner;

        public static CoreTeamApplication read(CoreResult result) throws SQLException {
            return CoreTeamApplication
                    .builder()
                    .name(result.readRow())
                    .owner(new UUID(result.readRow(), result.readRow()))
                    .build();
        }

        public static CoreStatement<CoreTeamApplication> insertStatement() {
            return CoreStatement.<CoreTeamApplication>builder()
                    .sql("INSERT INTO team_applications (name, uuid_most, uuid_least) VALUES (?, ?, ?)")
                    .parameter(CoreTeamApplication::getName)
                    .parameter(application -> application.getOwner().getMostSignificantBits())
                    .parameter(application -> application.getOwner().getLeastSignificantBits())
                    .build();
        }

        public static CoreStatement<String> selectByNameStatement() {
            return CoreStatement.<String>builder()
                    .sql("SELECT * FROM team_application WHERE name = ?")
                    .parameter(name -> name)
                    .build();
        }

        public static CoreStatement<Integer> selectSingleStatement() {
            return CoreStatement.<Integer>builder()
                    .sql("SELECT * FROM team_applications LIMIT ?, 1")
                    .parameter(value -> value)
                    .build();
        }

        public static CoreStatement<CoreTeamApplication> deleteStatement() {
            return CoreStatement.<CoreTeamApplication>builder()
                    .sql("DELETE FROM team_application WHERE name = ? AND uuid_most = ? and uuid_least = ?")
                    .parameter(CoreTeamApplication::getName)
                    .parameter(application -> application.getOwner().getMostSignificantBits())
                    .parameter(application -> application.getOwner().getLeastSignificantBits())
                    .build();
        }

    }

    public static CoreTeam read(CoreResult result) throws SQLException {
        return CoreTeam.builder()
                .id(result.readRow())
                .name(result.readRow())
                .description(result.readRow())
                .owner(new UUID(result.readRow(), result.readRow()))
                .build();
    }

    public static CoreStatement<CoreTeamCreator> insertStatement() {
        return CoreStatement.<CoreTeamCreator>builder()
                .sql("""
                        INSERT INTO teams
                        (name, description, owner_uuid_most, owner_uuid_least)
                        VALUES (?, ?, ?, ?)
                        """)
                .parameter(CoreTeamCreator::getName)
                .parameter(CoreTeamCreator::getDescription)
                .parameter(creator -> creator.getOwner().getMostSignificantBits())
                .parameter(creator -> creator.getOwner().getLeastSignificantBits())
                .build();
    }

    public static CoreStatement<Integer> selectByIdStatement() {
        return CoreStatement.<Integer>builder()
                .sql("SELECT * FROM teams WHERE id = ?")
                .parameter(id -> id)
                .build();
    }

    public static CoreStatement<String> selectByNameStatement() {
        return CoreStatement.<String>builder()
                .sql("SELECT * FROM teams WHERE name = ?")
                .parameter(name -> name)
                .build();
    }

    public static CoreStatement<CoreTeam> updateStatement() {
        return CoreStatement.<CoreTeam>builder()
                .sql("""
                        UPDATE teams
                        SET name = ?, description = ?, owner_uuid_most = ?, owner_uuid_least = ?
                        WHERE id = ?
                        """)
                .parameter(CoreTeam::getName)
                .parameter(CoreTeam::getDescription)
                .parameter(team -> team.getOwner().getMostSignificantBits())
                .parameter(team -> team.getOwner().getLeastSignificantBits())
                .parameter(CoreTeam::getId)
                .build();
    }

    private final int id;
    private String name;
    private String description;
    private UUID owner;

}
