package space.cubicworld.core.message;

import lombok.Cleanup;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.slf4j.Logger;
import space.cubicworld.core.model.CorePlayer;
import space.cubicworld.core.model.CoreTeam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;

import static net.kyori.adventure.text.Component.*;

@UtilityClass
public class CoreMessage {

    private final TextColor FAIL_COLOR = NamedTextColor.WHITE;
    private final TextColor MENTION_COLOR = NamedTextColor.BLUE;
    private final TextColor SUCCESS_COLOR = NamedTextColor.WHITE;
    private final TextColor INFORMATION_COLOR = NamedTextColor.WHITE;

    public void register(ClassLoader classLoader, Logger logger) {
        TranslationRegistry registry = TranslationRegistry.create(Key.key("cwcore", "main"));
        registry.defaultLocale(Locale.ENGLISH);
        for (Locale translation : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("ru")}) {
            try (InputStream translationIs = classLoader.getResourceAsStream(
                    "translation/%s.properties".formatted(translation.getLanguage())
            )) {
                if (translationIs == null) {
                    logger.warn("Failed to find {} translation", translation.getLanguage());
                    continue;
                }
                registry.registerAll(
                        translation,
                        new PropertyResourceBundle(new InputStreamReader(translationIs, StandardCharsets.UTF_8)),
                        false
                );
            } catch (IOException e) {
                logger.warn("Failed to load {} translation: ", translation.getLanguage(), e);
            }
        }
        if (!GlobalTranslator.translator().addSource(registry)) {
            logger.error("Failed to add cwcore translation registry");
        }
    }

    public Component provideTeamName() {
        return translatable("cwcore.command.provide.team")
                .color(FAIL_COLOR);
    }

    public Component providePlayerName() {
        return translatable("cwcore.command.provide.player")
                .color(FAIL_COLOR);
    }

    public Component forPlayer() {
        return translatable("cwcore.command.for.player")
                .color(FAIL_COLOR);
    }

    public Component playerNotExist(String playerName) {
        return translatable("cwcore.player.not_exist")
                .args(text(playerName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component teamNotExist(String teamName) {
        return translatable("cwcore.team.not_exist")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component playerMention(CorePlayer player) {
        TextColor color = player.getGlobalColor();
        return text(player.getName())
                .hoverEvent(HoverEvent.showEntity(
                        Key.key("minecraft", "player"),
                        player.getUuid()
                ))
                .color(color == null ? MENTION_COLOR : color);
    }

    public Component playerReputation(CorePlayer player) {
        return translatable("cwcore.reputation.see")
                .args(
                        playerMention(player),
                        text(Integer.toString(player.getReputation())).color(MENTION_COLOR)
                )
                .color(INFORMATION_COLOR);
    }

    public Component teamMention(CoreTeam team) {
        return text(team.getName()).color(MENTION_COLOR);
    }

    public Component teamAlreadyExist(String teamName) {
        return translatable("cwcore.team.already.exist")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component alreadyInTeam(CorePlayer player, CoreTeam team) {
        return translatable("cwcore.team.already.in")
                .args(playerMention(player), teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component teamCreated(CoreTeam team) {
        return translatable("cwcore.team.created")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component teamInvitedAlready(CorePlayer invited, CoreTeam team) {
        return translatable("cwcore.team.already.invited")
                .args(playerMention(invited), teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component teamInvitationSend(CorePlayer invited, CoreTeam team) {
        return translatable("cwcore.team.invited")
                .args(playerMention(invited), teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component teamInvite(CorePlayer inviter, CoreTeam team) {
        return translatable("cwcore.team.invite")
                .args(playerMention(inviter), teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component notInvited(CoreTeam team) {
        return translatable("cwcore.team.invited.not")
                .args(teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component inviteAccepted(CoreTeam team) {
        return translatable("cwcore.team.invite.accept")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component oneTeamNotVerified(CoreTeam team) {
        return translatable("cwcore.team.verified.not")
                .args(teamMention(team))
                .color(FAIL_COLOR);
    }

}
