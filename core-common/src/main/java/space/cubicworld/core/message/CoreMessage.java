package space.cubicworld.core.message;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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
import space.cubicworld.core.database.*;
import space.cubicworld.core.json.CoreLightPlayer;
import space.cubicworld.core.util.ImmutablePair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.kyori.adventure.text.Component.*;

@UtilityClass
public class CoreMessage {

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");

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

    public Component playerMention(CoreLightPlayer player) {
        TextColor color = player.getResolvedGlobalColor();
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
        return text(team.getName())
                .hoverEvent(HoverEvent.showText(empty()
                        .append(team.getDescription() == null ?
                                empty() :
                                text(team.getDescription()).append(newline())
                        )
                        .append(translatable("cwcore.team.about.owner")
                                .args(playerMention(team.getOwner()))
                        )
                ))
                .color(MENTION_COLOR);
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
                        .args(teamMention(team).append(
                                team.isVerified() ? text(" ✔") : empty()
                        ))
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
                        .args(
                                membersMessage,
                                text(team.getRelationsCount(CorePTRelation.Value.MEMBERSHIP))
                                        .color(MENTION_COLOR),
                                text(team.getMaxMembers()).color(MENTION_COLOR)
                        )
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
                .skip(page * 5L)
                .map(team ->
                        listElement(
                                empty()
                                        .append(teamMention(team))
                                        .append(space())
                                        .append(clickable(
                                                translatable("cwcore.team.invites.accept")
                                                        .clickEvent(ClickEvent.runCommand("/team accept " + team.getName()))
                                        ))
                                        .append(space())
                                        .append(clickable(
                                                translatable("cwcore.team.invites.read")
                                                        .clickEvent(ClickEvent.runCommand("/team read " + team.getName()))
                                        ))
                        ).append(newline())
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
                                clickable(translatable("cwcore.previous")
                                        .color(previous ? CLICKABLE_COLOR : INACTIVE_COLOR)
                                ).clickEvent(previous ? ClickEvent.runCommand("/team invites " + page) : null)
                        )
                        .append(space())
                        .append(
                                clickable(translatable("cwcore.next")
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

    public Component message(CoreLightPlayer sender, Component message) {
        return empty()
                .append(playerMention(sender)
                        .color(NamedTextColor.WHITE)
                        .append(space())
                        .append(text(">")
                                .decorate(TextDecoration.BOLD)
                                .color(sender.getResolvedGlobalColor())
                        )
                        .append(space())
                )
                .append(message);
    }

    public Component teamMessage(CoreTeam team, CorePlayer sender, String message) {
        return empty()
                .append(teamMention(team)
                        .append(text(":"))
                        .append(space())
                )
                .append(message(sender, text(message)));
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
                result = result
                        .append(translatable("cwcore.color.default." + color)
                                .color(color)
                                .clickEvent(ClickEvent.runCommand("/color " + color))
                        ).append(space());
                if (++counter % 4 == 0) result = result.append(newline());
            }
            result = result.append(translatable("cwcore.color.custom")
                    .color(INFORMATION_COLOR)
            ).append(newline());
        }
        result = result
                .append(translatable("cwcore.color.header.advancements").color(INFORMATION_COLOR))
                .append(newline());
        int counter = 0;
        for (ImmutablePair<ColorRule, TextColor> rule : rules) {
            boolean active = rule.getFirst().isMatches(player);
            TextColor color = active ? rule.getSecond() : INACTIVE_COLOR;
            result = result.append(
                    listElement(rule.getFirst().getMessage()
                            .color(color)
                            .clickEvent(active ? ClickEvent.runCommand("/color - " + counter) : null)
                    )
            ).append(newline());
            counter += 1;
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

    public Component readSuccess(CoreTeam team) {
        return translatable("cwcore.team.read.success")
                .args(teamMention(team));
    }

    public Component verifies(CoreDatabase database, int page, List<Integer> notVerified) {
        if (page < 0) throw new IllegalArgumentException("page is negative");
        int totalPages = notVerified.size() / 5 + (notVerified.size() % 5 == 0 ? 0 : 1);
        if (totalPages == 0) return translatable("cwcore.team.verifies.nothing")
                .color(FAIL_COLOR);
        boolean previous = page != 0;
        boolean next = totalPages != page + 1;
        List<Component> teams = notVerified
                .stream()
                .skip(page * 5)
                .map(id -> database.fetchTeam(id).orElseThrow())
                .map(team -> listElement(teamMention(team)
                        .append(space())
                        .append(clickable(translatable("cwcore.team.verifies.verify")
                                .clickEvent(ClickEvent.runCommand("/team verify " + team.getName()))
                        ))
                ))
                .toList();
        return empty()
                .append(translatable("cwcore.team.verifies.header")
                        .args(
                                text(page + 1).color(MENTION_COLOR),
                                text(totalPages).color(MENTION_COLOR)
                        )
                )
                .append(newline())
                .append(join(JoinConfiguration.newlines(), teams))
                .append(newline())
                .append(clickable(translatable("cwcore.previous")
                        .color(previous ? CLICKABLE_COLOR : INACTIVE_COLOR)
                        .clickEvent(previous ? ClickEvent.runCommand("/team verifies " + (page)) : null)
                ))
                .append(space())
                .append(clickable(translatable("cwcore.next")
                        .color(next ? CLICKABLE_COLOR : INACTIVE_COLOR)
                        .clickEvent(next ? ClickEvent.runCommand("/team verifies " + (page + 2)) : null)
                ));
    }

    public Component playersLimitIncreased(CoreTeam team) {
        return translatable("cwcore.team.invite.accept.limit.increased")
                .args(teamMention(team))
                .color(FAIL_COLOR);
    }

    public Component boostAbout(CoreBoost boost) {
        return empty()
                .append(translatable("cwcore.boost.info.boost")
                        .args(text(boost.getId()).color(MENTION_COLOR))
                        .color(INFORMATION_COLOR)
                )
                .append(space())
                .append(clickable(translatable("cwcore.boost.info.extend")
                        .clickEvent(ClickEvent.runCommand("/boost activate " + boost.getId()))
                ))
                .append(newline())
                .append(translatable("cwcore.boost.info.boost.before")
                        .args(text(DATE_FORMAT.format(new Date(boost.getEnd()))))
                        .color(INFORMATION_COLOR)
                )
                .append(newline())
                .append(translatable("cwcore.boost.info.boost.target")
                        .args(Optional.ofNullable(boost.getTeam())
                                .map(CoreMessage::teamMention)
                                .orElseGet(() -> translatable("cwcore.boost.info.boost.target.none")
                                        .color(MENTION_COLOR)
                                )
                        )
                        .color(INFORMATION_COLOR)
                )
                .append(newline())
                .append(translatable("cwcore.boost.info.use")
                        .args(empty()
                                .append(clickable(translatable("cwcore.boost.info.use.team")
                                        .clickEvent(ClickEvent.suggestCommand("/boost use %s team ".formatted(boost.getId())))
                                ))
                                .append(space())
                                .append(clickable(translatable("cwcore.boost.info.use.clear")
                                        .clickEvent(ClickEvent.runCommand("/boost use %s clear".formatted(boost.getId())))
                                ))
                        )
                );
    }

    public Component boostMenu(CorePlayer player, int page) {
        if (page < 0) throw new IllegalArgumentException("page is negative");
        List<CoreBoost> boosts = player.getBoosts();
        int totalPages = boosts.size() / 5 + (boosts.size() % 5 == 0 ? 0 : 1);
        boolean next = page + 1 < totalPages;
        boolean previous = page != 0;
        return empty()
                .append(translatable("cwcore.boost.info.header")
                        .args(
                                playerMention(player),
                                text(page + 1).color(MENTION_COLOR),
                                text(totalPages).color(MENTION_COLOR)
                        )
                        .color(INFORMATION_COLOR)
                )
                .append(newline())
                .append(translatable("cwcore.boost.info.inactive")
                        .args(text(player.getInactiveBoosts())
                                .color(MENTION_COLOR)
                        )
                        .color(INFORMATION_COLOR)
                )
                .append(space())
                .append(clickable(translatable("cwcore.boost.info.activate")
                        .clickEvent(ClickEvent.runCommand("/boost activate"))
                ))
                .append(newline())
                .append(join(
                        JoinConfiguration.newlines(),
                        boosts
                                .stream()
                                .skip(page * 5L)
                                .map(boost -> listElement(boostAbout(boost)))
                                .toList()
                ))
                .append(newline())
                .append(clickable(translatable("cwcore.previous")
                        .color(previous ? CLICKABLE_COLOR : INACTIVE_COLOR)
                        .clickEvent(previous ?
                                ClickEvent.runCommand("/boost info " + (page)) : null)
                ))
                .append(space())
                .append(clickable(translatable("cwcore.next")
                        .color(next ? CLICKABLE_COLOR : INACTIVE_COLOR)
                        .clickEvent(next ?
                                ClickEvent.runCommand("/boost info " + (page + 2)) : null)
                ));
    }

    public Component addedOneBoost(CorePlayer player) {
        return translatable("cwcore.boost.add.success")
                .args(playerMention(player))
                .color(SUCCESS_COLOR);
    }

    public Component boostActivateConfirm(String command) {
        return clickable(translatable("cwcore.boost.activate.confirm")
                .clickEvent(ClickEvent.runCommand(command))
        );
    }

    public Component boostActivateNoBoosts() {
        return translatable("cwcore.boost.activate.boosts.none")
                .color(INFORMATION_COLOR);
    }

    public Component boostActivateOwningFalse() {
        return translatable("cwcore.boost.activate.owning.false")
                .color(FAIL_COLOR);
    }

    public Component boostNotEdit() {
        return translatable("cwcore.boost.edit.not")
                .color(FAIL_COLOR);
    }

}
