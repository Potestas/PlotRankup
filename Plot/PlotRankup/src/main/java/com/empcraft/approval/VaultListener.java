package com.empcraft.approval;

import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultListener implements Listener {

    Main main;

    private static Permission perms = null;

    public VaultListener(final Main main, final Plugin vaultPlugin) {
        this.main = main;
        setupPermissions();
    }

    private boolean setupPermissions() {
        final RegisteredServiceProvider<Permission> rsp = this.main.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public static String getGroup(final String world, final UUID uuid) {
        return perms.getPrimaryGroup(world, Bukkit.getOfflinePlayer(uuid));
    }

    public static String getNextRank(final String world, final String group) {
        final List<String> ranks = Main.config.getStringList(world + ".approval.rankLadder");
        for (int i = 0; i < (ranks.size() - 1); i++) {
            final String rank = ranks.get(i);
            if (rank.equals(group)) {
                return ranks.get(i + 1);
            }
        }
        return "INVALID_RANK[" + world + ":" + group + "]";
    }

    // TODO events

}
