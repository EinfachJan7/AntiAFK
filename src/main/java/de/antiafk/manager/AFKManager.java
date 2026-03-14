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
    private final Map<UUID, Long> afkStartTime = new HashMap<>();
    private final Map<UUID, Long> lastMovementTime = new HashMap<>();
    private final Map<UUID, Boolean> afkCommandExecuted = new HashMap<>();
    private final Map<UUID, Long> externalForceTime = new HashMap<>();
    private final Map<UUID, Location> lastLocationSnapshot = new HashMap<>();
    private static final long EXTERNAL_FORCE_COOLDOWN = 1000;
    private BukkitTask afkCheckTask;
    private BukkitTask tickPollingTask;
    private boolean debugMode = false;

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
        boolean result = type == Material.WATER || type == Material.LAVA;
        if (result) {
            debug("Spieler " + player.getName() + " ist in Liquid: " + type);
        }
        return result;
    }

    /**
     * Prüft ob Spieler auf einem Piston steht oder von extensiven Piston-Strukturen umgeben ist
     */
    private boolean isPlayerOnPiston(Player player) {
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Erweiterte Suche: Prüfbereich 3x3x3 um den Spieler
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = player.getWorld().getBlockAt(x + dx, y + dy, z + dz);
                    if (isPistonBlock(block)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Prüft ob Spieler durch externe Kräfte bewegt wurde (ohne Spieler-Input)
     */
    private boolean isPlayerMovedByExternalForce(Player player, Location from, Location to) {
        // Spieler in Fahrzeug → externe Bewegung
        if (player.isInsideVehicle()) {
            return true;
        }

        // Nur vertikale Änderung → Gravitation oder Piston
        boolean onlyVertical = from.getBlockX() == to.getBlockX()
                            && from.getBlockZ() == to.getBlockZ();
        return onlyVertical;
    }

    /**
     * Markiert dass Spieler echte Input-Bewegung ausgeführt hat (WASD, Springen, etc.)
     * Löst den Back-Command aus wenn Spieler vorher AFK war.
     */
    public void markPlayerInputMovement(Player player) {
        UUID uuid = player.getUniqueId();
        lastMovementTime.put(uuid, System.currentTimeMillis());

        // Back-Command auslösen wenn Spieler AFK war und sich jetzt bewegt
        // WICHTIG: Prüfung VOR dem Löschen von afkCommandExecuted
        if (afkCommandExecuted.getOrDefault(uuid, false)) {
            debug("Spieler " + player.getName() + " war AFK und bewegt sich - Back-Command wird ausgelöst");
            executeBackCommand(player);
        }

        // AFK-Status zurücksetzen
        afkStartTime.remove(uuid);
        afkCommandExecuted.remove(uuid);
        debug("Echte Spielerbewegung für " + player.getName() + " - AFK-Status RESETTET");
    }

    /**
     * Prüft ob ein Block ein Piston ist oder ein Block, der von Pistons bewegt wird (EXTENDED oder RETRACTED)
     */
    private boolean isPistonBlock(Block block) {
        Material type = block.getType();
        return type == Material.PISTON || type == Material.STICKY_PISTON ||
               type == Material.PISTON_HEAD ||
               type == Material.SLIME_BLOCK ||  // Slime blocks werden oft mit Pistons bewegt
               type == Material.HONEY_BLOCK;    // Honey blocks auch
    }

    /**
     * Prüft ob Spieler unter externe Kraft steht (Knockback, Velocity, etc.)
     */
    private boolean isUnderExternalForce(Player player) {
        UUID uuid = player.getUniqueId();
        if (!externalForceTime.containsKey(uuid)) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - externalForceTime.get(uuid);
        if (elapsed > EXTERNAL_FORCE_COOLDOWN) {
            externalForceTime.remove(uuid);
            return false;
        }

        debug(player.getName() + " unter externe Kraft (Knockback/Velocity) - Cooldown: " + elapsed + "ms");
        return true;
    }

    /**
     * Markiert dass Spieler externe Kraft (Knockback) erhalten hat
     */
    public void markUnderExternalForce(Player player) {
        externalForceTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Prüft ob Spieler in Wasser/Lava oder auf Pistons ist (externe Bewegungskräfte)
     */
    public boolean isPlayerInExternalMovement(Player player) {
        boolean inLiquid = isPlayerInLiquid(player);
        boolean onPiston = isPlayerOnPiston(player);
        boolean inVehicle = player.isInsideVehicle();
        boolean underForce = isUnderExternalForce(player);

        boolean isExternal = inLiquid || onPiston || inVehicle || underForce;

        if (isExternal) {
            String reason = "";
            if (inLiquid) reason += "[LIQUID] ";
            if (onPiston) reason += "[PISTON] ";
            if (inVehicle) reason += "[VEHICLE] ";
            if (underForce) reason += "[VELOCITY] ";
            debug("External Movement: " + player.getName() + " " + reason);
        }

        return isExternal;
    }

    /**
     * Registriert die aktuelle Position eines Spielers
     */
    public void recordPosition(Player player) {
        UUID uuid = player.getUniqueId();
        lastValidPosition.put(uuid, player.getLocation().clone());
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
            return true;
        }

        return false;
    }

    /**
     * Überprüft, ob ein Spieler AFK ist und die Timeout-Zeit erreicht hat (HART - keine Cheats möglich)
     */
    public boolean isAFK(Player player) {
        UUID uuid = player.getUniqueId();

        // Wenn Spieler sich bewegt hat
        if (hasPlayerMoved(player)) {
            recordPosition(player);
            return false;
        }

        // Spieler bewegt sich nicht - prüfe AFK-Zeit
        if (!afkStartTime.containsKey(uuid)) {
            afkStartTime.put(uuid, System.currentTimeMillis());
            debug("Stillstand begonnen für " + player.getName());
            return false;
        }

        // Prüfe ob Timeout erreicht ist
        long afkTime = (System.currentTimeMillis() - afkStartTime.get(uuid)) / 1000;
        long timeout = configManager.getAfkTimeout();

        if (afkTime >= timeout) {
            debug("PLAYER AFK! " + player.getName() +
                " - Stillstandszeit: " + afkTime + "s >= " + timeout + "s");
            return true;
        }

        // Debug: Zeige Fortschritt an (alle 30s)
        if (afkTime % 30 == 0 && afkTime > 0) {
            debug(player.getName() +
                " immer noch still: " + afkTime + "s / " + timeout + "s");
        }

        return false;
    }

    /**
     * Führt AFK-Befehl aus (nur einmal pro AFK-Periode)
     */
    public void executeAFKCommand(Player player) {
        UUID uuid = player.getUniqueId();

        // Prüfe ob Befehl bereits ausgeführt wurde
        if (afkCommandExecuted.getOrDefault(uuid, false)) {
            debug("AFK-Command für " + player.getName() + " wurde bereits ausgeführt (Skip)");
            return;
        }

        String command = configManager.getCommand()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        // Entferne Slash am Anfang falls vorhanden
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Führe Command vom Spieler aus
        try {
            debug("Führe AFK-Command aus für " + player.getName() + ": " + command);
            player.performCommand(command);
            afkCommandExecuted.put(uuid, true);  // Markiere als ausgeführt
            debug("AFK-Command erfolgreich ausgelöst für " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Ausführen des AFK-Befehls für " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Führt Back-Befehl aus wenn Spieler nicht mehr AFK ist.
     * Wird nur aufgerufen wenn afkCommandExecuted == true (Spieler war wirklich AFK).
     */
    public void executeBackCommand(Player player) {
        String command = configManager.getCommandBack()
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        if (command.isEmpty()) {
            debug("Back-Command ist leer - wird übersprungen");
            return;
        }

        // Entferne Slash am Anfang falls vorhanden
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Führe Command vom Spieler aus
        try {
            debug("Führe Back-Command aus für " + player.getName() + ": " + command);
            player.performCommand(command);
            debug("Back-Command erfolgreich ausgelöst für " + player.getName());
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

        // Starte auch Tick-Polling für passive Piston-Erkennung
        startTickPollingTask();
    }

    /**
     * Tick-Polling Task - Detektiert passive Bewegungen (Pistons, Loren, etc.)
     * Läuft jeden Tick und prüft ob Position sich ohne Spieler-Input geändert hat
     */
    private void startTickPollingTask() {
        tickPollingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!configManager.isEnabled()) {
                return;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Location current = player.getLocation().clone();
                Location last = lastLocationSnapshot.getOrDefault(uuid, current.clone());

                // Prüfe ob Block-Position sich geändert hat
                boolean blockPosChanged = current.getBlockX() != last.getBlockX()
                                       || current.getBlockY() != last.getBlockY()
                                       || current.getBlockZ() != last.getBlockZ();

                if (blockPosChanged) {
                    // Position hat sich geändert - war es echte Spielerbewegung?
                    if (isPlayerMovedByExternalForce(player, last, current)) {
                        // Passive Bewegung erkannt (Piston, Lore, etc.) - markiere mit externer Kraft
                        markUnderExternalForce(player);
                    }
                }

                lastLocationSnapshot.put(uuid, current);
            }
        }, 0L, 1L); // Jeden Tick
    }

    /**
     * Stoppt die AFK-Überprüfung
     */
    public void stopAfkCheckTask() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
        }
        if (tickPollingTask != null) {
            tickPollingTask.cancel();
        }
    }

    /**
     * Deregistriert einen Spieler (z.B. beim Logout)
     */
    public void unregisterPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastValidPosition.remove(uuid);
        afkStartTime.remove(uuid);
        lastMovementTime.remove(uuid);
        afkCommandExecuted.remove(uuid);
        externalForceTime.remove(uuid);
        lastLocationSnapshot.remove(uuid);
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

    /**
     * Gibt die Zeit zurück, wie lange ein Spieler stillsteht (nicht bewegt) in Sekunden
     */
    public long getPlayerStillTime(Player player) {
        UUID uuid = player.getUniqueId();

        if (!lastMovementTime.containsKey(uuid)) {
            return 0;
        }

        long stillTimeMillis = System.currentTimeMillis() - lastMovementTime.get(uuid);
        return stillTimeMillis / 1000;
    }

    /**
     * Toggelt Debug-Modus an/aus
     */
    public void toggleDebug() {
        debugMode = !debugMode;
    }

    /**
     * Gibt an ob Debug-Modus aktiviert ist
     */
    public boolean isDebugEnabled() {
        return debugMode;
    }

    /**
     * Gibt Debug-Nachricht aus wenn Debug aktiviert ist
     */
    public void debug(String message) {
        if (debugMode) {
            System.out.println("[AntiAFK] [DEBUG] " + message);
        }
    }
}