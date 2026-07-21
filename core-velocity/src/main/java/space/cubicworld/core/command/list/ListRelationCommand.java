package space.cubicworld.core.command.list;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import reactor.core.publisher.Mono;
import space.cubicworld.core.VelocityPlugin;
import space.cubicworld.core.command.AbstractCoreCommand;
import space.cubicworld.core.command.CoreCommandAnnotation;
import space.cubicworld.core.command.VelocityCoreCommandSource;
import space.cubicworld.core.database.CorePLRelation;
import space.cubicworld.core.database.nocache.CorePLRelationImpl;
import space.cubicworld.core.list.CoreListData;
import space.cubicworld.core.message.CoreMessage;

import java.util.*;

@CoreCommandAnnotation(
        name = "relation",
        permission = "cwcore.list.relation.change",
        aliases = "rel",
        admin = true
)
public class ListRelationCommand extends AbstractCoreCommand<VelocityCoreCommandSource> {
    private final VelocityPlugin plugin;
    private final List<String> listsTab;
    private final List<String> valuesTab;

    public ListRelationCommand(VelocityPlugin plugin) {
        this.plugin = plugin;
        listsTab = plugin
                .getCore()
                .getListContainer()
                .getLists()
                .keySet()
                .stream()
                .toList();
        valuesTab = Arrays
                .stream(CorePLRelation.Value.values())
                .map(val -> val.name().toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public void execute(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.provideListName());
            return;
        }
        String listName = args.next();
        CoreListData listData = plugin.getCore().getListContainer().getList(listName);
        if (listData == null) {
            source.sendMessage(CoreMessage.listNotExist(listName));
            return;
        }
        if (!args.hasNext()) {
            source.sendMessage(CoreMessage.providePlayerName());
            return;
        }
        String playerName = args.next();
        if (!args.hasNext()) {
            source.sendMessage(Component.text("Provide relation value"));
            return;
        }
        CorePLRelation.Value relation;
        try {
            relation = CorePLRelation.Value.valueOf(args.next().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            source.sendMessage(Component.text("Relation value is wrong"));
            return;
        }
        plugin.getDatabase()
                .fetchPlayer(playerName)
                .flatMap(player -> plugin.getCore()
                        .getListContainer()
                        .updateRelation(
                                player.getId(),
                                listData.getId(),
                                relation
                        )
                        .then(Mono.<Component>just(Component.text("Success")))
                )
                .defaultIfEmpty(CoreMessage.playerNotExist(playerName))
                .doOnNext(source::sendMessage)
                .doOnError(this.errorLog(plugin.getLogger()))
                .subscribe();
    }

    @Override
    public List<String> tab(VelocityCoreCommandSource source, Iterator<String> args) {
        if (!args.hasNext()) {
            return Collections.emptyList();
        }
        args.next();
        if (!args.hasNext()) {
            return listsTab;
        }
        args.next();
        if (!args.hasNext()) {
            return plugin.commandHelper().playersTab();
        }
        args.next();
        if (!args.hasNext()) {
            return valuesTab;
        }
        return Collections.emptyList();
    }
}
