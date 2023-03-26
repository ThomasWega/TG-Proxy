package net.trustgames.proxy.cache;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;

import java.time.Duration;
import java.util.UUID;

public class CacheHandler {

    private final Proxy proxy;
    private final ProxyServer server;
    private final RedisCache redisCache;

    public CacheHandler(Proxy proxy) {
        this.proxy = proxy;
        this.server = proxy.getServer();
        this.redisCache = new RedisCache(proxy);
    }

    @Subscribe
    private void onPlayerLeave(DisconnectEvent event){
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        // TODO take the uuid from the cache
        UUID uuid = player.getUniqueId();
        server.getScheduler().buildTask(proxy, () -> {
            redisCache.remove(uuid.toString());
            redisCache.remove(playerName);
        }).delay(Duration.ofSeconds(3L)).schedule();
    }
}
