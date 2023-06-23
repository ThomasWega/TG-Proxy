package net.trustgames.proxy.chat.staff_chat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.trustgames.toolkit.luckperms.LuckPermsManager;

import java.util.Optional;

public enum StaffChatMessagesConfig {
    MESSAGE("<source_prefix_spaced><color:#ffa6f9><source></color> <color:#7a5078>(<source_server>)</color> <white><message>");

    private final String message;

    StaffChatMessagesConfig(String message) {
        this.message = message;
    }

    public Component getFormatted(CommandSource source, String msg) {
        return MiniMessage.miniMessage().deserialize(
                this.message,
                TagResolver.builder()
                        .resolver(Placeholder.unparsed("source_prefix_spaced", (source instanceof Player player)
                                ? Optional.ofNullable(LuckPermsManager.getOnlinePlayerPrefix(player.getUniqueId()))
                                .map(prefix -> prefix + " ")
                                .orElse("")
                                : "")
                        )
                        .resolver(Placeholder.parsed("source", (source instanceof Player player)
                                ? player.getUsername()
                                : "CONSOLE")
                        )
                        .resolver(Placeholder.parsed("source_server", (source instanceof Player player)
                                ? player.getCurrentServer()
                                .map(serverConnection -> serverConnection.getServerInfo().getName())
                                .orElse("unknown")
                                : "")
                        )
                        .resolver(Placeholder.unparsed("message", msg))
                        .build()
        );
    }
}
