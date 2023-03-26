package net.trustgames.proxy.config;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface Config {

    static ConfigurationNode loadConfig(final File dir, String configName) throws IOException {
        loadFiles(dir, configName);
        final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                .setPath(dir.toPath().resolve(configName))
                .build();

        return loader.load();
    }

    private static void loadFiles(File dir, String configName) throws IOException {
        Path configPath = dir.toPath().resolve(configName);
        if (!Files.exists(configPath)) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();

            try (var stream = Config.class.getClassLoader().getResourceAsStream(configName)) {
                Files.copy(Objects.requireNonNull(stream), configPath);
            }
        }
    }
}
