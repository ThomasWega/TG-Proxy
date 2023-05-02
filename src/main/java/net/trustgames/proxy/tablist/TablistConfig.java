package net.trustgames.proxy.tablist;

public enum TablistConfig {
    TABLIST_HEADER("&#ffa600&lTRUSTGAMES\n&#363c3dMiniGames Playground\n"),
    TABLIST_FOOTER("\n&astore.trustgames.net");

    public final String value;

    TablistConfig(String value) {
        this.value = value;
    }
}

