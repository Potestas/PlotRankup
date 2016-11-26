package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.BooleanFlag;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.flag.LongFlag;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "approve", permission = "plots.approve", category = CommandCategory.ADMINISTRATION, requiredType = RequiredType.NONE, description = "Used to approve player's plots", usage = "/plot approve")
public class ApproveCommand extends SubCommand {
	private static BooleanFlag flagDone;
	private static LongFlag flagTimestamp;

	public ApproveCommand() {
		flagDone = Main.getFlagDone();
		flagTimestamp = Main.getFlagTimestamp();
	}

	@Override
	public boolean onCommand(final PlotPlayer player, final String... args) {
		final List<String> validArgs = Arrays.asList("approve", "list", "next", "listworld", "deny");
		if ((args.length == 0) || !validArgs.contains(args[0].toLowerCase())) {
			MainUtil.sendMessage(player, "&7Syntax: &c/plots approval <approve|deny|list|listworld|next>");
			return false;
		}
		final Location loc = player.getLocation();
		args[0] = args[0].toLowerCase();
		if (args[0].equals("approve")) {
			final Plot plot = Plot.getPlot(loc);
			if (plot == null) {
				sendMessage(player, C.NOT_IN_PLOT);
				return false;
			}
			final String world = loc.getWorld();
			if (!plot.hasOwner()) {
				sendMessage(player, C.NOT_IN_PLOT);
				return false;
			}
			if ((flagDone == null) || flagDone.isTrue(plot)) {
				if (flagDone == null) {
					MainUtil.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
				} else {
					MainUtil.sendMessage(player, "&7This plot has already been approved.");
				}
				return false;
			}
			plot.setFlag(flagDone, Boolean.TRUE);
			final PlotPlayer owner = UUIDHandler.getPlayer(plot.guessOwner());
			if (owner != null) {
				MainUtil.sendMessage(owner, "&7Your plot &a" + plot.toString() + "&7 has been approved!");
			}

			final int count = countApproved(plot.guessOwner(), world);

			for (final String commandargs : Main.config.getStringList(world + ".approval.actions")) {
				try {
					final int required = Integer.parseInt(commandargs.split(":")[0]);
					if (required == count) {
						String ownername = UUIDHandler.getName(plot.guessOwner());
						if (ownername == null) {
							ownername = "";
						}
						String cmd = commandargs.substring(commandargs.indexOf(":") + 1);
						if (cmd.contains("%player%")) {
							cmd = cmd.replaceAll("%player%", ownername);
						}
						cmd = cmd.replaceAll("%world%", world);

						if (Main.vaultFeatures) {
							if (cmd.contains("%nextrank%")) {
								cmd.replaceAll("%nextrank%", VaultListener.getNextRank(world,
										VaultListener.getGroup(world, plot.guessOwner())));
							}
						}
						MainUtil.sendMessage(null, "Console: " + cmd);
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
					}
				} catch (final Exception e) {
					MainUtil.sendMessage(null, "[PlotApproval] &cInvalid approval command " + commandargs + "!");
					MainUtil.sendMessage(player, "[PlotApproval] &cInvalid approval command " + commandargs + "!");
					return true;
				}
			}
			MainUtil.sendMessage(player, "&aSuccessfully approved plot!");
			return true;
		}
		if (args[0].equals("listworld")) { // Plots are sorted in claim order.
			final String world = loc.getWorld();
			final ArrayList<PlotWrapper> plots = getPlots(world);
			if (plots.size() == 0) {
				MainUtil.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
				return true;
			}
			MainUtil.sendMessage(player, "&7There are currently &c" + plots.size() + "&7 plots pending for approval.");
			for (final PlotWrapper current : plots) {
				String ownername = UUIDHandler.getName(current.getOwner());
				if (ownername == null) {
					ownername = "unknown";
				}
				MainUtil.sendMessage(player, "&8 - &3" + current.getPlotArea() + "&7;&3" + current.getPlotId().x + "&7;&3" + current.getPlotId().y
						+ " &7: " + ownername);
			}
			return true;
		}
		if (args[0].equals("list")) {
			final ArrayList<PlotWrapper> plots = getPlots();
			if (plots.size() == 0) {
				MainUtil.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
				return true;
			}
			MainUtil.sendMessage(player, "&7There are currently &c" + plots.size() + "&7 plots pending for approval.");
			for (final PlotWrapper current : plots) {
				String ownername = UUIDHandler.getName(current.getOwner());
				if (ownername == null) {
					ownername = "unknown";
				}
				MainUtil.sendMessage(player, "&8 - &3" + current.getPlotArea() + "&7;&3" + current.getPlotId().x + "&7;&3" + current.getPlotId().y
						+ " &7: " + ownername);
			}
			return true;
		}
		if (args[0].equals("next")) {
			final Plot plot = loc.getPlot();
			final PlotArea area = loc.getPlotArea();
			final ArrayList<PlotWrapper> plots = getPlots();
			if (plots.size() > 0) {
				if (plot != null) {
					if (plot.hasOwner()) {
						for (int i = 0; i < plots.size(); i++) {
							if (plots.get(i).getPlotId().equals(plot.getId()) && plots.get(i).getPlotArea().equals(area)) {
								if (i < (plots.size() - 1)) {
									final PlotWrapper wrap = plots.get(i + 1);
									final Plot p2 = PS.get().getPlot(wrap.getPlotArea(), wrap.getPlotId());
									p2.teleportPlayer(player);
								}
								break;
							}
						}
					}
				}
				final PlotWrapper wrap = plots.get(0);
				final Plot p2 = PS.get().getPlot(wrap.getPlotArea(), wrap.getPlotId());
				p2.teleportPlayer(player);
				return true;
			}
			MainUtil.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
			return true;
		}
		if (args[0].equals("deny")) {
			final Plot plot = loc.getPlot();
			if ((plot == null) || !plot.hasOwner()) {
				sendMessage(player, C.NOT_IN_PLOT);
				return false;
			}
			loc.getWorld();
			if (flagDone == null) {
				MainUtil.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
				return false;
			}
			plot.setFlag(flagDone, Boolean.FALSE);
			final String owner = UUIDHandler.getName(plot.guessOwner());
			if (owner != null) {
				Main.cooldown.put(owner, (System.currentTimeMillis() / 1000));
			}
			MainUtil.sendMessage(player, "&aSuccessfully unapproved plot!");
			return true;
		}
		return true;
	}

