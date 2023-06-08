package net.trustgames.proxy.player.vanish;

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
    VANISH_CHECK_SELF_OFF(PREFIX.value + "<dark_gray>You are currently not vanished"),
    VANISH_CHECK_SELF_ON(PREFIX.value + "<dark_gray>You have been vanished since <color:#05edf5><date>"),
    VANISH_CHECK_TARGET_OFF(PREFIX.value + "<white><target_name><dark_gray> is not vanished"),
    VANISH_CHECK_TARGET_ON(PREFIX.value + "<white><target_name><dark_gray> has been vanished since <color:#05edf5><date>");

    private final String value;

    PlayerVanishCommandsMessagesConfig(String value) {
        this.value = value;
    }

    public Component getFormatted(@NotNull String targetName) {
        return MiniMessage.miniMessage().deserialize(
                value, TagResolver.builder()
                        .resolver(Placeholder.parsed("target_name", targetName))
                        .build()
        );
    }

    public Component getFormattedWithDate(@NotNull String targetName,
                                          @NotNull Timestamp timestamp) {
        return MiniMessage.miniMessage().deserialize(
                value, TagResolver.builder()
                        .resolver(Placeholder.parsed("target_name", targetName))
                        .resolver(Placeholder.parsed("date", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a")
                                .format(Date.from(timestamp.toInstant()))))
                        .build()
        );
    }
}
