package space.cubicworld.core.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.VelocityUtils;
import space.cubicworld.core.model.CoreMember;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.sql.SQLException;
import java.util.*;

@RequiredArgsConstructor
public class TeamCommand implements SimpleCommand {

    private static final List<String> SUGGESTS = List.of(
            "create",
            "invite",
            "accept",
            "about",
            "delete",
            "remove",
            "update"
    );

    private final VelocityPlugin plugin;

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) return; // TODO print suggests
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component
                    .text("Only for players")
                    .color(NamedTextColor.RED)
            );
            return;
        }
        // <sub_command> ...
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> {
                    // <team_name>
                    if (args.length < 2) return; // TODO print scheme
                    String teamName = args[1];
                    if (plugin.getCache().loadTeam(teamName) != null &&
                        !CoreTeam.CoreTeamApplication
                                .selectByNameStatement()
                                .query(
                                        plugin.getDatabase(),
                                        CoreTeam.CoreTeamApplication::read,
                                        teamName
                                ).isEmpty())
                        return; // TODO say that command with this name already exists
                    CoreTeam.CoreTeamApplication.insertStatement()
                            .update(
                                    plugin.getDatabase(),
                                    CoreTeam.CoreTeamApplication
                                            .builder()
                                            .name(teamName)
                                            .owner(player.getUniqueId())
                                            .build()
                                    );
                    plugin.getServer().getAllPlayers()
                            .stream()
                            .filter(onlinePlayer -> onlinePlayer.hasPermission("cwcore.team.approve"))
                            .forEach(onlinePlayer -> VelocityUtils.sendChat(
                                    onlinePlayer,
                                    plugin.getMessageContainer().newApplication(teamName),
                                    plugin.getMessageContainer()
                            ));
                    VelocityUtils.sendChat(
                            invocation.source(),
                            plugin.getMessageContainer().applicationSent(),
                            plugin.getMessageContainer()
                    );
                }
                case "invite" -> {
                    // <team_name> <player>
                }
                case "accept" -> {
                    // <team_name>
                }
                case "about" -> {
                    if (args.length < 2) return;
                    about(invocation, args[1]);
                }
                case "delete" -> {
                    // <team_name> [confirm]
                }
                case "remove" -> {
                    // <team_name> <player>
                }
                case "update" -> {
                    // <team_name> <description|name|owner> <value>
                }
                case "approve" -> {
                    // <team_name>
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warn(
                    "Exception while team command execution args: {}:",
                    Arrays.toString(args), e
            );
        }
    }

    private void about(Invocation invocation, String teamName) throws SQLException {
        CoreTeam team = plugin.getCache().loadTeam(teamName);
        if (team == null) return;
        List<UUID> members = plugin.getCache().loadTeamMembers(team.getId());
        List<CorePlayer> memberNames = new ArrayList<>(members.size());
        for (UUID uuid: members) {
            memberNames.add(plugin.getCache().loadPlayer(uuid, true));
        }
        VelocityUtils.sendChat(
                invocation.source(),
                plugin.getMessageContainer().teamAbout(
                        team,
                        plugin.getCache().loadPlayer(team.getOwner(), true),
                        memberNames
                ),
                plugin.getMessageContainer()
        );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return SUGGESTS;
        }
        return null;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().getPermissionValue("cwcore.team") != Tristate.FALSE;
    }
}
