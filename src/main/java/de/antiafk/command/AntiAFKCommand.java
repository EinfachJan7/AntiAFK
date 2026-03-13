package de.antiafk.command;

import de.antiafk.AntiAFK;
import de.antiafk.manager.AFKManager;
import de.antiafk.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AntiAFKCommand implements CommandExecutor {

    private final AntiAFK plugin;
    private final AFKManager afkManager;
    private final ConfigManager configManager;

    public AntiAFKCommand(AntiAFK plugin, AFKManager afkManager, ConfigManager configManager) {
        this.plugin = plugin;
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
        } else {
            sender.sendMessage(configManager.getHelpStatus());
            sender.sendMessage(configManager.getHelpReload());
            sender.sendMessage("<#FFFFBA>/antiafk check <Spieler> <#90EE90>- Prüft wie lange der Spieler AFK ist");
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
        
        if (afkTime == -1) {
            sender.sendMessage("<#BAE1FF>ℹ Spieler <#E0BBE4>" + targetPlayer.getName() + " <#BAE1FF>ist nicht AFK");
            return;
        }

        long seconds = afkTime % 60;
        long minutes = (afkTime / 60) % 60;
        long hours = afkTime / 3600;

        StringBuilder timeStr = new StringBuilder();
        if (hours > 0) {
            timeStr.append(hours).append("h ");
        }
        if (minutes > 0) {
            timeStr.append(minutes).append("m ");
        }
        timeStr.append(seconds).append("s");

        sender.sendMessage("<#BAFFC9>✓ <#E0BBE4>" + targetPlayer.getName() + " <#BAFFC9>ist seit <#FFFFBA>" + timeStr.toString() + " <#BAFFC9>AFK");
    }
}
