package net.trustgames.proxy.chat.staff_chat;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.config.PermissionConfig;

public class StaffChatCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final ProxyServer server;

    public StaffChatCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.server = proxy.getServer();
        register();
    }

    private void register() {
        Command.Builder<CommandSource> mainCommand = commandManager.commandBuilder("staffchat",
                SimpleCommandMeta.simple().with(CommandMeta.DESCRIPTION,
                        "Write a message in chat that only staff can see"
                ).build(),
                "sc"
        );

        CommandArgument<CommandSource, String> messageArg = StringArgument.of("message", StringArgument.StringMode.GREEDY);

        commandManager.command(mainCommand
                .permission(PermissionConfig.STAFF.getPermission())
                .argument(messageArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    String message = context.get(messageArg);
                    server.getAllPlayers().stream()
                            .filter(player -> player.hasPermission(PermissionConfig.STAFF.getPermission()))
                            .forEach(player -> player.sendMessage(StaffChatMessagesConfig.MESSAGE.getFormatted(source, message)));
                })
        );
    }
}
