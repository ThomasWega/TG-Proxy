package net.trustgames.proxy.utils;

import com.velocitypowered.api.proxy.Player;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.luckperms.api.model.user.User;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.managers.permission.LuckPermsManager;
import net.trustgames.toolkit.utils.LevelUtils;

import java.util.Optional;
import java.util.UUID;

public class PlaceholderUtils {

    private final Toolkit toolkit;

    public PlaceholderUtils(Toolkit toolkit) {
        this.toolkit = toolkit;
    }


    public void initialize() {
        Expansion expansion = Expansion.builder("tg")
                .filter(Player.class)
                .audiencePlaceholder("player_prefix_spaced", ((audience, queue, ctx) -> 
                        Tag.selfClosingInserting(formatPrefix(((Player) audience)))))

                .audiencePlaceholder("player_level", ((audience, queue, ctx) ->
                    Tag.selfClosingInserting(Component.text(getLevel(((Player) audience).getUniqueId())))))

                .audiencePlaceholder("player_level_progress", ((audience, queue, ctx) ->
                        Tag.selfClosingInserting(Component.text(
                                String.format("%.1f", getLevelProgress(((Player) audience).getUniqueId()) * 100)))))

                .build();

        expansion.register();
    }

    private int getLevel(UUID uuid) {
        System.out.println("GET LEVAAA");
        return new PlayerDataFetcher(toolkit).resolveIntData(uuid, PlayerDataType.LEVEL).orElse(0);
    }

    private float getLevelProgress(UUID uuid) {
        System.out.println("GET PROGAAA");
        int xp = new PlayerDataFetcher(toolkit).resolveIntData(uuid, PlayerDataType.XP).orElse(0);
        return LevelUtils.getProgress(xp);
    }

    private Component formatPrefix(Player player) {
        Optional<User> optUser = LuckPermsManager.getOnlineUser(player.getUniqueId());
        if (optUser.isEmpty()){
            return Component.empty();
        }
        User user = optUser.get();
        String primaryGroup = user.getPrimaryGroup();
        Component prefix = ColorUtils.color(LuckPermsManager.getOnlinePlayerPrefix(user));
        if (!(primaryGroup.equals("default"))) {
            prefix = prefix.appendSpace();
        }
        return prefix;
    }
}
