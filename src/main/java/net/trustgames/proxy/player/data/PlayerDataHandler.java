package net.trustgames.proxy.player.data;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.cache.RedisCache;
import net.trustgames.middleware.config.rabbit.RabbitQueues;
import net.trustgames.middleware.database.player.data.uuid.PlayerUUIDFetcher;
import net.trustgames.middleware.managers.RabbitManager;
import net.trustgames.proxy.Proxy;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public class PlayerDataHandler {

    private final Proxy proxy;
    private final ProxyServer server;
    @Nullable
    private final RabbitManager rabbitManager;
    private final PlayerUUIDFetcher uuidFetcher;
    private final RedisCache redisCache;

    public PlayerDataHandler(Proxy proxy) {
        this.proxy = proxy;
        this.server = proxy.getServer();
        Middleware middleware = proxy.getMiddleware();
        this.rabbitManager = middleware.getRabbitManager();
        this.uuidFetcher = new PlayerUUIDFetcher(middleware);
        this.redisCache = new RedisCache(middleware);
        handleQueueMessages();
    }

    @Subscribe
    private void onPlayerJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        uuidFetcher.setIfNotExists(player.getUsername(), player.getUniqueId());
    }

    @Subscribe
    private void onPlayerLeave(DisconnectEvent event){
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        // TODO take the uuid from the cache (maybe??)
        UUID uuid = player.getUniqueId();
        server.getScheduler().buildTask(proxy, () -> {
            redisCache.remove(uuid.toString());
            redisCache.remove(playerName);
        }).delay(Duration.ofSeconds(2L)).schedule();
    }

    /**
     * Handles the receiving and sending of messages from
     * the message queue
     */
    private void handleQueueMessages() {
        if (rabbitManager == null) return;

        server.getScheduler().buildTask(proxy, () ->
                rabbitManager.onDelivery(RabbitQueues.PROXY_PLAYER_MESSAGES.name, json -> {
                    String playerName = json.getString("player");
                    Component message = GsonComponentSerializer.gson().deserialize(
                            json.getString("message"));
                    server.getPlayer(playerName).ifPresent(player -> player.sendMessage(message));
                })).delay(Duration.ofSeconds(2)).schedule();
    }
}
