package net.trustgames.proxy.player.activity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.database.player.activity.PlayerActivity;
import net.trustgames.toolkit.database.player.activity.PlayerActivityFetcher;
import net.trustgames.toolkit.database.player.activity.config.PlayerAction;

import java.net.InetSocketAddress;
import java.sql.Timestamp;

public class PlayerActivityHandler {

    private final PlayerActivityFetcher activityFetcher;
    private final Proxy proxy;
    private final ProxyServer server;

    public PlayerActivityHandler(Proxy proxy) {
        this.activityFetcher = new PlayerActivityFetcher(proxy.getToolkit().getHikariManager());
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
