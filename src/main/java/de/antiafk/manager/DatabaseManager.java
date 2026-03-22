package de.antiafk.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.antiafk.AntiAFK;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final AntiAFK plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private boolean isConnected = false;

    // Wird nicht mehr für Session-Tracking genutzt (übernimmt AFKManager),
    // bleibt aber für Kompatibilität (DataConverter etc.)
    private final Map<UUID, Long> sessionStartTime = new HashMap<>();
    private final Map<UUID, Long> totalSessionAFKTime = new HashMap<>();

    public DatabaseManager(AntiAFK plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // -------------------------------------------------------------------------
    // Verbindung
    // -------------------------------------------------------------------------

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

    public void reconnect() {
        closeConnection();
        initialize();
    }

    public boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    // -------------------------------------------------------------------------
    // Session-Tracking (Legacy – wird intern nicht mehr genutzt, bleibt für
    // eventuelle externe Aufrufe via DataConverter o.Ä.)
    // -------------------------------------------------------------------------

    public void startAFKSession(Player player) {
        sessionStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        totalSessionAFKTime.put(player.getUniqueId(), 0L);
    }

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
        addAFKSessionFinal(player, sessionAFKTime);
    }

    // -------------------------------------------------------------------------
    // Speichern
    // -------------------------------------------------------------------------

    /**
     * Inkrementelles Speichern (wird regelmäßig aufgerufen, z.B. alle 30s)
     * Erhöht NUR Zeit, nicht afk_count
     */
    public void addAFKSession(Player player, long afkTimeSeconds) {
        if (!isConnected) {
            plugin.getLogger().warning("DB nicht verbunden – AFK-Zeit konnte nicht gespeichert werden");
            return;
        }
        if (afkTimeSeconds <= 0) return;

        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO afk_statistics (uuid, player_name, total_afk_time, afk_count) " +
                    "VALUES (?, ?, ?, 0) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_afk_time = total_afk_time + VALUES(total_afk_time)," +
                    "player_name = VALUES(player_name)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setLong(3, afkTimeSeconds);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Speichern von AFK-Zeit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Finales Speichern am Session-Ende
     * Erhöht Zeit UND afk_count um 1
     */
    public void addAFKSessionFinal(Player player, long afkTimeSeconds) {
        if (!isConnected) {
            plugin.getLogger().warning("DB nicht verbunden – AFK-Session konnte nicht beendet werden");
            return;
        }
        if (afkTimeSeconds < 0) return;

        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO afk_statistics (uuid, player_name, total_afk_time, afk_count) " +
                    "VALUES (?, ?, ?, 1) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_afk_time = total_afk_time + VALUES(total_afk_time)," +
                    "afk_count = afk_count + 1," +
                    "last_afk_date = CURRENT_TIMESTAMP," +
                    "player_name = VALUES(player_name)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setLong(3, afkTimeSeconds);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Beenden der AFK-Session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setzt Spielerstatistiken zurück
     */
    public void resetPlayerStats(UUID uuid, String resetType) {
        if (!isConnected) {
            plugin.getLogger().warning("DB nicht verbunden – Stats konnten nicht zurückgesetzt werden");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = switch (resetType) {
                case "time" -> "UPDATE afk_statistics SET total_afk_time = 0 WHERE uuid = ?";
                case "count" -> "UPDATE afk_statistics SET afk_count = 0 WHERE uuid = ?";
                case "all" -> "UPDATE afk_statistics SET total_afk_time = 0, afk_count = 0 WHERE uuid = ?";
                default -> null;
            };

            if (sql != null) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Zurücksetzen der Stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Abfragen
    // -------------------------------------------------------------------------

    public long getTotalAFKTime(UUID uuid) {
        if (!isConnected) return 0;

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
        }
        return 0;
    }

    public Map<String, Object> getPlayerStats(UUID uuid) {
        Map<String, Object> stats = new HashMap<>();
        if (!isConnected) return stats;

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
        }
        return stats;
    }

    public Map<String, Object> getPlayerStatsByName(String playerName) {
        Map<String, Object> stats = new HashMap<>();
        if (!isConnected || playerName == null) return stats;

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT uuid, player_name, total_afk_time, afk_count, last_afk_date, first_recorded " +
                    "FROM afk_statistics WHERE player_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerName);
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
            plugin.getLogger().severe("Fehler beim Abrufen von Spielerstatistiken nach Name: " + e.getMessage());
        }
        return stats;
    }

    public List<Map<String, Object>> getTopAFKPlayers(int limit) {
        List<Map<String, Object>> topPlayers = new ArrayList<>();
        if (!isConnected) return topPlayers;

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
        }
        return topPlayers;
    }

    // -------------------------------------------------------------------------
    // PlaceholderAPI-Methoden
    // -------------------------------------------------------------------------

    /**
     * Gibt die gespeicherte AFK-Zeit in Sekunden zurück (als String).
     * Der Placeholder addiert die laufende Session selbst dazu.
     * FIX: Gibt rohe Sekunden zurück, kein formatiertes Format – vermeidet Parse-Fehler.
     */
    public Optional<String> getPlayerAFKTime(String playerName) {
        if (!isConnected) return Optional.empty();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT total_afk_time FROM afk_statistics WHERE player_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // FIX: Sekunden als String zurückgeben, nicht formatiert
                        return Optional.of(String.valueOf(rs.getLong("total_afk_time")));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Abrufen von AFK-Zeit für " + playerName + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getPlayerAFKCount(String playerName) {
        if (!isConnected) return Optional.empty();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT afk_count FROM afk_statistics WHERE player_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(String.valueOf(rs.getInt("afk_count")));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Abrufen von AFK-Anzahl für " + playerName + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getPlayerLastAFKDate(String playerName) {
        if (!isConnected) return Optional.empty();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT last_afk_date FROM afk_statistics WHERE player_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp("last_afk_date");
                        if (ts != null) {
                            return Optional.of(ts.toString());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Fehler beim Abrufen von letzter AFK-Zeit für " + playerName + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

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

        return result.toString().trim();
    }
}