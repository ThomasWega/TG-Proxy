package net.trustgames.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.trustgames.database.HikariManager;
import net.trustgames.database.RabbitManager;
import net.trustgames.proxy.cache.CacheHandler;
import net.trustgames.proxy.config.Config;
import net.trustgames.proxy.tablist.TablistHandler;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;


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
    private HikariManager hikariManager;
    @Getter
    private RabbitManager rabbitManager;
    @Getter
    public static JedisPool jedisPool;
    private final File dataFolder;

    @Inject
    public Proxy(Logger logger, ProxyServer server, @DataDirectory Path data) {
        this.logger = logger;
        this.server = server;
        this.dataFolder = data.toFile();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        initializeRabbit();
        initializeHikari();
        jedisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);

        // TODO fix shading
        // TODO add config for rabbitmq

        EventManager eventManager = server.getEventManager();
        eventManager.register(this, new CacheHandler(this));
        eventManager.register(this, new TablistHandler());

        // TODO move this
        server.getScheduler().buildTask(this, () -> {
            try {
                rabbitManager.onDelivery(json -> {
                    String playerName = json.getString("player");
                    Component message = GsonComponentSerializer.gson().deserialize(json.getString("message"));
                    server.getPlayer(playerName).ifPresent(player -> player.sendMessage(message));
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).delay(Duration.ofSeconds(2)).schedule();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        jedisPool.close();
        try {
            rabbitManager.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void initializeHikari(){

        ConfigurationNode config = Config.loadConfig(dataFolder, "mariadb.yml");

        hikariManager = new HikariManager(
                Objects.requireNonNull(config.getNode("mariadb", "user").getString()),
                Objects.requireNonNull(config.getNode("mariadb", "password").getString()),
                Objects.requireNonNull(config.getNode("mariadb", "ip").getString()),
                Objects.requireNonNull(config.getNode("mariadb", "port").getString()),
                Objects.requireNonNull(config.getNode("mariadb", "database").getString()),
                config.getNode("hikaricp", "pool-size").getInt(),
                !config.getNode("mariadb", "enable").getBoolean()
        );
    }

    private void initializeRabbit(){
        rabbitManager = new RabbitManager(
                "guest",
                "guest",
                "localhost",
                5672,
                "proxy",
                false
        );
    }
}
