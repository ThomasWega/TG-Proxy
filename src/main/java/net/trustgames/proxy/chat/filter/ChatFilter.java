package net.trustgames.proxy.chat.filter;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.config.PermissionConfig;
import net.trustgames.toolkit.config.chat.ChatConfig;
import net.trustgames.toolkit.filter.AdvertisementRegexList;
import net.trustgames.toolkit.filter.ProfanityList;

import java.io.File;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Filters out blocked words or advertisements
 * using wordlists and regex expressions
 */
public class ChatFilter {

    private final HashSet<String> badWords;
    private final HashSet<Pattern> adsRegexes;

    public ChatFilter(Proxy proxy) {
        proxy.getServer().getEventManager().register(proxy, this);
        File dataFolder = proxy.getDataFolder();
        this.badWords = ProfanityList.loadSet(dataFolder);
        this.adsRegexes = AdvertisementRegexList.loadSet(dataFolder);
    }

    @Subscribe(order = PostOrder.EARLY)
    private EventTask onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (event.getResult() == PlayerChatEvent.ChatResult.denied()
                || player.hasPermission(PermissionConfig.STAFF.getPermission())) {
            return null;
        }
        return EventTask.withContinuation(continuation -> {
            long startTime = System.nanoTime();

            if (checkAdvertisement(event)) {
                player.sendMessage(ChatConfig.ON_ADVERTISEMENT.getFormatted());
            } else if (checkProfanity(event)) {
                player.sendMessage(ChatConfig.ON_SWEAR.getFormatted());
            }

            double finishTime = (System.nanoTime() - startTime) / 1_000_000_000d;
            Proxy.LOGGER.finer("Time total to filter message \"" + event.getMessage() + "\" was: " + finishTime + "s");

            continuation.resume();
        });
    }

    /**
     * Check for swear or inappropriate words using wordlist.
     * The word is replaced by asterisks (***).
     *
     * @return true if message contains blocked word, otherwise
     */
    private boolean checkProfanity(PlayerChatEvent event) {
        long startTime = System.nanoTime();

        String message = event.getMessage();
        String lowerCaseMsg = message.toLowerCase();
        String newMessage = message;

        boolean didSwear = false;
        for (String s : badWords) {
            if (lowerCaseMsg.contains(s)) {
                newMessage = newMessage.replaceAll("(?i)" + s, "*".repeat(s.length()));
                didSwear = true;
            }
        }
        if (didSwear) {
            event.setResult(PlayerChatEvent.ChatResult.message(newMessage));
        }

        double finishTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        Proxy.LOGGER.finer("Time to filter chat Profanity for message \"" + message + "\" was: " + finishTime + "s");

        return didSwear;
    }

    /**
     * Check for advertisements using regular expressions
     *
     * @return true if message contains advertisement, false otherwise
     */
    private boolean checkAdvertisement(PlayerChatEvent event) {
        long startTime = System.nanoTime();

        String message = event.getMessage();
        boolean foundAd = false;
        for (Pattern p : adsRegexes) {
            if (p.matcher(message.toLowerCase()).find()) {
                foundAd = true;
                break;
            }
        }
        if (foundAd) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }

        double finishTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        Proxy.LOGGER.finer("Time to filter chat Ads for message \"" + message + "\" was: " + finishTime + "s");

        return foundAd;
    }
}
