package com.simas.civ.components;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class IslandGuard implements PluginComponent {
    private App plugin;
    private boolean enabled;
    private int islandOrigin, islandSize, islandSpawnSize;

    public IslandGuard(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("island-guard-enabled");

        islandOrigin = plugin.getConfig().getInt("island-origin");
        islandSize = plugin.getConfig().getInt("island-size");
        islandSpawnSize = plugin.getConfig().getInt("island-spawn-size");
    }

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onPlayerMove(PlayerMoveEvent event) {
                if ( ! enabled ) return;

                Player player = event.getPlayer();

                if ( player.hasPermission("civ.staff") || player.getGameMode() != GameMode.SURVIVAL ) return;

                if ( outsideIsland(event.getTo()) ) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Do not try to leave the island!");
                }
            }

            @EventHandler
            public void onPlayerInteract(PlayerInteractEvent event) {
                if ( ! enabled ) return;

                Player player = event.getPlayer();

                if ( player.hasPermission("civ.staff") ) return;

                if ( event.getClickedBlock() == null ) return;

                Location location = event.getClickedBlock().getLocation();
                if ( outsideIsland(location) ) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Do not build outside the island!");
                } else if ( inSpawnArea(location) ) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Do not build in the spawn area!");
                }
            }

            @EventHandler
            public void onBlockCanBuild(BlockCanBuildEvent event) {
                if ( ! enabled ) return;

                Player player = event.getPlayer();

                if ( player.hasPermission("civ.staff") ) return;

                Location location = event.getBlock().getLocation();
                if ( outsideIsland(location) ) {
                    event.setBuildable(false);
                    player.sendMessage(ChatColor.RED + "Do not build outside the island!");
                } else if ( inSpawnArea(location) ) {
                    event.setBuildable(false);
                    player.sendMessage(ChatColor.RED + "Do not build in the spawn area!");
                }
            }
        });
    }

    private boolean outsideIsland(Location location) {
        return (
            Math.abs(location.getX()) < islandOrigin - islandSize / 2 ||
            Math.abs(location.getX()) > islandOrigin + islandSize / 2 ||
            Math.abs(location.getZ()) < islandOrigin - islandSize / 2 ||
            Math.abs(location.getZ()) > islandOrigin + islandSize / 2
        );
    }

    private boolean inSpawnArea(Location location) {
        return (
            Math.abs(location.getX()) >= islandOrigin - islandSpawnSize / 2 &&
            Math.abs(location.getX()) <= islandOrigin + islandSpawnSize / 2 &&
            Math.abs(location.getZ()) >= islandOrigin - islandSpawnSize / 2 &&
            Math.abs(location.getZ()) <= islandOrigin + islandSpawnSize / 2
        );
    }
}
