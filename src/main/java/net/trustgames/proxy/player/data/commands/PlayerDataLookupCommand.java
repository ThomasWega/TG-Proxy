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
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.PlayerDataCache;
import net.trustgames.toolkit.cache.UUIDCache;
import net.trustgames.toolkit.config.CommandConfig;
import net.trustgames.toolkit.database.player.data.config.PlayerDataConfig;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class PlayerDataLookupCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final Toolkit toolkit;
    private final ProxyServer server;

    public PlayerDataLookupCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.toolkit = proxy.getToolkit();
        this.server = proxy.getServer();

        // don't include NAME and UUID
        List<PlayerDataType> dataTypesFiltered = Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
                .toList();

        dataTypesFiltered.forEach(dataType -> {

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
                    String senderName = player.getUsername();

                    PlayerDataCache dataCache = new PlayerDataCache(toolkit, player.getUniqueId(), dataType);
                    dataCache.get(optData -> optData.ifPresent(data ->
                            player.sendMessage(PlayerDataConfig.GET_PERSONAL.formatMessage(senderName, dataType, data))));
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
                        PlayerDataCache dataCache = new PlayerDataCache(toolkit, targetUuid.get(), dataType);
                        dataCache.get(data -> {
                            if (data.isEmpty()) {
                                source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                                return;
                            }
                            source.sendMessage(PlayerDataConfig.GET_OTHER.formatMessage(targetName, dataType, data.get()));
                        });
                    });
                }));
    }
}
