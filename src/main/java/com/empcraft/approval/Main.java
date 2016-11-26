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
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.api.PlotAPI;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.flag.BooleanFlag;
import com.intellectualcrafters.plot.flag.LongFlag;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.Permissions;
import com.plotsquared.bukkit.util.BukkitUtil;

public class Main extends JavaPlugin implements Listener {

	public String version;
	public Main plugin;
	private static PlotAPI api = null;
	public static boolean vaultFeatures = false;
	public static HashMap<String, Long> cooldown = new HashMap<String, Long>();
	public static HashSet<String> toRemove = new HashSet<String>();
	public static FileConfiguration config;
	private static BooleanFlag flagDone;
	private static LongFlag flagTimestamp;

	public static HashMap<String, Integer> worldChanged = new HashMap<String, Integer>();

	@Override
	public void onEnable() {
		PluginManager manager = Bukkit.getServer().getPluginManager();
		Plugin plotsquared = manager.getPlugin("PlotSquared");
		if (plotsquared != null && !plotsquared.isEnabled()) {
			getLogger().severe("PlotSquared was not found! Plugin is disabling!");
			manager.disablePlugin(this);
			return;
		}

		api = new PlotAPI();

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
		for (final Plot plot : api.getAllPlots()) {
			if (flagDone != null) {
				if (flagDone.isSet(plot) == Boolean.TRUE) {
					plot.countsTowardsMax = false;
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void setupConfig() {
		getConfig().options().copyDefaults(true);
		final Map<String, Object> options = new HashMap<String, Object>();
		getConfig().set("version", this.version);
		options.put("reapproval-wait-time-sec", 300);
		options.put("build-while-approved", false);
		for (final String world : api.getPlotWorlds()) {
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
			worldChanged.put(world.getName(),
					getConfig().getInt(world.getName() + ".approval.min-required-changed-blocks"));
		}
	}

	@SuppressWarnings("deprecation")
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
		flagDone = new BooleanFlag("done") {

			@Override
			public String getValueDescription() {
				return "Value must be a boolean 'true' or 'false'; which determines whether your build has been finalized.";
			}

			@Override
			public String valueToString(Object arg0) {
				return null;
			}

		};
		api.addFlag(flagDone);
		flagTimestamp = new LongFlag("timestamp") {

			@Override
			public Long parseValue(final String value) {
				final Long n = Long.parseLong(value);
				return n;
			}

			@Override
			public String getValueDescription() {
				return "This flag is set by the server";
			}

			@Override
			public String valueToString(Object arg0) {
				return arg0.toString();
			}
		};
		api.addFlag(flagTimestamp);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	private static void onBlockPlace(final BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		final String world = player.getWorld().getName();
		if (!PS.get().hasPlotArea(world)) {
			return;
		}
		final Location loc = event.getBlock().getLocation();
		if (isAllowedToInteract(player, world, loc)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	private static void onBlockBreak(final BlockBreakEvent event) {
		final Player player = event.getPlayer();
		final String world = player.getWorld().getName();
		if (!PS.get().hasPlotArea(world)) {
			return;
		}
		final Location loc = event.getBlock().getLocation();
		if (isAllowedToInteract(player, world, loc)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	private static void onInteract(final PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final String world = player.getWorld().getName();
		if (!PS.get().hasPlotArea(world)) {
			return;
		}
		final Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		final Location loc = block.getLocation();
		if (isAllowedToInteract(player, world, loc)) {
			event.setCancelled(true);
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

	private static boolean isAllowedToInteract(final Player player, final String world, final Location loc) {
		com.intellectualcrafters.plot.object.Location loc1 = new com.intellectualcrafters.plot.object.Location();
		loc1.setX(loc.getBlockX());
		loc1.setY(loc.getBlockY());
		loc1.setZ(loc.getBlockZ());
		loc1.setWorld(world);
		final Plot plot = loc1.getPlot();
		if (plot == null) {
			return false;
		}
		final PlotPlayer pp = BukkitUtil.getPlayer(player);
		final boolean rights = plot.isAdded(pp.getUUID());
		if (!rights) {
			return false;
		}
		if (flagDone == null) {
			return false;
		}
		if (Permissions.hasPermission(pp, "plots.admin")) {
			return false;
		}
		if (flagDone.isTrue(plot)) {
			if (!config.getBoolean("build-while-approved")) {
				sendMessage(player, "&7Your plot has been marked as done. To remove it from the queue and continue building please use:\n&a/plots continue");
			} else {
				sendMessage(player, "&7Your plot has been approved. To continue building, please get an admin to unapprove the plot.");
			}
			return true;
		}
		return false;
	}

	public static BooleanFlag getFlagDone() {
		return flagDone;
	}

	public static LongFlag getFlagTimestamp() {
		return flagTimestamp;
	}
}
