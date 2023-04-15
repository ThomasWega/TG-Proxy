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
import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.managers.HikariManager;
import net.trustgames.middleware.managers.RabbitManager;
import net.trustgames.proxy.managers.ConfigManager;
import net.trustgames.proxy.player.data.PlayerDataHandler;
import net.trustgames.proxy.player.data.commands.PlayerDataCommand;
import net.trustgames.proxy.player.data.commands.PlayerDataCommandTest;
import net.trustgames.proxy.tablist.TablistHandler;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.IOException;
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
    private final Middleware middleware = new Middleware();

    private final File dataFolder;

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

        // TEST add the player to database on join

        registerCommands();
        registerEvents();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (middleware.getJedisPool() != null) {
            middleware.getJedisPool().close();
        }
        if (middleware.getRabbitManager() != null) {
            middleware.getRabbitManager().close();
        }
    }

    private void registerCommands(){
        // commands
        new PlayerDataCommand(this);

        VelocityCommandManager<CommandSource> manager = new VelocityCommandManager<>(
                server.getPluginManager().ensurePluginContainer(this),
                server,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );

        manager.brigadierManager().setNativeNumberSuggestions(false);
        new PlayerDataCommandTest(manager, server);
    }

    private void registerEvents(){
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new TablistHandler());
        eventManager.register(this, new PlayerDataHandler(this));
    }

    private void initializeHikari(){
        try {
            ConfigurationNode hikariConfig = ConfigManager.loadConfig(dataFolder, "mariadb.yml");

            if (!hikariConfig.getNode("mariadb", "enable").getBoolean()) {
                logger.warn("HikariCP is disabled");
                return;
            }

            middleware.setHikariManager(new HikariManager(
                    Objects.requireNonNull(hikariConfig.getNode("mariadb", "user").getString()),
                    Objects.requireNonNull(hikariConfig.getNode("mariadb", "password").getString()),
                    Objects.requireNonNull(hikariConfig.getNode("mariadb", "ip").getString()),
                    Objects.requireNonNull(hikariConfig.getNode("mariadb", "port").getString()),
                    Objects.requireNonNull(hikariConfig.getNode("mariadb", "database").getString()),
                    hikariConfig.getNode("hikaricp", "pool-size").getInt()
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeRabbit(){
        try {
            ConfigurationNode rabbitConfig = ConfigManager.loadConfig(dataFolder, "rabbitmq.yml");

            if (!rabbitConfig.getNode("rabbitmq", "enable").getBoolean()) {
                logger.warn("RabbitMQ is disabled");
                return;
            }

            middleware.setRabbitManager(new RabbitManager(
                    Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "user").getString()),
                    Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "password").getString()),
                    Objects.requireNonNull(rabbitConfig.getNode("rabbitmq", "ip").getString()),
                    rabbitConfig.getNode("rabbitmq", "port").getInt())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeRedis() {
        try {
            ConfigurationNode redisConfig = ConfigManager.loadConfig(dataFolder, "redis.yml");
            if (!redisConfig.getNode("redis", "enable").getBoolean()) {
                logger.warn("Redis is disabled");
                return;
            }

            middleware.setJedisPool(new JedisPool(
                    redisConfig.getNode("redis", "ip").getString(),
                    redisConfig.getNode("redis", "port").getInt(),
                    redisConfig.getNode("redis", "user").getString(),
                    redisConfig.getNode("redis", "password").getString()
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
