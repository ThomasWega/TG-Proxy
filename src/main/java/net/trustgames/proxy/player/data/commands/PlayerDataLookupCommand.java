package net.trustgames.proxy.player.data.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.trustgames.proxy.Proxy;
import net.trustgames.proxy.player.data.commands.config.PlayerDataCommandsMessagesConfig;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.config.CommandConfig;
import net.trustgames.toolkit.database.player.data.PlayerData;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class PlayerDataLookupCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final Toolkit toolkit;
    private final ProxyServer server;

    public PlayerDataLookupCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.toolkit = proxy.getToolkit();
        this.server = proxy.getServer();

        // don't include NAME and UUID
        Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
                .forEach(dataType -> {

            // MAIN COMMAND
            String label = dataType.name().toLowerCase();
            Command.Builder<CommandSource> command = commandManager.commandBuilder(
                    label,
                    CommandMeta.simple().with(CommandMeta.DESCRIPTION,
                                    "Check the amount of " + label + " a player has. " +
                                            "If no target is specified, it returns your own balance")
                            .build()
            );
            registerSelfCommand(command, dataType);
            registerTargetCommand(command, dataType);
        });
    }

    /**
     * Gets the data value for the command sender and prints it for him.
     * Handles if the sender is not player, as console doesn't have any data.
     *
     * @param dataType Type of data to get
     */
    private void registerSelfCommand(@NotNull Command.Builder<CommandSource> command,
                                     @NotNull PlayerDataType dataType) {

        // REGISTER
        commandManager.command(command
                .senderType(Player.class)
                .handler(context -> {
                    Player player = ((Player) context.getSender());

                    PlayerDataType finalType = dataType;
                    if (dataType == PlayerDataType.LEVEL){
                        finalType = PlayerDataType.XP;
                    }
                    new PlayerData(toolkit, player.getUniqueId(), finalType).getData(optData -> {
                        if (optData.isEmpty()) {
                            player.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(player.getUsername())));
                            return;
                        }

                        player.sendMessage(Objects.requireNonNull(PlayerDataCommandsMessagesConfig.Personal.getByDataType(dataType)).formatMessage(optData.get()));
                    });
                }));
    }

    /**
     * Gets the data value for the supplied target and prints it for the command sender.
     * Handles if there is no data
     *
     * @param dataType Type of data to get
     */
    private void registerTargetCommand(@NotNull Command.Builder<CommandSource> command,
                                       @NotNull PlayerDataType dataType) {

        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        // REGISTER
        commandManager.command(command
                .argument(targetArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    String targetName = context.get(targetArg);

                    UUIDCache uuidCache = new UUIDCache(toolkit, targetName);
                    uuidCache.get(targetUuid -> {
                        if (targetUuid.isEmpty()) {
                            source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                            return;
                        }
                        PlayerDataType finalType = dataType;
                        if (dataType == PlayerDataType.LEVEL){
                            finalType = PlayerDataType.XP;
                        }
                        new PlayerData(toolkit, targetUuid.get(), finalType).getData(optData -> {
                            if (optData.isEmpty()) {
                                source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                                return;
                            }

                            source.sendMessage(Objects.requireNonNull(PlayerDataCommandsMessagesConfig.Target.getByDataType(dataType)).formatMessage(targetName, optData.get()));
                        });
                    });
                }));
    }
}
