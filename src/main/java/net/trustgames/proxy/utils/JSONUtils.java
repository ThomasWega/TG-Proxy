package net.trustgames.proxy.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class JSONUtils {

    /**
     * @param jsonString String from JSONObject
     * @return Component from JSONString
     */
    public static Component toComponent(String jsonString) {
        return GsonComponentSerializer.gson().deserialize(jsonString);
    }
}
