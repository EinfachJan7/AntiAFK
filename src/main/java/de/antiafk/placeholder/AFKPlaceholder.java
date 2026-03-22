package de.antiafk.placeholder;

import de.antiafk.manager.AFKManager;
import de.antiafk.manager.FileStorageManager;
import de.antiafk.manager.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AFKPlaceholder extends PlaceholderExpansion {

    private final FileStorageManager fileStorageManager;
    private final DatabaseManager databaseManager;
    private final boolean isDatabaseEnabled;
    private final AFKManager afkManager;

    public AFKPlaceholder(FileStorageManager fileStorageManager, DatabaseManager databaseManager,
                          boolean isDatabaseEnabled, AFKManager afkManager) {
        this.fileStorageManager = fileStorageManager;
        this.databaseManager = databaseManager;
        this.isDatabaseEnabled = isDatabaseEnabled;
        this.afkManager = afkManager;
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
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }

        // total_afk_time_<player>
        if (params.startsWith("total_afk_time_")) {
            String targetPlayer = params.substring("total_afk_time_".length());
            targetPlayer = resolvePlayerName(player, targetPlayer);
            if (targetPlayer == null) return "";
            return getTotalAFKTime(targetPlayer);
        }

        // afk_count_<player>
        if (params.startsWith("afk_count_")) {
            String targetPlayer = params.substring("afk_count_".length());
            targetPlayer = resolvePlayerName(player, targetPlayer);
            if (targetPlayer == null) return "";
            return getAFKCount(targetPlayer);
        }

        // last_afk_<player>
        if (params.startsWith("last_afk_")) {
            String targetPlayer = params.substring("last_afk_".length());
            targetPlayer = resolvePlayerName(player, targetPlayer);
            if (targetPlayer == null) return "";
            return getLastAFKTime(targetPlayer);
        }

        // top_1_player, top_1_time, etc.
        if (params.startsWith("top_")) {
            return handleTopPlaceholder(params);
        }

        return null;
    }

    private String resolvePlayerName(OfflinePlayer player, String target) {
        if (target.equals("player") || target.equals("player_name")) {
            if (player == null) return null;
            return player.getName();
        }
        return target;
    }

    private String getTotalAFKTime(String playerName) {
        long savedSeconds = 0;

        try {
            Optional<String> saved;
            if (isDatabaseEnabled && databaseManager != null) {
                saved = databaseManager.getPlayerAFKTime(playerName);
            } else if (fileStorageManager != null) {
                saved = fileStorageManager.getPlayerAFKTime(playerName);
            } else {
                return "0";
            }
            savedSeconds = parseFormattedTime(saved.orElse("0"));
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error getting AFK time for " + playerName + ": " + e.getMessage());
        }

        // Laufende (noch nicht gespeicherte) Session dazurechnen
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null && afkManager != null) {
            long sessionSeconds = afkManager.getCurrentSessionSeconds(onlinePlayer);
            savedSeconds += sessionSeconds;
        }

        // Gebe rohe Sekunden zurück (nicht formatiert)
        return String.valueOf(savedSeconds);
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
            String[] parts = identifier.split("_");
            if (parts.length < 3) {
                return null;
            }

            int position = Integer.parseInt(parts[1]) - 1;
            String dataType = parts[2];

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

    /**
     * Parst einen Zeitstring zurück in Sekunden.
     * DB liefert rohe Sekunden als String (z.B. "300").
     * FileStorage liefert formatierte Strings ("2h 30m", "45 Minuten", etc.).
     */
    private long parseFormattedTime(String formatted) {
        if (formatted == null || formatted.isEmpty() || formatted.equals("0")) return 0;
        try {
            // Rohe Sekunden aus DB (nur Zahl, kein Text)
            if (formatted.matches("\\d+")) {
                return Long.parseLong(formatted);
            }
            // Format: "2d 3h"
            if (formatted.contains("d") && formatted.contains("h")) {
                String[] parts = formatted.split("d ");
                long days = Long.parseLong(parts[0].trim());
                long hours = Long.parseLong(parts[1].replace("h", "").trim());
                return days * 86400 + hours * 3600;
            }
            // Format: "2h 30m"
            if (formatted.contains("h") && formatted.contains("m")) {
                String[] parts = formatted.split("h ");
                long hours = Long.parseLong(parts[0].trim());
                long minutes = Long.parseLong(parts[1].replace("m", "").trim());
                return hours * 3600 + minutes * 60;
            }
            // Format: "45 Minuten"
            if (formatted.contains("Minuten")) {
                return Long.parseLong(formatted.replace("Minuten", "").trim()) * 60;
            }
            // Format: "30 Sekunden"
            if (formatted.contains("Sekunden")) {
                return Long.parseLong(formatted.replace("Sekunden", "").trim());
            }
        } catch (NumberFormatException e) {
            Bukkit.getLogger().warning("[AntiAFK] Konnte Zeit nicht parsen: " + formatted);
        }
        return 0;
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