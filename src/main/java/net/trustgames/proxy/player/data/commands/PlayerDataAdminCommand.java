package net.trustgames.proxy.player.data.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.config.CommandConfig;
import net.trustgames.toolkit.config.PermissionConfig;
import net.trustgames.toolkit.database.player.data.PlayerData;
import net.trustgames.toolkit.database.player.data.config.PlayerDataConfig;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

import java.util.Arrays;
import java.util.List;

public class PlayerDataAdminCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final ProxyServer server;
    private final Toolkit toolkit;

    public PlayerDataAdminCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.server = proxy.getServer();
        this.toolkit = proxy.getToolkit();

        // don't include NAME and UUID
        List<PlayerDataType> dataTypesFiltered = Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
                .toList();

        dataTypesFiltered.forEach(this::register);
    }

    /**
     * Command for staff to modify the value of each player data (gems, kills, deaths etc.).
     * Console is allowed
     *
     * @param dataType The type of data to create the command for
     */
    public void register(PlayerDataType dataType) {

        // COMMAND
        String label = dataType.name().toLowerCase();
        Command.Builder<CommandSource> adminCommand = commandManager.commandBuilder(
                label + "admin",
                CommandMeta.simple().with(CommandMeta.DESCRIPTION, "Manage the amount of " + label + " a player has." +
                                "Available actions are set/add/remove. " +
                                "Can also use flag \"--silent\" to prevent notifying the player of any changes made.")
                        .build()
        );

        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        // ACTION argument
        CommandArgument<CommandSource, ActionType> actionArg = EnumArgument.of(ActionType.class, "action");

        // VALUE argument
        CommandArgument<CommandSource, Integer> valueArg = IntegerArgument.<CommandSource>builder("value")
                .withMin(0)
                .withSuggestionsProvider(((commandSourceCommandContext, s) ->
                        Arrays.asList("0", "1", "5", "10", "25", "50", "100", "500", "1000",
                                "5000", "10000", "25000", "50000", "75000", "100000")))
                .build();

        // SILENT flag
        CommandFlag<Void> silentFlag = CommandFlag.builder("silent")
                .withDescription(ArgumentDescription.of(
                        "Whether the target player should be notified" +
                                "of the modifications or not"
                ))
                .build();

        // REGISTER
        commandManager.command(adminCommand
                .permission(PermissionConfig.STAFF.getPermission())
                .argument(targetArg)
                .argument(actionArg)
                .argument(valueArg)
                .flag(silentFlag)
                .handler((context -> {
                    CommandSource source = context.getSender();
                    String target = context.get(targetArg);
                    ActionType action = context.get(actionArg);
                    int value = context.get(valueArg);
                    boolean silent = context.flags().isPresent(silentFlag);

                    admin(source, target, dataType, action, value, silent);
                }))
        );
    }

    /**
     * Modify the data value for the supplied target and prints it for the command sender.
     * Also sends a message to the target (if online), that his data was modified.
     * If the data is modified from console, the source will be "CONSOLE"
     * Handles if there is no data yet for the player (unknown player).
     *
     * @param source     Sender of the command
     * @param targetName Name of the player whom data to modify
     * @param dataType   Type of the data to modify
     * @param actionType Which action to do with the data (remove, set, ...)
     * @param value      Which value to modify it with
     */
    private void admin(CommandSource source,
                       String targetName,
                       PlayerDataType dataType,
                       ActionType actionType,
                       int value,
                       boolean silent) {
        UUIDCache uuidCache = new UUIDCache(toolkit, targetName);
        uuidCache.get(targetUuid -> {
            if (targetUuid.isEmpty()) {
                source.sendMessage(CommandConfig.COMMAND_PLAYER_UNKNOWN.addComponent(Component.text(targetName)));
                return;
            }

            String sourceName = (source instanceof Player player)
                    ? player.getUsername() : "CONSOLE";

            PlayerData playerData = new PlayerData(toolkit, targetUuid.get(), dataType);
            switch (actionType) {
                case SET -> {
                    playerData.setData(value);
                    source.sendMessage(PlayerDataConfig.SET_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    if (!silent) {
                        server.getPlayer(targetName).ifPresent(player -> player.sendMessage(
                                PlayerDataConfig.SET_TARGET.formatMessage(
                                        sourceName, dataType, String.valueOf(value))));
                    }
                }
                case ADD -> {
                    playerData.addData(value);
                    source.sendMessage(PlayerDataConfig.ADD_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    if (!silent) {
                        server.getPlayer(targetName).ifPresent(player -> player.sendMessage(
                                PlayerDataConfig.ADD_TARGET.formatMessage(
                                        sourceName, dataType, String.valueOf(value))));
                    }
                }
                case REMOVE -> {
                    playerData.removeData(value);
                    source.sendMessage(PlayerDataConfig.REMOVE_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    if (!silent) {
                        server.getPlayer(targetName).ifPresent(player -> player.sendMessage(
                                PlayerDataConfig.REMOVE_TARGET.formatMessage(
                                        sourceName, dataType, String.valueOf(value))));
                    }
                }
            }
        });
    }

    private enum ActionType {
        SET,
        ADD,
        REMOVE
    }
}
