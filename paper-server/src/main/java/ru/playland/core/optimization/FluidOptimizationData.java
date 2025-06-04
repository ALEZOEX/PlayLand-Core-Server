package ru.playland.core.optimization;

import org.bukkit.Location;

/**
 * Data class for fluid optimization caching
 */
public class FluidOptimizationData {
    
    private Location position;
    private int flowLevel;
    private boolean optimized;
    private long timestamp;
    
    public FluidOptimizationData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public FluidOptimizationData(Location position, int flowLevel, boolean optimized) {
        this.position = position;
        this.flowLevel = flowLevel;
        this.optimized = optimized;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Location getPosition() {
        return position;
    }
    
    public void setPosition(Location position) {
        this.position = position;
    }
    
    public int getFlowLevel() {
        return flowLevel;
    }
    
    public void setFlowLevel(int flowLevel) {
        this.flowLevel = flowLevel;
    }
    
    public boolean isOptimized() {
        return optimized;
    }
    
    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isExpired(long expirationTime) {
        return (System.currentTimeMillis() - timestamp) > expirationTime;
    }
    
    @Override
    public String toString() {
        return "FluidOptimizationData{" +
                "position=" + position +
                ", flowLevel=" + flowLevel +
                ", optimized=" + optimized +
                ", timestamp=" + timestamp +
                '}';
    }
}
