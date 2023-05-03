package net.trustgames.proxy.tablist;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;

public class TablistHandler {

    @Subscribe(order = PostOrder.NORMAL)
    public void onPlayerJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();

        player.sendPlayerListHeaderAndFooter(
                TablistConfig.TABLIST_HEADER.getFormatted(),
                TablistConfig.TABLIST_FOOTER.getFormatted()
        );
    }
}
