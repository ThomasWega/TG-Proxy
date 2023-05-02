package net.trustgames.proxy.chat.commands;

import cloud.commandframework.Command;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;

/**
 * Just string or list of strings from a config
 * sent to the player on a given command
 */
public final class TextCommands {

    private final VelocityCommandManager<CommandSource> commandManager;

    /**
     * Goes through the list of all specified commands in the config and
     * registers each one
     */
    public TextCommands(VelocityCommandManager<CommandSource> commandManager) {
        this.commandManager = commandManager;

        for (TextCommandsList command : TextCommandsList.values()) {
            register(command);
        }
    }

    /**
     * Registers a new command with the enum's lowercase name.
     * In handler, it gets the message from the enum.
     *
     * @param command Configurable TextCommand Enum with message
     */
    public void register(TextCommandsList command) {

        Command.Builder<CommandSource> textCommand = commandManager.commandBuilder(command.name().toLowerCase());

        commandManager.command(textCommand
                .handler(context -> {
                    CommandSource source = context.getSender();
                    source.sendMessage(command.getMessage());
                })
        );
    }
}
