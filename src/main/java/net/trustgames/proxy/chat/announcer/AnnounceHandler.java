package net.trustgames.proxy.chat.announcer;

import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;

import java.time.Duration;

/**
 * Chat messages which are announced to all online players
 * on the server.
 */
public final class AnnounceHandler {

    private final Proxy proxy;
    private final ProxyServer server;

    public AnnounceHandler(Proxy proxy) {
        this.server = proxy.getServer();
        this.proxy = proxy;
        announceMessages();
    }

    /**
     * Announce a set of messages every x seconds for all
     * the online players on the server. The messages can be configured
     * in the announcer.yml config
     */
    public void announceMessages() {
        final AnnouncerMessagesConfig[] msgList = AnnouncerMessagesConfig.values();
        /*
         run every X seconds, every loop, it increases the index by 1, to move to the next message
         if the index is same as the number of messages, go back to the start by setting the index to 0
        */
        final int[] index = {0};

        server.getScheduler().buildTask(proxy, () -> {
            if (index[0] == msgList.length) {
                index[0] = 0;
            }
            server.getAllPlayers().forEach(player -> player.sendMessage(msgList[index[0]].getMessage()));
            index[0]++;
        }).repeat(Duration.ofMinutes(5)).schedule();
    }
}
