package net.trustgames.proxy.player.data.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.trustgames.proxy.Proxy;
import net.trustgames.proxy.player.data.commands.config.PlayerDataCommandsMessagesConfig;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.config.CommandConfig;
import net.trustgames.toolkit.database.player.data.PlayerData;
import net.trustgames.toolkit.database.player.data.PlayerDataFetcher;
import net.trustgames.toolkit.database.player.data.config.PlayerDataType;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;

public class PlayerDataLookupCommand {

    private final VelocityCommandManager<CommandSource> commandManager;
    private final Toolkit toolkit;
    private final ProxyServer server;

    public PlayerDataLookupCommand(Proxy proxy) {
        this.commandManager = proxy.getCommandManager();
        this.toolkit = proxy.getToolkit();
        this.server = proxy.getServer();

        // don't include NAME and UUID
        Arrays.stream(PlayerDataType.values())
                .filter(dataType -> dataType != PlayerDataType.NAME && dataType != PlayerDataType.UUID)
                .forEach(dataType -> {

                    // MAIN COMMAND
                    String label = dataType.name().toLowerCase();
                    Command.Builder<CommandSource> dataTypeCommand = commandManager.commandBuilder(
                            label,
                            CommandMeta.simple().with(CommandMeta.DESCRIPTION,
                                            "Check the amount of " + label + " a player has. " +
                                                    "If no target is specified, it returns your own balance")
                                    .build()
                    );
                    registerSelfCommand(dataTypeCommand, dataType);
                    registerTargetCommand(dataTypeCommand, dataType);
                });


        Command.Builder<CommandSource> allCommand = commandManager.commandBuilder("data",
                CommandMeta.simple().with(CommandMeta.DESCRIPTION,
                                "Check the amount of all data of the player" +
                                        "If no target is specified, it returns your own balance")
                        .build()
        );

        registerSelfAllCommand(allCommand);
        registerTargetAllCommand(allCommand);
    }

    /**
     * Gets the data value for the command sender and prints it for him.
     * Handles if the sender is not player, as console doesn't have any data.
     *
     * @param dataType Type of data to get
     */
    private void registerSelfCommand(@NotNull Command.Builder<CommandSource> rootCommand,
                                     @NotNull PlayerDataType dataType) {

        // REGISTER
        commandManager.command(rootCommand
                .senderType(Player.class)
                .handler(context -> {
                    Player player = ((Player) context.getSender());

                    System.out.println("HEMEME");
                    new PlayerDataFetcher(toolkit).resolveDataAsync(player.getUniqueId(), dataType).thenAccept(optData -> {
                        System.out.println("LOL - " + optData);
                        if (optData.isEmpty()) {
                            player.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(player.getUsername())));
                            return;
                        }

                        System.out.println("OBJC - " + optData.get());
                        player.sendMessage(Objects.requireNonNull(PlayerDataCommandsMessagesConfig.Personal.getByDataType(dataType)).formatMessage(player, Integer.parseInt(optData.get().toString())));
                    });
                }));
    }

    /**
     * Gets the data value for the supplied target and prints it for the command sender.
     * Handles if there is no data
     *
     * @param dataType Type of data to get
     */
    private void registerTargetCommand(@NotNull Command.Builder<CommandSource> rootCommand,
                                       @NotNull PlayerDataType dataType) {

        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        // REGISTER
        commandManager.command(rootCommand
                .argument(targetArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    String targetName = context.get(targetArg);

                    System.out.println("SO LIKE HUUUH???");
                    PlayerDataFetcher dataFetcher = new PlayerDataFetcher(toolkit);
                    System.out.println("BEFORE ALL");
                    dataFetcher.resolveDataAsync(targetName, dataType).thenAccept(optData -> {
                        System.out.println("OPTIK DATA - " + optData);
                        if (optData.isEmpty()) {
                            source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                            return;
                        }

                        source.sendMessage(Objects.requireNonNull(PlayerDataCommandsMessagesConfig.Target.getByDataType(dataType)).formatMessage(toolkit, targetName, Integer.parseInt(optData.get().toString())));
                    });
                }));
    }

    private void registerSelfAllCommand(Command.Builder<CommandSource> rootCommand) {
        commandManager.command(rootCommand
                .senderType(Player.class)
                .handler(context -> {
                    Player player = ((Player) context.getSender());
                    PlayerData.getPlayerDataAsync(toolkit, player.getUniqueId()).thenAccept(optPlayerData -> {
                        if (optPlayerData.isEmpty()) {
                            player.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(player.getUsername())));
                            return;
                        }

                        player.sendMessage(getAllDataMessage(optPlayerData.get()));
                    });
                })
        );
    }

    private void registerTargetAllCommand(Command.Builder<CommandSource> rootCommand) {

        // TARGET argument
        CommandArgument<CommandSource, String> targetArg = StringArgument.<CommandSource>builder("target")
                .withSuggestionsProvider((context, s) -> server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .toList())
                .asRequired()
                .build();

        commandManager.command(rootCommand
                .argument(targetArg)
                .handler(context -> {
                    CommandSource source = context.getSender();
                    String targetName = context.get(targetArg);
                    PlayerData.getPlayerDataAsync(toolkit, targetName).thenAccept(optPlayerData -> {
                        if (optPlayerData.isEmpty()) {
                            source.sendMessage(CommandConfig.COMMAND_NO_PLAYER_DATA.addComponent(Component.text(targetName)));
                            return;
                        }

                        source.sendMessage(getAllDataMessage(optPlayerData.get()));
                    });
                })
        );
    }

    private Component getAllDataMessage(PlayerData playerData) {
        return Component.newline()
                .append(Component.text("-----------------------",
                        Style.style(NamedTextColor.DARK_GRAY, TextDecoration.BOLD)))
                .appendNewline()
                .append(Component.text("Name: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getName(), NamedTextColor.GRAY)))
                .appendNewline()
                .append(Component.text("UUID: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getUuid().toString(), NamedTextColor.DARK_GRAY)))
                .appendNewline()
                .appendNewline()
                .append(Component.text("Gems: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getGems(), NamedTextColor.RED)))
                .appendNewline()
                .append(Component.text("Rubies: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getRubies(), NamedTextColor.LIGHT_PURPLE)))
                .appendNewline()
                .appendNewline()
                .append(Component.text("Level: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getLevel(), NamedTextColor.GREEN)
                                .appendSpace()
                                .append(Component.text("(" +
                                        new DecimalFormat("0.0").format(playerData.getLevelProgress() * 100) +
                                        "%)", NamedTextColor.GRAY))))
                .appendNewline()
                .append(Component.text("XP: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getXp(), NamedTextColor.DARK_GREEN)))
                .appendNewline()
                .appendNewline()
                .append(Component.text("Kills: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getKills(), NamedTextColor.AQUA)))
                .appendNewline()
                .append(Component.text("Deaths: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getDeaths(), NamedTextColor.DARK_AQUA)))
                .appendNewline()
                .append(Component.text("Games Played: ", NamedTextColor.WHITE)
                        .append(Component.text(playerData.getGamesPlayed(), NamedTextColor.GOLD)))
                .appendNewline()
                .append(Component.text("Playtime: ", NamedTextColor.WHITE)
                        .append(Component.text(new DecimalFormat("0.0").format(
                                playerData.getPlaytimeSeconds() / 3600d) + " hours", NamedTextColor.YELLOW)
                        )
                )
                .appendNewline()
                .append(Component.text("-----------------------", Style.style(NamedTextColor.DARK_GRAY, TextDecoration.BOLD)))
                .appendNewline();
    }
}
