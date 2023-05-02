package net.trustgames.proxy.chat.cooldowns;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.trustgames.proxy.utils.ColorUtils;
import net.trustgames.toolkit.config.PermissionConfig;
import net.trustgames.toolkit.config.chat.ChatConfig;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Limit the time player can send the next message. Each permission can have different times set.
 * If the message is the same as the last one, the timeout can be longer.
 */
public final class ChatLimiter {
    private final HashMap<String, PlayerChatCooldown> cooldowns = new HashMap<>();

    @Subscribe(order = PostOrder.FIRST)
    public void limit(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(PermissionConfig.STAFF.permission)) return;
        String playerName = player.getUsername();

        // add the player if not yet contained with new cooldown
        cooldowns.computeIfAbsent(playerName, name -> new PlayerChatCooldown(player));

        PlayerChatCooldown cooldown = cooldowns.get(player.getUsername());
        String playerMessage = ColorUtils.stripColor(Component.text(event.getMessage()));
        boolean same = cooldown.isSameMessage(playerMessage);
        if (cooldown.isOnCooldown(same)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            sendMessage(player, same);
        } else {
            cooldown.setLastMessageTime(System.currentTimeMillis());
        }
    }

    private void sendMessage(@NotNull Player player, boolean sameMessage) {
        PlayerChatCooldown cooldown = cooldowns.get(player.getUsername());

        ChatConfig unformattedMessage = sameMessage ? ChatConfig.ON_SAME_COOLDOWN : ChatConfig.ON_COOLDOWN;
        Component message = unformattedMessage.addComponent(Component.text(
                new DecimalFormat("0.0").format(cooldown.getWaitTime(sameMessage))));

        player.sendMessage(message);
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        cooldowns.remove(event.getPlayer().getUsername());
    }
}
