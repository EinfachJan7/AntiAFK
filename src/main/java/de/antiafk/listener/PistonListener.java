package de.antiafk.listener;

import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.List;

/**
 * Überwacht Piston-Bewegungen und markiert betroffene Spieler
 * damit diese nicht als AFK-Cheater erkannt werden
 */
public class PistonListener implements Listener {

    private final AFKManager afkManager;
    private final ConfigManager configManager;

    public PistonListener(AFKManager afkManager, ConfigManager configManager) {
        this.afkManager = afkManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!configManager.isEnabled() || event.isCancelled()) {
            return;
        }

        checkPlayersOnPiston(event.getBlocks(), event.getDirection());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!configManager.isEnabled() || event.isCancelled()) {
            return;
        }

        checkPlayersOnPiston(event.getBlocks(), event.getDirection());
    }

    /**
     * Prüft ob Spieler auf betroffenen Blöcken stehen und markiert sie
     */
    private void checkPlayersOnPiston(List<Block> blocks, BlockFace direction) {
        for (Block block : blocks) {
            // Berechne neue Position des Blocks nach Piston-Bewegung
            Location newBlockLocation = block.getLocation().add(
                direction.getModX(),
                direction.getModY(),
                direction.getModZ()
            );

            // Prüfe ob Spieler auf diesem Block steht
            for (Player player : block.getWorld().getPlayers()) {
                // Spieler steht auf Block wenn Spieler.Location.Y ist ~0.3-0.5 über Block
                Location playerLoc = player.getLocation();
                int playerBlockX = playerLoc.getBlockX();
                int playerBlockY = playerLoc.getBlockY();
                int playerBlockZ = playerLoc.getBlockZ();

                int newBlockX = newBlockLocation.getBlockX();
                int newBlockY = newBlockLocation.getBlockY();
                int newBlockZ = newBlockLocation.getBlockZ();

                // Prüfe ob Spieler auf diesem Block ist (Y-1 weil Spieler über dem Block steht)
                if (playerBlockX == block.getX() && 
                    playerBlockY - 1 == block.getY() && 
                    playerBlockZ == block.getZ()) {
                    
                    afkManager.debug("Spieler " + player.getName() + " wird durch Piston verschoben");
                    
                    // Markiere Spieler als "unter Piston-Bewegung"
                    afkManager.markUnderExternalForce(player);
                }
            }
        }
    }
}
