package net.trustgames.proxy.player.data.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.cache.PlayerDataCache;
import net.trustgames.middleware.cache.UUIDCache;
import net.trustgames.middleware.config.RabbitQueues;
import net.trustgames.middleware.database.player.data.PlayerData;
import net.trustgames.middleware.database.player.data.config.PlayerDataConfig;
import net.trustgames.middleware.database.player.data.config.PlayerDataType;
import net.trustgames.middleware.managers.RabbitManager;
import net.trustgames.proxy.Proxy;
import net.trustgames.proxy.config.ProxyPermissionConfig;
import net.trustgames.proxy.utils.ComponentUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public final class PlayerDataCommand {

    private final Middleware middleware;

    public PlayerDataCommand(Proxy proxy) {
        this.middleware = proxy.getMiddleware();
        ProxyServer server = proxy.getServer();

        CommandManager commandManager = server.getCommandManager();
        BrigadierCommand dataCommand = dataCommand(server);
        CommandMeta dataCommandMeta = commandManager.metaBuilder(dataCommand)
                .plugin(this)
                .aliases("deaths", "games", "playtime", "xp", "level", "gems", "rubies")
                .build();
        commandManager.register(dataCommandMeta, dataCommand);
    }

    private BrigadierCommand dataCommand(final ProxyServer server) {

        // MAIN COMMAND
        LiteralCommandNode<CommandSource> dataNode = LiteralArgumentBuilder.<CommandSource>literal("kills")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    String label = context.getInput().split(" ")[0];
                    PlayerDataType dataType = PlayerDataType.valueOf(label.toUpperCase());

                    self(source, dataType);
                    return Command.SINGLE_SUCCESS;
                }).build();

        // TARGET ARGUMENT
        ArgumentCommandNode<CommandSource, String> targetNode = RequiredArgumentBuilder.<CommandSource, String>argument("target", StringArgumentType.word())
                .suggests((context, builder) -> {
                    server.getAllPlayers().forEach(player -> builder.suggest(player.getUsername(), VelocityBrigadierMessage.tooltip(Component.text("ADD"))));
                    return builder.buildFuture();
                })
                .executes(context -> {
                    CommandSource source = context.getSource();
                    String targetName = context.getArgument("target", String.class);
                    String label = context.getInput().split(" ")[0];
                    PlayerDataType dataType = PlayerDataType.valueOf(label.toUpperCase());

                    target(source, targetName, dataType);
                    return Command.SINGLE_SUCCESS;

                }).build();


        // ACTION ARGUMENT - WITH PERMISSION (incomplete command)
        ArgumentCommandNode<CommandSource, String> actionNode = RequiredArgumentBuilder.<CommandSource, String>argument("action", StringArgumentType.word())
                                .requires(source -> source.hasPermission(ProxyPermissionConfig.STAFF.permission))
                                .suggests(((context, builder) -> {
                                    builder.suggest("add", VelocityBrigadierMessage.tooltip(Component.text("ADD")));
                                    builder.suggest("remove", VelocityBrigadierMessage.tooltip(Component.text("ADD")));
                                    builder.suggest("set", VelocityBrigadierMessage.tooltip(Component.text("ADD")));
                                    return builder.buildFuture();
                                }))
                                .executes((context -> {
                                    CommandSource source = context.getSource();
                                    source.sendMessage(Component.text("ADD (missing value) -- /" + context.getInput() + " <value>"));

                                    return Command.SINGLE_SUCCESS;
                                })).build();

        // VALUE ARGUMENT
        ArgumentCommandNode<CommandSource, Integer> valueNode = RequiredArgumentBuilder.<CommandSource, Integer>
                        argument("value", IntegerArgumentType.integer(0))
                                        .suggests(((context, builder) -> {
                                            int[] values = {0, 1, 5, 10, 25, 50, 100, 500, 1000, 5000, 10000, 25000, 50000, 75000, 100000};
                                            for (int value : values) {
                                                builder.suggest(value, VelocityBrigadierMessage.tooltip(Component.text("ADD")));
                                            }
                                            return builder.buildFuture();
                                        }))
                                        .executes((context -> {
                                            CommandSource source = context.getSource();
                                            String targetName = context.getArgument("target", String.class);
                                            String label = context.getInput().split(" ")[0];
                                            String action = context.getArgument("action", String.class);

                                            PlayerDataType dataType = PlayerDataType.valueOf(label.toUpperCase());
                                            ActionType actionType = ActionType.valueOf(action.toUpperCase());
                                            int value = context.getArgument("value", Integer.class);


                                            modify(source, targetName, dataType, actionType, value);

                                            return Command.SINGLE_SUCCESS;
                                        })).build();

        // connect all of these in one command
        dataNode.addChild(targetNode);
        targetNode.addChild(actionNode);
        actionNode.addChild(valueNode);

        return new BrigadierCommand(dataNode);
    }

    private void self(CommandSource source,
                      PlayerDataType dataType) {
        if (source instanceof ConsoleCommandSource) {
            //  sender.sendMessage(CommandConfig.COMMAND_PLAYER_ONLY.getText());
            System.out.println("1");
            return;
        }
        Player sender = ((Player) source);
        String senderName = sender.getUsername();
        UUIDCache uuidCache = new UUIDCache(middleware, senderName);
        uuidCache.get(uuid -> {
                /*
                 will most likely mean that console executed this command
                 and console can't have uuid
                */
            if (uuid == null) {
                //   sender.sendMessage(CommandConfig.COMMAND_PLAYER_ONLY.getText());
                System.out.println("2");
                return;
            }
            PlayerDataCache dataCache = new PlayerDataCache(middleware, uuid, dataType);
            dataCache.get(data -> {
                //  sender.sendMessage(PlayerDataConfig.GET_PERSONAL.formatMessage(senderName, dataType, String.valueOf(data)));
                System.out.println("3");
            });
        });
    }

    private void target(CommandSource source,
                        String targetName,
                        PlayerDataType dataType) {
        UUIDCache uuidCache = new UUIDCache(middleware, targetName);
        uuidCache.get(targetUuid -> {
            if (targetUuid != null) {
                PlayerDataCache dataCache = new PlayerDataCache(middleware, targetUuid, dataType);
                dataCache.get(data -> {
                    if (data != null) {
                        //    sender.sendMessage(PlayerDataConfig.GET_OTHER.formatMessage(targetName, dataType, data));
                        System.out.println("4");
                    } else {
                        //    sender.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                        System.out.println("5");
                    }
                });
            } else {
                //  sender.sendMessage(CommandConfig.COMMAND_PLAYER_UNKNOWN.addComponent(Component.text(targetName)));
                System.out.println("6");
            }
        });
    }

    private void modify(CommandSource source,
                        String targetName,
                        PlayerDataType dataType,
                        ActionType actionType,
                        int value) {
        UUIDCache uuidCache = new UUIDCache(middleware, targetName);
        uuidCache.get(targetUuid -> {
            if (targetUuid == null) {
             //   source.sendMessage(CommandConfig.COMMAND_PLAYER_UNKNOWN.addComponent(Component.text(targetName)));
                System.out.println("7");
                return;
            }

            String sourceName = (source instanceof Player player) 
                    ? player.getUsername() : "CONSOLE";

            PlayerData playerData = new PlayerData(middleware, targetUuid, dataType);
            switch (actionType) {
                case SET -> {
                    playerData.setData(value);
                    source.sendMessage(PlayerDataConfig.SET_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    handleMessageQueue(source, targetName,
                            PlayerDataConfig.SET_TARGET.formatMessage(
                                    sourceName, dataType, String.valueOf(value)));
                }
                case ADD -> {
                    playerData.addData(value);
                    source.sendMessage(PlayerDataConfig.ADD_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    handleMessageQueue(source, targetName,
                            PlayerDataConfig.ADD_TARGET.formatMessage(
                                    sourceName, dataType, String.valueOf(value)));
                }
                case REMOVE -> {
                    playerData.removeData(value);
                    source.sendMessage(PlayerDataConfig.REMOVE_SENDER.formatMessage(targetName, dataType, String.valueOf(value)));
                    handleMessageQueue(source, targetName,
                            PlayerDataConfig.REMOVE_TARGET.formatMessage(
                                    sourceName, dataType, String.valueOf(value)));
                }
            }
        });
    }

    /**
     * When the RabbitMQ is disabled, the sender will be notified
     * that a message to target won't be sent.
     * If RabbitMQ is enabled, a message to RabbitMQ is sent
     *
     * @param source Sender of the command
     * @param targetName Name of the target
     * @param message Message to send to the target
     */
    private void handleMessageQueue(@NotNull CommandSource source,
                                    @NotNull String targetName,
                                    @NotNull Component message) {
        RabbitManager rabbitManager = middleware.getRabbitManager();
        if (rabbitManager == null) {
          //  source.sendMessage(CommandConfig.COMMAND_MESSAGE_QUEUE_OFF.getText().append(Component.text(" Not sending a message to target player").color(NamedTextColor.DARK_GRAY)));
            System.out.println("8");
        } else {
            JSONObject json = new JSONObject();
            json.put("player", targetName);
            json.put("message", ComponentUtils.toJson(message));
            rabbitManager.send(RabbitQueues.PROXY_PLAYER_MESSAGES.name, json);
        }
    }

    private enum ActionType {
        SET,
        ADD,
        REMOVE
    }
}
