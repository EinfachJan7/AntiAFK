package de.antiafk.manager;

import de.antiafk.AntiAFK;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataConverter {

    private final AntiAFK plugin;
    private final DatabaseManager databaseManager;
    private final FileStorageManager fileStorageManager;

    public DataConverter(AntiAFK plugin, DatabaseManager databaseManager, FileStorageManager fileStorageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.fileStorageManager = fileStorageManager;
    }

    /**
     * Konvertiert Daten von Datei zu Datenbank
     */
    public void convertFileToDatabase(CommandSender sender) {
        if (!databaseManager.isConnected()) {
            sender.sendMessage("<#FFB3BA>❌ Datenbank nicht verbunden!");
            return;
        }

        try {
            sender.sendMessage("<#FFFFBA>⏳ Starte Konvertierung von Datei zu Datenbank...");
            
            Map<String, FileStorageManager.PlayerStatsData> fileData = fileStorageManager.getAllStats();
            
            if (fileData.isEmpty()) {
                sender.sendMessage("<#FFB3BA>❌ Keine Daten in der Datei vorhanden!");
                return;
            }

            int successCount = 0;
            int errorCount = 0;

            for (FileStorageManager.PlayerStatsData stats : fileData.values()) {
                try {
                    insertOrUpdateInDatabase(stats);
                    successCount++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Konvertieren von " + stats.playerName + ": " + e.getMessage());
                    errorCount++;
                }
            }

            sender.sendMessage("<#BAFFC9>✓ Konvertierung abgeschlossen!");
            sender.sendMessage("<#90EE90>" + successCount + " Spieler erfolgreich konvertiert");
            if (errorCount > 0) {
                sender.sendMessage("<#FFB3BA>" + errorCount + " Fehler bei der Konvertierung");
            }

        } catch (Exception e) {
            sender.sendMessage("<#FFB3BA>❌ Fehler bei der Konvertierung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Konvertiert Daten von Datenbank zu Datei
     */
    public void convertDatabaseToFile(CommandSender sender) {
        if (!databaseManager.isConnected()) {
            sender.sendMessage("<#FFB3BA>❌ Datenbank nicht verbunden!");
            return;
        }

        try {
            sender.sendMessage("<#FFFFBA>⏳ Starte Konvertierung von Datenbank zu Datei...");
            
            Map<String, FileStorageManager.PlayerStatsData> dbData = getAllDataFromDatabase();
            
            if (dbData.isEmpty()) {
                sender.sendMessage("<#FFB3BA>❌ Keine Daten in der Datenbank vorhanden!");
                return;
            }

            fileStorageManager.importFromDatabase(dbData);
            
            sender.sendMessage("<#BAFFC9>✓ Konvertierung abgeschlossen!");
            sender.sendMessage("<#90EE90>" + dbData.size() + " Spieler erfolgreich konvertiert");

        } catch (Exception e) {
            sender.sendMessage("<#FFB3BA>❌ Fehler bei der Konvertierung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fügt oder aktualisiert Daten in der Datenbank
     */
    private void insertOrUpdateInDatabase(FileStorageManager.PlayerStatsData stats) throws SQLException {
        try (Connection conn = databaseManager.getDataSource().getConnection()) {
            String upsertSQL = "INSERT INTO afk_statistics (uuid, player_name, total_afk_time, afk_count, last_afk_date, first_recorded) " +
                    "VALUES (?, ?, ?, ?, FROM_UNIXTIME(?), FROM_UNIXTIME(?)) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_afk_time = ?, " +
                    "afk_count = ?, " +
                    "last_afk_date = FROM_UNIXTIME(?)";

            try (PreparedStatement stmt = conn.prepareStatement(upsertSQL)) {
                stmt.setString(1, stats.uuid);
                stmt.setString(2, stats.playerName);
                stmt.setLong(3, stats.totalAFKTime);
                stmt.setInt(4, stats.afkCount);
                stmt.setLong(5, stats.lastAFKDate / 1000);
                stmt.setLong(6, stats.firstRecorded / 1000);
                stmt.setLong(7, stats.totalAFKTime);
                stmt.setInt(8, stats.afkCount);
                stmt.setLong(9, stats.lastAFKDate / 1000);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Holt alle Daten aus der Datenbank
     */
    private Map<String, FileStorageManager.PlayerStatsData> getAllDataFromDatabase() throws SQLException {
        Map<String, FileStorageManager.PlayerStatsData> data = new HashMap<>();

        try (Connection conn = databaseManager.getDataSource().getConnection()) {
            String query = "SELECT uuid, player_name, total_afk_time, afk_count, UNIX_TIMESTAMP(last_afk_date) as last_afk_date, UNIX_TIMESTAMP(first_recorded) as first_recorded FROM afk_statistics";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        FileStorageManager.PlayerStatsData stats = new FileStorageManager.PlayerStatsData();
                        stats.uuid = rs.getString("uuid");
                        stats.playerName = rs.getString("player_name");
                        stats.totalAFKTime = rs.getLong("total_afk_time");
                        stats.afkCount = rs.getInt("afk_count");
                        stats.lastAFKDate = rs.getLong("last_afk_date") * 1000;
                        stats.firstRecorded = rs.getLong("first_recorded") * 1000;
                        
                        data.put(stats.uuid, stats);
                    }
                }
            }
        }

        return data;
    }
}
