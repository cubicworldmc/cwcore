package space.cubicworld.core.repository;

import lombok.RequiredArgsConstructor;
import space.cubicworld.core.database.DatabaseModule;
import space.cubicworld.core.model.CoreTeam;
import space.cubicworld.core.model.CoreTeamMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RequiredArgsConstructor
public class CoreTeamRepository {

    private final DatabaseModule databaseModule;

    public Optional<CoreTeam> findTeam(String name) throws SQLException {
        try (Connection connection = databaseModule.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("""
                     SELECT * FROM teams WHERE name = ?
                     """)
        ) {
            selectStatement.setString(1, name);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(CoreTeam.fromSQL(resultSet));
            }
            return Optional.empty();
        }
    }

    public List<CoreTeamMember> findMembers(int teamId) throws SQLException {
        try (Connection connection = databaseModule.getConnection();
            PreparedStatement selectStatement = connection.prepareStatement("""
                    SELECT member.team_id, member.member_uuid_most, member.member_uuid_least,
                    (team.owner_most = member.member_uuid_most AND team.owner_least = member.member_owner_least)
                    FROM team_members member
                    INNER JOIN teams team ON team.id = member.team_id
                    WHERE team_id = ?
                    """)
        ) {
            selectStatement.setLong(1, teamId);
            ResultSet resultSet = selectStatement.executeQuery();
            List<CoreTeamMember> members = new ArrayList<>();
            while (resultSet.next()) {
                members.add(CoreTeamMember.fromSQL(resultSet, true));
            }
            return Collections.unmodifiableList(members);
        }
    }

    public List<CoreTeamMember> findTeams(UUID member) throws SQLException {
        try (Connection connection = databaseModule.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("""
                    SELECT member.team_id, member.member_uuid_most, member.member_uuid_least,
                    (team.owner_most = member.member_uuid_most AND team.owner_least = member.member_owner_least)
                    FROM team_members member
                    INNER JOIN teams team ON team.id = member.team_id
                    WHERE member.member_uuid_most = ? AND member.member_uuid_least = ?
                     """)
        ) {
            selectStatement.setLong(1, member.getMostSignificantBits());
            selectStatement.setLong(2, member.getLeastSignificantBits());
            ResultSet resultSet = selectStatement.executeQuery();
            List<CoreTeamMember> members = new ArrayList<>();
            while (resultSet.next()) {
                members.add(CoreTeamMember.fromSQL(resultSet, true));
            }
            return Collections.unmodifiableList(members);
        }
    }

}
