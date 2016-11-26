package com.empcraft.approval;

import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.BooleanFlag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.flag.LongFlag;
import com.intellectualcrafters.plot.generator.HybridPlotWorld;
import com.intellectualcrafters.plot.generator.HybridUtils;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(command = "done", permission = "plots.done", category = CommandCategory.ADMINISTRATION, requiredType = RequiredType.NONE, description = "Mark your plot as finished", usage = "/plot done")
public class DoneCommand extends SubCommand {
	private static BooleanFlag flagDone;
	private static LongFlag flagTimestamp;

	public DoneCommand() {
		flagDone = Main.getFlagDone();
		flagTimestamp = Main.getFlagTimestamp();
	}

	@Override
	public boolean onCommand(final PlotPlayer player, final String... args) {
		final Location loc = player.getLocation();
		final Plot plot = loc.getPlot();
		if (plot == null) {
			sendMessage(player, C.NOT_IN_PLOT);
			return false;
		}
		if (!plot.isAdded(player.getUUID())) {
			sendMessage(player, C.NO_PLOT_PERMS);
			return false;
		}
		if (flagDone != null) {
			if (flagDone.isFalse(plot)) {
				MainUtil.sendMessage(player, "&7This plot is already marked as &cdone&7 and is waiting for approval.");
			} else {
				MainUtil.sendMessage(player, "&7This plot is already marked as &cdone&7 and has been approved.");
			}
			return false;
		}

		final int coolTime = Main.config.getInt("reapproval-wait-time-sec");

		final Long playerCool = Main.cooldown.get(player.getName());
		if (playerCool != null) {
			final long diff = (System.currentTimeMillis() / 1000) - playerCool;
			if (diff < coolTime) {
				MainUtil.sendMessage(player,
						"&cSorry... &7you need to wait a bit longer until you can resubmit your build (&a"
								+ (coolTime - diff) + " seconds&7)");
				return false;
			}
			Main.cooldown.remove(player.getName());
		}
		final Integer goal = Main.worldChanged.get(loc.getWorld());
		final PlotArea plotworld = plot.getArea();
		if (plotworld instanceof HybridPlotWorld) {
			HybridUtils.manager.checkModified(plot, new RunnableVal<Integer>() {

				@Override
				public void run(Integer value) {
					if ((goal == null) || (this.value == -1) || (this.value >= goal)) {
						for (final String user : Main.toRemove) {
							if (((System.currentTimeMillis() / 1000) - Main.cooldown.get(user)) >= coolTime) {
								Main.cooldown.remove(user);
								Main.toRemove.remove(user);
							}
						}
						FlagManager.addPlotFlag(plot, flagTimestamp, (System.currentTimeMillis() / 1000));
						FlagManager.addPlotFlag(plot, flagDone, false);
						MainUtil.sendMessage(player,
								"&7Your plot has been marked as &adone&7 and should be approved shortly.");
					} else {
						MainUtil.sendMessage(player,
								"&7You've changed an &cinsignificant&7 number of blocks in your current build. It has potential, and if you put some more time into it you'll definitely have something special.\n&8 - &7You can resubmit this build in &a"
										+ coolTime + "&7 seconds. Good luck!");
						Main.cooldown.put(player.getName(), System.currentTimeMillis() / 1000);
						return;
					}
				}
			});
		}
		return true;
	}
}
