package com.simas.civ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.puregero.multilib.DataStorageImpl;
import com.github.puregero.multilib.MultiLib;

public class PermissionSaver {
    private final String[] TRACKING_PERMS = {"civ.staff", "civ.muted"};

    private App plugin;

    public PermissionSaver(App plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                DataStorageImpl store = MultiLib.getDataStorage();

                for ( Player player : MultiLib.getLocalOnlinePlayers() ) {
                    List<String> playerPermissions = new ArrayList<>();
                    for ( String permission : TRACKING_PERMS ) {
                        if ( player.hasPermission(permission) ) playerPermissions.add(permission);
                    }

                    store.set("permissions-" + player.getUniqueId(), String.join(",", playerPermissions));
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    public boolean hasPermission(Player player, String permission) {
        return hasPermission(player.getUniqueId(), permission);
    }

    public boolean hasPermission(UUID uuid, String permission) {
        try {
            return hasPermissionThrows(uuid, permission);
        } catch ( InterruptedException | ExecutionException e ) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasPermissionThrows(UUID uuid, String permission) throws InterruptedException, ExecutionException {
        List<String> playerPermissions = Arrays.asList(MultiLib.getDataStorage().get("permissions-" + uuid.toString(), "").get());

        return playerPermissions.contains(permission);
    }
}
