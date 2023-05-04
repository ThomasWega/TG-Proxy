package net.trustgames.proxy;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.trustgames.proxy.chat.announcer.AnnounceHandler;
import net.trustgames.proxy.chat.commands.TextCommands;
import net.trustgames.proxy.chat.cooldowns.ChatLimiter;
import net.trustgames.proxy.managers.ConfigManager;
import net.trustgames.proxy.player.activity.PlayerActivityHandler;
import net.trustgames.proxy.player.data.PlayerDataHandler;
import net.trustgames.proxy.player.data.commands.PlayerDataAdminCommand;
import net.trustgames.proxy.player.data.commands.PlayerDataLookupCommand;
import net.trustgames.proxy.tablist.TablistHandler;
import net.trustgames.toolkit.Toolkit;
import net.trustgames.toolkit.managers.HikariManager;
import net.trustgames.toolkit.managers.rabbit.RabbitManager;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;


@Plugin(
        id = "proxy",
        name = "TG-PROXY",
        version = "0.1-SNAPSHOT",
        description = "Proxy plugin for TrustGames.net",
        url = "www.trustgames.net",
        authors = {"Wega"}
)
public class Proxy {

    @Getter
    private final Logger logger;
    @Getter
    private final ProxyServer server;
    @Getter
    private final Toolkit toolkit = new Toolkit();
    private final File dataFolder;
    @Getter
    private VelocityCommandManager<CommandSource> commandManager;

    @Inject
    public Proxy(Logger logger, ProxyServer server, @DataDirectory Path data) {
        this.logger = logger;
        this.server = server;
        this.dataFolder = data.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initializeHikari();
        initializeRedis();
        initializeRabbit();

        registerCommands();
        registerEvents();
        new AnnounceHandler(this);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        System.out.println("BEFORE CLOSE");
        toolkit.closeConnections();
        System.out.println("AFTER CLOSE");
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
        new PlayerDataAdminCommand(this);
        new TextCommands(commandManager);
    }

    private void registerEvents() {
        new PlayerActivityHandler(this);
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new TablistHandler());
        eventManager.register(this, new PlayerDataHandler(this));
        eventManager.register(this, new ChatLimiter());
    }

    private void initializeHikari() {
        ConfigurationNode hikariConfig = ConfigManager.loadConfig(dataFolder, "mariadb.yml");

        if (!hikariConfig.getNode("mariadb", "enable").getBoolean()) {
            logger.warn("HikariCP is disabled");
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
    }

    private void initializeRabbit() {
        ConfigurationNode rabbitConfig = ConfigManager.loadConfig(dataFolder, "rabbitmq.yml");

        if (!rabbitConfig.getNode("rabbitmq", "enable").getBoolean()) {
            logger.warn("RabbitMQ is disabled");
            return;
        }

        toolkit.setRabbitManager(new RabbitManager(
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "user").getString()),
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "password").getString()),
                Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "ip").getString()),
                rabbitConfig.getNode("rabbitmq", "port").getInt())
        );
    }

    private void initializeRedis() {
        ConfigurationNode redisConfig = ConfigManager.loadConfig(dataFolder, "redis.yml");
        if (!redisConfig.getNode("redis", "enable").getBoolean()) {
            logger.warn("Redis is disabled");
            return;
        }

        toolkit.setJedisPool(new JedisPool(
                redisConfig.getNode("redis", "ip").getString(),
                redisConfig.getNode("redis", "port").getInt(),
                redisConfig.getNode("redis", "user").getString(),
                redisConfig.getNode("redis", "password").getString()
        ));
    }
}
