package net.trustgames.proxy.player.data;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.cache.RedisCache;
import net.trustgames.toolkit.database.player.data.uuid.PlayerUUIDFetcher;

import java.time.Duration;
import java.util.UUID;

public class PlayerDataHandler {

    private final Proxy proxy;
    private final ProxyServer server;
    private final PlayerUUIDFetcher uuidFetcher;
    private final RedisCache redisCache;

    public PlayerDataHandler(Proxy proxy) {
        this.proxy = proxy;
        this.server = proxy.getServer();
        Toolkit toolkit = proxy.getToolkit();
        this.uuidFetcher = new PlayerUUIDFetcher(toolkit);
        this.redisCache = new RedisCache(toolkit);
    }

    @Subscribe
    private void onPlayerJoin(LoginEvent event) {
        Player player = event.getPlayer();
        // TODO rather replace
        uuidFetcher.setIfNotExists(player.getUsername(), player.getUniqueId());
    }

    @Subscribe
    private void onPlayerLeave(DisconnectEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        UUID uuid = player.getUniqueId();
        server.getScheduler().buildTask(proxy, () -> {
            redisCache.removeKey(uuid.toString());
            redisCache.removeKey(playerName);
        }).delay(Duration.ofSeconds(2L)).schedule();
    }
}
