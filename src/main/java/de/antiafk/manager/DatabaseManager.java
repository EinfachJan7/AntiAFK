package de.antiafk.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.antiafk.AntiAFK;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class DatabaseManager {

    private final AntiAFK plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private boolean isConnected = false;
    private final Map<UUID, Long> sessionStartTime = new HashMap<>();
    private final Map<UUID, Long> totalSessionAFKTime = new HashMap<>();

    public DatabaseManager(AntiAFK plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Initialisiert Datenbankverbindung
     */
    public boolean initialize() {
        try {
            String host = configManager.getDatabaseHost();
            int port = configManager.getDatabasePort();
            String database = configManager.getDatabaseName();
            String username = configManager.getDatabaseUser();
            String password = configManager.getDatabasePassword();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
            
            // Teste Verbindung
            try (Connection conn = dataSource.getConnection()) {
                plugin.getLogger().info("Datenbankverbindung erfolgreich hergestellt!");
            }

            createTables();
            isConnected = true;
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Verbinden mit der Datenbank: " + e.getMessage());
            e.printStackTrace();
            isConnected = false;
            return false;
        }
    }

    /**
     * Erstellt die notwendigen Tabellen
     */
    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS afk_statistics (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36) UNIQUE NOT NULL," +
                    "player_name VARCHAR(16) NOT NULL," +
                    "total_afk_time BIGINT DEFAULT 0," +
                    "afk_count INT DEFAULT 0," +
                    "last_afk_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "first_recorded TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
                plugin.getLogger().info("AFK-Statistik-Tabelle erstellt/überprüft");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Erstellen der Tabelle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Startet die AFK-Session für einen Spieler
     */
    public void startAFKSession(Player player) {
        sessionStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        totalSessionAFKTime.put(player.getUniqueId(), 0L);
    }

    /**
     * Beendet die AFK-Session und speichert die AFK-Zeit in der Datenbank
     */
    public void endAFKSession(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!sessionStartTime.containsKey(uuid)) {
            return;
        }

        long sessionAFKTime = (System.currentTimeMillis() - sessionStartTime.get(uuid)) / 1000;
        sessionStartTime.remove(uuid);
        totalSessionAFKTime.remove(uuid);

        if (sessionAFKTime <= 0) {
            return;
        }

        // Speichere in DB
        addAFKTime(player, sessionAFKTime);
    }

    /**
     * Addiert AFK-Zeit für einen Spieler in die Datenbank
     */
    private void addAFKTime(Player player, long afkTimeSeconds) {
        if (!isConnected) {
            plugin.getLogger().warning("Datenbankverbindung nicht hergestellt - AFK-Zeit konnte nicht gespeichert werden");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String upsertSQL = "INSERT INTO afk_statistics (uuid, player_name, total_afk_time, afk_count) " +
                    "VALUES (?, ?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_afk_time = total_afk_time + ?," +
                    "afk_count = afk_count + 1," +
                    "last_afk_date = CURRENT_TIMESTAMP";

            try (PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setLong(3, afkTimeSeconds);
                stmt.setLong(4, afkTimeSeconds);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Speichern von AFK-Zeit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Öffentliche Methode um AFK-Session zu speichern (wird vom AFKManager aufgerufen)
     */
    public void addAFKSession(Player player, long afkTimeSeconds) {
        addAFKTime(player, afkTimeSeconds);
    }

    /**
     * Gibt die gesamte AFK-Zeit eines Spielers in Sekunden zurück
     */
    public long getTotalAFKTime(UUID uuid) {
        if (!isConnected) {
            return 0;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT total_afk_time FROM afk_statistics WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("total_afk_time");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen von AFK-Zeit: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Gibt die AFK-Statistiken eines Spielers zurück
     */
    public Map<String, Object> getPlayerStats(UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        
        if (!isConnected) {
            return stats;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT uuid, player_name, total_afk_time, afk_count, last_afk_date, first_recorded " +
                    "FROM afk_statistics WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("uuid", rs.getString("uuid"));
                        stats.put("playerName", rs.getString("player_name"));
                        stats.put("totalAFKTime", rs.getLong("total_afk_time"));
                        stats.put("afkCount", rs.getInt("afk_count"));
                        stats.put("lastAFKDate", rs.getTimestamp("last_afk_date"));
                        stats.put("firstRecorded", rs.getTimestamp("first_recorded"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen von Spielerstatistiken: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Gibt die Top-10 AFK-Spieler zurück
     */
    public List<Map<String, Object>> getTopAFKPlayers(int limit) {
        List<Map<String, Object>> topPlayers = new ArrayList<>();
        
        if (!isConnected) {
            return topPlayers;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT uuid, player_name, total_afk_time, afk_count " +
                    "FROM afk_statistics ORDER BY total_afk_time DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> playerData = new HashMap<>();
                        playerData.put("uuid", rs.getString("uuid"));
                        playerData.put("playerName", rs.getString("player_name"));
                        playerData.put("totalAFKTime", rs.getLong("total_afk_time"));
                        playerData.put("afkCount", rs.getInt("afk_count"));
                        topPlayers.add(playerData);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen der Top-AFK-Spieler: " + e.getMessage());
            e.printStackTrace();
        }

        return topPlayers;
    }

    /**
     * Schließt die Datenbankverbindung
     */
    public void closeConnection() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                isConnected = false;
                plugin.getLogger().info("Datenbankverbindung geschlossen");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Schließen der Datenbankverbindung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reconnected zur Datenbank
     */
    public void reconnect() {
        closeConnection();
        initialize();
    }

    /**
     * Gibt an ob Verbindung aktiv ist
     */
    public boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    /**
     * Formatiert Sekunden in ein lesbares Format
     */
    public static String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        result.append(secs).append("s");

        return result.toString();
    }
}
