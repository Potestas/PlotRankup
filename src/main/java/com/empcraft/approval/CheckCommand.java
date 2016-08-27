package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Collections;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.CommandDeclaration;
@CommandDeclaration(
        command = "check",
        permission = "plots.check",
        category = CommandCategory.ACTIONS,
        requiredType = RequiredType.NONE,
        description = "Check the status of your plot in the queue",
        usage = "/plot check"
)
public class CheckCommand extends SubCommand {

    @Override
    public boolean onCommand(final PlotPlayer player, final String... args) {
        final Plot plot = MainUtil.getPlot(player.getLocation());
        if (plot == null) {
            sendMessage(player, C.NOT_IN_PLOT);
            return false;
        }
        if (!plot.isAdded(player.getUUID())) {
            sendMessage(player, C.NO_PLOT_PERMS);
            return false;
        }

        final Flag flag = FlagManager.getPlotFlag(plot, "done");
        if ((flag == null) || flag.getValue().equals("true")) {
            MainUtil.sendMessage(player, "&cThis plot is not in the queue!");
            return false;
        }
        Long time;
        try {
            time = (System.currentTimeMillis() / 1000) - ((Long) flag.getValue());
        } catch (final Exception e) {
            e.printStackTrace();
            time = System.currentTimeMillis() / 1000;
        }
        MainUtil.sendMessage(player, "&6Plot&7: &a" + plot.id + "\n&6Submitted&7: &a" + secToTime(time) + "&7ago\n");

        final ArrayList<PlotWrapper> plots = getPlots();
        final Integer index = plots.indexOf(new PlotWrapper(0L, plot.id, plot.world, plot.owner));
        if (index == null) {
            MainUtil.sendMessage(player, "&6Queue rank&7: &aUnknown");
        } else {
            MainUtil.sendMessage(player, "&6Queue rank&7: &a" + index);
        }
        return true;
    }

    private ArrayList<PlotWrapper> getPlots() {

        final ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();

        for (final Plot plot : PS.get().getPlots()) {
            if (plot.hasOwner()) {
                final Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag != null) {
                    if (flag.getValue() instanceof Long) {
                        final Long value = (Long) flag.getValue();
                        final PlotWrapper wrap = new PlotWrapper(value, plot.id, plot.world, plot.owner);
                        plots.add(wrap);
                    }
                }
            }
        }
        Collections.sort(plots);
        return plots;
    }

    private String secToTime(Long time) {
        final StringBuilder toreturn = new StringBuilder();
        int years = 0;
        int weeks = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (time >= 33868800) {
            years = (int) (time / 33868800);
            time -= years * 33868800;
            if (time > 1) {
                toreturn.append(years + " years ");
            } else {
                toreturn.append(years + " year ");
            }
        }
        if (time >= 604800) {
            weeks = (int) (time / 604800);
            time -= weeks * 604800;
            if (time > 1) {
                toreturn.append(weeks + " weeks ");
            } else {
                toreturn.append(weeks + " week ");
            }
        }
        if (time >= 86400) {
            days = (int) (time / 86400);
            time -= days * 86400;
            if (time > 1) {
                toreturn.append(days + " days ");
            } else {
                toreturn.append(days + " day ");
            }
        }
        if (time >= 3600) {
            hours = (int) (time / 3600);
            time -= hours * 3600;
            if (time > 1) {
                toreturn.append(hours + " hours ");
            } else {
                toreturn.append(hours + " hour ");
            }
        }
        if (time >= 60) {
            minutes = (int) (time / 60);
            time -= minutes * 60;
            if (time > 1) {
                toreturn.append(minutes + " minutes ");
            } else {
                toreturn.append(minutes + " minute ");
            }
        }
        if (toreturn.equals("") || (time > 0)) {
            if (time > 1) {
                toreturn.append((time) + " seconds ");
            } else {
                toreturn.append((time) + " second ");
            }
        }
        return toreturn.toString();
    }
}
