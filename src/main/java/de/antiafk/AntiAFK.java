package de.antiafk;

import org.bukkit.plugin.java.JavaPlugin;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import de.antiafk.listener.PlayerMoveListener;
import de.antiafk.listener.PistonListener;
import de.antiafk.command.AntiAFKCommand;

public class AntiAFK extends JavaPlugin {

    private AFKManager afkManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Speichert die config.yml falls sie nicht existiert
        saveDefaultConfig();

        // Manager initialisieren
        configManager = new ConfigManager(this);
        afkManager = new AFKManager(this, configManager);

        // Listener registrieren
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(afkManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PistonListener(afkManager, configManager), this);

        // Command registrieren
        AntiAFKCommand command = new AntiAFKCommand(this, afkManager, configManager);
        getCommand("antiafk").setExecutor(command);

        // AFK Check Task starten
        afkManager.startAfkCheckTask();

        getLogger().info("AntiAFK Plugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        if (afkManager != null) {
            afkManager.stopAfkCheckTask();
        }
        getLogger().info("AntiAFK Plugin wurde deaktiviert!");
    }

    public AFKManager getAFKManager() {
        return afkManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
