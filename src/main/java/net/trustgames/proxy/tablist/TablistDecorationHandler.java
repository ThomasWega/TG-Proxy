package net.trustgames.proxy.tablist;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;

public class TablistDecorationHandler {

    public TablistDecorationHandler(Proxy proxy) {
        proxy.getServer().getEventManager().register(proxy, this);
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onPlayerJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();

        player.sendPlayerListHeaderAndFooter(
                TablistDecorationConfig.TABLIST_HEADER.getFormatted(),
                TablistDecorationConfig.TABLIST_FOOTER.getFormatted()
        );
    }
}
