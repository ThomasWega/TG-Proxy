package net.trustgames.proxy.player.data.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataPlaytimeHandler {

    private final Toolkit toolkit;
    private final Map<UUID, Long> startTimes = new HashMap<>();

    public PlayerDataPlaytimeHandler(Proxy proxy) {
        this.toolkit = proxy.getToolkit();
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        long startTime = System.currentTimeMillis();
        startTimes.put(playerId, startTime);
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long endTime = System.currentTimeMillis();
        long durationInMillis = endTime - startTimes.get(uuid);
        int durationInSec = (int) Math.floor(durationInMillis / 1000d);
        new PlayerDataFetcher(toolkit).addDataAsync(uuid, PlayerDataType.PLAYTIME, durationInSec);
    }
}

