package de.antiafk.manager;

import de.antiafk.AntiAFK;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileStorageManager {

    private final AntiAFK plugin;
    private final ConfigManager configManager;
    private final Gson gson;
    private final Path dataFolder;
    private final Path statsFile;
    private Map<String, PlayerStatsData> playerStats = new HashMap<>();

    public FileStorageManager(AntiAFK plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = plugin.getDataFolder().toPath();
        this.statsFile = dataFolder.resolve("player_stats.json");
    }

    /**
     * Initialisiert den File Storage Manager
     */
    public boolean initialize() {
        try {
            // Erstelle Ordner falls nicht vorhanden
            Files.createDirectories(dataFolder);
            
            // Lade vorhandene Daten
            loadStats();
            
            plugin.getLogger().info("File Storage Manager initialisiert!");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Initialisieren von File Storage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lädt Spielerstatistiken aus der Datei
     */
    private void loadStats() {
        try {
            if (Files.exists(statsFile)) {
                String content = Files.readString(statsFile);
                playerStats = gson.fromJson(content, new TypeToken<Map<String, PlayerStatsData>>(){}.getType());
                if (playerStats == null) {
                    playerStats = new HashMap<>();
                }
                plugin.getLogger().info("Statistiken geladen: " + playerStats.size() + " Spieler");
            } else {
                playerStats = new HashMap<>();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Laden von Statistiken: " + e.getMessage());
            playerStats = new HashMap<>();
        }
    }

    /**
     * Speichert Spielerstatistiken in die Datei
     */
    private void saveStats() {
        try {
            String json = gson.toJson(playerStats);
            Files.writeString(statsFile, json);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern von Statistiken: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Addiert AFK-Zeit für einen Spieler
     */
    public void addAFKSession(Player player, long afkTimeSeconds) {
        if (afkTimeSeconds <= 0) {
            return;
        }

        String uuid = player.getUniqueId().toString();
        PlayerStatsData stats = playerStats.getOrDefault(uuid, new PlayerStatsData());
        
        stats.uuid = uuid;
        stats.playerName = player.getName();
        stats.totalAFKTime += afkTimeSeconds;
        stats.afkCount += 1;
        stats.lastAFKDate = System.currentTimeMillis();
        
        if (stats.firstRecorded == 0) {
            stats.firstRecorded = System.currentTimeMillis();
        }

        playerStats.put(uuid, stats);
        saveStats();
    }

    /**
     * Gibt die gesamte AFK-Zeit eines Spielers zurück
     */
    public long getTotalAFKTime(UUID uuid) {
        PlayerStatsData stats = playerStats.get(uuid.toString());
        return stats != null ? stats.totalAFKTime : 0;
    }

    /**
     * Gibt die Statistiken eines Spielers zurück
     */
    public Map<String, Object> getPlayerStats(UUID uuid) {
        Map<String, Object> result = new HashMap<>();
        PlayerStatsData stats = playerStats.get(uuid.toString());
        
        if (stats != null) {
            result.put("uuid", stats.uuid);
            result.put("playerName", stats.playerName);
            result.put("totalAFKTime", stats.totalAFKTime);
            result.put("afkCount", stats.afkCount);
            result.put("lastAFKDate", new Date(stats.lastAFKDate));
            result.put("firstRecorded", new Date(stats.firstRecorded));
        }
        
        return result;
    }

    /**
     * Gibt die Top-AFK-Spieler zurück
     */
    public List<Map<String, Object>> getTopAFKPlayers(int limit) {
        List<Map<String, Object>> topPlayers = new ArrayList<>();
        
        playerStats.values().stream()
            .sorted((a, b) -> Long.compare(b.totalAFKTime, a.totalAFKTime))
            .limit(limit)
            .forEach(stats -> {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("uuid", stats.uuid);
                playerData.put("playerName", stats.playerName);
                playerData.put("totalAFKTime", stats.totalAFKTime);
                playerData.put("afkCount", stats.afkCount);
                topPlayers.add(playerData);
            });
        
        return topPlayers;
    }

    /**
     * Exportiert alle Daten für Datenbankkonvertierung
     */
    public Map<String, PlayerStatsData> getAllStats() {
        return new HashMap<>(playerStats);
    }

    /**
     * Importiert Daten aus der Datenbank
     */
    public void importFromDatabase(Map<String, PlayerStatsData> data) {
        playerStats = new HashMap<>(data);
        saveStats();
        plugin.getLogger().info("Daten von Datenbank importiert: " + data.size() + " Spieler");
    }

    /**
     * Gibt den Pfad der Datenspeicherdatei zurück
     */
    public Path getStatsFile() {
        return statsFile;
    }

    /**
     * Datenklasse für Spielerstatistiken
     */
    public static class PlayerStatsData {
        public String uuid;
        public String playerName;
        public long totalAFKTime = 0;
        public int afkCount = 0;
        public long lastAFKDate = 0;
        public long firstRecorded = 0;
    }
}
