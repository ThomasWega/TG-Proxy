package net.trustgames.proxy.player.vanish;

import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.database.player.vanish.PlayerVanishFetcher;

import java.util.UUID;

public class PlayerVanishCommand {
    private final VelocityCommandManager<CommandSource> commandManager;
    private final PlayerVanishFetcher vanishFetcher;

    public PlayerVanishCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.vanishFetcher = new PlayerVanishFetcher(proxy.getToolkit());
        register();
    }

    private void register() {
        Command.Builder<CommandSource> rootCommand = commandManager.commandBuilder("vanish",
                SimpleCommandMeta.simple().with(CommandMeta.DESCRIPTION,
                        "Makes you or the target invisible."
                ).build()
        );

        commandManager.command(rootCommand
                .senderType(Player.class)
                .handler(context -> {
                    Player player = ((Player) context.getSender());
                    UUID uuid = player.getUniqueId();
                    boolean isVanished = vanishFetcher.isVanished(uuid);
                    if (isVanished) {
                        player.sendMessage(Component.text("REMOVED VANISH"));
                        vanishFetcher.removeVanish(uuid);
                    } else {
                        vanishFetcher.setVanish(uuid);
                        player.sendMessage(Component.text("SET VANISH"));
                    }
                })
        );
    }
}
