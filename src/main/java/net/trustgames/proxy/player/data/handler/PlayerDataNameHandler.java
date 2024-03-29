package net.trustgames.proxy.player.data.handler;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

public class PlayerDataNameHandler {

    private final PlayerDataFetcher dataFetcher;

    public PlayerDataNameHandler(Proxy proxy) {
        this.dataFetcher = new PlayerDataFetcher(proxy.getToolkit());
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe(order = PostOrder.EARLY)
    private EventTask onPlayerJoin(LoginEvent event) {
        return EventTask.withContinuation(continuation -> {
            Player player = event.getPlayer();
            dataFetcher.setDataAsync(player.getUniqueId(), PlayerDataType.NAME, player.getUsername());
            continuation.resume();
        });
    }
}
