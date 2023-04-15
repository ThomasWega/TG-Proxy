package net.trustgames.proxy.config;

public enum ProxyPermissionConfig {
    ADMIN("proxy.admin"),
    STAFF("proxy.staff"),
    TITAN("proxy.titan"),
    LORD("proxy.lord"),
    KNIGHT("proxy.knight"),
    PRIME("proxy.prime"),
    DEFAULT("proxy.default");

    public final String permission;

    ProxyPermissionConfig(String permission) {
        this.permission = permission;
    }
}
