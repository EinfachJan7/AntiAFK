package de.antiafk.placeholder;

import de.antiafk.manager.FileStorageManager;
import de.antiafk.manager.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
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
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }

        // total_afk_time_<player>
        if (params.startsWith("total_afk_time_")) {
            String targetPlayer = params.substring("total_afk_time_".length());
            // Wenn "player" oder "player_name" → benutze den aktuellen Spieler
            if (targetPlayer.equals("player") || targetPlayer.equals("player_name")) {
                targetPlayer = player.getName();
            }
            return getTotalAFKTime(targetPlayer);
        }

        // afk_count_<player>
        if (params.startsWith("afk_count_")) {
            String targetPlayer = params.substring("afk_count_".length());
            // Wenn "player" oder "player_name" → benutze den aktuellen Spieler
            if (targetPlayer.equals("player") || targetPlayer.equals("player_name")) {
                targetPlayer = player.getName();
            }
            return getAFKCount(targetPlayer);
        }

        // last_afk_<player>
        if (params.startsWith("last_afk_")) {
            String targetPlayer = params.substring("last_afk_".length());
            // Wenn "player" oder "player_name" → benutze den aktuellen Spieler
            if (targetPlayer.equals("player") || targetPlayer.equals("player_name")) {
                targetPlayer = player.getName();
            }
            return getLastAFKTime(targetPlayer);
        }

        // top_1_player, top_1_time, etc
        if (params.startsWith("top_")) {
            return handleTopPlaceholder(params);
        }

        return null;
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
