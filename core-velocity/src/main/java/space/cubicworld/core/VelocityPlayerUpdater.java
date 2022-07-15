package space.cubicworld.core;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.parser.CoreSerializer;
import space.cubicworld.core.util.Pair;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@RequiredArgsConstructor
public class VelocityPlayerUpdater {

    public static final ChannelIdentifier PLAYER_UPDATE_IDENTIFIER =
            MinecraftChannelIdentifier.create(CoreStatic.CWCORE_KEY, CoreStatic.PLAYER_UPDATE_CHANNEL);

    private final VelocityPlugin plugin;

    public void update(Player player, String key, Object value) {
        update(player, new Pair<>(key, value));
    }

    @SafeVarargs
    public final void update(Player player, Pair<String, Object>... values) {
        if (values.length == 0) return;
        player.getCurrentServer().ifPresentOrElse(
                server -> server.sendPluginMessage(
                        PLAYER_UPDATE_IDENTIFIER,
                        CoreSerializer.write(values).getBytes(StandardCharsets.UTF_8)
                ),
                () -> CoreStatic.getLogger()
                        .warn("Sending update of the player ({}) which is not connected to the server", player)
        );
        StringBuilder statementBuilder = new StringBuilder();
        statementBuilder.append("UPDATE players SET ");
        int valuesCounter = 0;
        for (Pair<String, Object> value: values) {
            statementBuilder.append(CoreDataValue.getSQLName(value.getFirst()))
                    .append(" = ?");
            if (++valuesCounter != values.length) statementBuilder.append(',');
        }
        statementBuilder.append(" WHERE uuid_most = ? AND uuid_least = ?");
        try (Connection connection = plugin.getDatabaseModule().getConnection();
            PreparedStatement statement = connection.prepareStatement(statementBuilder.toString())
        ) {
            int counter = 1;
            for (Pair<String, Object> value: values) {
                statement.setObject(counter++, CoreSerializer.getPrimitive(value.getSecond()));
            }
            statement.setLong(counter++, player.getUniqueId().getMostSignificantBits());
            statement.setLong(counter, player.getUniqueId().getLeastSignificantBits());
            statement.executeUpdate();
        } catch (SQLException e) {
            CoreStatic.getLogger().error("Failed to update player. Generated SQL: {} : \n", statementBuilder, e);
        }
    }

    @Subscribe
    public void messageReceived(PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

}
