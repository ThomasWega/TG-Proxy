package net.trustgames.proxy.chat.cooldowns;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.config.CommandConfig;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles the command cooldowns
 */
public final class CommandsLimiter {

    public CommandsLimiter(Proxy proxy) {
        proxy.getServer().getEventManager().register(proxy, this);
    }

    private final HashMap<UUID, Long> commandCooldown = new HashMap<>();

    int i = 1;

    /**
     * limit the number of commands player can send, to avoid spamming.
     * it checks, if the player is already in the hashmap, in case he is not,
     * it puts him there. If he is already in the hashmap, it checks if the
     * current time - the time in hashmap is less than a second. If it's less
     * than a second, it checks, if "i" is more than specified value (default is 10).
     * If it's more, that means it's too spammy and the player used a command more times
     * than the maximum allowed number in config. If "i" is less than the value specified
     * in the config.yml, it just adds +1 to "i". If neither of these checks are valid, meaning
     * the player is in the hashmap, but more than a second has passed till last command, he is
     * put again in the hashmap with a new time and the "i" is reset to 0
     *
     * @param event Command preprocess event
     */
    @Subscribe(order = PostOrder.FIRST)
    private void onPlayerPreCommand(CommandExecuteEvent event) {
        CommandSource commandSource = event.getCommandSource();
        if (!(commandSource instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        // if player is in the cooldown hashmap
        if (!commandCooldown.containsKey(uuid)) {
            commandCooldown.put(uuid, System.currentTimeMillis());
        }
        // if the last time of the command is less than a second (1000 milliseconds)
        else if (System.currentTimeMillis() - commandCooldown.get(uuid) < 1000) {
            /*
             if "i" is more than the config value number.
             Meaning the player typed a command in the last second more than the allowed count.
            */
            if (i >= CommandConfig.MAX_PER_SEC.getDouble()) {
                player.sendMessage(CommandConfig.COMMAND_SPAM.getText());
                event.setResult(CommandExecuteEvent.CommandResult.denied());
            }
            // add i + 1 to increase the amount of times the player has typed a command in the last second
            i++;
        }
        // if the last time player typed a command is more than a second.
        else {
            // put him in the cooldown with the new time of last command used
            commandCooldown.put(uuid, System.currentTimeMillis());
            // reset the integer "i" to default value
            i = 1;
        }
    }

    @Subscribe(order = PostOrder.LATE)
    private void onPlayerQuit(DisconnectEvent event) {
        commandCooldown.remove(event.getPlayer().getUniqueId());
    }
}

