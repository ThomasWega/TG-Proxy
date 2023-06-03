package net.trustgames.proxy.managers;

import net.trustgames.proxy.Proxy;
import net.trustgames.toolkit.managers.file.FileLoader;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    /**
     * Loads the file from the paths
     *
     * @param directory        Directory in which the desired file is
     * @param configName Name of the desired file
     * @return new ConfigurationNode
     */
    public static ConfigurationNode loadConfig(final File directory, String configName) {
        try {
            // creates the file if not existent
            File file = FileLoader.loadFile(Proxy.class.getClassLoader(), directory, configName);

            final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                    .setFile(file)
                    .build();

            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file " + configName, e);
        }
    }
}
