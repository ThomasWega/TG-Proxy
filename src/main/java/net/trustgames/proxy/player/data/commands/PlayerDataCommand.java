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
import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.cache.PlayerDataCache;
import net.trustgames.middleware.cache.UUIDCache;
import net.trustgames.middleware.config.CommandConfig;
import net.trustgames.middleware.database.player.data.PlayerData;
import net.trustgames.middleware.database.player.data.config.PlayerDataConfig;
import net.trustgames.middleware.database.player.data.config.PlayerDataType;
import net.trustgames.proxy.Proxy;
import net.trustgames.proxy.config.ProxyPermissionConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PlayerDataCommand {

    private final Middleware middleware;
    private final ProxyServer server;
    private final VelocityCommandManager<CommandSource> commandManager;

    public PlayerDataCommand(Proxy proxy) {
        this.middleware = proxy.getMiddleware();
        this.commandManager = proxy.getCommandManager();
        this.server = proxy.getServer();

        // don't include NAME and UUID
        List<PlayerDataType> dataTypesFiltered = Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
                .toList();

        // create a get and admin command for each datatype
        for (PlayerDataType dataType : dataTypesFiltered) {
            getDataCommand(dataType);
            adminDataCommand(dataType);
        }
    }

    /**
     * Command to check the value of each player data (gems, kills, deaths etc.).
     * Can check personal or target's value.
     *
     * @param dataType The type of data to create the command for
     */
    private void getDataCommand(PlayerDataType dataType) {

        // COMMAND
        String label = dataType.name().toLowerCase();
        Command.Builder<CommandSource> command = commandManager.commandBuilder(
                label,
                CommandMeta.simple().with(CommandMeta.DESCRIPTION,
                        "Check the number of " + label + " a player has. " +
                                "If no target is specified, it returns your own balance")
                        .build()
        );

        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asOptional()
                .build();

        // REGISTER
        commandManager.command(command
                .argument(targetArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    Optional<String> targetOpt = context.getOptional("target");
                    targetOpt.ifPresentOrElse(target ->
                                    target(source, target, dataType),
                            () -> self(source, dataType));
                }));
    }

    /**
     * Command for staff to modify the value of each player data (gems, kills, deaths etc.).
     * Console is allowed
     *
     * @param dataType The type of data to create the command for
     */
    public void adminDataCommand(PlayerDataType dataType) {

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

        // REGISTER
        commandManager.command(adminCommand
                .permission(ProxyPermissionConfig.STAFF.permission)
                .argument(targetArg)
                .argument(actionArg)
                .argument(valueArg)
                .flag(CommandFlag.builder("silent")
                        .withDescription(ArgumentDescription.of(
                                "Whether the target player should be notified" +
                                        "of the modifications or not"
                        )))
                .handler((context -> {
                    CommandSource source = context.getSender();
                    String target = context.get("target");
                    ActionType action = context.get("action");
                    int value = context.get("value");
                    boolean silent = context.flags().isPresent("silent");

                    admin(source, target, dataType, action, value, silent);
                }))
        );
    }

    /**
     * Gets the data value for the command sender and prints it for him.
     * Handles if the sender is not player, as console doesn't have any data.
     * Also handles if there is no data (although that shouldn't happen)
     *
     * @param source   Sender of the command
     * @param dataType Type of data to get
     */
    private void self(CommandSource source,
                      PlayerDataType dataType) {
        if (!(source instanceof Player sender)) {
            source.sendMessage(CommandConfig.COMMAND_PLAYER_ONLY.getText());
            return;
        }
        String senderName = sender.getUsername();
        UUIDCache uuidCache = new UUIDCache(middleware, senderName);
        uuidCache.get(uuid -> {
            if (uuid == null) {
                sender.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(senderName)));
                return;
            }
            PlayerDataCache dataCache = new PlayerDataCache(middleware, uuid, dataType);
            dataCache.get(data ->
                    sender.sendMessage(PlayerDataConfig.GET_PERSONAL.formatMessage(senderName, dataType, String.valueOf(data))));
        });
    }

    /**
     * Gets the data value for the supplied target and prints it for the command sender.
     * Handles if there is no data
     *
     * @param source     Sender of the command
     * @param targetName Name of the target player whom data to get
     * @param dataType   Type of data to get
     */
    private void target(CommandSource source,
                        String targetName,
                        PlayerDataType dataType) {
        UUIDCache uuidCache = new UUIDCache(middleware, targetName);
        uuidCache.get(targetUuid -> {
            if (targetUuid != null) {
                PlayerDataCache dataCache = new PlayerDataCache(middleware, targetUuid, dataType);
                dataCache.get(data -> {
                    if (data != null) {
                        source.sendMessage(PlayerDataConfig.GET_OTHER.formatMessage(targetName, dataType, data));
                    } else {
                        source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                    }
                });
            } else {
                source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
            }
        });
    }

    /**
     * Modify the data value for the supplied target and prints it for the command sender.
     * Also sends a message to the target (if online), that his data was modified.
     * If the data is modified from console, the source will be "CONSOLE"
     * Handles if there is no data yet for the player (unknown player).
     *
     * @param source Sender of the command
     * @param targetName Name of the player whom data to modify
     * @param dataType Type of the data to modify
     * @param actionType Which action to do with the data (remove, set, ...)
     * @param value Which value to modify it with
     */
    private void admin(CommandSource source,
                       String targetName,
                       PlayerDataType dataType,
                       ActionType actionType,
                       int value,
                       boolean silent) {
        UUIDCache uuidCache = new UUIDCache(middleware, targetName);
        uuidCache.get(targetUuid -> {
            if (targetUuid == null) {
                source.sendMessage(CommandConfig.COMMAND_PLAYER_UNKNOWN.addComponent(Component.text(targetName)));
                return;
            }

            String sourceName = (source instanceof Player player)
                    ? player.getUsername() : "CONSOLE";

            PlayerData playerData = new PlayerData(middleware, targetUuid, dataType);
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
