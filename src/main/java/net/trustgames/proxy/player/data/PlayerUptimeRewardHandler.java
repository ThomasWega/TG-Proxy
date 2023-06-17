package net.trustgames.proxy.player.data;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerUptimeRewardHandler {
    private final Proxy proxy;
    private final ProxyServer server;
    private final PlayerDataFetcher dataFetcher;
    private final HashMap<UUID, Long> previousUptimes = new HashMap<>();

    private static final int rewardAmount = 10;
    private static final PlayerDataType rewardType = PlayerDataType.GEMS;
    private static final long rewardPeriodMillis = 300000L;  // 5 mins

    public PlayerUptimeRewardHandler(Proxy proxy) {
        this.proxy = proxy;
        this.server = proxy.getServer();
        this.dataFetcher = new PlayerDataFetcher(proxy.getToolkit());
        server.getEventManager().register(proxy, this);
        createTask();
    }

    private void createTask() {
        server.getScheduler().buildTask(proxy, () -> server.getAllPlayers().forEach(player -> {
                    UUID uuid = player.getUniqueId();
                    long previousTimeMillis = previousUptimes.getOrDefault(uuid, System.currentTimeMillis());
                    if ((System.currentTimeMillis() - previousTimeMillis) > rewardPeriodMillis) {
                        addReward(player);
                        previousUptimes.replace(uuid, System.currentTimeMillis());
                    }
                }))
                .repeat(5, TimeUnit.SECONDS)
                .schedule();
    }

    private void addReward(Player receiver) {
        dataFetcher.addDataAsync(receiver.getUniqueId(), rewardType, rewardAmount);
        receiver.sendMessage(MiniMessage.miniMessage().deserialize(
                "<color:#fffb05>Bonus |</color> " +
                        "<dark_gray>You have been rewarded <yellow><amount> <data_type_name>" +
                        "<dark_gray> for playing on the server for <white><time> minutes",
                TagResolver.builder()
                        .resolver(Placeholder.parsed("amount", String.valueOf(rewardAmount)))
                        .resolver(Placeholder.parsed("data_type_name", rewardType.getColumnName()))
                        .resolver(Placeholder.parsed("time", String.valueOf(rewardPeriodMillis / 60000)))
                        .build()
                )
        );
    }

    @Subscribe
    private void onPlayerJoin(LoginEvent event) {
        previousUptimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @Subscribe
    private void onPlayerQuit(DisconnectEvent event) {
        previousUptimes.remove(event.getPlayer().getUniqueId());
    }
}
