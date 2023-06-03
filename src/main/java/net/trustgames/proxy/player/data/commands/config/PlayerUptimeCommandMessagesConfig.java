package net.trustgames.proxy.player.data.commands.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public enum PlayerUptimeCommandMessagesConfig {
    PREFIX("<color:#ac75ff>Uptime | </color>"),
    SELF(PREFIX.value + "<dark_gray>Your current uptime is <color:#8330ff><time_with_unit>"),
    TARGET(PREFIX.value + "<dark_gray><target_name>'s current uptime is <color:#8330ff><time_with_unit>");

    private final String value;

    PlayerUptimeCommandMessagesConfig(String value) {
        this.value = value;
    }

    public Component getFormatted(@NotNull String targetName, long uptimeMillis) {
        return MiniMessage.miniMessage().deserialize(
                value, TagResolver.builder()
                        .resolver(Placeholder.parsed("target_name", targetName))
                        .resolver(Placeholder.parsed("time_with_unit", handleTimeUnits(uptimeMillis)))
                        .build()
        );
    }

    private String handleTimeUnits(long uptimeMillis) {
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        float uptimeSec = uptimeMillis / 1000f;
        if (uptimeSec < 60) {
            return decimalFormat.format(uptimeSec) + " seconds";
        }
        if (uptimeSec >= 60 && uptimeSec <= 3600) {
            return decimalFormat.format(uptimeSec / 60f) + " minutes";
        } else {
            return decimalFormat.format(uptimeSec / 3600f) + " hours";
        }
    }
}
