package net.trustgames.proxy.player.data.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.trustgames.middleware.database.player.data.config.PlayerDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PlayerDataCommandTest {

    private final ProxyServer server;
    private final VelocityCommandManager<CommandSource> manager;
    // don't include NAME and UUID
    private final List<PlayerDataType> dataTypesFiltered = Arrays.stream(PlayerDataType.values())
            .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
            .toList();

    public PlayerDataCommandTest(VelocityCommandManager<CommandSource> manager, ProxyServer server) {
        this.manager = manager;
        this.server = server;
        initialize();
    }

    private void initialize() {
    /*    for (PlayerDataType dataType : dataTypesFiltered) {
            command1(dataType);
            command2(dataType);
        }

     */

        command1(PlayerDataType.GEMS);
        command2(PlayerDataType.GEMS);
    }

    private void command1(PlayerDataType dataType) {

        // COMMAND
        Command.Builder<CommandSource> command = manager.commandBuilder(
                dataType.name().toLowerCase() + 1,
                CommandMeta.simple().with(CommandMeta.DESCRIPTION, "ADD2").build()
        );

        // TARGET
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asOptional()
                .build();

        manager.command(command
                .argument(targetArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    Optional<String> targetOpt = context.getOptional("target");
                    targetOpt.ifPresentOrElse(target -> {
                        source.sendMessage(Component.text("target check"));
                    }, () -> {
                        if (source instanceof Player) {
                            source.sendMessage(Component.text("self check"));
                        } else {
                            source.sendMessage(Component.text("Cant check console balance lol"));
                        }
                    });
                }));
    }

    public void command2(PlayerDataType dataType) {

        // COMMAND
        Command.Builder<CommandSource> command = manager.commandBuilder(
                dataType.name().toLowerCase() + 1,
                CommandMeta.simple().with(CommandMeta.DESCRIPTION, "ADD2").build()
        );

        // TARGET
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target2")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        // ACTION
        CommandArgument<CommandSource, ActionType> actionArg = EnumArgument.of(ActionType.class, "action");

        // VALUE
        CommandArgument<CommandSource, Integer> valueArg = IntegerArgument.<CommandSource>builder("value")
                .withMin(0)
                .withSuggestionsProvider(((commandSourceCommandContext, s) ->
                        Arrays.asList("0", "1", "5", "10", "25", "50", "100", "500", "1000",
                                "5000", "10000", "25000", "50000", "75000", "100000")))
                .build();

        manager.command(command
                .argument(targetArg, ArgumentDescription.of("ADD"))
                .argument(actionArg, ArgumentDescription.of("ADD"))
                .argument(valueArg, ArgumentDescription.of("ADD"))
                .handler(context -> {
                    String target = context.get("target2");
                    ActionType action = context.get("action");
                    int value = context.get("value");

                    // TODO
                })
        );
    }

    private enum ActionType {
        SET,
        ADD,
        REMOVE
    }
}
