package net.trustgames.proxy.utils;

import com.velocitypowered.api.proxy.Player;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.trustgames.proxy.managers.LuckPermsManager;

public class PlaceholderUtils {

    static Expansion expansion = Expansion.builder("tg")
            .filter(Player.class)
            .audiencePlaceholder("player_name", ((audience, argumentQueue, context) ->
                    Tag.selfClosingInserting(Component.text((((Player) audience).getUsername())))))
            .audiencePlaceholder("player_prefix", ((audience, queue, ctx) ->
                    Tag.selfClosingInserting((LuckPermsManager.getPlayerPrefix((Player) audience)))))
            .build();

    public static void initialize() {
        expansion.register();

    }
}
