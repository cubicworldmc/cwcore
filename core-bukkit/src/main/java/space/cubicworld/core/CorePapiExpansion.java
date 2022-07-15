package space.cubicworld.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CorePapiExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "cwcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "jenya705";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;
        return switch (params) {
            case "color" ->
                    color(player, CorePlayer::getGlobalColor, "defaults.global-color");
            case "overworld_color" ->
                    color(player, CorePlayer::getOverworldColor, "defaults.overworld-color");
            case "nether_color" ->
                    color(player, CorePlayer::getNetherColor, "defaults.nether-color");
            case "end_color" ->
                    color(player, CorePlayer::getEndColor, "defaults.end-color");
            default -> null;
        };
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String color(OfflinePlayer offlinePlayer, Function<CorePlayer, TextColor> colorFunction, String defaultKey) {
        return BukkitCoreUtils.toColorCode(
                BukkitPlugin.getInstance()
                        .getOptionalPlayer(offlinePlayer.getUniqueId())
                        .map(colorFunction)
                        .orElseGet(() -> BukkitCoreUtils.getColorNamed(
                                BukkitPlugin.getInstance()
                                        .getConfig()
                                        .getString(defaultKey)
                        ))
        );
    }

}
