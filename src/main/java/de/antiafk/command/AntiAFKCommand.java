package de.antiafk.command;

import de.antiafk.AntiAFK;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AntiAFKCommand implements CommandExecutor {

    private final AFKManager afkManager;
    private final ConfigManager configManager;

    public AntiAFKCommand(AntiAFK plugin, AFKManager afkManager, ConfigManager configManager) {
        this.afkManager = afkManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiafk.admin")) {
            sender.sendMessage(configManager.getNoPermission());
            return true;
        }

        if (args.length == 0) {
            showStatus(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("status")) {
            showStatus(sender);
        } else if (subCommand.equals("reload")) {
            reloadPlugin(sender);
        } else if (subCommand.equals("check")) {
            if (args.length < 2) {
                sender.sendMessage("<#FFB3BA>Benutzung: /antiafk check <Spieler>");
                return true;
            }
            checkPlayerAFK(sender, args[1]);
        } else if (subCommand.equals("debug")) {
            toggleDebug(sender);
        } else {
            sender.sendMessage(configManager.getHelpStatus());
            sender.sendMessage(configManager.getHelpReload());
            sender.sendMessage("<#FFFFBA>/antiafk check <Spieler> <#90EE90>- Prüft wie lange der Spieler AFK ist");
            sender.sendMessage("<#FFFFBA>/antiafk debug <#90EE90>- Toggled Debug-Modus AN/AUS");
        }

        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(configManager.getStatusMessageHeader());
        sender.sendMessage(configManager.isEnabled() ? 
            configManager.getStatusMessageEnabled() : 
            configManager.getStatusMessageDisabled());
        sender.sendMessage(configManager.getStatusMessageTimeout()
            .replace("%timeout%", String.valueOf(configManager.getAfkTimeout())));
        sender.sendMessage(configManager.getStatusMessageInterval()
            .replace("%interval%", String.valueOf(configManager.getCheckInterval())));
        sender.sendMessage(configManager.getStatusMessagePlayers()
            .replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size())));
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            configManager.reloadConfig();
            afkManager.stopAfkCheckTask();
            afkManager.startAfkCheckTask();
            sender.sendMessage(configManager.getReloadSuccessMessage());
        } catch (Exception e) {
            sender.sendMessage(configManager.getReloadErrorMessage());
            e.printStackTrace();
        }
    }

    private void checkPlayerAFK(CommandSender sender, String playerName) {
        org.bukkit.entity.Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer == null) {
            sender.sendMessage("<#FFB3BA>❌ Spieler <#E0BBE4>" + playerName + " <#FFB3BA>nicht online!");
            return;
        }

        long afkTime = afkManager.getPlayerAFKTime(targetPlayer);
        long stillTime = afkManager.getPlayerStillTime(targetPlayer);

        // Formatiere Stillstandszeit
        long stillSeconds = stillTime % 60;
        long stillMinutes = (stillTime / 60) % 60;
        long stillHours = stillTime / 3600;

        StringBuilder stillTimeStr = new StringBuilder();
        if (stillHours > 0) {
            stillTimeStr.append(stillHours).append("h ");
        }
        if (stillMinutes > 0) {
            stillTimeStr.append(stillMinutes).append("m ");
        }
        stillTimeStr.append(stillSeconds).append("s");

        // Angezeige Status
        sender.sendMessage("<#E0BBE4>━━━ Spieler Info: <#FFFFBA>" + targetPlayer.getName() + " <#E0BBE4>━━━");
        sender.sendMessage("<#BAE1FF>⏱ Stillstandszeit: <#FFFFBA>" + stillTimeStr.toString());
        
        long timeout = configManager.getAfkTimeout();
        if (stillTime >= timeout) {
            sender.sendMessage("<#FFB3BA>⚠ Status: <#FF6B6B>AFK");
        } else {
            sender.sendMessage("<#BAFFC9>✓ Status: <#90EE90>AKTIV");
        }
    }

    private void toggleDebug(CommandSender sender) {
        afkManager.toggleDebug();
        if (afkManager.isDebugEnabled()) {
            sender.sendMessage("<#BAFFC9>✓ Debug-Modus: <#90EE90>AKTIVIERT");
        } else {
            sender.sendMessage("<#FFB3BA>✗ Debug-Modus: <#FF6B6B>DEAKTIVIERT");
        }
    }
}
