# TG-Proxy
Proxy for TrustGames.net

###### Configs:
- Chat announcer messages
- Text commands messages
- Player data commands messages
- Tablist header/footer

###### Features:
- Chat announcer
- Chat info commands (/discord, /store, ...)
- Chat limiter
- Config manager
- LuckPerms manager
- Player activity saving (join/leave)
- Player name database + cache update 
- Player playtime database + cache update
- Tablist handler
- Color conversion Util
- Component conversion Util
- JSON conversion Util
- Placeholder conversion Util (MiniPlaceholders)

###### Commands:
- /gems <name> (gold/xp/level/...)
- /gemsadmin <name> set 1000 (xp/level/kills/...)

###### How to get Toolkit:
TG-Toolkit is self-hosted on a server. To be able to reach that server you need to set the server up in your maven settings.xml. Insert the following lines in the server section

**_settings.xml_**
```
<servers>
    <server>
      <id>trustgames-repo</id>
      <username>{username}</username>
      <password>{secret}</password>
    </server>
</servers>
```
