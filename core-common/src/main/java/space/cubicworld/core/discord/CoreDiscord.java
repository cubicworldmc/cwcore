package space.cubicworld.core.discord;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import space.cubicworld.core.CorePlugin;

public class CoreDiscord {

    private final CorePlugin plugin;
    private final GatewayDiscordClient discordClient;

    public CoreDiscord(CorePlugin plugin) {
        this.plugin = plugin;
        discordClient = DiscordClient
                .create(plugin.getConfig().get("discord.token"))
                .login()
                .block();
    }

}
