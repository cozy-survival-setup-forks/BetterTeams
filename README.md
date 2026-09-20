# BetterTeams (cozy fork)

A fork of [BetterTeams](https://github.com/booksaw/BetterTeams) by booksaw (MIT) with simpler levels, a team list menu and less
baggage. Everything else works as in the original.

## What is different

- **`levels.yml`**: team levels have their own file. Add as many levels as you like, each with its own limits
  (members, chests, warps, admins, owners, bank), a price paid with team money or score, an icon, extra
  description lines and commands for reaching or leaving the level. Levels in an old `config.yml` are moved to
  `levels.yml` on the first start.
- **`/team levels [team]`**: a menu that shows every level with its limits, what it costs to reach it, and marks the level
  of the team. The text of the menu is in `levels.yml` too.
- **`/teamlist`**: a menu with every team on the server (banner in the colour of the team, description, leader and member
  count). Click a team to see its members with their rank and online status. The look is in `menus.yml`.
- **Removed** because most servers never use them: the extension system and the Discord, LuckPerms and zKoth extensions,
  HolographicDisplays and DecentHolograms holograms, the Lunar Client (Apollo) and UltimateClaims integrations, bStats,
  the update checker, and the pre 4.0 flat file storage with its converter. Storage is YAML or SQL.

Permissions: `betterTeams.levels` and `betterTeams.teamlist` (both given to everyone by default).

The original README follows.

---

## Better Teams

# Introduction:

Create teams to compete to be the best. This plugin is designed to encourage teamwork and foster a sense of community
within a server. BetterTeams includes features such as:

- Teaming up with friends
- Having private chats, unique to each team
- Protecting team members from team-killing.
- Individual homes for each team

[View the wiki for this project](https://booksaw.github.io/BetterTeams/)
[Looking for the Discord Server for support?](https://discord.gg/JF9DNs3)
