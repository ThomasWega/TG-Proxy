package net.trustgames.proxy.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum TablistConfig {
    TABLIST_HEADER("&#ffa600&lTRUSTGAMES\n&#363c3dMiniGames Playground\n"),
    TABLIST_FOOTER("\n&astore.trustgames.net");

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

