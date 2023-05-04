package net.trustgames.proxy.player.data.handler;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.cache.RedisCache;

import java.util.UUID;

public class PlayerDataRemoveHandler {

    private final RedisCache redisCache;

    public PlayerDataRemoveHandler(Proxy proxy) {
        this.redisCache = new RedisCache(proxy.getToolkit());
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe(order = PostOrder.LAST)
    private void onPlayerLeave(DisconnectEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        UUID uuid = player.getUniqueId();
        redisCache.removeKey(uuid.toString());
        redisCache.removeKey(playerName);
    }
}
