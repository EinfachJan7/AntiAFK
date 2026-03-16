package de.antiafk.manager;

import de.antiafk.AntiAFK;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final AntiAFK plugin;
    private FileConfiguration config;

    public ConfigManager(AntiAFK plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        // Regeneriere Config falls sie fehlt
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public int getAfkTimeout() {
        return config.getInt("afk-timeout", 300);
    }

    public int getCheckInterval() {
        return config.getInt("check-interval", 20);
    }

    public String getCommand() {
        return config.getString("command", "say %player% was AFK");
    }

    public String getCommandBack() {
        return config.getString("command-back", "say %player% is back");
    }

    // Status Messages
    public String getStatusMessageHeader() {
        return config.getString("status-message-header", "§6=== AntiAFK Status ===");
    }

    public String getStatusMessageEnabled() {
        return config.getString("status-message-enabled", "§eStatus: §aAKTIV");
    }

    public String getStatusMessageDisabled() {
        return config.getString("status-message-disabled", "§eStatus: §cDEAKTIV");
    }

    public String getStatusMessageTimeout() {
        return config.getString("status-message-timeout", "§eAFK-Timeout: §7%timeout% Sekunden");
    }

    public String getStatusMessageInterval() {
        return config.getString("status-message-interval", "§eCheck-Interval: §7%interval% Sekunden");
    }

    public String getStatusMessagePlayers() {
        return config.getString("status-message-players", "§eOnline Spieler: §7%players%");
    }

    // Reload Messages
    public String getReloadSuccessMessage() {
        return config.getString("reload-success", "§aAntiAFK Config reloaded!");
    }

    public String getReloadErrorMessage() {
        return config.getString("reload-error", "§cFehler beim Reload!");
    }

    // Command Messages
    public String getHelpStatus() {
        return config.getString("help-status", "§e/antiafk §7- Zeigt Status");
    }

    public String getHelpReload() {
        return config.getString("help-reload", "§e/antiafk reload §7- Reloaded Config");
    }

    public String getNoPermission() {
        return config.getString("no-permission", "§cKeine Berechtigung!");
    }
}
