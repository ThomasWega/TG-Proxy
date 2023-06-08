package net.trustgames.proxy.player.vanish;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.config.PermissionConfig;
import net.trustgames.toolkit.database.player.vanish.PlayerVanishFetcher;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class PlayerIsVanishedCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final PlayerVanishFetcher vanishFetcher;

    private final ProxyServer server;

    public PlayerIsVanishedCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.vanishFetcher = new PlayerVanishFetcher(proxy.getToolkit());
        this.server = proxy.getServer();

        Command.Builder<CommandSource> rootCommand = commandManager.commandBuilder("vanished",
                SimpleCommandMeta.simple().with(CommandMeta.DESCRIPTION,
                        "Check whether you or the target is invisible"
                ).build()
        );

        registerSelfCommand(rootCommand);
        registerTargetCommand(rootCommand);
    }

    private void registerSelfCommand(Command.Builder<CommandSource> rootCommand) {
        commandManager.command(rootCommand
                        .senderType(Player.class)
                .permission(PermissionConfig.STAFF.getPermission())
                .handler(context -> {
                    Player player = ((Player) context.getSender());
                    String playerName = player.getUsername();
                    boolean isVanished = vanishFetcher.isVanished(player.getUniqueId());

                    if (!isVanished) {
                        player.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_CHECK_SELF_OFF.getFormatted(player, playerName));
                        return;
                    }

                    Optional<Timestamp> optTimestamp = vanishFetcher.resolveVanishTime(playerName);
                    // time should always be present, however if for some reason it isn't, the current time will be used
                    Timestamp timestamp = optTimestamp.orElse(Timestamp.from(Instant.now()));

                    player.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_CHECK_SELF_ON.getFormattedWithDate(player, playerName, timestamp));
                })
        );
    }

    private void registerTargetCommand(Command.Builder<CommandSource> rootCommand) {
        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        commandManager.command(rootCommand
                .argument(targetArg)
                .permission(PermissionConfig.STAFF.getPermission())
                .handler(context -> {
                    CommandSource sender = context.getSender();
                    String targetName = context.get(targetArg);
                    boolean isTargetVanished = vanishFetcher.isVanished(targetName);

                    if (!isTargetVanished) {
                        sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_CHECK_TARGET_OFF.getFormatted(sender, targetName));
                        return;
                    }

                    Optional<Timestamp> optTimestamp = vanishFetcher.resolveVanishTime(targetName);
                    // time should always be present, however if for some reason it isn't, the current time will be used
                    Timestamp timestamp = optTimestamp.orElse(Timestamp.from(Instant.now()));

                    sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_CHECK_TARGET_ON.getFormattedWithDate(sender, targetName, timestamp));
                })
        );
    }
}
