package de.antiafk.manager;

import de.antiafk.AntiAFK;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager {

    private final AntiAFK plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Location> lastValidPosition = new HashMap<>();
    private final Map<UUID, Long> afkStartTime = new HashMap<>();  // Wann wurde der Spieler AFK
    private final Map<UUID, Boolean> afkCommandExecuted = new HashMap<>();  // Wurde Befehl bereits ausgeführt?
    private BukkitTask afkCheckTask;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public AFKManager(AntiAFK plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Prüft ob Spieler in Wasser/Lava ist
     */
    private boolean isPlayerInLiquid(Player player) {
        Block block = player.getLocation().getBlock();
        Material type = block.getType();
        return type == Material.WATER || type == Material.LAVA;
    }

    /**
     * Prüft ob Spieler auf einem Piston steht
     */
    private boolean isPlayerOnPiston(Player player) {
        Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
        Material type = blockBelow.getType();
        return type == Material.PISTON || type == Material.STICKY_PISTON || 
               type == Material.PISTON_HEAD;
    }

    /**
     * Prüft ob Spieler in einem Fahrzeug ist oder Flüssigkeit/Pistons nutzt
     */
    private boolean isPlayerCheatMoving(Player player) {
        return isPlayerInLiquid(player) || 
               isPlayerOnPiston(player) || 
               player.isInsideVehicle();
    }

    /**
     * Registriert die Position eines Spielers
     */
    public void recordPosition(Player player) {
        // Ignoriere wenn Spieler in Wasser/Lava/Piston/Fahrzeug ist
        if (isPlayerCheatMoving(player)) {
            return;
        }

        // Spieler hat sich bewegt - AFK-Zustand zurücksetzen
        UUID uuid = player.getUniqueId();
        lastValidPosition.put(uuid, player.getLocation().clone());
        afkStartTime.remove(uuid);
        afkCommandExecuted.remove(uuid);
    }

    /**
     * Prüft ob Spieler sich bewegt hat
     */
    private boolean hasPlayerMoved(Player player) {
        UUID uuid = player.getUniqueId();

        if (!lastValidPosition.containsKey(uuid)) {
            recordPosition(player);
            return false;
        }

        Location lastPos = lastValidPosition.get(uuid);
        Location currentPos = player.getLocation();

        // Vergleiche X, Y, Z (ignoriere Rotation)
        double distance = lastPos.distance(currentPos);
        
        // Wenn Spieler sich bewegt hat
        if (distance > 0.5) {
            // Prüfe ob Spieler war AFK und ist jetzt aktiv
            if (afkStartTime.containsKey(uuid) && afkCommandExecuted.getOrDefault(uuid, false)) {
                executeBackCommand(player);
            }
            return true;
        }
        
        return false;
    }

    /**
     * Überprüft, ob ein Spieler AFK ist und die Timeout-Zeit erreicht hat
     */
    public boolean isAFK(Player player) {
        UUID uuid = player.getUniqueId();

        // Ignoriere Spieler die cheaten (Wasser, Pistons, Fahrzeuge)
        if (isPlayerCheatMoving(player)) {
            recordPosition(player);
            return false;
        }

        // Wenn Spieler sich bewegt hat
        if (hasPlayerMoved(player)) {
            recordPosition(player);
            return false;
        }

        // Spieler bewegt sich nicht - prüfe AFK-Zeit
        if (!afkStartTime.containsKey(uuid)) {
            afkStartTime.put(uuid, System.currentTimeMillis());
            return false;
        }

        // Prüfe ob Timeout erreicht ist
        long afkTime = (System.currentTimeMillis() - afkStartTime.get(uuid)) / 1000;
        return afkTime >= configManager.getAfkTimeout();
    }

    /**
     * Führt AFK-Befehl aus (nur einmal pro AFK-Periode)
     */
    public void executeAFKCommand(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Prüfe ob Befehl bereits ausgeführt wurde
        if (afkCommandExecuted.getOrDefault(uuid, false)) {
            return;
        }

        String command = configManager.getCommand()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        // Unterstütze MiniMessage Tags wie <red>, <bold>, etc.
        try {
            player.performCommand(command);
            afkCommandExecuted.put(uuid, true);  // Markiere als ausgeführt
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Ausführen des AFK-Befehls für " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Führt Back-Befehl aus wenn Spieler nicht mehr AFK ist
     */
    public void executeBackCommand(Player player) {
        UUID uuid = player.getUniqueId();
        
        String command = configManager.getCommandBack()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        try {
            player.performCommand(command);
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Ausführen des Back-Befehls für " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Startet die periodische AFK-Überprüfung
     */
    public void startAfkCheckTask() {
        int checkInterval = configManager.getCheckInterval() * 20; // Ticks (20 Ticks = 1 Sekunde)

        afkCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!configManager.isEnabled()) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isAFK(player)) {
                    executeAFKCommand(player);
                }
            }
        }, checkInterval, checkInterval);
    }

    /**
     * Stoppt die AFK-Überprüfung
     */
    public void stopAfkCheckTask() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
        }
    }

    /**
     * Deregistriert einen Spieler (z.B. beim Logout)
     */
    public void unregisterPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastValidPosition.remove(uuid);
        afkStartTime.remove(uuid);
        afkCommandExecuted.remove(uuid);
    }

    /**
     * Gibt die AFK-Zeit eines Spielers in Sekunden zurück (-1 falls nicht AFK)
     */
    public long getPlayerAFKTime(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!afkStartTime.containsKey(uuid)) {
            return -1;
        }

        long afkTimeMillis = System.currentTimeMillis() - afkStartTime.get(uuid);
        return afkTimeMillis / 1000;
    }
}
