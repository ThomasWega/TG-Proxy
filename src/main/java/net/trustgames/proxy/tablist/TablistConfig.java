package net.trustgames.proxy.tablist;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum TablistConfig {
    TABLIST_HEADER("<color:#ffa600><b>TRUSTGAMES</b><newline><color:#363c3d>MiniGames Playground<newline>"),
    TABLIST_FOOTER("<newline><green>store.trustgames.net");

    @Getter
    private final String value;

    TablistConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getFormatted() {
        return MiniMessage.miniMessage().deserialize(value);
    }
}

