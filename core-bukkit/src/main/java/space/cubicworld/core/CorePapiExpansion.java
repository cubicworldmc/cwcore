package space.cubicworld.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.cubicworld.core.model.CorePlayer;

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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return null;
        return switch (params) {
            case "color" ->
                    color(player, CorePlayer::getGlobalColor, "defaults.global-color", "cwcore.color");
            case "overworld_color" ->
                    color(player, CorePlayer::getOverworldColor, "defaults.overworld-color", "cwcore.overworld.color");
            case "nether_color" ->
                    color(player, CorePlayer::getNetherColor, "defaults.nether-color", "cwcore.nether.color");
            case "end_color" ->
                    color(player, CorePlayer::getEndColor, "defaults.end-color", "cwcore.end.color");
            default -> null;
        };
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String color(Player player, Function<CorePlayer, TextColor> colorFunction, String defaultKey, String permission) {
        return BukkitCoreUtils.toColorCode(
                BukkitPlugin.getInstance()
                        .getPlayerLoader().get(player.getUniqueId())
                        .filter(value -> player.hasPermission(permission))
                        .map(colorFunction)
                        .orElseGet(() -> CoreUtils.getColorNamed(
                                BukkitPlugin.getInstance()
                                        .getConfig()
                                        .getString(defaultKey)
                        ))
        );
    }

}
