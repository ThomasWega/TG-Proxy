package net.trustgames.proxy.managers;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigManager {

    /**
     * Loads the file from the paths
     *
     * @param dir        Directory in which the desired file is
     * @param configName Name of the desired file
     * @return new ConfigurationNode
     */
    public static ConfigurationNode loadConfig(final File dir, String configName) {
        try {
            loadFiles(dir, configName);
            final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                    .setPath(dir.toPath().resolve(configName))
                    .build();

            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Trying to load files", e);
        }
    }

    /**
     * Creates the new file (if not exists)
     * with the default contents of the same file in resources
     *
     * @param dir        Directory where to save the desired file
     * @param configName Name of the file
     * @throws IOException Trying to get the default content from resources
     */
    private static void loadFiles(File dir, String configName) throws IOException {
        Path configPath = dir.toPath().resolve(configName);
        if (!Files.exists(configPath)) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();

            try (var stream = ConfigManager.class.getClassLoader().getResourceAsStream(configName)) {
                Files.copy(Objects.requireNonNull(stream), configPath);
            }
        }
    }
}
