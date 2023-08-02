package com.simas.civ;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.puregero.multilib.MultiLib;
import com.simas.civ.components.ChatRadius;
import com.simas.civ.components.HardcoreMode;
import com.simas.civ.components.IslandGuard;
import com.simas.civ.components.ItemBan;
import com.simas.civ.components.PlayerList;
import com.simas.civ.components.PlayerSpawn;
import com.simas.civ.components.SecureState;

public class App extends JavaPlugin {
    public ChatColor[] islandColors = new ChatColor[4];

    public PermissionSaver permissions;

    private PluginComponent[] components;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        permissions = new PermissionSaver(this);
        permissions.onEnable();

        components = new PluginComponent[] {
            new HardcoreMode(this),
            new IslandGuard(this),
            new ChatRadius(this),
            new ItemBan(this),
            new PlayerSpawn(this),
            new PlayerList(this),
            new SecureState(this)
        };

        loadIslandColors();
        for ( PluginComponent component : components ) {
            component.loadConfig();
            component.onEnable();
        }

        getCommand("civreload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                reloadComponentConfig();
                MultiLib.notify("civ:reload", "");

                sender.sendMessage(ChatColor.GREEN + "Reloaded Civ config.yml!");

                return true;
            }
        });

        MultiLib.onString(this, "civ:reload", new Consumer<String>() {
            @Override
            public void accept(String data) {
                reloadComponentConfig();
            }
        });
    }

    private void reloadComponentConfig() {
        reloadConfig();

        loadIslandColors();
        for ( PluginComponent component : components ) component.loadConfig();
    }
    
    private void loadIslandColors() {
        List<String> islandColorStrings = getConfig().getStringList("island-colors");
        for ( int i = 0; i < 4; i++ ) {
            islandColors[i] = ChatColor.valueOf(islandColorStrings.get(i));
        }
    }

    public void addListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {

    }
}