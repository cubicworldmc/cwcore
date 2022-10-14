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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import space.cubicworld.core.CoreStatic;
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

    public Component simpleLimitedList(List<? extends Component> components, long baseCount, long limit) {
        Component result = join(JoinConfiguration.commas(true), components);
        return baseCount > limit ?
                result.append(text("...").color(INFORMATION_COLOR)) : result;
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

    public TextColor mentionColor(TextColor color) {
        return orDefault(color, MENTION_COLOR);
    }

    public TextColor orDefault(TextColor color, TextColor defaultColor) {
        return color == null ? defaultColor : color;
    }

    public Component playerMention(CoreLightPlayer player) {
        return playerMention(player, MENTION_COLOR);
    }

    public Component playerMention(CoreLightPlayer player, TextColor defaultColor) {
        return text(player.getName())
                .hoverEvent(HoverEvent.showEntity(
                        Key.key("minecraft", "player"),
                        player.getId()
                ))
                .clickEvent(ClickEvent.runCommand("/tell %s".formatted(player.getName())))
                .color(orDefault(player.getResolvedGlobalColor(), defaultColor));
    }

    public Mono<? extends Component> playerReputation(CorePlayer player) {
        return player.asLight()
                .map(lightPlayer -> translatable("cwcore.reputation.see")
                        .args(
                                playerMention(lightPlayer),
                                text(Integer.toString(player.getReputation())).color(MENTION_COLOR)
                        )
                        .color(INFORMATION_COLOR)
                );
    }

    public Mono<? extends Component> teamMention(CoreTeam team) {
        return team.getOwner()
                .flatMap(CorePlayer::asLight)
                .map(lightPlayer -> text(team.getName())
                        .hoverEvent(HoverEvent.showText(empty()
                                .append(team.getDescription() == null ?
                                        empty() :
                                        text(team.getDescription()).append(newline())
                                )
                                .append(translatable("cwcore.team.about.owner")
                                        .args(playerMention(lightPlayer))
                                )
                        ))
                        .color(MENTION_COLOR)
                );
    }

    public Component teamAlreadyExist(String teamName) {
        return translatable("cwcore.team.already.exist")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Mono<? extends Component> alreadyInTeam(CorePlayer player, CoreTeam team) {
        return player.asLight()
                .flatMap(lightPlayer -> teamMention(team)
                        .map(teamMentionComponent -> translatable("cwcore.team.already.in")
                                .args(playerMention(lightPlayer), teamMentionComponent)
                                .color(FAIL_COLOR)
                        )
                );
    }

    public Mono<? extends Component> teamCreated(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.created")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Mono<? extends Component> teamInvitedAlready(CorePlayer invited, CoreTeam team) {
        return invited.asLight()
                .flatMap(lightInvited -> teamMention(team)
                        .map(teamMentionComponent -> translatable("cwcore.team.already.invited")
                                .args(playerMention(lightInvited), teamMentionComponent)
                                .color(FAIL_COLOR)
                        )
                );
    }

    public Mono<? extends Component> teamInvitationSend(CorePlayer invited, CoreTeam team) {
        return invited.asLight()
                .flatMap(lightInvited -> teamMention(team)
                        .map(teamMentionComponent -> translatable("cwcore.team.invited")
                                .args(playerMention(lightInvited), teamMentionComponent)
                                .color(SUCCESS_COLOR)
                        )
                );
    }

    public Mono<? extends Component> teamInvite(CorePlayer inviter, CoreTeam team) {
        return inviter.asLight()
                .flatMap(lightInviter -> teamMention(team)
                        .map(teamMentionComponent -> empty()
                                .append(translatable("cwcore.team.invite")
                                        .args(playerMention(lightInviter), teamMentionComponent)
                                        .color(INFORMATION_COLOR)
                                )
                                .append(space())
                                .append(clickable(translatable("cwcore.join")
                                                .clickEvent(ClickEvent.runCommand("/team join " + team.getName()))
                                        )
                                )
                        )
                );
    }

    public Mono<? extends Component> notInvited(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.invited.not")
                        .args(teamMentionComponent)
                        .color(FAIL_COLOR)
                );
    }

    public Mono<? extends Component> inviteAccepted(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.invite.accept")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Mono<? extends Component> oneTeamNotVerified(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.verified.not")
                        .args(teamMentionComponent)
                        .color(FAIL_COLOR)
                );
    }

    public Mono<? extends Component> teamAlreadyVerified(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.verified.already")
                        .args(teamMentionComponent)
                        .color(FAIL_COLOR)
                );
    }

    public Mono<? extends Component> teamVerifiedSet(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.verified.set")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    @SneakyThrows
    public Mono<? extends Component> teamAbout(CoreTeam team, boolean forMember) {
        return teamMention(team)
                .map(teamMentionComponent -> empty()
                        .append(translatable("cwcore.team.about.name")
                                .args(teamMentionComponent.append(
                                        team.isVerified() ? text(" ✔") : empty()
                                ))
                        )
                        .append(newline())
                        .append(team.getDescription() == null ?
                                empty() :
                                translatable("cwcore.team.about.description")
                                        .args(text(team.getDescription()))
                                        .append(newline())
                        )
                )
                .flatMap(result -> team.getOwner()
                        .flatMap(CorePlayer::asLight)
                        .map(lightOwner -> result
                                .append(translatable("cwcore.team.about.owner")
                                        .args(playerMention(lightOwner))
                                )
                                .append(newline())
                        )
                )
                .flatMap(result -> (forMember || !team.isHide() ?
                                team.getRelations(CorePTRelation.Value.MEMBERSHIP, 11)
                                        .flatMap(CorePlayer::asLight)
                                        .map(CoreMessage::playerMention)
                                        .collectList()
                                        .map(list -> simpleLimitedList(list.stream().limit(10).toList(), list.size(), 10)) :
                                Mono.just(translatable("cwcore.team.about.hide"))
                        ).flatMap(membersMessage -> team.getRelationsCount(CorePTRelation.Value.MEMBERSHIP)
                                .flatMap(membersCount -> team.getMaxMembers()
                                        .map(maxMembers -> result.append(
                                                translatable("cwcore.team.about.members")
                                                        .args(
                                                                membersMessage,
                                                                text(membersCount).color(MENTION_COLOR),
                                                                text(maxMembers).color(MENTION_COLOR)
                                                        )
                                        ))
                                )
                        )
                );
    }

    public Component teamLeaveCanNot(String teamName) {
        return translatable("cwcore.team.leave.not")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Mono<? extends Component> teamLeaved(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.leave")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Component teamDeleteCanNot(String teamName) {
        return translatable("cwcore.team.delete.not")
                .args(text(teamName).color(MENTION_COLOR))
                .color(FAIL_COLOR);
    }

    public Mono<? extends Component> teamDeleteConfirm(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> empty()
                        .append(translatable("cwcore.team.delete.confirm")
                                .args(teamMentionComponent)
                                .color(INFORMATION_COLOR)
                        )
                        .append(space())
                        .append(confirm()
                                .clickEvent(ClickEvent
                                        .runCommand("/team delete %s confirm".formatted(team.getName()))
                                )
                        )
                        .decorate(TextDecoration.BOLD)
                );
    }

    public Mono<? extends Component> teamDeleted(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.delete")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Component teamInviteJoinNotification(long count) {
        return translatable("cwcore.team.invite.join.notification")
                .args(
                        text(count).color(MENTION_COLOR),
                        clickable(text("/team invites"))
                                .clickEvent(ClickEvent.runCommand("/team invites"))
                )
                .color(INFORMATION_COLOR);
    }

    public Mono<? extends Component> teamInvitesPage(CorePlayer player, int page) {
        if (page < -1) throw new IllegalArgumentException("Page is negative");
        return player.getRelations(CorePTRelation.Value.INVITE, CoreStatic.INVITES_PAGE_SIZE, page * CoreStatic.INVITES_PAGE_SIZE)
                .flatMap(team -> teamMention(team)
                        .map(teamMentionComponent -> listElement(
                                        empty()
                                                .append(teamMentionComponent)
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
                )
                .collectList()
                .flatMap(teams -> teams.isEmpty() ?
                        Mono.just(translatable("cwcore.team.invites.nothing").color(FAIL_COLOR)) :
                        player.getRelationsCount(CorePTRelation.Value.INVITE).map(invites -> {
                            long totalPages = invites / CoreStatic.INVITES_PAGE_SIZE + (invites % CoreStatic.INVITES_PAGE_SIZE == 0 ? 0 : 1);
                            boolean previous = page != 0;
                            boolean next = page + 1 != totalPages;
                            return empty()
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
                        })
                );
    }

    public Mono<? extends Component> teamNotMember(CorePlayer player, CoreTeam team) {
        return player.asLight()
                .flatMap(lightPlayer -> teamMention(team)
                        .map(teamMentionComponent -> translatable("cwcore.team.member.not")
                                .args(playerMention(lightPlayer), teamMentionComponent)
                                .color(FAIL_COLOR)
                        )
                );
    }

    public Mono<? extends Component> kickSuccess(CorePlayer player, CoreTeam team) {
        return player.asLight()
                .flatMap(lightPlayer -> teamMention(team)
                        .map(teamMentionComponent -> translatable("cwcore.team.kick.success")
                                .args(playerMention(lightPlayer), teamMentionComponent)
                                .color(SUCCESS_COLOR)
                        )
                );
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
                                .color(orDefault(sender.getResolvedGlobalColor(), INACTIVE_COLOR))
                        )
                        .append(space())
                )
                .append(message);
    }

    public Mono<? extends Component> teamMessage(CoreTeam team, CorePlayer sender, String message) {
        return sender.asLight()
                .flatMap(lightSender -> teamMention(team)
                        .map(teamMentionComponent -> empty()
                                .append(teamMentionComponent
                                        .append(text(":"))
                                        .append(space())
                                )
                                .append(message(lightSender, text(message)))
                        )
                );
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

    public Mono<? extends Component> teamSettingsHideUpdated(CoreTeam team, boolean value) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.settings.value.hide.success." + value)
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Component teamSettingsDescriptionBad(int maxLength) {
        return translatable("cwcore.team.settings.value.description.bad")
                .args(text(maxLength).color(MENTION_COLOR));
    }

    public Mono<? extends Component> teamSettingsDescriptionUpdated(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.settings.value.description.success")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
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

    public Mono<? extends Component> readSuccess(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.read.success")
                        .args(teamMentionComponent)
                );
    }

    public Mono<? extends Component> verifies(CoreDatabase database, int page) {
        if (page < 0) throw new IllegalArgumentException("page is negative");
        return database.fetchTeamsCountByVerified(false)
                .flatMap(notVerifiedTeamsCount -> {
                    long totalPages = notVerifiedTeamsCount / 5 + (notVerifiedTeamsCount % 5 == 0 ? 0 : 1);
                    if (totalPages == 0)
                        return Mono.just(translatable("cwcore.team.verifies.nothing").color(FAIL_COLOR));
                    boolean previous = page != 0;
                    boolean next = totalPages != page + 1;
                    return database.fetchTeamsByVerified(
                                    false,
                                    CoreStatic.VERIFIES_PAGE_SIZE,
                                    CoreStatic.VERIFIES_PAGE_SIZE * page
                            )
                            .flatMap(team -> teamMention(team)
                                    .map(teamMentionComponent -> listElement(teamMentionComponent)
                                            .append(space())
                                            .append(clickable(translatable("cwcore.team.verifies.verify")
                                                    .clickEvent(ClickEvent.runCommand("/team verify " + team.getName()))
                                            ))
                                    )
                            )
                            .collectList()
                            .map(teams -> empty()
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
                                    ))
                            );
                });
    }

    public Mono<? extends Component> playersLimitIncreased(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.invite.accept.limit.increased")
                        .args(teamMentionComponent)
                        .color(FAIL_COLOR)
                );
    }

    public Mono<? extends Component> boostAbout(CoreBoost boost) {
        return boost.getTeam()
                .flatMap(CoreMessage::teamMention)
                .map(component -> (Component) component)
                .defaultIfEmpty(translatable("cwcore.none").color(MENTION_COLOR))
                .map(teamComponent -> empty()
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
                                .args(teamComponent)
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
                        )
                );
    }

    public Mono<? extends Component> boostMenu(CorePlayer player, int page) {
        if (page < 0) throw new IllegalArgumentException("page is negative");
        return player.getBoostsCount()
                .flatMap(boostsCount -> player.asLight()
                        .flatMap(lightPlayer -> player
                                .getBoosts(CoreStatic.BOOSTS_PAGE_SIZE, CoreStatic.BOOSTS_PAGE_SIZE * page)
                                .flatMap(CoreMessage::boostAbout)
                                .collectList()
                                .map(boosts -> {
                                    long totalPages = boostsCount / CoreStatic.BOOSTS_PAGE_SIZE +
                                            (boostsCount % CoreStatic.BOOSTS_PAGE_SIZE == 0 ? 0 : 1);
                                    boolean next = page + 1 < totalPages;
                                    boolean previous = page != 0;
                                    return empty()
                                            .append(translatable("cwcore.boost.info.header")
                                                    .args(
                                                            playerMention(lightPlayer),
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
                                })
                        )
                );
    }

    public Mono<? extends Component> addedOneBoost(CorePlayer player) {
        return player.asLight()
                .map(lightPlayer -> translatable("cwcore.boost.add.success")
                        .args(playerMention(lightPlayer))
                        .color(SUCCESS_COLOR)
                );
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

    public Component joinMessage(CoreLightPlayer player) {
        return empty()
                .append(text(">")
                        .decorate(TextDecoration.BOLD)
                        .color(orDefault(player.getResolvedGlobalColor(), INACTIVE_COLOR))
                )
                .append(space())
                .append(translatable("multiplayer.player.joined")
                        .args(playerMention(player, INACTIVE_COLOR))
                );
    }

    public Component quitMessage(CoreLightPlayer player) {
        return empty()
                .append(text("<")
                        .decorate(TextDecoration.BOLD)
                        .color(orDefault(player.getResolvedGlobalColor(), INACTIVE_COLOR))
                )
                .append(space())
                .append(translatable("multiplayer.player.left")
                        .args(playerMention(player, INACTIVE_COLOR))
                );
    }

    public Mono<? extends Component> profile(CorePlayer player) {
        return player.asLight()
                .flatMap(lightPlayer -> player
                        .getRelationsCount(CorePTRelation.Value.MEMBERSHIP)
                        .flatMap(teamsCount -> player.getRelations(CorePTRelation.Value.MEMBERSHIP, CoreStatic.TOP_SIZE)
                                .flatMap(CoreMessage::teamMention)
                                .collectList()
                                .map(teams -> empty()
                                        .color(INFORMATION_COLOR)
                                        .append(translatable("cwcore.profile.header")
                                                .args(playerMention(lightPlayer))
                                        )
                                        .append(newline())
                                        .append(translatable("cwcore.profile.reputation")
                                                .args(text(player.getReputation()).color(MENTION_COLOR))
                                        )
                                        .append(newline())
                                        .append(translatable("cwcore.profile.teams")
                                                .args(teamsCount == 0 ?
                                                        translatable("cwcore.none").color(MENTION_COLOR) :
                                                        simpleLimitedList(
                                                                teams,
                                                                teamsCount,
                                                                CoreStatic.TOP_SIZE
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    public Mono<? extends Component> selectTeamSuccess(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.select.success")
                        .args(teamMentionComponent)
                        .color(SUCCESS_COLOR)
                );
    }

    public Component selectTeamNeed() {
        return translatable("cwcore.team.select.not")
                .args(clickable(text("/team select")
                        .clickEvent(ClickEvent.suggestCommand("/team select "))
                ))
                .color(FAIL_COLOR);
    }

    public Mono<? extends Component> teamNotMemberSelf(CoreTeam team) {
        return teamMention(team)
                .map(teamMentionComponent -> translatable("cwcore.team.member.not.self")
                        .args(teamMentionComponent)
                        .color(FAIL_COLOR)
                );
    }

    public Component teamMessageEmpty() {
        return translatable("cwcore.team.message.empty")
                .color(FAIL_COLOR);
    }

    public Mono<? extends Component> teamsReputationTop(List<? extends CoreTeam> teams) {
        return Flux.fromIterable(teams)
                .flatMap(team -> team.getReputation()
                        .flatMap(reputation -> teamMention(team)
                                .map(teamMentionComponent -> listElement(
                                                translatable("cwcore.top.reputation.element")
                                                        .args(
                                                                teamMentionComponent,
                                                                text(reputation).color(MENTION_COLOR)
                                                        )
                                        )
                                )
                        )
                )
                .collectList()
                .map(teamsComponent -> empty()
                        .append(translatable("cwcore.top.reputation.teams.header")
                                .color(INFORMATION_COLOR))
                        .append(newline())
                        .append(join(JoinConfiguration.newlines(), teamsComponent))
                );
    }

    public Mono<? extends Component> playersReputationTop(List<? extends CorePlayer> players) {
        return Flux.fromIterable(players)
                .flatMap(player -> player.asLight()
                        .map(lightPlayer -> listElement(
                                translatable("cwcore.top.reputation.element")
                                        .args(
                                                playerMention(lightPlayer),
                                                text(player.getReputation())
                                        )
                        ))
                )
                .collectList()
                .map(playersComponent -> empty()
                        .append(translatable("cwcore.top.reputation.players.header")
                                .color(INFORMATION_COLOR))
                        .append(newline())
                        .append(join(JoinConfiguration.newlines(), playersComponent))
                );
    }

}
