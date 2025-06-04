package ru.playland.core.optimization;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * PlayLand Core - Multi-Level Cache Hierarchy System
 * Implements L1/L2/L3 cache levels for maximum performance
 */
public class MultiLevelCacheHierarchy {

    // Metrics
    private final AtomicLong l1CacheHits = new AtomicLong(0);
    private final AtomicLong l2CacheHits = new AtomicLong(0);
    private final AtomicLong l3CacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);

    // Cache levels
    private final Map<String, Object> l1Cache = new ConcurrentHashMap<>(256);
    private final Map<String, Object> l2Cache = new ConcurrentHashMap<>(1024);
    private final Map<String, Object> l3Cache = new ConcurrentHashMap<>(4096);

    private final ScheduledExecutorService executor;
    private volatile boolean vanillaSafeMode = true;

    public MultiLevelCacheHierarchy(Plugin plugin) {
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PlayLand-CacheHierarchy");
            t.setDaemon(true);
            return t;
        });

        startCacheManagement();
        Bukkit.getLogger().info("[PlayLand Core] Multi-Level Cache Hierarchy initialized");
    }

    private void startCacheManagement() {
        executor.scheduleAtFixedRate(this::cleanupCaches, 30, 30, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::updateMetrics, 5, 5, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        totalRequests.incrementAndGet();

        // L1 Cache check
        Object value = l1Cache.get(key);
        if (value != null) {
            l1CacheHits.incrementAndGet();
            return value;
        }

        // L2 Cache check
        value = l2Cache.get(key);
        if (value != null) {
            l2CacheHits.incrementAndGet();
            promoteToL1(key, value);
            return value;
        }

        // L3 Cache check
        value = l3Cache.get(key);
        if (value != null) {
            l3CacheHits.incrementAndGet();
            promoteToL2(key, value);
            return value;
        }

        cacheMisses.incrementAndGet();
        return null;
    }

    public void put(String key, Object value) {
        if (vanillaSafeMode && !isVanillaSafe(value)) return;

        l1Cache.put(key, value);
        updateMemoryUsage();
    }

    private void promoteToL1(String key, Object value) {
        if (l1Cache.size() < 256) {
            l1Cache.put(key, value);
        }
    }

    private void promoteToL2(String key, Object value) {
        if (l2Cache.size() < 1024) {
            l2Cache.put(key, value);
        }
    }

    private void cleanupCaches() {
        if (l1Cache.size() > 200) {
            l1Cache.entrySet().removeIf(entry -> Math.random() < 0.1);
        }
        if (l2Cache.size() > 800) {
            l2Cache.entrySet().removeIf(entry -> Math.random() < 0.1);
        }
        if (l3Cache.size() > 3200) {
            l3Cache.entrySet().removeIf(entry -> Math.random() < 0.1);
        }
        updateMemoryUsage();
    }

    private void updateMemoryUsage() {
        memoryUsage.set(estimateMemoryUsage());
    }

    private void updateMetrics() {
        updateMemoryUsage();
    }

    private long estimateMemoryUsage() {
        return (l1Cache.size() + l2Cache.size() + l3Cache.size()) * 64L;
    }

    private boolean isVanillaSafe(Object value) {
        return value != null && !(value instanceof Plugin);
    }

    public void setVanillaSafeMode(boolean enabled) {
        this.vanillaSafeMode = enabled;
    }

    // Getters for metrics
    public long getL1CacheHits() { return l1CacheHits.get(); }
    public long getL2CacheHits() { return l2CacheHits.get(); }
    public long getL3CacheHits() { return l3CacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getTotalRequests() { return totalRequests.get(); }
    public long getMemoryUsage() { return memoryUsage.get(); }

    public void shutdown() {
        executor.shutdown();
        l1Cache.clear();
        l2Cache.clear();
        l3Cache.clear();
    }
}
