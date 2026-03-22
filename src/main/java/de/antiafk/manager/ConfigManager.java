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

    public String getStatusMessageStatsUpdateInterval() {
        return config.getString("status-message-stats-update-interval", "§b▶ Stats-Update Interval: §6%stats-update-interval% Sekunden");
    }

    public String getStatusMessagePlayers() {
        return config.getString("status-message-players", "§eOnline Spieler: §7%players%");
    }

    public String getStatusMessagePlaceholderSeconds() {
        return config.getString("status-message-placeholder-seconds",
            "§ePlaceholder: Sekunden anzeigen: §7%placeholder-show-seconds%");
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

    public String getHelpCheck() {
        return config.getString("help-check", "§e/antiafk check <Spieler> §7- Prüft AFK-Zeit");
    }

    public String getHelpStats() {
        return config.getString("help-stats", "§e/antiafk stats <Spieler> §7- Zeigt AFK-Statistiken");
    }

    public String getHelpReset() {
        return config.getString("help-reset", "§e/antiafk reset <Spieler> <time|count|all> §7- Setzt Stats zurück");
    }

    public String getNoPermission() {
        return config.getString("no-permission", "§cKeine Berechtigung!");
    }

    // Stats Messages
    public String getStatsMessageHeader() {
        return config.getString("stats-message-header", "<#E0BBE4>=== %player% AFK-Statistiken ===");
    }

    public String getStatsMessageTotalTime() {
        return config.getString("stats-message-total-time", "<#BAFFC9>Gesamt AFK-Zeit: <#FFFFBA>%time%");
    }

    public String getStatsMessageAverageAfkTime() {
        return config.getString("stats-message-average-afk-time",
            "<#BAFFC9>Durchschnittliche AFK-Zeit (pro Session): <#FFFFBA>%avg%");
    }

    public String getStatsMessageAverageAfkEmpty() {
        return config.getString("stats-message-average-afk-empty", "<#808080>—");
    }

    public String getStatsMessageAfkCount() {
        return config.getString("stats-message-afk-count", "<#BAFFC9>AFK-Vorkommnisse: <#FFFFBA>%count%");
    }

    public String getStatsMessageLastAfk() {
        return config.getString("stats-message-last-afk", "<#BAFFC9>Letzter AFK: <#FFFFBA>%date%");
    }

    public String getStatsMessageFirstRecorded() {
        return config.getString("stats-message-first-recorded", "<#BAFFC9>Erstes Aufzeichnen: <#FFFFBA>%date%");
    }

    public String getStatsMessageNotFound() {
        return config.getString("stats-message-not-found", "<#FFB3BA>❌ Spieler <#E0BBE4>%player% <#FFB3BA>nicht gefunden!");
    }

    // Reset Messages
    public String getResetSuccessTime() {
        return config.getString("reset-success-time", "<#BAFFC9>✓ AFK-Zeit von <#E0BBE4>%player% <#BAFFC9>zurückgesetzt!");
    }

    public String getResetSuccessCount() {
        return config.getString("reset-success-count", "<#BAFFC9>✓ AFK-Vorkommnisse von <#E0BBE4>%player% <#BAFFC9>zurückgesetzt!");
    }

    public String getResetSuccessAll() {
        return config.getString("reset-success-all", "<#BAFFC9>✓ Alle Stats von <#E0BBE4>%player% <#BAFFC9>zurückgesetzt!");
    }

    public String getResetInvalidOption() {
        return config.getString("reset-invalid-option", "<#FFB3BA>❌ Ungültige Option: <#E0BBE4>%option% <#FFB3BA>Nutze: time|count|all");
    }

    public String getResetPlayerNotFound() {
        return config.getString("reset-player-not-found", "<#FFB3BA>❌ Spieler <#E0BBE4>%player% <#FFB3BA>nicht gefunden!");
    }

    // Placeholder Settings
    public boolean isPlaceholderShowSeconds() {
        return config.getBoolean("placeholder-show-seconds", false);
    }

    public int getStatsUpdateInterval() {
        return config.getInt("stats-update-interval", 60);
    }

    // Database Settings
    public String getDatabaseHost() {
        return config.getString("database.host", "localhost");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return config.getString("database.name", "antiafk");
    }

    public String getDatabaseUser() {
        return config.getString("database.user", "root");
    }

    public String getDatabasePassword() {
        return config.getString("database.password", "");
    }

    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enabled", false);
    }
}
