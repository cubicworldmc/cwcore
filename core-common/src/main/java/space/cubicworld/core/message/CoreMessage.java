package space.cubicworld.core.message;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.slf4j.Logger;
import space.cubicworld.core.color.ColorRule;
import space.cubicworld.core.database.CorePTRelation;
import space.cubicworld.core.database.CorePlayer;
import space.cubicworld.core.database.CoreTeam;
import space.cubicworld.core.util.ImmutablePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;

import static net.kyori.adventure.text.Component.*;

@UtilityClass
public class CoreMessage {

    public final TextColor FAIL_COLOR = NamedTextColor.WHITE;
    public final TextColor MENTION_COLOR = NamedTextColor.BLUE;
    public final TextColor SUCCESS_COLOR = NamedTextColor.WHITE;
    public final TextColor INFORMATION_COLOR = NamedTextColor.WHITE;
    public final TextColor CLICKABLE_COLOR = NamedTextColor.GOLD;
    public final TextColor INACTIVE_COLOR = NamedTextColor.GRAY;

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

    private Component confirm() {
        return clickable(translatable("cwcore.command.confirm"));
    }

    public Component listElement(Component component) {
        return empty()
                .append(text("-").color(INACTIVE_COLOR))
                .append(space())
                .append(component);
    }

    public Component clickable(Component component) {
        return empty()
                .append(text("[").color(INFORMATION_COLOR))
                .append(component.colorIfAbsent(CLICKABLE_COLOR))
                .append(text("]").color(INFORMATION_COLOR));
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
        return translatable("cwcore.player.exist.not")
                .args(text(playerName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component teamNotExist(String teamName) {
        return translatable("cwcore.team.exist.not")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component playerMention(CorePlayer player) {
        TextColor color = player.getGlobalColor();
        return text(player.getName())
                .hoverEvent(HoverEvent.showEntity(
                        Key.key("minecraft", "player"),
                        player.getId()
                ))
                .clickEvent(ClickEvent.runCommand("/tell %s".formatted(player.getName())))
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

    public Component teamAlreadyVerified(CoreTeam team) {
        return translatable("cwcore.team.verified.already")
                .args(teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component teamVerifiedSet(CoreTeam team) {
        return translatable("cwcore.team.verified.set")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    @SneakyThrows
    public Component teamAbout(CoreTeam team, boolean forMember) {
        Component membersMessage;
        if (forMember || !team.isHide()) {
            membersMessage = empty();
            List<CorePlayer> members = team.getRelations(CorePTRelation.Value.MEMBERSHIP, 11);
            if (!members.isEmpty()) {
                for (int i = 0; ; ++i) {
                    if (i == 10) {
                        membersMessage = membersMessage.append(text("..."));
                        break;
                    }
                    CorePlayer member = members.get(i);
                    membersMessage = membersMessage.append(playerMention(member));
                    if (i < members.size() - 1) {
                        membersMessage = membersMessage.append(text(", "));
                    } else {
                        break;
                    }
                }
            }
        } else {
            membersMessage = translatable("cwcore.team.about.hide");
        }
        return empty()
                .append(translatable("cwcore.team.about.name")
                        .args(teamMention(team))
                )
                .append(Component.newline())
                .append(team.getDescription() == null ?
                        empty() :
                        translatable("cwcore.team.about.description")
                                .args(text(team.getDescription()))
                                .append(newline())
                )
                .append(translatable("cwcore.team.about.owner")
                        .args(playerMention(team.getOwner()))
                )
                .append(newline())
                .append(translatable("cwcore.team.about.members")
                        .args(membersMessage)
                );
    }

    public Component teamLeaveCanNot(String teamName) {
        return translatable("cwcore.team.leave.not")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component teamLeaved(CoreTeam team) {
        return translatable("cwcore.team.leave")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component teamDeleteCanNot(String teamName) {
        return translatable("cwcore.team.delete.not")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Component teamDeleteConfirm(CoreTeam team) {
        return empty()
                .append(translatable("cwcore.team.delete.confirm")
                        .args(teamMention(team))
                        .color(INFORMATION_COLOR)
                )
                .append(space())
                .append(confirm()
                        .clickEvent(ClickEvent
                                .runCommand("/team delete %s confirm".formatted(team.getName()))
                        )
                )
                .decorate(TextDecoration.BOLD);
    }

    public Component teamDeleted(CoreTeam team) {
        return translatable("cwcore.team.delete")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component teamInviteJoinNotification(int count) {
        return translatable("cwcore.team.invite.join.notification")
                .args(
                        text(count).color(MENTION_COLOR),
                        clickable(text("/team invites"))
                                .clickEvent(ClickEvent.runCommand("/team invites"))
                )
                .color(INFORMATION_COLOR);
    }

    public Component teamInvitesPage(CorePlayer player, int page) {
        if (page < -1) throw new IllegalArgumentException("Page is negative");
        int invites = player.getRelationsCount(CorePTRelation.Value.INVITE);
        int totalPages = invites / 5 + (invites % 5 == 0 ? 0 : 1);
        boolean previous = page != 0;
        boolean next = page + 1 != totalPages;
        List<Component> teams = player
                .getAllRelations(CorePTRelation.Value.INVITE)
                .stream()
                .skip(page * 5)
                .map(team ->
                        listElement(teamMention(team) // should it be mention or clickable object?
                                .clickEvent(ClickEvent.runCommand("/team accept " + team.getName()))
                                .append(newline()))
                )
                .toList();
        return teams.isEmpty() ?
                translatable("cwcore.team.invites.nothing").color(FAIL_COLOR) :
                empty()
                        .append(
                                translatable("cwcore.team.invites.header")
                                        .args(
                                                text(page + 1).color(MENTION_COLOR),
                                                text(totalPages).color(MENTION_COLOR)
                                        )
                                        .color(INFORMATION_COLOR)
                        )
                        .append(newline())
                        .append(join(JoinConfiguration.noSeparators(), teams))
                        .append(
                                clickable(translatable("cwcore.team.invites.previous")
                                        .color(previous ? CLICKABLE_COLOR : INACTIVE_COLOR)
                                ).clickEvent(previous ? ClickEvent.runCommand("/team invites " + page) : null)
                        )
                        .append(space())
                        .append(
                                clickable(translatable("cwcore.team.invites.next")
                                        .color(next ? CLICKABLE_COLOR : INACTIVE_COLOR)
                                ).clickEvent(next ? ClickEvent.runCommand("/team invites " + (page + 2)) : null)
                        );
    }

    public Component teamNotMember(CorePlayer player, CoreTeam team) {
        return translatable("cwcore.team.member.not")
                .args(playerMention(player), teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component kickSuccess(CorePlayer player, CoreTeam team) {
        return translatable("cwcore.team.kick.success")
                .args(playerMention(player), teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component kickSelf() {
        return translatable("cwcore.team.kick.self")
                .color(FAIL_COLOR);
    }

    public Component message(CorePlayer sender, String message) {
        return empty()
                .append(playerMention(sender)
                        .append(space())
                        .append(text(">"))
                        .append(space())
                )
                .append(text(message));
    }

    public Component teamMessage(CoreTeam team, CorePlayer sender, String message) {
        return empty()
                .append(teamMention(team)
                        .append(text(":"))
                        .append(space())
                )
                .append(message(sender, message));
    }

    public Component teamProvideSettingsValueType() {
        return translatable("cwcore.team.settings.value.type.provide")
                .args(join(
                        JoinConfiguration.commas(true),
                        List.of(
                                text("description").color(MENTION_COLOR),
                                text("hide").color(MENTION_COLOR)
                        )
                ));
    }

    public Component teamSettingsHideBad() {
        return translatable("cwcore.team.settings.value.hide.bad")
                .args(
                        text("true").color(MENTION_COLOR),
                        text("false").color(MENTION_COLOR)
                );
    }

    public Component teamSettingsHideUpdated(CoreTeam team, boolean value) {
        return translatable("cwcore.team.settings.value.hide.success." + value)
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component teamSettingsDescriptionBad(int maxLength) {
        return translatable("cwcore.team.settings.value.description.bad")
                .args(text(maxLength).color(MENTION_COLOR));
    }

    public Component teamSettingsDescriptionUpdated(CoreTeam team) {
        return translatable("cwcore.team.settings.value.description.success")
                .args(teamMention(team))
                .color(SUCCESS_COLOR);
    }

    public Component colorInfo(
            boolean customColors,
            CorePlayer player,
            List<ImmutablePair<ColorRule, TextColor>> rules
    ) {
        Component result = empty();
        if (customColors) {
            result = result
                    .append(translatable("cwcore.color.header.custom")
                            .color(INFORMATION_COLOR)
                    ).append(newline());
            int counter = 0;
            for (NamedTextColor color : NamedTextColor.NAMES.values()) {
                if (++counter % 4 == 0) result = result.append(newline());
                result = result
                        .append(translatable("cwcore.color.default." + color)
                                .color(color)
                        ).append(space());
            }
            result = result.append(translatable("cwcore.color.custom")
                    .color(INFORMATION_COLOR)
            );
        }
        result = result
                .append(translatable("cwcore.color.header.advancements").color(INFORMATION_COLOR))
                .append(newline());
        for (ImmutablePair<ColorRule, TextColor> rule : rules) {
            TextColor color = customColors || rule.getFirst().isMatches(player) ?
                    rule.getSecond() : INACTIVE_COLOR;
            result = result.append(
                    listElement(rule.getFirst().getMessage().color(color))
            ).append(newline());
        }
        return result;
    }

    public Component colorSuccess(TextColor color) {
        return translatable("cwcore.color.success")
                .color(color);
    }

    public Component colorBad() {
        return translatable("cwcore.color.bad")
                .color(FAIL_COLOR);
    }


}
