package net.trustgames.proxy.player.data.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import cloud.commandframework.velocity.arguments.PlayerArgument;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.proxy.player.data.commands.config.PlayerUptimeCommandMessagesConfig;
import net.trustgames.proxy.player.data.handler.PlayerDataPlaytimeHandler;

public class PlayerUptimeCommand {
    private final VelocityCommandManager<CommandSource> commandManager;

    public PlayerUptimeCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();

        Command.Builder<CommandSource> rootCommand = commandManager.commandBuilder("uptime",
                CommandMeta.simple().with(CommandMeta.DESCRIPTION,
                                "Check the time the player has been online for" +
                                        "If no target is specified, it returns your own uptime."
                        )
                        .build()
        );

        registerSelfCommand(rootCommand);
        registerTargetCommand(rootCommand);
    }


    private void registerSelfCommand(Command.Builder<CommandSource> rootCommand){
        commandManager.command(rootCommand
                .senderType(Player.class)
                .handler(context -> {
                    Player source = ((Player) context.getSender());
                    source.sendMessage(PlayerUptimeCommandMessagesConfig.SELF.getFormatted(
                            source.getUsername(),
                            PlayerDataPlaytimeHandler.getCurrentUptimeInMillis(source.getUniqueId())
                    ));
                })
        );
    }

    private void registerTargetCommand(Command.Builder<CommandSource> rootCommand){
        CommandArgument<CommandSource, Player> targetArg = PlayerArgument.of("target");

        commandManager.command(rootCommand
                .senderType(Player.class)
                .argument(targetArg)
                .handler(context -> {
                    Player source = ((Player) context.getSender());
                    Player target = context.get(targetArg);
                    source.sendMessage(PlayerUptimeCommandMessagesConfig.TARGET.getFormatted(
                            target.getUsername(),
                            PlayerDataPlaytimeHandler.getCurrentUptimeInMillis(target.getUniqueId())
                    ));
                })
        );
    }
}
