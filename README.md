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
- Chat limiter (cooldown)
- Command limiter (cooldown)
- Chat filter (profanity, ads)
- Config manager
- LuckPerms manager
- Player activity saving (join/leave)
- Player name database + cache update 
- Player playtime database + cache update
- Tablist handler

###### Commands:
- /gems <name> (gold/xp/level/...)
- /gemsadmin <name> set 1000 (xp/level/kills/...)

###### How to get Toolkit:
TG-Toolkit is self-hosted on a server. To be able to reach that server you need to set the server up credentials first.
Open (or create) gradle.properties in your local ~/.gradle/gradle.properties

**_gradle.properties_**
```
trustgamesRepoPrivateUsername={username}
trustgamesRepoPrivatePassword={secret}
```
