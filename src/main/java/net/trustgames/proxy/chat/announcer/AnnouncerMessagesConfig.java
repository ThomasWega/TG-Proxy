package net.trustgames.proxy.chat.announcer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum AnnouncerMessagesConfig {
    WEB(
            "<newline>" +
                    "<yellow>1111" +
                    "<newline>" +
                    "<hover:show_text:'<green>CLICK TO OPEN'><gold><bold><click:open_url:'http://www.trustgames.net'>OPEN URL<reset> non CLICK" +
                    "<newline>" +
                    "<yellow>1111" +
                    "<newline>"
    ),
    STORE(
            "<newline>" +
                    "<yellow>2222" +
                    "<newline>" +
                    "<hover:show_text:'<green>CLICK TO OPEN'><gold><bold><click:open_url:'http://store.trustgames.net'>OPEN URL<reset> non CLICK" +
                    "<newline>" +
                    "<yellow>2222" +
                    "<newline>"
    );

    private final String value;

    AnnouncerMessagesConfig(String value) {
        this.value = value;
    }

    /**
     * @return Formatted component message
     */
    public final Component getMessage() {
        return MiniMessage.miniMessage().deserialize(value);
    }
}
