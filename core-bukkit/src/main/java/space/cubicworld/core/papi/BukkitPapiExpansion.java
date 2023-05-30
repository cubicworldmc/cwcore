package space.cubicworld.core.papi;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.cubicworld.core.BukkitPlugin;
import space.cubicworld.core.json.CoreLightPlayer;

import java.util.Locale;

@RequiredArgsConstructor
public class BukkitPapiExpansion extends PlaceholderExpansion {

    private final BukkitPlugin plugin;

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        CoreLightPlayer corePlayer = plugin.getCorePlayers().get(player.getUniqueId());
        if (corePlayer == null) return null;
        if (params.equalsIgnoreCase("selected_team")) {
            return corePlayer.getSelectedTeamPrefix();
        }
        return null;
    }
}
