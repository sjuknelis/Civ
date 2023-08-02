package com.simas.civ.components;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.App;
import com.simas.civ.PluginComponent;

public class ItemBan implements PluginComponent {
    private App plugin;
    private final Set<Enchantment> bannedEnchants = new HashSet<>();
    private final Set<Enchantment> limitedEnchants = new HashSet<>();
    private final Set<Material> bannedItems = new HashSet<>();

    public ItemBan(App plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        for ( String enchant : plugin.getConfig().getStringList("banned-enchants") ) {
            bannedEnchants.add(Enchantment.getByKey(NamespacedKey.minecraft(enchant)));
        }

        for ( String enchant : plugin.getConfig().getStringList("limited-enchants") ) {
            limitedEnchants.add(Enchantment.getByKey(NamespacedKey.minecraft(enchant)));
        }
        
        for ( String item : plugin.getConfig().getStringList("banned-items") ) {
            bannedItems.add(Material.getMaterial(item));
        }
    }

    public void onEnable() {
        plugin.addListener(new Listener() {
            @EventHandler
            public void onEnchantItem(EnchantItemEvent event) {
                Player player = event.getEnchanter();
                
                if ( player.hasPermission("civ.staff") ) return;

                Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
                for ( Enchantment enchant : enchants.keySet() ) {
                    if ( bannedEnchants.contains(enchant) ) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Cannot enchant with " + enchant.getKey().getKey() + "!");
                        return;
                    } else if ( limitedEnchants.contains(enchant) && enchants.get(enchant) > 1 ) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Cannot enchant with " + enchant.getKey().getKey() + " past level 1!");
                        return;
                    }
                }
            }

            @EventHandler
            public void onProjectileLaunch(ProjectileLaunchEvent event) {
                if ( event.getEntity() instanceof EnderPearl ) event.setCancelled(true);
            }

            @EventHandler
            public void onEntityExplode(EntityExplodeEvent event) {
                event.setCancelled(true);
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                for ( Player player : MultiLib.getLocalOnlinePlayers() ) {
                    Inventory inventory = player.getInventory();
                    for ( Material bannedItem : bannedItems ) {
                        inventory.remove(bannedItem);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}