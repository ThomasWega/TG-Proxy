package net.trustgames.proxy.chat.cooldowns;

import com.velocitypowered.api.proxy.Player;
import lombok.Setter;
import net.trustgames.toolkit.config.chat.ChatLimitConfig;
import net.trustgames.toolkit.luckperms.LuckPermsManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PlayerChatCooldown {
    private final double cooldown;
    private final double sameCooldown;
    @Setter
    private long lastMessageTime;
    private String lastMessage;

    /**
     * @param player Player to handle the cooldowns for
     */
    PlayerChatCooldown(Player player) {
        ChatLimitConfig group = getGroup(player);
        this.cooldown = group.getChatLimitSec();
        this.sameCooldown = group.getChatLimitSameSec();
    }

    /**
     * @param player Player to get the group for
     * @return The highest group from the config that the player has permission to
     */
    private ChatLimitConfig getGroup(Player player) {
        // maps the enums to a lowercase name of the enum
        List<String> groups = Arrays.stream(ChatLimitConfig.getSorted())
                .map(Enum::name)
                .map(String::toLowerCase)
                .toList();

        Optional<String> group = LuckPermsManager.getOnlinePlayerGroupFromList(player.getUniqueId(), groups);
        return group.map(s -> ChatLimitConfig.valueOf(s.toUpperCase())).orElse(ChatLimitConfig.DEFAULT);
    }

    /**
     * @param sameMessage If the message is the same as the last time (different cooldown)
     * @return If the player is on cooldown or not
     */
    public boolean isOnCooldown(boolean sameMessage) {
        if (sameMessage) {
            return !(sameCooldown < ((System.currentTimeMillis() - lastMessageTime) / 1000d));
        } else {
            return !(cooldown < ((System.currentTimeMillis() - lastMessageTime) / 1000d));
        }
    }

    /**
     * @param message The message currently sent
     * @return If the current message is the same as the last message
     */
    public boolean isSameMessage(String message) {
        boolean same = false;
        if (lastMessage != null) {
            same = lastMessage.replaceAll("[^\\p{Alnum}]", "")
                    .equalsIgnoreCase(message.replaceAll("[^\\p{Alnum}]", ""));
        }
        lastMessage = message;
        return same;
    }

    /**
     * @param sameMessage If the message is the same as the last time (different cooldown)
     * @return Time to wait till the player can write in chat again
     */
    public double getWaitTime(boolean sameMessage) {
        if (sameMessage)
            return (sameCooldown - ((System.currentTimeMillis() - lastMessageTime) / 1000d));
        else
            return (cooldown - ((System.currentTimeMillis() - lastMessageTime) / 1000d));
    }
}