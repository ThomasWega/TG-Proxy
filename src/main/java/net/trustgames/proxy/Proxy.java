package net.trustgames.proxy;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.trustgames.proxy.chat.announcer.AnnounceHandler;
import net.trustgames.proxy.chat.commands.TextCommands;
import net.trustgames.proxy.chat.cooldowns.ChatLimiter;
import net.trustgames.proxy.chat.cooldowns.CommandsLimiter;
import net.trustgames.proxy.chat.filter.ChatFilter;
import net.trustgames.proxy.config.ConfigManager;
import net.trustgames.proxy.player.data.commands.PlayerDataLookupCommand;
import net.trustgames.proxy.player.data.commands.PlayerDataModifyCommand;
import net.trustgames.proxy.player.data.commands.PlayerUptimeCommand;
import net.trustgames.proxy.player.data.handler.PlayerDataNameHandler;
import net.trustgames.proxy.player.data.handler.PlayerDataPlaytimeHandler;
import net.trustgames.proxy.tablist.TablistDecorationHandler;
import net.trustgames.proxy.utils.PlaceholderUtils;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.database.HikariManager;
import net.trustgames.toolkit.database.player.data.PlayerDataDB;
import net.trustgames.toolkit.message_queue.RabbitManager;
import ninja.leaping.configurate.ConfigurationNode;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;


@Plugin(
        id = "tg-proxy",
        name = "TG-PROXY",
        version = "0.1-SNAPSHOT",
        description = "Proxy plugin for TrustGames.net",
        url = "www.trustgames.net",
        authors = {"Wega"},
        dependencies = {
                @Dependency(id = "miniplaceholders"),
                @Dependency(id = "player-expansion"),
                @Dependency(id = "luckperms-expansion")
        }
)
public class Proxy {
    public static Logger LOGGER;
    @Getter
    private final ProxyServer server;
    @Getter
    private final Toolkit toolkit = new Toolkit();
    @Getter
    private final File dataFolder;
    @Getter
    private VelocityCommandManager<CommandSource> commandManager;

    @Inject
    public Proxy(Logger logger, ProxyServer server, @DataDirectory Path dataDir) {
        LOGGER = logger;
        this.server = server;
        this.dataFolder = dataDir.toFile();
    }

    @Subscribe
    public EventTask onProxyInitialization(ProxyInitializeEvent event) {
        return EventTask.async(() -> {
            initializeHikari();
            initializeRedis();
            initializeRabbit();

            new PlaceholderUtils(toolkit).initialize();
            registerCommands();
            registerEvents();
            new AnnounceHandler(this);
        });
    }

    @Subscribe
    public EventTask onProxyShutdown(ProxyShutdownEvent event) {
        return EventTask.async(toolkit::closeConnections);
    }

    private void registerCommands() {
        commandManager = new VelocityCommandManager<>(
                server.getPluginManager().ensurePluginContainer(this),
                server,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );
        commandManager.brigadierManager().setNativeNumberSuggestions(false);

        new PlayerDataLookupCommand(this);
        new PlayerDataModifyCommand(this);
        new PlayerUptimeCommand(this);
        new TextCommands(commandManager);
    }

    private void registerEvents() {
        new PlayerDataNameHandler(this);
        new PlayerDataPlaytimeHandler(this);
        new TablistDecorationHandler(this);
        new ChatLimiter(this);
        new ChatFilter(this);
        new CommandsLimiter(this);
    }

    private void initializeHikari() {
        ConfigurationNode hikariConfig = ConfigManager.getConfig(dataFolder, "mariadb.yml");

        if (!hikariConfig.getNode("mariadb", "enable").getBoolean()) {
            LOGGER.warning("HikariCP is disabled");
            return;
        }

        toolkit.setHikariManager(new HikariManager(
                Objects.requireNonNull(hikariConfig.getNode("mariadb", "user").getString()),
                Objects.requireNonNull(hikariConfig.getNode("mariadb", "password").getString()),
                Objects.requireNonNull(hikariConfig.getNode("mariadb", "ip").getString()),
                Objects.requireNonNull(hikariConfig.getNode("mariadb", "port").getString()),
                Objects.requireNonNull(hikariConfig.getNode("mariadb", "database").getString()),
                hikariConfig.getNode("hikaricp", "pool-size").getInt()
        ));

        HikariManager hikariManager = toolkit.getHikariManager();
        if (hikariManager == null) {
            throw new RuntimeException("HikariManager wasn't initialized");
        }

        hikariManager.onDataSourceInitialized(() -> new PlayerDataDB(hikariManager));

        LOGGER.info("HikariCP is enabled");

    }

    private void initializeRabbit() {
        ConfigurationNode rabbitConfig = ConfigManager.getConfig(dataFolder, "rabbitmq.yml");

        if (!rabbitConfig.getNode("rabbitmq", "enable").getBoolean()) {
            LOGGER.warning("RabbitMQ is disabled");
            return;
        }

        toolkit.setRabbitManager(new RabbitManager(
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "user").getString()),
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "password").getString()),
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "ip").getString()),
                rabbitConfig.getNode("rabbitmq", "port").getInt())
        );

        if (toolkit.getRabbitManager() == null) {
            throw new RuntimeException("RabbitManager wasn't initialized");
        }

        LOGGER.info("RabbitMQ is enabled");
    }

    private void initializeRedis() {
        ConfigurationNode redisConfig = ConfigManager.getConfig(dataFolder, "redis.yml");
        if (!redisConfig.getNode("redis", "enable").getBoolean()) {
            LOGGER.warning("Redis is disabled");
            return;
        }

        toolkit.setJedisPool(new JedisPool(
                redisConfig.getNode("redis", "ip").getString(),
                redisConfig.getNode("redis", "port").getInt(),
                redisConfig.getNode("redis", "user").getString(),
                redisConfig.getNode("redis", "password").getString()
        ));

        if (toolkit.getJedisPool() == null) {
            throw new RuntimeException("JedisPool wasn't initialized");
        }

        LOGGER.info("Redis is enabled");
    }
}
