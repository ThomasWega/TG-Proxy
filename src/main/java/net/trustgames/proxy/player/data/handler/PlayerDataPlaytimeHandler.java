package net.trustgames.proxy.player.data.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataPlaytimeHandler {

    private static final Map<UUID, Long> startTimes = new HashMap<>();
    private final Toolkit toolkit;

    public PlayerDataPlaytimeHandler(Proxy proxy) {
        this.toolkit = proxy.getToolkit();
        proxy.getServer().getEventManager().register(proxy, this);
    }

    public static long getCurrentUptimeInMillis(UUID uuid) {
        if (startTimes.get(uuid) == null) return 0L;

        long endTime = System.currentTimeMillis();
        return endTime - startTimes.get(uuid);
    }

    @Subscribe
    private void onPlayerJoin(LoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long startTime = System.currentTimeMillis();
        startTimes.put(uuid, startTime);
    }

    @Subscribe
    private void onPlayerQuit(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        handleTimeUpdate(uuid);
        startTimes.remove(uuid);
    }

    @Subscribe
    private void onPlayerSwitchServer(ServerConnectedEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        handleTimeUpdate(uuid);
    }

    private void handleTimeUpdate(UUID uuid) {
        long durationInMillis = getCurrentUptimeInMillis(uuid);
        int durationInSec = (int) Math.floor(durationInMillis / 1000d);
        new PlayerDataFetcher(toolkit).addDataAsync(uuid, PlayerDataType.PLAYTIME, durationInSec);
    }
}

