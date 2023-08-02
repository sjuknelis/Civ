package com.simas.civ.components;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class ChatRadius implements PluginComponent {
    private App plugin;
    private int chatRadius;

    private Map<Player, Location> playerLocations = Collections.synchronizedMap(new HashMap<Player, Location>());

    public ChatRadius(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        chatRadius = plugin.getConfig().getInt("chat-radius");
    }

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
                Player player = event.getPlayer();                
                
                if ( plugin.permissions.hasPermission(player, "civ.muted") ) {
                    event.setCancelled(true);
                    return;
                }

                for ( Iterator<Player> it = event.getRecipients().iterator(); it.hasNext(); ) {
                    Player recipient = it.next();
                    if ( playerLocations.get(player).distance(playerLocations.get(recipient)) > chatRadius ) {
                        it.remove();
                    }
                }
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    playerLocations.put(player, player.getLocation());
                }
            }
        }.runTaskTimer(plugin, 0, 10);

        plugin.getCommand("broadcast").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if ( args.length == 0 ) {
                    sender.sendMessage(ChatColor.RED + "Usage: /broadcast <message>");
                    return true;
                }

                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    player.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + String.format(
                        "<%s> %s",
                        sender.getName().toUpperCase(),
                        String.join(" ", args)
                    ));
                }

                return true;
            }
        });
    }
}
