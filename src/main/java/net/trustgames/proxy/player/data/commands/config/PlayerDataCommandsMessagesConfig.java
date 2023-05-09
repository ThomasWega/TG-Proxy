package net.trustgames.proxy.player.data.commands.config;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import io.github.miniplaceholders.api.MiniPlaceholders;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import net.trustgames.toolkit.utils.LevelUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public enum PlayerDataCommandsMessagesConfig {
    PREFIX("<color:#3498db>Economy | </color>");

    @Getter
    private final String stringPrefix;

    PlayerDataCommandsMessagesConfig(String stringPrefix) {
        this.stringPrefix = stringPrefix;
    }

    public enum Personal {
        KILLS(PREFIX.stringPrefix + "<dark_gray>You have <yellow><value> kills", PlayerDataType.KILLS),
        DEATHS(PREFIX.stringPrefix + "<dark_gray>You've <yellow>died <value> times", PlayerDataType.DEATHS),
        GAMES(PREFIX.stringPrefix + "<dark_gray>You've <yellow>played <value> games<dark_gray> in total", PlayerDataType.GAMES_PLAYED),
        PLAYTIME(PREFIX.stringPrefix + "<dark_gray>You've played for <yellow><value> hours<dark_gray> in total", PlayerDataType.PLAYTIME),
        XP(PREFIX.stringPrefix + "<dark_gray>You have <yellow><value> xp", PlayerDataType.XP),
        LEVEL(PREFIX.stringPrefix + "<dark_gray>You are at <yellow>level <value><dark_gray>. Progress till next level: <yellow><tg_player_level_progress>%", PlayerDataType.LEVEL),
        GEMS(PREFIX.stringPrefix + "<dark_gray>You have <yellow><value> gems", PlayerDataType.GEMS),
        RUBIES(PREFIX.stringPrefix + "<dark_gray>You have <yellow><value> rubies", PlayerDataType.RUBIES);

        private final String message;
        @Getter
        private final PlayerDataType dataType;

        Personal(String message, PlayerDataType dataType) {
            this.message = message;
            this.dataType = dataType;
        }

        public static PlayerDataCommandsMessagesConfig.Personal getByDataType(PlayerDataType dataType) {
            for (Personal config : values()) {
                if (config.getDataType() == dataType) {
                    System.out.println("OKA GOOD");
                    return config;
                }
            }
            System.out.println("BAD :(");
            return null;
        }

        public Component formatMessage(@NotNull Player sender, int value) {
            System.out.println("SENDER - " + value);
            System.out.println("SO WTF?");
            // convert seconds to hours
            if (dataType == PlayerDataType.PLAYTIME) {
                value = ((value / 60) / 60);
            }
            TagResolver tags = TagResolver.builder()
                    .resolver(MiniPlaceholders.getAudiencePlaceholders(sender))
                    .resolver(Placeholder.unparsed("value", String.valueOf(value)))
                    .build();

            System.out.println("BUILD HIH3");
            return MiniMessage.miniMessage().deserialize(message, tags);
        }
    }

    public enum Target {
        KILLS(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has <yellow><value> kills", PlayerDataType.KILLS),
        DEATHS(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has <yellow>died <value> times", PlayerDataType.DEATHS),
        GAMES(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has played <yellow><value> games<dark_gray> in total", PlayerDataType.GAMES_PLAYED),
        PLAYTIME(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has played for <yellow><value> hours<dark_gray> in total", PlayerDataType.PLAYTIME),
        XP(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has <yellow><value> xp", PlayerDataType.XP),
        LEVEL(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>is at <yellow>level <value><dark_gray>. Their progress till next level: <yellow><level_progress>%", PlayerDataType.LEVEL),
        GEMS(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has <yellow><value> gems", PlayerDataType.GEMS),
        RUBIES(PREFIX.stringPrefix + "<dark_gray>Player <white><target_name> <dark_gray>has <yellow><value> rubies", PlayerDataType.RUBIES);

        private final String message;
        @Getter
        private final PlayerDataType dataType;

        Target(String message, PlayerDataType dataType) {
            this.message = message;
            this.dataType = dataType;
        }

        public static PlayerDataCommandsMessagesConfig.Target getByDataType(PlayerDataType dataType) {
            for (Target config : values()) {
                if (config.getDataType() == dataType) {
                    return config;
                }
            }
            return null;
        }

        public Component formatMessage(@NotNull Toolkit toolkit,
                                       @NotNull String targetName, int value) {
            System.out.println("TARGET - " + value);
            TagResolver.Builder builder = TagResolver.builder()
                    .resolver(Placeholder.unparsed("target_name", targetName))
                    .resolver(Placeholder.unparsed("value", String.valueOf(value)));


            if (dataType == PlayerDataType.LEVEL) {
                int xp = new PlayerDataFetcher(toolkit).resolveIntData(targetName, PlayerDataType.XP).orElse(0);
                builder.resolver(Placeholder.unparsed("level_progress",
                        String.format("%.1f", LevelUtils.getProgress(xp) * 100)));
            }

            return MiniMessage.miniMessage().deserialize(
                    message, builder.build()
            );
        }
    }

    public static class Modify {
        public enum Sender {
            SET(PREFIX.stringPrefix + "<dark_gray>Set <white><target_name>'s <yellow><data_type> <dark_gray>to <yellow><value>"),
            ADD(PREFIX.stringPrefix + "<dark_gray>Added <yellow><value> <data_type> <dark_gray>to player <white><target_name><dark_gray>. His current amount: <yellow><current_balance> <data_type>"),
            REMOVE(PREFIX.stringPrefix + "<dark_gray>Removed <yellow><value> <data_type> <dark_gray>from player <white><target_name><dark_gray>. His current amount: <yellow><current_balance> <data_type>");

            private final String message;

            Sender(String message) {
                this.message = message;
            }

            public void formatMessage(@NotNull Toolkit toolkit,
                                      @NotNull String targetName,
                                      int value,
                                      @NotNull ModifyAction modifyAction,
                                      @NotNull PlayerDataType dataType,
                                      Consumer<Optional<Component>> callback) {
                int[] valueArr = {value};
                PlayerDataFetcher dataFetcher = new PlayerDataFetcher(toolkit);
                dataFetcher.resolveIntDataAsync(targetName, dataType).thenAccept(optCurrentBalance -> {
                    if (optCurrentBalance.isEmpty()) {
                        callback.accept(Optional.empty());
                        return;
                    }
                    // convert seconds to hours
                    if (dataType == PlayerDataType.PLAYTIME) {
                        valueArr[0] = ((valueArr[0] / 60) / 60);
                    }

                    int currentBalance = optCurrentBalance.getAsInt();
                    if (modifyAction == ModifyAction.ADD){
                        currentBalance += value;
                    } else if (modifyAction == ModifyAction.REMOVE) {
                        currentBalance -= value;
                    }

                    TagResolver.Builder builder = TagResolver.builder()
                            .resolver(Placeholder.unparsed("value", String.valueOf(valueArr[0])))
                            .resolver(Placeholder.unparsed("target_name", targetName))
                            .resolver(Placeholder.unparsed("data_type", Objects.requireNonNull(DataTypeDisplay.getDisplayNameByDataType(dataType))))
                            .resolver(Placeholder.unparsed("current_balance", String.valueOf(currentBalance)));
                    callback.accept(Optional.of(MiniMessage.miniMessage().deserialize(message, builder.build())));
                });
            }
        }

        public enum Target {
            SET(PREFIX.stringPrefix + "<dark_gray>You've been set <yellow><data_type> <dark_gray>to <yellow><value><dark_gray> by <white><source_name>"),
            ADD(PREFIX.stringPrefix + "<dark_gray>You've been added <yellow><value> <data_type><dark_gray> by <white><source_name><dark_gray>. Current amount: <yellow><current_balance> <data_type>"),
            REMOVE(PREFIX.stringPrefix + "<dark_gray>You've been removed <yellow><value> <data_type><dark_gray> by <white><source_name><dark_gray>. Current amount: <yellow><current_balance> <data_type>");

            private final String message;

            Target(String message) {
                this.message = message;
            }

            public void formatMessage(@NotNull Toolkit toolkit,
                                      @NotNull CommandSource source,
                                      @NotNull String targetName,
                                      int value,
                                      @NotNull ModifyAction modifyAction,
                                      @NotNull PlayerDataType dataType,
                                      Consumer<Optional<Component>> callback) {
                int[] valueArr = {value};
                new PlayerDataFetcher(toolkit).resolveIntDataAsync(targetName, dataType)
                        .thenAccept(optCurrentBalance -> {
                            if (optCurrentBalance.isEmpty()) {
                                callback.accept(Optional.empty());
                                return;
                            }
                            // convert seconds to hours
                            if (dataType == PlayerDataType.PLAYTIME) {
                                valueArr[0] = ((valueArr[0] / 60) / 60);
                            }

                            int currentBalance = optCurrentBalance.getAsInt();
                            if (modifyAction == ModifyAction.ADD){
                                currentBalance += value;
                            } else if (modifyAction == ModifyAction.REMOVE) {
                                currentBalance -= value;
                            }

                            String sourceName = (source instanceof Player player)
                                    ? player.getUsername() : "CONSOLE";

                            TagResolver.Builder builder = TagResolver.builder()
                                    .resolver(Placeholder.unparsed("value", String.valueOf(valueArr[0])))
                                    .resolver(Placeholder.unparsed("source_name", sourceName))
                                    .resolver(Placeholder.unparsed("data_type", Objects.requireNonNull(DataTypeDisplay.getDisplayNameByDataType(dataType))))
                                    .resolver(Placeholder.unparsed("current_balance", String.valueOf(currentBalance)));
                            callback.accept(Optional.of(MiniMessage.miniMessage().deserialize(message, builder.build())));
                        });
            }
        }

        private enum DataTypeDisplay {
            KILLS("kills", PlayerDataType.KILLS),
            DEATHS("deaths", PlayerDataType.DEATHS),
            GAMES("games played", PlayerDataType.GAMES_PLAYED),
            PLAYTIME("hours of playtime", PlayerDataType.PLAYTIME),
            XP("xp", PlayerDataType.XP),
            LEVEL("levels", PlayerDataType.LEVEL),
            GEMS("gems", PlayerDataType.GEMS),
            RUBIES("rubies", PlayerDataType.RUBIES);

            @Getter
            private final String displayName;

            private final PlayerDataType dataType;

            DataTypeDisplay(String displayName, PlayerDataType dataType) {
                this.displayName = displayName;
                this.dataType = dataType;
            }

            public static String getDisplayNameByDataType(PlayerDataType dataType) {
                for (DataTypeDisplay data : values()) {
                    if (data.dataType == dataType) {
                        return data.getDisplayName();
                    }
                }
                return null;
            }
        }
        public enum ModifyAction {
            SET,
            ADD,
            REMOVE
        }
    }
}
