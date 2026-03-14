package de.antiafk.listener;

import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class PlayerMoveListener implements Listener {

    private final AFKManager afkManager;
    private final ConfigManager configManager;

    public PlayerMoveListener(AFKManager afkManager, ConfigManager configManager) {
        this.afkManager = afkManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Prüfe ob Block-Position sich geändert hat
        boolean blockPosChanged = from.getBlockX() != to.getBlockX()
                              || from.getBlockY() != to.getBlockY()
                              || from.getBlockZ() != to.getBlockZ();

        if (!blockPosChanged) {
            // Nur Rotation, keine Positionsänderung → ignorieren
            return;
        }

        double distance = from.distance(to);

        // Ignoriere sehr kleine Bewegungen (< 0.1m)
        if (distance < 0.1) {
            return;
        }

        // Prüfe ob externe Kraft wirkt (Piston, Knockback, Fahrzeug, etc.)
        if (afkManager.isPlayerInExternalMovement(player)) {
            return;
        }

        // Echte Spielerbewegung erkannt
        afkManager.markPlayerInputMovement(player);
        afkManager.recordPosition(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Markiere dass Spieler externe Kraft erhielt (Knockback, Plugin-Effects, etc.)
        // Relevante Velocities sind > 0.1 block/tick
        double velocity = event.getVelocity().length();
        if (velocity > 0.1) {
            afkManager.markUnderExternalForce(player);
            afkManager.debug(player.getName() + " erhielt Velocity: " + 
                String.format("%.3f", velocity) + " blocks/tick (externe Kraft)");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        afkManager.recordPosition(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        afkManager.unregisterPlayer(player);
    }
}
