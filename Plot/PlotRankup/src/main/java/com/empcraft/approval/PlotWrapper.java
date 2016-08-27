package com.empcraft.approval;

import java.util.UUID;

import com.intellectualcrafters.plot.object.PlotId;

public class PlotWrapper implements Comparable<PlotWrapper> {

    Long timestamp;
    PlotId id;
    String world;
    UUID owner;

    public PlotWrapper(final Long timestamp, final PlotId id, final String world, final UUID owner) {
        this.timestamp = timestamp;
        this.id = id;
        this.world = world;
        this.owner = owner;
    }

    @Override
    public int compareTo(final PlotWrapper other) {
        return (int) (this.timestamp == other.timestamp ? this.id.x - other.id.x : other.timestamp - this.timestamp);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlotWrapper other = (PlotWrapper) obj;
        return ((this.id.x == other.id.x) && (this.id.y == other.id.y) && (this.world.equals(other.world)));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.id.x;
        result = (prime * result) + this.id.y;
        result = (prime * result) + this.world.hashCode();
        return result;
    }
}
