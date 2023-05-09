package net.trustgames.proxy.player.data.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.cache.PlayerDataCache;
import redis.clients.jedis.JedisPool;

public class PlayerDataExpiryHandler {

    private final JedisPool jedisPool;

    public PlayerDataExpiryHandler(Proxy proxy) {
        this.jedisPool = proxy.getToolkit().getJedisPool();
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe
    private void onPlayerQuit(DisconnectEvent event){
        Player player = event.getPlayer();
        new PlayerDataCache(jedisPool).expire(player.getUniqueId(), player.getUsername());
    }
}
