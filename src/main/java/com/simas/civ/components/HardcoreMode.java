package com.simas.civ.components;

import java.util.concurrent.ExecutionException;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import com.github.puregero.multilib.DataStorageImpl;
import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class HardcoreMode implements PluginComponent {
    private App plugin;

    public HardcoreMode(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {}

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onPlayerDeath(PlayerDeathEvent event) {
                Player player = event.getEntity();

                if ( player.hasPermission("civ.staff") ) return;

                MultiLib.getDataStorage().set("deathMessage-" + player.getUniqueId().toString(), event.getDeathMessage());

                player.kickPlayer(getKickMessage(event.getDeathMessage()));
            }

            @EventHandler
            public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
                if ( SecureState.state != SecureState.State.NORMAL ) return;

                DataStorageImpl store = MultiLib.getDataStorage();

                try {
                    if ( plugin.permissions.hasPermissionThrows(event.getUniqueId(), "civ.staff") ) return;
                } catch ( InterruptedException | ExecutionException e ) {
                    e.printStackTrace();
                    event.disallow(Result.KICK_OTHER, "Server error - try again");
                    return;
                }

                String deathMessage;
                try {
                    deathMessage = store.get("deathMessage-" + event.getUniqueId(), null).get();
                } catch ( InterruptedException | ExecutionException e ) {
                    e.printStackTrace();
                    event.disallow(Result.KICK_OTHER, "Server error - try again");
                    return;
                }

                if ( deathMessage != null ) {
                    boolean shouldRevive;
                    try {
                        shouldRevive = store.get("shouldRevive-" + event.getName()).get() != null;
                    } catch ( InterruptedException | ExecutionException e ) {
                        e.printStackTrace();
                        event.disallow(Result.KICK_OTHER, "Server error - try again");
                        return;
                    }

                    if ( shouldRevive ) {
                        store.set("deathMessage-" + event.getUniqueId(), null);
                        event.allow();
                    } else {
                        event.disallow(Result.KICK_OTHER, getKickMessage(deathMessage));
                    }
                } else {
                    event.allow();
                }
            }

            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                event.setJoinMessage(null);

                Player player = event.getPlayer();

                player.getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);

                if ( player.hasPermission("civ.staff") ) return;

                try {
                    if ( MultiLib.getDataStorage().get("shouldRevive-" + player.getName()).get() != null ) {
                        MultiLib.getDataStorage().set("shouldRevive-" + player.getName(), null);
                        player.sendMessage(ChatColor.GREEN + "Welcome back to the server! You have been revived.");
                    }
                } catch ( InterruptedException | ExecutionException e ) {
                    e.printStackTrace();
                }
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                event.setQuitMessage(null);
            }
        });

        plugin.getCommand("revive").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if ( args.length != 1 ) {
                    sender.sendMessage(ChatColor.RED + "Usage: /revive <player>");
                    return true;
                }

                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    if ( player.getName().equals(args[0]) ) {
                        sender.sendMessage(ChatColor.RED + "That player is already alive");
                        return true;
                    }
                }

                MultiLib.getDataStorage().set("shouldRevive-" + args[0], "true");

                sender.sendMessage(ChatColor.GREEN + "Player " + args[0] + " will be revived when they attempt to rejoin.");

                return true;
            }
        });
    }

    private String getKickMessage(String deathMessage) {
        return String.format(
            ChatColor.GOLD + ChatColor.ITALIC.toString() + "%s\n\n" + ChatColor.RESET + ChatColor.GOLD + "You have died! You will be able to rejoin as a spectator to see the world again after the event ends.\nThank you for participating in Civ!",
            deathMessage
        );
    }
}
