package net.trustgames.proxy.player.activity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.database.player.activity.PlayerActivity;
import net.trustgames.toolkit.database.player.activity.PlayerActivityFetcher;
import net.trustgames.toolkit.database.player.activity.config.PlayerActivityType;

import java.net.InetSocketAddress;
import java.sql.Timestamp;

public class PlayerActivityHandler {

    private final PlayerActivityFetcher activityFetcher;

    public PlayerActivityHandler(Proxy proxy) {
        this.activityFetcher = new PlayerActivityFetcher(proxy.getToolkit().getHikariManager());
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe
    private void onPlayerJoin(LoginEvent event) {
        insertNewAction(event.getPlayer(), PlayerActivityType.JOIN.getAction());
    }

    @Subscribe
    private void onPlayerQuit(DisconnectEvent event) {
        insertNewAction(event.getPlayer(), PlayerActivityType.LEAVE.getAction());

    }

    private void insertNewAction(Player player, String action) {
        InetSocketAddress playerIp = player.getRemoteAddress();
        String playerIpString = (playerIp == null) ? null : playerIp.getHostString();
        activityFetcher.insertNew(new PlayerActivity.Activity(
                player.getUniqueId(),
                playerIpString,
                action,
                new Timestamp(System.currentTimeMillis())
        ));
    }
}
