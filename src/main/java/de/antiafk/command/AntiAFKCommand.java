package de.antiafk.command;

import de.antiafk.AntiAFK;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import de.antiafk.manager.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class AntiAFKCommand implements CommandExecutor, TabCompleter {

    private final AFKManager afkManager;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;

    public AntiAFKCommand(AntiAFK plugin, AFKManager afkManager, ConfigManager configManager, DatabaseManager databaseManager) {
        this.afkManager = afkManager;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiafk.admin")) {
            sender.sendMessage(configManager.getNoPermission());
            return true;
        }

        if (args.length == 0) {
            showStatus(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status" -> showStatus(sender);
            case "reload" -> reloadPlugin(sender);
            case "check" -> {
                if (args.length < 2) {
                    sender.sendMessage("<#FFB3BA>Benutzung: /antiafk check <Spieler>");
                    return true;
                }
                checkPlayerAFK(sender, args[1]);
            }
            case "debug" -> toggleDebug(sender);
            case "stats" -> {
                if (args.length < 2) {
                    sender.sendMessage("<#FFB3BA>Benutzung: /antiafk stats <Spieler>");
                    return true;
                }
                showPlayerStats(sender, args[1]);
            }
            case "top" -> showTopPlayers(sender);
            default -> {
                sender.sendMessage(configManager.getHelpStatus());
                sender.sendMessage(configManager.getHelpReload());
                sender.sendMessage("<#FFFFBA>/antiafk check <Spieler> <#90EE90>- Prüft wie lange der Spieler AFK ist");
                sender.sendMessage("<#FFFFBA>/antiafk stats <Spieler> <#90EE90>- Zeigt AFK-Statistiken des Spielers");
                sender.sendMessage("<#FFFFBA>/antiafk top <#90EE90>- Zeigt Top-10 AFK-Spieler");
                sender.sendMessage("<#FFFFBA>/antiafk debug <#90EE90>- Toggled Debug-Modus AN/AUS");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("antiafk.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Erste Argument - Subcommands
            String prefix = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("status", "reload", "check", "stats", "top", "debug");
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(prefix)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Zweites Argument - Spielernamen für check und stats
            String subCommand = args[0].toLowerCase();
            if ((subCommand.equals("check") || subCommand.equals("stats")) && sender instanceof Player) {
                String prefix = args[1].toLowerCase();
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        completions.sort(String::compareTo);
        return completions;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(configManager.getStatusMessageHeader());
        sender.sendMessage(configManager.isEnabled() ? 
            configManager.getStatusMessageEnabled() : 
            configManager.getStatusMessageDisabled());
        sender.sendMessage(configManager.getStatusMessageTimeout()
            .replace("%timeout%", String.valueOf(configManager.getAfkTimeout())));
        sender.sendMessage(configManager.getStatusMessageInterval()
            .replace("%interval%", String.valueOf(configManager.getCheckInterval())));
        sender.sendMessage(configManager.getStatusMessagePlayers()
            .replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size())));
        
        if (databaseManager.isConnected()) {
            sender.sendMessage("<#BAFFC9>✓ Datenbank: <#90EE90>VERBUNDEN");
        } else {
            sender.sendMessage("<#FFB3BA>✗ Datenbank: <#FF6B6B>GETRENNT");
        }
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            configManager.reloadConfig();
            afkManager.stopAfkCheckTask();
            
            // Reconnect zur Datenbank
            if (configManager.isDatabaseEnabled()) {
                databaseManager.reconnect();
            }
            
            afkManager.startAfkCheckTask();
            sender.sendMessage(configManager.getReloadSuccessMessage());
        } catch (Exception e) {
            sender.sendMessage(configManager.getReloadErrorMessage());
            e.printStackTrace();
        }
    }

    private void checkPlayerAFK(CommandSender sender, String playerName) {
        org.bukkit.entity.Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            sender.sendMessage("<#FFB3BA>❌ Spieler <#E0BBE4>" + playerName + " <#FFB3BA>nicht online!");
            return;
        }

        long afkTime = afkManager.getPlayerAFKTime(targetPlayer);
        long stillTime = afkManager.getPlayerStillTime(targetPlayer);

        // Formatiere Stillstandszeit
        long stillSeconds = stillTime % 60;
        long stillMinutes = (stillTime / 60) % 60;
        long stillHours = stillTime / 3600;

        StringBuilder stillTimeStr = new StringBuilder();
        if (stillHours > 0) {
            stillTimeStr.append(stillHours).append("h ");
        }
        if (stillMinutes > 0) {
            stillTimeStr.append(stillMinutes).append("m ");
        }
        stillTimeStr.append(stillSeconds).append("s");

        // Angezeige Status
        sender.sendMessage("<#E0BBE4>━━━ Spieler Info: <#FFFFBA>" + targetPlayer.getName() + " <#E0BBE4>━━━");
        sender.sendMessage("<#BAE1FF>⏱ Stillstandszeit: <#FFFFBA>" + stillTimeStr.toString());
        
        long timeout = configManager.getAfkTimeout();
        if (stillTime >= timeout) {
            sender.sendMessage("<#FFB3BA>⚠ Status: <#FF6B6B>AFK");
        } else {
            sender.sendMessage("<#BAFFC9>✓ Status: <#90EE90>AKTIV");
        }
    }

    private void showPlayerStats(CommandSender sender, String playerName) {
        if (!databaseManager.isConnected()) {
            sender.sendMessage("<#FFB3BA>❌ Datenbank nicht verbunden!");
            return;
        }

        // Versuche UUID vom Namen zu bekommen
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID playerUUID = null;

        if (onlinePlayer != null) {
            playerUUID = onlinePlayer.getUniqueId();
        } else {
            // Versuche von Offline-Spieler
            try {
                playerUUID = UUID.fromString(playerName);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("<#FFB3BA>❌ Spieler <#E0BBE4>" + playerName + " <#FFB3BA>nicht gefunden!");
                return;
            }
        }

        Map<String, Object> stats = databaseManager.getPlayerStats(playerUUID);

        if (stats.isEmpty()) {
            sender.sendMessage("<#FFB3BA>❌ Keine Statistiken für <#E0BBE4>" + playerName + " <#FFB3BA>gefunden!");
            return;
        }

        String playerStatsName = (String) stats.get("playerName");
        long totalAFKTime = (long) stats.get("totalAFKTime");
        int afkCount = (int) stats.get("afkCount");

        sender.sendMessage("<#E0BBE4>━━━ AFK-Statistiken: <#FFFFBA>" + playerStatsName + " <#E0BBE4>━━━");
        sender.sendMessage("<#BAE1FF>⏱ Gesamt AFK-Zeit: <#FFFFBA>" + DatabaseManager.formatTime(totalAFKTime));
        sender.sendMessage("<#BAE1FF>📊 AFK-Vorkommnisse: <#FFFFBA>" + afkCount);
        sender.sendMessage("<#BAE1FF>⏰ Durchschnitt pro AFK: <#FFFFBA>" + 
            DatabaseManager.formatTime(afkCount > 0 ? totalAFKTime / afkCount : 0));
    }

    private void showTopPlayers(CommandSender sender) {
        if (!databaseManager.isConnected()) {
            sender.sendMessage("<#FFB3BA>❌ Datenbank nicht verbunden!");
            return;
        }

        List<Map<String, Object>> topPlayers = databaseManager.getTopAFKPlayers(10);

        if (topPlayers.isEmpty()) {
            sender.sendMessage("<#FFB3BA>❌ Keine AFK-Statistiken verfügbar!");
            return;
        }

        sender.sendMessage("<#E0BBE4>━━━━━━━━ Top 10 AFK-Spieler ━━━━━━━━");
        
        int rank = 1;
        for (Map<String, Object> playerData : topPlayers) {
            String playerName = (String) playerData.get("playerName");
            long totalAFKTime = (long) playerData.get("totalAFKTime");
            int afkCount = (int) playerData.get("afkCount");

            String formattedTime = DatabaseManager.formatTime(totalAFKTime);
            sender.sendMessage("<#FFFFBA>#" + rank + " <#E0BBE4>" + playerName + 
                    " <#90EE90>- " + formattedTime + " <#BAE1FF>(" + afkCount + "x)");
            rank++;
        }
    }

    private void toggleDebug(CommandSender sender) {
        afkManager.toggleDebug();
        if (afkManager.isDebugEnabled()) {
            sender.sendMessage("<#BAFFC9>✓ Debug-Modus: <#90EE90>AKTIVIERT");
        } else {
            sender.sendMessage("<#FFB3BA>✗ Debug-Modus: <#FF6B6B>DEAKTIVIERT");
        }
    }
}

