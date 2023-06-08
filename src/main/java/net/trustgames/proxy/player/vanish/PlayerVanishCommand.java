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
import net.trustgames.toolkit.database.player.vanish.PlayerVanishFetcher;

import java.util.UUID;

public class PlayerVanishCommand {
    private final VelocityCommandManager<CommandSource> commandManager;
    private final PlayerVanishFetcher vanishFetcher;
    private final ProxyServer server;

    public PlayerVanishCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.vanishFetcher = new PlayerVanishFetcher(proxy.getToolkit());
        this.server = proxy.getServer();

        Command.Builder<CommandSource> rootCommand = commandManager.commandBuilder("vanish",
                SimpleCommandMeta.simple().with(CommandMeta.DESCRIPTION,
                        "Makes you or the target invisible to players."
                ).build()
        );

        registerSelfCommand(rootCommand);
        registerTargetCommand(rootCommand);
    }

    private void registerSelfCommand(Command.Builder<CommandSource> rootCommand) {
        commandManager.command(rootCommand
                .senderType(Player.class)
                .handler(context -> {
                    Player player = ((Player) context.getSender());
                    String playerName = player.getUsername();
                    UUID uuid = player.getUniqueId();
                    boolean isVanished = vanishFetcher.isVanished(uuid);
                    if (isVanished) {
                        player.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_SELF_OFF.getFormatted(player, playerName));
                        vanishFetcher.removeVanish(uuid);
                    } else {
                        player.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_SELF_ON.getFormatted(player, playerName));
                        vanishFetcher.setVanish(uuid);
                    }
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
                .handler(context -> {
                    CommandSource sender = context.getSender();
                    String senderName = (sender instanceof Player player) ? player.getUsername() : "CONSOLE";
                    String targetName = context.get(targetArg);

                    boolean isVanished = vanishFetcher.isVanished(targetName);
                    if (isVanished) {
                        vanishFetcher.removeVanish(targetName);

                        // if the target is the sender himself
                        if (senderName.equalsIgnoreCase(targetName)) {
                            sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_SELF_OFF.getFormatted(sender, senderName));
                            return;
                        }
                        sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_TARGET_SENDER_OFF.getFormatted(sender, targetName));

                        server.getPlayer(targetName).ifPresent(target ->
                                target.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_TARGET_OFF.getFormatted(sender, senderName))
                        );
                    } else {
                        vanishFetcher.setVanish(targetName);

                        // if the target is the sender himself
                        if (senderName.equalsIgnoreCase(targetName)) {
                            sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_SELF_ON.getFormatted(sender, senderName));
                            return;
                        }

                        sender.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_TARGET_SENDER_ON.getFormatted(sender, targetName));
                        server.getPlayer(targetName).ifPresent(target ->
                                target.sendMessage(PlayerVanishCommandsMessagesConfig.VANISH_TARGET_ON.getFormatted(sender, senderName))
                        );
                    }
                })
        );
    }
}
