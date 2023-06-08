package net.trustgames.proxy.player.vanish;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public enum PlayerVanishCommandsMessagesConfig {
    PREFIX("<color:#26c4c9>Vanish | </color>"),
    VANISH_SELF_ON(PREFIX.value + "<dark_gray>From now on, players won't be able to see you"),
    VANISH_SELF_OFF(PREFIX.value + "<dark_gray>From now on, player will be able to see you again"),
    VANISH_TARGET_SENDER_ON(PREFIX.value + "<dark_gray>You've enabled vanish for <white><target_name><dark_gray>. From now on, players won't be able to see him"),
    VANISH_TARGET_SENDER_OFF(PREFIX.value + "<dark_gray>You've disabled vanish for <white><target_name><dark_gray>. From now on, players will be able to see him again"),
    VANISH_TARGET_ON(PREFIX.value + "<dark_gray>You've been enabled vanish by <white><sender_name><dark_gray>. From now on, players won't be able to see you"),
    VANISH_TARGET_OFF(PREFIX.value + "<dark_gray>You've been disabled vanish by <white><sender_name><dark_gray>. From now on, players will be able to see you again"),
    VANISH_CHECK_SELF_ON(PREFIX.value + "<dark_gray>You have been vanished since <color:#05edf5><date>"),
    VANISH_CHECK_SELF_OFF(PREFIX.value + "<dark_gray>You are currently not vanished"),
    VANISH_CHECK_TARGET_ON(PREFIX.value + "<white><target_name><dark_gray> has been vanished since <color:#05edf5><date>"),
    VANISH_CHECK_TARGET_OFF(PREFIX.value + "<white><target_name><dark_gray> is not vanished");

    private final String value;

    PlayerVanishCommandsMessagesConfig(String value) {
        this.value = value;
    }

    public Component getFormatted(@NotNull CommandSource source,
                                  @NotNull String targetName) {
        return MiniMessage.miniMessage().deserialize(
                value, TagResolver.builder()
                        .resolver(Placeholder.parsed("target_name", targetName))
                        .resolver(Placeholder.parsed("sender_name",
                                (source instanceof Player player) ? player.getUsername() : "CONSOLE"))
                        .build()
        );
    }

    public Component getFormattedWithDate(@NotNull CommandSource source,
                                          @NotNull String targetName,
                                          @NotNull Timestamp timestamp) {
        return MiniMessage.miniMessage().deserialize(
                value, TagResolver.builder()
                        .resolver(Placeholder.parsed("target_name", targetName))
                        .resolver(Placeholder.parsed("sender_name",
                                (source instanceof Player player) ? player.getUsername() : "CONSOLE"))
                        .resolver(Placeholder.parsed("date", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a")
                                .format(Date.from(timestamp.toInstant()))))
                        .build()
        );
    }
}
