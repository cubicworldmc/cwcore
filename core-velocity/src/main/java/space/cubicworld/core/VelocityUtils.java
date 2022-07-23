package space.cubicworld.core;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import space.cubicworld.core.message.CoreMessageContainer;

import java.util.Locale;

@UtilityClass
public class VelocityUtils {

    public Locale getLocale(CommandSource source) {
        if (source instanceof Player player) {
            return player.getPlayerSettings().getLocale();
        }
        return Locale.ENGLISH;
    }

    public void sendChat(CommandSource source, Component component, CoreMessageContainer messageContainer) {
        source.sendMessage(messageContainer.renderChat(component, getLocale(source)));
    }

    public void sendExternal(CommandSource source, Component component, CoreMessageContainer messageContainer) {
        source.sendMessage(messageContainer.renderExternal(component, getLocale(source)));
    }

}
