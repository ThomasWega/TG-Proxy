package net.trustgames.proxy.cache;

import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCache {

    private static final JedisPool pool = Proxy.getJedisPool();

    private final Proxy proxy;
    private final ProxyServer server;

    public RedisCache(Proxy proxy) {
        this.proxy = proxy;
        this.server = proxy.getServer();
    }

    public void remove(String string) {
        server.getScheduler().buildTask(proxy, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.del(string);
            }
        }).schedule();
    }
}
