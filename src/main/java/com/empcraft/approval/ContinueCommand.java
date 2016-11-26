package com.empcraft.approval;

import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.BooleanFlag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.flag.LongFlag;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.CommandDeclaration;
@CommandDeclaration(
        command = "continue",
        permission = "plots.continue",
        category = CommandCategory.ADMINISTRATION,
        requiredType = RequiredType.NONE,
        description = "Continue editing your plot",
        usage = "/plot continue"
)
public class ContinueCommand extends SubCommand {
	private static BooleanFlag flagDone;
	private static LongFlag flagTimestamp;
	public ContinueCommand(){
		flagTimestamp=Main.getFlagTimestamp();
		flagDone=Main.getFlagDone();
	}

    @Override
    public boolean onCommand(final PlotPlayer player, final String... args) {
        final Plot plot = player.getLocation().getPlot();
        if (plot == null) {
            sendMessage(player, C.NOT_IN_PLOT);
            return false;
        }
        if (!plot.isAdded(player.getUUID())) {
            sendMessage(player, C.NO_PLOT_PERMS);
            return false;
        }

        if (flagDone == null) {
            MainUtil.sendMessage(player, "&7This plot is already in &cbuild&7 mode.");
            return false;
        }

        if (flagDone.isTrue(plot)) {
            MainUtil.sendMessage(player, "&7This plot has been &a approved &7 and &c locked &7 by an admin.");
            return false;
        }
        FlagManager.removePlotFlag(plot, flagDone);
        FlagManager.removePlotFlag(plot, flagTimestamp);
        MainUtil.sendMessage(player, "&7You may now &acontinue &7building.");

        return true;
    }
}
