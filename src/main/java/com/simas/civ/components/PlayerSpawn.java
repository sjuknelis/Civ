package com.simas.civ.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class PlayerSpawn implements PluginComponent {
    private App plugin;
    private int islandOrigin, islandSpawnY, invincibilitySeconds;
    private List<String> islandNames;

    private int nextIslandIndex = 0;
    
    private Map<Player, Long> invincibleUntil = new HashMap<>();

    public PlayerSpawn(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        islandOrigin = plugin.getConfig().getInt("island-origin");
        islandSpawnY = plugin.getConfig().getInt("island-spawn-y");
        islandNames = plugin.getConfig().getStringList("island-names");
        invincibilitySeconds = plugin.getConfig().getInt("spawn-invincibility-seconds");
    }

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                Player player = event.getPlayer();

                if ( MultiLib.getPersistentData(player, "island") != null ) {
                    String styledName = plugin.islandColors[Integer.parseInt(MultiLib.getPersistentData(player, "island"))] + player.getName();
                    player.setDisplayName(styledName);
                    player.setPlayerListName(styledName);
                    return;
                }

                int islandIndex = nextIslandIndex++;
                nextIslandIndex %= 4;
                MultiLib.notify("civ:newplayer", "");

                Location spawnLocation = new Location(
                    player.getLocation().getWorld(),
                    2 * ((int) (islandIndex / 2) - 0.5) * islandOrigin,
                    islandSpawnY,
                    2 * ((islandIndex % 2) - 0.5) * islandOrigin
                );
                player.setBedSpawnLocation(spawnLocation, true);
                player.teleport(spawnLocation);

                player.setGameMode(GameMode.SURVIVAL);
                String styledName = plugin.islandColors[islandIndex] + player.getName();
                player.setDisplayName(styledName);
                player.setPlayerListName(styledName);

                player.sendMessage(ChatColor.GREEN + "Welcome to the " + plugin.islandColors[islandIndex] + islandNames.get(islandIndex) + " Island!" + ChatColor.GREEN + " You are invincible for " + invincibilitySeconds + " seconds.");

                MultiLib.setPersistentData(player, "island", Integer.toString(islandIndex));

                invincibleUntil.put(player, System.currentTimeMillis() + invincibilitySeconds * 1000);
            }

            @EventHandler
            public void onPlayerDamage(EntityDamageEvent event) {
                if ( ! (event.getEntity() instanceof Player) ) return;

                Player player = (Player) event.getEntity();

                if ( invincibleUntil.containsKey(player) ) {
                    if ( System.currentTimeMillis() <= invincibleUntil.get(player) ) event.setCancelled(true);
                    else invincibleUntil.remove(player);
                }
            }
        });

        MultiLib.onString(plugin, "civ:newplayer", new Consumer<String>() {
            @Override
            public void accept(String data) {
                nextIslandIndex = (nextIslandIndex + 1) % 4;
            }
        });
    }
}
