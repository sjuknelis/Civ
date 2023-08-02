package com.simas.civ.components;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class SecureState implements PluginComponent {
    private App plugin;
    private String openMotd,closedMotd;

    static State state = State.NORMAL;

    public SecureState(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        openMotd = plugin.getConfig().getString("open-motd");
        closedMotd = plugin.getConfig().getString("closed-motd");
    }

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
                if ( state == State.NORMAL ) return;

                try {
                    if ( MultiLib.getDataStorage().get("isStaff-" + event.getUniqueId(),null).get() != null ) {
                        event.allow();
                        return;
                    }
                } catch ( InterruptedException | ExecutionException e ) {
                    e.printStackTrace();
                    event.disallow(Result.KICK_OTHER, "Server error - try again");
                    return;
                }

                if ( state == State.CLOSED ) event.disallow(Result.KICK_OTHER, ChatColor.GOLD + ChatColor.stripColor(closedMotd));
                else event.allow();
            }

            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();

                if ( player.hasPermission("civ.staff") ) return;

                if ( state == State.SPECTATOR ) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(ChatColor.GREEN + "Thank you for participating in Civ! The experiment has ended and you can now spectate the world.");
                }
            }

            @EventHandler
            public void onServerListPing(ServerListPingEvent event) {
                if ( state == State.CLOSED ) event.setMotd(closedMotd);
                else event.setMotd(openMotd);

                event.setMaxPlayers(1000);
            }
        });

        plugin.getCommand("securestate").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if ( args.length == 0 ) {
                    sender.sendMessage(ChatColor.GREEN + "Current secure state: " + state.name());
                    return true;
                }

                try {
                    state = State.valueOf(args[0].toUpperCase());
                } catch ( IllegalArgumentException e ) {
                    sender.sendMessage(ChatColor.RED + "Usage: /securestate [normal|spectator|closed]");
                    return true;
                }
                MultiLib.notify("civ:setsecurestate", state.name());

                String kickMessage;
                if ( state == State.CLOSED ) kickMessage = ChatColor.stripColor(closedMotd);
                else kickMessage = "The server state has been changed - you may attempt to relog.";

                for ( Player player : MultiLib.getAllOnlinePlayers() ) {
                    if ( ! player.hasPermission("civ.staff") ) player.kickPlayer(ChatColor.GOLD + kickMessage);
                }

                sender.sendMessage(ChatColor.GREEN + "Changed the secure state (and kicked all non-staff players).");

                return true;
            }
        });

        MultiLib.onString(plugin, "civ:setsecurestate", new Consumer<String>() {
            @Override
            public void accept(String name) {
                state = State.valueOf(name);
            }
        });
    }

    enum State {
        NORMAL,
        SPECTATOR,
        CLOSED
    };
}
