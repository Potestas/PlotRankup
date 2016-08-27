package com.empcraft.approval;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.flag.AbstractFlag;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.bukkit.util.BukkitUtil;


public class Main extends JavaPlugin implements Listener {

    public String version;
    public Main plugin;

    public static boolean vaultFeatures = false;
    public static HashMap<String, Long> cooldown = new HashMap<String, Long>();
    public static HashSet<String> toRemove = new HashSet<String>();
    public static FileConfiguration config;

    public static HashMap<String, Integer> worldChanged = new HashMap<String, Integer>();

    @Override
    public void onEnable() {
        this.version = getDescription().getVersion();
        this.plugin = this;
        setupPlotSquared();
        setupVault();
        setupConfig();
        setupFlags();
        Main.config = this.getConfig();
        setupPlots();
    }

    private static void setupPlots() {
        for (final Plot plot : PS.get().getPlots()) {
            final Flag flag = FlagManager.getPlotFlag(plot, "done");
            if (flag != null) {
                if (flag.getValue() == Boolean.TRUE) {
                    plot.countsTowardsMax = false;
                }
            }
        }
    }

    private void setupConfig() {
        getConfig().options().copyDefaults(true);
        final Map<String, Object> options = new HashMap<String, Object>();
        getConfig().set("version", this.version);
        options.put("reapproval-wait-time-sec", 300);
        options.put("build-while-approved", false);
        for (final String world : PS.get().getPlotWorlds()) {
            options.put(world + ".approval.min-required-changed-blocks", 0);
            final List<String> actions = Arrays.asList("1:manuadd %player% rank1", "2:manuadd %player% %nextrank%");
            options.put(world + ".approval.actions", actions);
            final List<String> rankLadder = Arrays.asList("rank1", "rank2");
            options.put(world + ".approval.rankLadder", rankLadder);
        }
        for (final Entry<String, Object> node : options.entrySet()) {
            if (!getConfig().contains(node.getKey())) {
                getConfig().set(node.getKey(), node.getValue());
            }
        }
        saveConfig();
        for (final World world : Bukkit.getWorlds()) {
            worldChanged.put(world.getName(), getConfig().getInt(world.getName() + ".approval.min-required-changed-blocks"));
        }
    }

    private void setupPlotSquared() {
        final Plugin plotsquared = Bukkit.getServer().getPluginManager().getPlugin("PlotSquared");
        if ((PS.get() == null) || !plotsquared.isEnabled()) {
            sendMessage(null, "&c[PlotApproval] Could not find plotsquared! Disabling plugin...");
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        MainCommand.getInstance().addCommand(new DoneCommand());
        MainCommand.getInstance().addCommand(new ContinueCommand());
        MainCommand.getInstance().addCommand(new ApproveCommand());
        MainCommand.getInstance().addCommand(new CheckCommand());
    }

    private void setupVault() {
        final Plugin vaultPlugin = Bukkit.getServer().getPluginManager().getPlugin("Vault");
        if ((vaultPlugin != null) && vaultPlugin.isEnabled()) {
            final VaultListener vault = new VaultListener(this, vaultPlugin);
            Bukkit.getServer().getPluginManager().registerEvents(vault, this);
            sendMessage(null, "&a[PlotApproval] Detected vault. Additional features enabled");
            Main.vaultFeatures = true;
        } else {
            sendMessage(null, "&a[PlotApproval] Detected vault. Additional features enabled");
        }
    }

    private static String colorise(final String mystring) {
        return ChatColor.translateAlternateColorCodes('&', mystring);
    }

    public static void sendMessage(final Player player, final String mystring) {
        if (ChatColor.stripColor(mystring).equals("")) {
            return;
        }
        if (player == null) {
            Bukkit.getServer().getConsoleSender().sendMessage(colorise(mystring));
        } else {
            player.sendMessage(colorise(mystring));
        }
    }

    private static void setupFlags() {
        final AbstractFlag doneFlag = new AbstractFlag("done") {

            @Override
            public Object parseValueRaw(final String value) {
                switch (value) {
                    case "true": {
                        return Boolean.TRUE;
                    }
                    case "false": {
                        return Boolean.TRUE;
                    }
                    default: {
                        try {
                            final Long n = Long.parseLong(value);
                            return n;
                        } catch (final Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                }
            }

            @Override
            public String getValueDesc() {
                return "Value must be a boolean 'true' or 'false'; which determines whether your build has been finalized.";
            }
        };
        FlagManager.addFlag(doneFlag);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private static void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final String world = player.getWorld().getName();
        if (!PS.get().isPlotWorld(world)) {
            return;
        }
        final Location loc = event.getBlock().getLocation();
        final Plot plot = MainUtil.getPlot(BukkitUtil.getLocation(loc));
        if (plot == null) {
            return;
        }
        final PlotPlayer pp = BukkitUtil.getPlayer(player);
        final boolean rights = plot.isAdded(pp.getUUID());
        if (!rights) {
            return;
        }
        final Flag flag = FlagManager.getPlotFlag(plot, "done");
        if (flag == null) {
            return;
        }
        if (Permissions.hasPermission(pp, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player, "&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                } else {
                    sendMessage(player, "&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private static void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final String world = player.getWorld().getName();
        if (!PS.get().isPlotWorld(world)) {
            return;
        }
        final Location loc = event.getBlock().getLocation();
        final Plot plot = MainUtil.getPlot(BukkitUtil.getLocation(loc));
        if (plot == null) {
            return;
        }
        final PlotPlayer pp = BukkitUtil.getPlayer(player);
        final boolean rights = plot.isAdded(pp.getUUID());
        if (!rights) {
            return;
        }
        final Flag flag = FlagManager.getPlotFlag(plot, "done");
        if (flag == null) {
            return;
        }
        if (Permissions.hasPermission(pp, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player, "&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                } else {
                    sendMessage(player, "&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private static void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final String world = player.getWorld().getName();
        if (!PS.get().isPlotWorld(world)) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        final Location loc = block.getLocation();
        final Plot plot = MainUtil.getPlot(BukkitUtil.getLocation(loc));
        if (plot == null) {
            return;
        }
        final PlotPlayer pp = BukkitUtil.getPlayer(player);
        final boolean rights = plot.isAdded(pp.getUUID());
        if (!rights) {
            return;
        }
        final Flag flag = FlagManager.getPlotFlag(plot, "done");
        if (flag == null) {
            return;
        }
        if (Permissions.hasPermission(pp, "plots.admin")) {
            return;
        }
        if (flag.getValue().equals(true)) {
            if (!config.getBoolean("build-while-approved")) {
                if (!flag.getValue().equals("true")) {
                    sendMessage(player, "&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
                } else {
                    sendMessage(player, "&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private static void onJoin(final PlayerJoinEvent event) {
        if (toRemove.contains(event.getPlayer().getName())) {
            toRemove.remove(event.getPlayer().getName());
        }
    }

    @EventHandler
    private static void onQuit(final PlayerQuitEvent event) {
        if (cooldown.containsKey(event.getPlayer().getName())) {
            toRemove.add(event.getPlayer().getName());
        }
    }

}
