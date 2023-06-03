package net.trustgames.proxy.player.activity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.activity.PlayerActivity;
import net.trustgames.toolkit.database.player.activity.PlayerActivityFetcher;
import net.trustgames.toolkit.database.player.activity.config.PlayerAction;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.UUID;

public class PlayerActivityHandler {
    private final PlayerDataFetcher dataFetcher;
    private final PlayerActivityFetcher activityFetcher;
    private final Proxy proxy;
    private final ProxyServer server;

    public PlayerActivityHandler(Proxy proxy) {
        Toolkit toolkit = proxy.getToolkit();
        this.dataFetcher = new PlayerDataFetcher(toolkit);
        this.activityFetcher = new PlayerActivityFetcher(toolkit.getHikariManager());
        this.proxy = proxy;
        this.server = proxy.getServer();
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe
    private void onPlayerJoin(LoginEvent event) {
        insertNewActionAsync(event.getPlayer(), PlayerAction.JOIN);
    }

    @Subscribe
    private void onPlayerQuit(DisconnectEvent event) {
        insertNewActionAsync(event.getPlayer(), PlayerAction.LEAVE);

    }

    @Subscribe(order = PostOrder.FIRST)
    private EventTask compareNamesOnJoin(LoginEvent event) {
        return EventTask.withContinuation(continuation -> {
            Player player = event.getPlayer();
            String currentName = player.getUsername();
            UUID uuid = player.getUniqueId();
            dataFetcher.resolveDataAsync(uuid, PlayerDataType.NAME).thenAccept(optName -> {
                if (optName.isEmpty()) {
                    continuation.resume();
                    return;
                }

                String dbName = optName.get().toString();
                if (currentName.equals(dbName)) {
                    continuation.resume();
                    return;
                }

                insertNewActionAsync(player, PlayerAction.NAME_CHANGE);
                continuation.resume();
            });
        });
    }

    private void insertNewActionAsync(Player player, PlayerAction action) {
        server.getScheduler().buildTask(proxy, () -> {
            InetSocketAddress playerIp = player.getRemoteAddress();
            String playerIpString = (playerIp == null) ? null : playerIp.getHostString();
            activityFetcher.insertNewAction(new PlayerActivity.Activity(
                    player.getUniqueId(),
                    playerIpString,
                    action,
                    new Timestamp(System.currentTimeMillis())
            ));
        }).schedule();
    }
}
