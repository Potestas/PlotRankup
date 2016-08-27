package com.empcraft.approval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.CommandDeclaration;

@CommandDeclaration(
        command = "approve",
        permission = "plots.approve",
        category = CommandCategory.ACTIONS,
        requiredType = RequiredType.NONE,
        description = "Used to approve player's plots",
        usage = "/plot approve"
)
public class ApproveCommand extends SubCommand {

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
            final Plot plot = MainUtil.getPlot(loc);
            if (plot == null) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            final String world = loc.getWorld();
            if (!plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }

            final Flag flag = FlagManager.getPlotFlag(plot, "done");
            if ((flag == null) || flag.getValue().equals("true")) {
                if (flag == null) {
                    MainUtil.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                } else {
                    MainUtil.sendMessage(player, "&7This plot has already been approved.");
                }
                return false;
            }
            FlagManager.addPlotFlag(plot, new Flag(FlagManager.getFlag("done"), Boolean.TRUE));
            final PlotPlayer owner = UUIDHandler.getPlayer(plot.owner);
            if (owner != null) {
                MainUtil.sendMessage(owner, "&7Your plot &a" + plot.toString() + "&7 has been approved!");
            }

            final int count = countApproved(plot.owner, world);

            for (final String commandargs : Main.config.getStringList(world + ".approval.actions")) {
                try {
                    final int required = Integer.parseInt(commandargs.split(":")[0]);
                    if (required == count) {
                        String ownername = UUIDHandler.getName(plot.owner);
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
                                cmd.replaceAll("%nextrank%", VaultListener.getNextRank(world, VaultListener.getGroup(world, plot.owner)));
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
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername == null) {
                    ownername = "unknown";
                }
                MainUtil.sendMessage(player, "&8 - &3" + current.world + "&7;&3" + current.id.x + "&7;&3" + current.id.y + " &7: " + ownername);
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
                String ownername = UUIDHandler.getName(current.owner);
                if (ownername == null) {
                    ownername = "unknown";
                }
                MainUtil.sendMessage(player, "&8 - &3" + current.world + "&7;&3" + current.id.x + "&7;&3" + current.id.y + " &7: " + ownername);
            }
            return true;
        }
        if (args[0].equals("next")) {
            final Plot plot = MainUtil.getPlot(loc);
            final String world = loc.getWorld();
            final ArrayList<PlotWrapper> plots = getPlots();
            if (plots.size() > 0) {
                if (plot != null) {
                    if (plot.hasOwner()) {
                        for (int i = 0; i < plots.size(); i++) {
                            if (plots.get(i).id.equals(plot.id) && plots.get(i).world.equals(world)) {
                                if (i < (plots.size() - 1)) {
                                    final PlotWrapper wrap = plots.get(i + 1);
                                    final Plot p2 = PS.get().getPlots(wrap.world).get(wrap.id);
                                    MainUtil.teleportPlayer(player, player.getLocation(), p2);
                                }
                                break;
                            }
                        }
                    }
                }
                final PlotWrapper wrap = plots.get(0);
                final Plot p2 = PS.get().getPlots(wrap.world).get(wrap.id);
                MainUtil.teleportPlayer(player, player.getLocation(), p2);
                return true;
            }
            MainUtil.sendMessage(player, "&7There are currently &c0&7 plots pending for approval.");
            return true;
        }
        if (args[0].equals("deny")) {
            final Plot plot = MainUtil.getPlot(loc);
            if ((plot == null) || !plot.hasOwner()) {
                sendMessage(player, C.NOT_IN_PLOT);
                return false;
            }
            loc.getWorld();
            final Flag flag = FlagManager.getPlotFlag(plot, "done");
            if (flag == null) {
                MainUtil.sendMessage(player, "&7This plot is not &cpending&7 for approval.");
                return false;
            }
            FlagManager.removePlotFlag(plot, "done");
            final String owner = UUIDHandler.getName(plot.owner);
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
                final Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag != null) {
                    if (!(flag.getValue() instanceof Boolean) && (flag.getValue() instanceof Long)) {
                        final Long timestamp = (Long) flag.getValue();
                        final PlotWrapper wrap = new PlotWrapper(timestamp, plot.id, plot.world, plot.owner);
                        plots.add(wrap);
                    }
                }
            }
        }
        Collections.sort(plots);
        return plots;
    }

    private ArrayList<PlotWrapper> getPlots(final String world) {
        final ArrayList<PlotWrapper> plots = new ArrayList<PlotWrapper>();
        for (final Plot plot : PS.get().getPlots(world).values()) {
            if (plot.hasOwner()) {
                final Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag != null) {
                    if (!(flag.getValue() instanceof Boolean) && (flag.getValue() instanceof Long)) {
                        final Long timestamp = (Long) flag.getValue();
                        final PlotWrapper wrap = new PlotWrapper(timestamp, plot.id, plot.world, plot.owner);
                        plots.add(wrap);
                    }
                }
            }
        }
        Collections.sort(plots);
        return plots;
    }

    private int countApproved(final UUID owner, final String world) {
        int count = 0;
        for (final Plot plot : PS.get().getPlots(world).values()) {
            if (plot.owner.equals(owner)) {
                final Flag flag = FlagManager.getPlotFlag(plot, "done");
                if (flag != null) {
                    if (flag.getValue() == Boolean.TRUE) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
