package de.antiafk.listener;

import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerMoveListener implements Listener {

    private final AFKManager afkManager;
    private final ConfigManager configManager;

    public PlayerMoveListener(AFKManager afkManager, ConfigManager configManager) {
        this.afkManager = afkManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Trample nur echte Bewegungen (ignoriere kleine Abweichungen)
        if (event.getFrom().distance(event.getTo()) > 0.1) {
            afkManager.recordPosition(player);
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
