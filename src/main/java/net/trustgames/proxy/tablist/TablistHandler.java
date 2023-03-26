package net.trustgames.proxy.tablist;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.utils.ColorUtils;

public class TablistHandler {

    @Subscribe(order = PostOrder.NORMAL)
    public void onPlayerJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();

        player.sendPlayerListHeaderAndFooter(
                ColorUtils.color(TablistConfig.TABLIST_HEADER.value),
                ColorUtils.color(TablistConfig.TABLIST_FOOTER.value)
        );
    }
}
