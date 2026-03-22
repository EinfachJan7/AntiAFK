package de.antiafk.command;

import de.antiafk.AntiAFK;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import de.antiafk.manager.DatabaseManager;
import de.antiafk.manager.FileStorageManager;
import de.antiafk.manager.DataConverter;
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
    private final FileStorageManager fileStorageManager;
    private final DataConverter dataConverter;

    public AntiAFKCommand(AntiAFK plugin, AFKManager afkManager, ConfigManager configManager, 
                         DatabaseManager databaseManager, FileStorageManager fileStorageManager, DataConverter dataConverter) {
        this.afkManager = afkManager;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.fileStorageManager = fileStorageManager;
        this.dataConverter = dataConverter;
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
            case "convert" -> {
                if (args.length < 2) {
                    sender.sendMessage("<#FFB3BA>Benutzung: /antiafk convert <file-to-db | db-to-file>");
                    return true;
                }
                handleConvert(sender, args[1]);
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage("<#FFB3BA>Benutzung: /antiafk reset <Spieler> <time|count|all>");
                    return true;
                }
                resetPlayerStats(sender, args[1], args[2]);
            }
            default -> {
                sender.sendMessage(configManager.getHelpStatus());
                sender.sendMessage(configManager.getHelpReload());
                sender.sendMessage(configManager.getHelpCheck());
                sender.sendMessage(configManager.getHelpStats());
                sender.sendMessage(configManager.getHelpReset());
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
            String prefix = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("status", "reload", "check", "stats", "top", "convert", "debug", "reset");
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(prefix)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ((subCommand.equals("check") || subCommand.equals("stats") || subCommand.equals("reset")) && sender instanceof Player) {
                String prefix = args[1].toLowerCase();
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            } else if (subCommand.equals("convert")) {
                String prefix = args[1].toLowerCase();
                List<String> convertTypes = Arrays.asList("file-to-db", "db-to-file");
                
                for (String type : convertTypes) {
                    if (type.startsWith(prefix)) {
                        completions.add(type);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("reset")) {
                String prefix = args[2].toLowerCase();
                List<String> resetTypes = Arrays.asList("time", "count", "all");
                for (String type : resetTypes) {
                    if (type.startsWith(prefix)) {
                        completions.add(type);
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
        sender.sendMessage(configManager.getStatusMessageStatsUpdateInterval()
            .replace("%stats-update-interval%", String.valueOf(configManager.getStatsUpdateInterval())));
        sender.sendMessage(configManager.getStatusMessagePlayers()
            .replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size())));
        
        if (configManager.isDatabaseEnabled()) {
            if (databaseManager.isConnected()) {
                sender.sendMessage("<#BAFFC9>✓ Storage: <#90EE90>DATENBANK (verbunden)");
            } else {
                sender.sendMessage("<#FFB3BA>✗ Storage: <#FF6B6B>DATENBANK (getrennt)");
            }
        } else {
            sender.sendMessage("<#BAFFC9>✓ Storage: <#90EE90>DATEI");
        }
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            configManager.reloadConfig();
            afkManager.stopAfkCheckTask();
            
            // Reconnect zur Datenbank oder reload File Storage
            if (configManager.isDatabaseEnabled()) {
                databaseManager.reconnect();
            } else {
                fileStorageManager.initialize();
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
        boolean isDatabaseEnabled = configManager.isDatabaseEnabled();
        
        // Versuche Stats BY NAME zuerst (Offline+Online)
        Map<String, Object> stats = isDatabaseEnabled ?
            databaseManager.getPlayerStatsByName(playerName) :
            fileStorageManager.getPlayerStatsByName(playerName);

        if (stats.isEmpty()) {
            String message = configManager.getStatsMessageNotFound().replace("%player%", playerName);
            sender.sendMessage(message);
            return;
        }

        String playerStatsName = (String) stats.get("playerName");
        long totalAFKTime = (long) stats.get("totalAFKTime");
        int afkCount = (int) stats.get("afkCount");

        sender.sendMessage(configManager.getStatsMessageHeader().replace("%player%", playerStatsName));
        sender.sendMessage(configManager.getStatsMessageTotalTime()
            .replace("%time%", DatabaseManager.formatTime(totalAFKTime)));
        sender.sendMessage(configManager.getStatsMessageAfkCount()
            .replace("%count%", String.valueOf(afkCount)));
    }

    private void showTopPlayers(CommandSender sender) {
        boolean isDatabaseEnabled = configManager.isDatabaseEnabled();
        
        List<Map<String, Object>> topPlayers = isDatabaseEnabled ?
            databaseManager.getTopAFKPlayers(10) :
            fileStorageManager.getTopAFKPlayers(10);

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

    private void resetPlayerStats(CommandSender sender, String playerName, String resetType) {
        boolean isDatabaseEnabled = configManager.isDatabaseEnabled();
        
        // Zuerst versuchen, Stats BY NAME zu finden (für Offline-Spieler)
        Map<String, Object> stats = isDatabaseEnabled ?
            databaseManager.getPlayerStatsByName(playerName) :
            fileStorageManager.getPlayerStatsByName(playerName);

        if (stats.isEmpty()) {
            String message = configManager.getResetPlayerNotFound().replace("%player%", playerName);
            sender.sendMessage(message);
            return;
        }

        String uuidStr = (String) stats.get("uuid");
        UUID playerUUID = UUID.fromString(uuidStr);
        
        if (!resetType.equals("time") && !resetType.equals("count") && !resetType.equals("all")) {
            String message = configManager.getResetInvalidOption().replace("%option%", resetType);
            sender.sendMessage(message);
            return;
        }

        if (isDatabaseEnabled && databaseManager.isConnected()) {
            databaseManager.resetPlayerStats(playerUUID, resetType);
        } else if (fileStorageManager != null) {
            fileStorageManager.resetPlayerStats(playerUUID, resetType);
        }

        String message = switch (resetType) {
            case "time" -> configManager.getResetSuccessTime().replace("%player%", playerName);
            case "count" -> configManager.getResetSuccessCount().replace("%player%", playerName);
            case "all" -> configManager.getResetSuccessAll().replace("%player%", playerName);
            default -> "";
        };
        sender.sendMessage(message);
    }

    private void handleConvert(CommandSender sender, String convertType) {
        if (convertType.equalsIgnoreCase("file-to-db")) {
            if (!configManager.isDatabaseEnabled()) {
                sender.sendMessage("<#FFB3BA>❌ Datenbank ist nicht aktiviert!");
                return;
            }
            dataConverter.convertFileToDatabase(sender);
        } else if (convertType.equalsIgnoreCase("db-to-file")) {
            if (!configManager.isDatabaseEnabled()) {
                sender.sendMessage("<#FFB3BA>❌ Datenbank ist nicht aktiviert!");
                return;
            }
            dataConverter.convertDatabaseToFile(sender);
        } else {
            sender.sendMessage("<#FFB3BA>Ungültiger Konvertierungstyp!");
            sender.sendMessage("<#FFFFBA>/antiafk convert <file-to-db | db-to-file>");
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