	private ArrayList<PlotWrapper> getPlots() {
		final ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
		for (final Plot plot : PS.get().getPlots()) {
			if (plot.hasOwner()) {
				if (flagTimestamp != null) {
					final Long timestamp = FlagManager.getPlotFlagRaw(plot, flagTimestamp);
					final PlotWrapper wrap = new PlotWrapper(timestamp, plot.getId(), plot.getArea(), plot.guessOwner());
					plots.add(wrap);
				}
			}
		}
		Collections.sort(plots);
		return plots;
	}

	private ArrayList<PlotWrapper> getPlots(final String world) {
		final ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
		for (final Plot plot : PS.get().getPlots(world)) {
			if (plot.hasOwner()) {
				if (flagTimestamp != null) {
					final Long timestamp = FlagManager.getPlotFlagRaw(plot, flagTimestamp);
					final PlotWrapper wrap = new PlotWrapper(timestamp, plot.getId(), plot.getArea(), plot.guessOwner());
					plots.add(wrap);
				}
			}
		}
		Collections.sort(plots);
		return plots;
	}

	private int countApproved(final UUID owner, final String world) {
		int count = 0;
		for (final Plot plot : PS.get().getPlots(world)) {
			if (plot.guessOwner().equals(owner)) {
				if (flagDone != null) {
					if (flagDone.isTrue(plot)) {
						count++;
					}
				}
			}
		}
		return count;
	}
}
