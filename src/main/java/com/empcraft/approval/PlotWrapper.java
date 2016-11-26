package com.empcraft.approval;

import java.util.UUID;

import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;

public class PlotWrapper implements Comparable<PlotWrapper> {

    private Long timestamp;
    private PlotId id;
    private PlotArea plotArea;
    private UUID owner;

    public PlotWrapper(final Long timestamp, final PlotId id, final PlotArea plotArea, final UUID owner) {
        this.timestamp = timestamp;
        this.id = id;
        this.plotArea = plotArea;
        this.owner = owner;
    }

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
        return ((this.id.x == other.id.x) && (this.id.y == other.id.y) && (this.plotArea.equals(other.plotArea)));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + this.id.x;
        result = (prime * result) + this.id.y;
        result = (prime * result) + this.plotArea.hashCode();
        return result;
    }
    public UUID getOwner(){
    	return owner;
    }
    public PlotArea getPlotArea(){
    	return plotArea;
    }
    public PlotId getPlotId(){
    	return id;
    }
    public Long getTimestamp(){
    	return timestamp;
    }
}
