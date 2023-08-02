package com.simas.civ.components;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class PlayerList implements PluginComponent {
    private App plugin;

    public PlayerList(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {}

    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int livingPlayers = 0;
                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    if ( ! player.hasPermission("civ.staff") ) livingPlayers++;
                }

                for ( Player player : MultiLib.getLocalOnlinePlayers() ) {
                    player.setPlayerListHeaderFooter(
                        ChatColor.GOLD + ChatColor.BOLD.toString() + "WELCOME TO CIV",
                        ChatColor.AQUA + "Survivors online: " + livingPlayers
                    );
                }
            }
        }.runTaskTimer(plugin, 0, 20);

        plugin.getCommand("islandcounts").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                int[] islandCounts = new int[4];
                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    if ( ! player.hasPermission("civ.staff") ) {
                        islandCounts[Integer.parseInt(MultiLib.getPersistentData(player, "island"))]++;
                    }
                }

                List<String> islandNames = plugin.getConfig().getStringList("island-names");

                sender.sendMessage(ChatColor.GOLD + "--- Survivors by Island ---");
                for ( int i = 0; i < 4; i++ ) {
                    sender.sendMessage(plugin.islandColors[i] + islandNames.get(i) + " Island: " + ChatColor.RESET + islandCounts[i]);
                }

                return true;
            }
        });
    }
}
