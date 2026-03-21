package de.antiafk.placeholder;

import de.antiafk.manager.FileStorageManager;
import de.antiafk.manager.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class AFKPlaceholder extends PlaceholderExpansion {

    private final FileStorageManager fileStorageManager;
    private final DatabaseManager databaseManager;
    private final boolean isDatabaseEnabled;

    public AFKPlaceholder(FileStorageManager fileStorageManager, DatabaseManager databaseManager, boolean isDatabaseEnabled) {
        this.fileStorageManager = fileStorageManager;
        this.databaseManager = databaseManager;
        this.isDatabaseEnabled = isDatabaseEnabled;
    }

    @Override
    public String getIdentifier() {
        return "antiafk";
    }

    @Override
    public String getAuthor() {
        return "Admin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        // Parse inner placeholders like %player% and %player_name%
        String parsedIdentifier = parseInnerPlaceholders(player, identifier);
        String playerName = player.getName();

        // Support for %antiafk_total_afk_time_%player% or %antiafk_total_afk_time_%player_name%
        if (parsedIdentifier.equals("total_afk_time_" + playerName)) {
            return getTotalAFKTime(playerName);
        }

        // Support for %antiafk_afk_count_%player% or %antiafk_afk_count_%player_name%
        if (parsedIdentifier.equals("afk_count_" + playerName)) {
            return getAFKCount(playerName);
        }

        // Support for %antiafk_last_afk_%player% or %antiafk_last_afk_%player_name%
        if (parsedIdentifier.equals("last_afk_" + playerName)) {
            return getLastAFKTime(playerName);
        }

        if (parsedIdentifier.startsWith("top_")) {
            return handleTopPlaceholder(parsedIdentifier);
        }

        return null;
    }

    /**
     * Parses inner placeholders like %player% and %player_name% in the identifier
     */
    private String parseInnerPlaceholders(Player player, String identifier) {
        String parsed = identifier;
        
        // Replace %player% and %player_name% with the actual player name
        parsed = parsed.replace("%player%", player.getName());
        parsed = parsed.replace("%player_name%", player.getName());
        
        // Also try to parse with PlaceholderAPI if it contains other placeholders
        if (parsed.contains("%")) {
            try {
                String result = PlaceholderAPI.setPlaceholders(player, "%" + parsed + "%");
                // Remove the wrapper % signs we added
                if (result.startsWith("%") && result.endsWith("%")) {
                    parsed = result.substring(1, result.length() - 1);
                } else {
                    parsed = result;
                }
            } catch (Exception e) {
                // If parsing fails, use the original parsed identifier
                Bukkit.getLogger().warning("Could not parse placeholder: " + parsed);
            }
        }
        
        return parsed;
    }

    private String getTotalAFKTime(String playerName) {
        try {
            if (isDatabaseEnabled && databaseManager != null) {
                return databaseManager.getPlayerAFKTime(playerName).orElse("0 Minuten");
            } else if (fileStorageManager != null) {
                return fileStorageManager.getPlayerAFKTime(playerName).orElse("0 Minuten");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error getting AFK time for " + playerName + ": " + e.getMessage());
        }
        return "0 Minuten";
    }

    private String getAFKCount(String playerName) {
        try {
            if (isDatabaseEnabled && databaseManager != null) {
                return databaseManager.getPlayerAFKCount(playerName).orElse("0");
            } else if (fileStorageManager != null) {
                return fileStorageManager.getPlayerAFKCount(playerName).orElse("0");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error getting AFK count for " + playerName + ": " + e.getMessage());
        }
        return "0";
    }

    private String getLastAFKTime(String playerName) {
        try {
            if (isDatabaseEnabled && databaseManager != null) {
                return databaseManager.getPlayerLastAFKDate(playerName).orElse("Nie");
            } else if (fileStorageManager != null) {
                return fileStorageManager.getPlayerLastAFKDate(playerName).orElse("Nie");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error getting last AFK time for " + playerName + ": " + e.getMessage());
        }
        return "Nie";
    }

    private String handleTopPlaceholder(String identifier) {
        try {
            // Beispiel: top_1_player, top_1_time, top_2_player, top_2_time
            String[] parts = identifier.split("_");
            if (parts.length < 3) {
                return null;
            }

            int position = Integer.parseInt(parts[1]) - 1; // Convert to 0-indexed
            String dataType = parts[2]; // "player" oder "time"

            if (isDatabaseEnabled && databaseManager != null) {
                List<Map<String, Object>> topPlayers = databaseManager.getTopAFKPlayers(10);
                if (position < topPlayers.size()) {
                    Map<String, Object> entry = topPlayers.get(position);
                    if (dataType.equals("player")) {
                        return (String) entry.get("playerName");
                    } else if (dataType.equals("time")) {
                        long seconds = (long) entry.get("totalAFKTime");
                        return formatTime(seconds);
                    }
                }
            } else if (fileStorageManager != null) {
                List<Map<String, Object>> topPlayers = fileStorageManager.getTopAFKPlayers(10);
                if (position < topPlayers.size()) {
                    Map<String, Object> entry = topPlayers.get(position);
                    if (dataType.equals("player")) {
                        return (String) entry.get("playerName");
                    } else if (dataType.equals("time")) {
                        long seconds = (long) entry.get("totalAFKTime");
                        return formatTime(seconds);
                    }
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException | ClassCastException e) {
            Bukkit.getLogger().warning("Error parsing top placeholder: " + e.getMessage());
        }
        return null;
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " Sekunden";
        } else if (totalSeconds < 3600) {
            long minutes = totalSeconds / 60;
            return minutes + " Minuten";
        } else if (totalSeconds < 86400) {
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            long days = totalSeconds / 86400;
            long hours = (totalSeconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }
}
