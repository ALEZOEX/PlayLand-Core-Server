package ru.playland.core.vanilla;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single vanilla Minecraft mechanic
 * Tracks its status and ensures preservation
 */
public class VanillaMechanic {
    
    private final String id;
    private final String name;
    private final String description;
    private final int priority;
    private final AtomicBoolean enabled;
    private final AtomicBoolean working;
    private final AtomicLong usageCount;
    private final AtomicLong lastUsed;
    
    public VanillaMechanic(String id, String name, String description, int priority, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.enabled = new AtomicBoolean(enabled);
        this.working = new AtomicBoolean(true);
        this.usageCount = new AtomicLong(0);
        this.lastUsed = new AtomicLong(System.currentTimeMillis());
    }
    
    public void use() {
        usageCount.incrementAndGet();
        lastUsed.set(System.currentTimeMillis());
    }
    
    public void fix() {
        working.set(true);
    }
    
    public void markBroken() {
        working.set(false);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled.get(); }
    public boolean isWorking() { return working.get(); }
    public long getUsageCount() { return usageCount.get(); }
    public long getLastUsed() { return lastUsed.get(); }
    
    // Setters
    public void setEnabled(boolean enabled) { this.enabled.set(enabled); }
}
