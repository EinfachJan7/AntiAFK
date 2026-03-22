package de.antiafk;

import org.bukkit.plugin.java.JavaPlugin;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import de.antiafk.manager.DatabaseManager;
import de.antiafk.manager.FileStorageManager;
import de.antiafk.manager.DataConverter;
import de.antiafk.placeholder.AFKPlaceholder;
import de.antiafk.listener.PlayerMoveListener;
import de.antiafk.listener.PistonListener;
import de.antiafk.command.AntiAFKCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AntiAFK extends JavaPlugin implements Listener {

    private AFKManager afkManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private FileStorageManager fileStorageManager;
    private DataConverter dataConverter;

    @Override
    public void onEnable() {
        // Speichert die config.yml falls sie nicht existiert
        saveDefaultConfig();

        // Manager initialisieren
        configManager = new ConfigManager(this);
        afkManager = new AFKManager(this, configManager);
        databaseManager = new DatabaseManager(this, configManager);
        fileStorageManager = new FileStorageManager(this, configManager);
        dataConverter = new DataConverter(this, databaseManager, fileStorageManager);

        // Starte FileStorage
        fileStorageManager.initialize();

        // Starte Datenbankverbindung wenn aktiviert
        if (configManager.isDatabaseEnabled()) {
            if (databaseManager.initialize()) {
                getLogger().info("Datenbank erfolgreich initialisiert!");
            } else {
                getLogger().warning("Fehler beim Initialisieren der Datenbank!");
            }
        }

        // Setze Manager am AFKManager
        afkManager.setDatabaseManager(databaseManager);
        afkManager.setFileStorageManager(fileStorageManager);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(afkManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PistonListener(afkManager, configManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Command registrieren
        AntiAFKCommand command = new AntiAFKCommand(this, afkManager, configManager, databaseManager, fileStorageManager, dataConverter);
        getCommand("antiafk").setExecutor(command);
        getCommand("antiafk").setTabCompleter(command);

        // PlaceholderAPI registrieren (wenn verfügbar)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AFKPlaceholder(fileStorageManager, databaseManager, configManager.isDatabaseEnabled(), afkManager, configManager).register();
            getLogger().info("PlaceholderAPI Integration aktiviert!");
        }

        // AFK Check Task starten
        afkManager.startAfkCheckTask();

        getLogger().info("AntiAFK Plugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        if (afkManager != null) {
            afkManager.stopAfkCheckTask();
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("AntiAFK Plugin wurde deaktiviert!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (afkManager != null) {
            afkManager.unregisterPlayer(event.getPlayer());
        }
    }

    public AFKManager getAFKManager() {
        return afkManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FileStorageManager getFileStorageManager() {
        return fileStorageManager;
    }

    public DataConverter getDataConverter() {
        return dataConverter;
    }
}
