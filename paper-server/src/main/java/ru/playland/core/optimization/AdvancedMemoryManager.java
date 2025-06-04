package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;

/**
 * Advanced Memory Manager
 * ЭКСТРЕМАЛЬНОЕ управление памятью сервера
 * Умная сборка мусора, детекция утечек, оптимизация выделения
 */
public class AdvancedMemoryManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-AdvMemory");
    
    // Memory statistics
    private final AtomicLong totalMemoryOptimizations = new AtomicLong(0);
    private final AtomicLong gcOptimizations = new AtomicLong(0);
    private final AtomicLong memoryLeaksDetected = new AtomicLong(0);
    private final AtomicLong memoryFreed = new AtomicLong(0);
    private final AtomicLong memoryAllocationsOptimized = new AtomicLong(0);
    private final AtomicLong emergencyCleanups = new AtomicLong(0);
    
    // Memory monitoring
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final Map<String, MemoryUsageTracker> memoryTrackers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastGCTimes = new ConcurrentHashMap<>();
    
    // Memory management
    private final ScheduledExecutorService memoryManager = Executors.newScheduledThreadPool(3);
    private final Map<String, Object> memoryCache = new ConcurrentHashMap<>();
    private final List<MemoryRegion> memoryRegions = new ArrayList<>();
    
    // Configuration
    private boolean enableAdvancedMemoryManagement = true;
    private boolean enableMemoryOptimization = true;
    private boolean enableGCOptimization = true;
    private boolean enableSmartGC = true;
    private boolean enableMemoryLeakDetection = true;
    private boolean enableMemoryProfiling = true;
    private boolean enableEmergencyCleanup = true;
    private boolean enableVanillaSafeMode = true;

    private double memoryWarningThreshold = 80.0; // 80%
    private double memoryCriticalThreshold = 90.0; // 90%
    private double memoryEmergencyThreshold = 95.0; // 95%
    private double maxMemoryUsageThreshold = 0.8; // 80%
    private double memoryCleanupThreshold = 0.7; // 70%
    private long gcOptimizationInterval = 30000; // 30 seconds
    private long memoryCheckInterval = 5000; // 5 seconds
    
    public void initialize() {
        LOGGER.info("💾 Initializing Advanced Memory Manager...");
        
        loadMemorySettings();
        initializeMemoryRegions();
        startMemoryMonitoring();
        startGCOptimization();
        startMemoryLeakDetection();
        
        LOGGER.info("✅ Advanced Memory Manager initialized!");
        LOGGER.info("💾 Advanced memory management: " + (enableAdvancedMemoryManagement ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗑️ Smart GC: " + (enableSmartGC ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔍 Memory leak detection: " + (enableMemoryLeakDetection ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Memory profiling: " + (enableMemoryProfiling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🚨 Emergency cleanup: " + (enableEmergencyCleanup ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚠️ Warning threshold: " + memoryWarningThreshold + "%");
        LOGGER.info("🚨 Critical threshold: " + memoryCriticalThreshold + "%");
        LOGGER.info("💥 Emergency threshold: " + memoryEmergencyThreshold + "%");
        
        // Log current memory status
        logMemoryStatus();
    }
    
    private void loadMemorySettings() {
        try {
            LOGGER.info("⚙️ Loading memory settings...");

            // Load memory management parameters from system properties
            enableAdvancedMemoryManagement = Boolean.parseBoolean(System.getProperty("playland.memory.advanced.enabled", "true"));
            enableMemoryOptimization = Boolean.parseBoolean(System.getProperty("playland.memory.optimization.enabled", "true"));
            enableGCOptimization = Boolean.parseBoolean(System.getProperty("playland.memory.gc.optimization.enabled", "true"));
            enableMemoryLeakDetection = Boolean.parseBoolean(System.getProperty("playland.memory.leak.detection.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.memory.vanilla.safe", "true"));
            enableMemoryProfiling = Boolean.parseBoolean(System.getProperty("playland.memory.profiling.enabled", "true"));

            // Load memory parameters
            maxMemoryUsageThreshold = Double.parseDouble(System.getProperty("playland.memory.max.usage.threshold", "0.8"));
            memoryCleanupThreshold = Double.parseDouble(System.getProperty("playland.memory.cleanup.threshold", "0.7"));
            gcOptimizationInterval = Long.parseLong(System.getProperty("playland.memory.gc.optimization.interval", "30000"));
            memoryCheckInterval = Long.parseLong(System.getProperty("playland.memory.check.interval", "5000"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - more aggressive memory management
                enableMemoryOptimization = true;
                enableGCOptimization = true;
                maxMemoryUsageThreshold = Math.max(0.6, maxMemoryUsageThreshold - 0.1);
                memoryCleanupThreshold = Math.max(0.5, memoryCleanupThreshold - 0.1);
                gcOptimizationInterval = Math.max(15000, gcOptimizationInterval / 2);
                memoryCheckInterval = Math.max(2000, memoryCheckInterval / 2);
                LOGGER.info("🔧 Aggressive memory management for low TPS: threshold=" + maxMemoryUsageThreshold +
                           ", cleanup=" + memoryCleanupThreshold + ", gc_interval=" + (gcOptimizationInterval/1000) + "s");
            } else if (currentTPS > 19.5) {
                // Good TPS - less aggressive memory management
                maxMemoryUsageThreshold = Math.min(0.9, maxMemoryUsageThreshold + 0.05);
                memoryCleanupThreshold = Math.min(0.8, memoryCleanupThreshold + 0.05);
                gcOptimizationInterval = Math.min(60000, gcOptimizationInterval * 2);
                memoryCheckInterval = Math.min(10000, memoryCheckInterval * 2);
                LOGGER.info("🔧 Relaxed memory management for good TPS: threshold=" + maxMemoryUsageThreshold);
            }

            // Auto-adjust based on available memory
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double currentMemoryUsage = (double) usedMemory / maxMemory;

            if (maxMemory < 2L * 1024 * 1024 * 1024) { // Less than 2GB
                // Low memory system - very aggressive management
                enableMemoryOptimization = true;
                enableGCOptimization = true;
                enableMemoryLeakDetection = true;
                maxMemoryUsageThreshold = Math.max(0.5, maxMemoryUsageThreshold - 0.2);
                memoryCleanupThreshold = Math.max(0.4, memoryCleanupThreshold - 0.2);
                gcOptimizationInterval = Math.max(10000, gcOptimizationInterval / 3);
                LOGGER.warning("⚠️ Low memory system detected (" + (maxMemory / 1024 / 1024) + "MB) - aggressive management enabled");
            } else if (maxMemory > 8L * 1024 * 1024 * 1024) { // More than 8GB
                // High memory system - less aggressive management
                maxMemoryUsageThreshold = Math.min(0.9, maxMemoryUsageThreshold + 0.1);
                memoryCleanupThreshold = Math.min(0.85, memoryCleanupThreshold + 0.1);
                gcOptimizationInterval = Math.min(120000, gcOptimizationInterval * 2);
                enableMemoryLeakDetection = false; // Less critical on high-memory systems
                LOGGER.info("🔧 High memory system detected (" + (maxMemory / 1024 / 1024) + "MB) - relaxed management");
            }

            // Auto-adjust based on current memory usage
            if (currentMemoryUsage > 0.85) {
                // Critical memory usage - emergency settings
                enableMemoryOptimization = true;
                enableGCOptimization = true;
                maxMemoryUsageThreshold = 0.6;
                memoryCleanupThreshold = 0.5;
                gcOptimizationInterval = 5000;
                memoryCheckInterval = 1000;
                LOGGER.warning("🚨 Critical memory usage (" + String.format("%.1f%%", currentMemoryUsage * 100) +
                              ") - emergency memory management activated");
            }

            // Auto-adjust based on player count
            int playerCount = getOnlinePlayerCount();
            if (playerCount > 100) {
                // Many players - more memory pressure expected
                maxMemoryUsageThreshold = Math.max(0.6, maxMemoryUsageThreshold - 0.05);
                memoryCleanupThreshold = Math.max(0.5, memoryCleanupThreshold - 0.05);
                gcOptimizationInterval = Math.max(20000, gcOptimizationInterval - playerCount * 100);
                memoryCheckInterval = Math.max(3000, memoryCheckInterval - playerCount * 10);
                LOGGER.info("🔧 Optimized for " + playerCount + " players: threshold=" + maxMemoryUsageThreshold);
            } else if (playerCount < 10) {
                // Few players - less memory pressure
                maxMemoryUsageThreshold = Math.min(0.85, maxMemoryUsageThreshold + 0.05);
                enableMemoryLeakDetection = false;
                gcOptimizationInterval = Math.min(60000, gcOptimizationInterval * 2);
                LOGGER.info("🔧 Relaxed memory management for " + playerCount + " players");
            }

            // Auto-adjust based on world complexity
            int loadedChunks = getLoadedChunkCount();
            if (loadedChunks > 1000) {
                // Large world - more memory usage expected
                maxMemoryUsageThreshold = Math.max(0.65, maxMemoryUsageThreshold - 0.05);
                memoryCleanupThreshold = Math.max(0.55, memoryCleanupThreshold - 0.05);
                enableMemoryOptimization = true;
                LOGGER.info("🔧 Large world detected (" + loadedChunks + " chunks) - increased memory management");
            }

            LOGGER.info("✅ Memory settings loaded - Max threshold: " + String.format("%.1f%%", maxMemoryUsageThreshold * 100) +
                       ", Cleanup threshold: " + String.format("%.1f%%", memoryCleanupThreshold * 100) +
                       ", GC interval: " + (gcOptimizationInterval/1000) + "s, Check interval: " + (memoryCheckInterval/1000) + "s");

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading memory settings: " + e.getMessage() + " - using defaults");
        }
    }

    private double getCurrentTPS() {
        try {
            return org.bukkit.Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }

    private int getOnlinePlayerCount() {
        try {
            return org.bukkit.Bukkit.getOnlinePlayers().size();
        } catch (Exception e) {
            return 50; // Default moderate count
        }
    }

    private int getLoadedChunkCount() {
        try {
            int totalChunks = 0;
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                totalChunks += world.getLoadedChunks().length;
            }
            return totalChunks;
        } catch (Exception e) {
            return 200; // Default moderate count
        }
    }
    
    private void initializeMemoryRegions() {
        // Initialize memory regions for tracking
        memoryRegions.add(new MemoryRegion("heap", "Heap Memory"));
        memoryRegions.add(new MemoryRegion("non_heap", "Non-Heap Memory"));
        memoryRegions.add(new MemoryRegion("chunks", "Chunk Data"));
        memoryRegions.add(new MemoryRegion("entities", "Entity Data"));
        memoryRegions.add(new MemoryRegion("plugins", "Plugin Data"));
        memoryRegions.add(new MemoryRegion("cache", "Cache Data"));
        
        LOGGER.info("🗺️ Memory regions initialized: " + memoryRegions.size());
    }
    
    private void startMemoryMonitoring() {
        // Monitor memory usage every 5 seconds
        memoryManager.scheduleAtFixedRate(() -> {
            try {
                monitorMemoryUsage();
                checkMemoryThresholds();
            } catch (Exception e) {
                LOGGER.warning("Memory monitoring error: " + e.getMessage());
            }
        }, memoryCheckInterval, memoryCheckInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📊 Memory monitoring started");
    }
    
    private void startGCOptimization() {
        if (!enableSmartGC) return;
        
        // Optimize GC every 30 seconds
        memoryManager.scheduleAtFixedRate(() -> {
            try {
                optimizeGarbageCollection();
                analyzeGCPerformance();
            } catch (Exception e) {
                LOGGER.warning("GC optimization error: " + e.getMessage());
            }
        }, gcOptimizationInterval, gcOptimizationInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🗑️ GC optimization started");
    }
    
    private void startMemoryLeakDetection() {
        if (!enableMemoryLeakDetection) return;
        
        // Check for memory leaks every 60 seconds
        memoryManager.scheduleAtFixedRate(() -> {
            try {
                detectMemoryLeaks();
                analyzeMemoryGrowth();
            } catch (Exception e) {
                LOGGER.warning("Memory leak detection error: " + e.getMessage());
            }
        }, 60000, 60000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔍 Memory leak detection started");
    }
    
    /**
     * Monitor current memory usage
     */
    private void monitorMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        // Update memory trackers
        updateMemoryTracker("heap", heapUsage.getUsed(), heapUsage.getMax());
        updateMemoryTracker("non_heap", nonHeapUsage.getUsed(), nonHeapUsage.getMax());
        
        // Calculate memory usage percentages
        double heapUsagePercent = (heapUsage.getUsed() * 100.0) / heapUsage.getMax();
        double nonHeapUsagePercent = (nonHeapUsage.getUsed() * 100.0) / nonHeapUsage.getMax();
        
        // Log memory status periodically
        if (System.currentTimeMillis() % 30000 < memoryCheckInterval) { // Every 30 seconds
            LOGGER.info("💾 Memory usage - Heap: " + String.format("%.1f%%", heapUsagePercent) + 
                       " (" + formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax()) + ")" +
                       ", Non-Heap: " + String.format("%.1f%%", nonHeapUsagePercent) + 
                       " (" + formatBytes(nonHeapUsage.getUsed()) + " / " + formatBytes(nonHeapUsage.getMax()) + ")");
        }
    }
    
    /**
     * Check memory thresholds and trigger actions
     */
    private void checkMemoryThresholds() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double heapUsagePercent = (heapUsage.getUsed() * 100.0) / heapUsage.getMax();
        
        if (heapUsagePercent >= memoryEmergencyThreshold) {
            // Emergency cleanup
            performEmergencyCleanup();
            emergencyCleanups.incrementAndGet();
        } else if (heapUsagePercent >= memoryCriticalThreshold) {
            // Critical memory usage - aggressive cleanup
            performAggressiveCleanup();
        } else if (heapUsagePercent >= memoryWarningThreshold) {
            // Warning level - gentle cleanup
            performGentleCleanup();
        }
    }
    
    /**
     * Optimize garbage collection
     */
    private void optimizeGarbageCollection() {
        if (!enableSmartGC) return;
        
        try {
            // Analyze current GC performance
            long totalGCTime = 0;
            long totalCollections = 0;
            
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                totalGCTime += gcBean.getCollectionTime();
                totalCollections += gcBean.getCollectionCount();
            }
            
            // Check if GC optimization is needed
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double heapUsagePercent = (heapUsage.getUsed() * 100.0) / heapUsage.getMax();
            
            if (heapUsagePercent > 70.0) {
                // Suggest GC if memory usage is high
                System.gc();
                gcOptimizations.incrementAndGet();
                
                // Wait a bit and check improvement
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                MemoryUsage newHeapUsage = memoryBean.getHeapMemoryUsage();
                long memoryFreedAmount = heapUsage.getUsed() - newHeapUsage.getUsed();
                
                if (memoryFreedAmount > 0) {
                    memoryFreed.addAndGet(memoryFreedAmount);
                    LOGGER.info("🗑️ GC freed " + formatBytes(memoryFreedAmount) + " of memory");
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("GC optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze GC performance
     */
    private void analyzeGCPerformance() {
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            String gcName = gcBean.getName();
            long currentTime = gcBean.getCollectionTime();
            Long lastTime = lastGCTimes.get(gcName);
            
            if (lastTime != null) {
                long timeDiff = currentTime - lastTime;
                if (timeDiff > 1000) { // More than 1 second of GC time
                    LOGGER.warning("⚠️ High GC time detected for " + gcName + ": " + timeDiff + "ms");
                }
            }
            
            lastGCTimes.put(gcName, currentTime);
        }
    }
    
    /**
     * Detect memory leaks
     */
    private void detectMemoryLeaks() {
        if (!enableMemoryLeakDetection) return;
        
        try {
            // Check for continuously growing memory usage
            for (Map.Entry<String, MemoryUsageTracker> entry : memoryTrackers.entrySet()) {
                MemoryUsageTracker tracker = entry.getValue();
                
                if (tracker.isMemoryLeakSuspected()) {
                    memoryLeaksDetected.incrementAndGet();
                    LOGGER.warning("🔍 Potential memory leak detected in: " + entry.getKey());
                    
                    // Trigger cleanup for this region
                    cleanupMemoryRegion(entry.getKey());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Memory leak detection error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze memory growth patterns
     */
    private void analyzeMemoryGrowth() {
        for (MemoryUsageTracker tracker : memoryTrackers.values()) {
            tracker.analyzeGrowthPattern();
        }
    }
    
    /**
     * Perform emergency cleanup
     */
    private void performEmergencyCleanup() {
        if (!enableEmergencyCleanup) return;
        
        LOGGER.warning("🚨 EMERGENCY MEMORY CLEANUP TRIGGERED!");
        
        try {
            // Clear all caches
            clearAllCaches();
            
            // Force aggressive GC
            for (int i = 0; i < 3; i++) {
                System.gc();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Clean up memory regions
            for (MemoryRegion region : memoryRegions) {
                cleanupMemoryRegion(region.getName());
            }
            
            totalMemoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.severe("Emergency cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Perform aggressive cleanup
     */
    private void performAggressiveCleanup() {
        LOGGER.warning("⚠️ Aggressive memory cleanup triggered");
        
        try {
            // Clear non-essential caches
            clearNonEssentialCaches();
            
            // Suggest GC
            System.gc();
            
            // Clean up specific regions
            cleanupMemoryRegion("cache");
            cleanupMemoryRegion("entities");
            
            totalMemoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Aggressive cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Perform gentle cleanup
     */
    private void performGentleCleanup() {
        try {
            // Light cache cleanup
            cleanupOldCacheEntries();
            
            // Optional GC suggestion
            if (Math.random() < 0.3) { // 30% chance
                System.gc();
            }
            
            totalMemoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Gentle cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Clear all caches
     */
    private void clearAllCaches() {
        memoryCache.clear();
        // Clear other caches through PlayLand systems
        clearPlayLandCaches();
        
        LOGGER.info("🧹 All caches cleared");
    }
    
    /**
     * Clear non-essential caches
     */
    private void clearNonEssentialCaches() {
        // Clear only non-critical caches
        int sizeBefore = memoryCache.size();
        memoryCache.entrySet().removeIf(entry -> !isCriticalCacheEntry(entry.getKey()));
        int sizeAfter = memoryCache.size();
        
        LOGGER.info("🧹 Cleared " + (sizeBefore - sizeAfter) + " non-essential cache entries");
    }
    
    /**
     * Cleanup old cache entries
     */
    private void cleanupOldCacheEntries() {
        // Remove old entries (simplified implementation)
        long cutoffTime = System.currentTimeMillis() - 300000; // 5 minutes
        
        memoryCache.entrySet().removeIf(entry -> {
            // This would check entry timestamp in real implementation
            return Math.random() < 0.1; // Remove 10% randomly for demo
        });
    }
    
    /**
     * Clear PlayLand system caches
     */
    private void clearPlayLandCaches() {
        // This would interface with other PlayLand systems to clear their caches
        // For now, it's a placeholder
    }
    
    /**
     * Check if cache entry is critical
     */
    private boolean isCriticalCacheEntry(String key) {
        return key.startsWith("player_") || key.startsWith("world_") || key.startsWith("chunk_");
    }
    
    /**
     * Cleanup specific memory region
     */
    private void cleanupMemoryRegion(String regionName) {
        try {
            switch (regionName) {
                case "cache":
                    clearNonEssentialCaches();
                    break;
                case "entities":
                    // Cleanup entity data
                    cleanupEntityData();
                    break;
                case "chunks":
                    // Cleanup chunk data
                    cleanupChunkData();
                    break;
                case "plugins":
                    // Cleanup plugin data
                    cleanupPluginData();
                    break;
                default:
                    LOGGER.fine("No specific cleanup for region: " + regionName);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Region cleanup error for " + regionName + ": " + e.getMessage());
        }
    }
    
    private void cleanupEntityData() {
        // Placeholder for entity data cleanup
        LOGGER.fine("Cleaning up entity data");
    }
    
    private void cleanupChunkData() {
        // Placeholder for chunk data cleanup
        LOGGER.fine("Cleaning up chunk data");
    }
    
    private void cleanupPluginData() {
        // Placeholder for plugin data cleanup
        LOGGER.fine("Cleaning up plugin data");
    }
    
    /**
     * Update memory tracker
     */
    private void updateMemoryTracker(String name, long used, long max) {
        MemoryUsageTracker tracker = memoryTrackers.computeIfAbsent(name, k -> new MemoryUsageTracker(name));
        tracker.updateUsage(used, max);
    }
    
    /**
     * Log current memory status
     */
    private void logMemoryStatus() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        LOGGER.info("💾 Current Memory Status:");
        LOGGER.info("   Heap: " + formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax()) + 
                   " (" + String.format("%.1f%%", (heapUsage.getUsed() * 100.0) / heapUsage.getMax()) + ")");
        LOGGER.info("   Non-Heap: " + formatBytes(nonHeapUsage.getUsed()) + " / " + formatBytes(nonHeapUsage.getMax()) + 
                   " (" + String.format("%.1f%%", (nonHeapUsage.getUsed() * 100.0) / nonHeapUsage.getMax()) + ")");
    }
    
    /**
     * Format bytes to human readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Get memory management statistics
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_memory_optimizations", totalMemoryOptimizations.get());
        stats.put("gc_optimizations", gcOptimizations.get());
        stats.put("memory_leaks_detected", memoryLeaksDetected.get());
        stats.put("memory_freed_bytes", memoryFreed.get());
        stats.put("memory_allocations_optimized", memoryAllocationsOptimized.get());
        stats.put("emergency_cleanups", emergencyCleanups.get());
        
        // Current memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        stats.put("heap_used", heapUsage.getUsed());
        stats.put("heap_max", heapUsage.getMax());
        stats.put("heap_usage_percent", (heapUsage.getUsed() * 100.0) / heapUsage.getMax());
        
        stats.put("non_heap_used", nonHeapUsage.getUsed());
        stats.put("non_heap_max", nonHeapUsage.getMax());
        stats.put("non_heap_usage_percent", (nonHeapUsage.getUsed() * 100.0) / nonHeapUsage.getMax());
        
        return stats;
    }
    
    // Getters
    public long getTotalMemoryOptimizations() { return totalMemoryOptimizations.get(); }
    public long getGcOptimizations() { return gcOptimizations.get(); }
    public long getMemoryLeaksDetected() { return memoryLeaksDetected.get(); }
    public long getMemoryFreed() { return memoryFreed.get(); }
    public long getMemoryAllocationsOptimized() { return memoryAllocationsOptimized.get(); }
    public long getEmergencyCleanups() { return emergencyCleanups.get(); }
    
    public void shutdown() {
        memoryManager.shutdown();
        memoryCache.clear();
        memoryTrackers.clear();
        
        LOGGER.info("💾 Advanced Memory Manager shutdown complete");
    }
    
    /**
     * Memory usage tracker
     */
    private static class MemoryUsageTracker {
        private final String name;
        private final List<Long> usageHistory = new ArrayList<>();
        private final List<Long> timestamps = new ArrayList<>();
        private long lastUsage = 0;
        private int consecutiveIncreases = 0;
        
        public MemoryUsageTracker(String name) {
            this.name = name;
        }
        
        public void updateUsage(long used, long max) {
            long currentTime = System.currentTimeMillis();
            
            usageHistory.add(used);
            timestamps.add(currentTime);
            
            // Keep only recent history (last 10 minutes)
            while (timestamps.size() > 0 && currentTime - timestamps.get(0) > 600000) {
                usageHistory.remove(0);
                timestamps.remove(0);
            }
            
            // Track consecutive increases
            if (used > lastUsage) {
                consecutiveIncreases++;
            } else {
                consecutiveIncreases = 0;
            }
            
            lastUsage = used;
        }
        
        public boolean isMemoryLeakSuspected() {
            // Simple heuristic: if memory keeps increasing for 5 consecutive measurements
            return consecutiveIncreases >= 5 && usageHistory.size() >= 5;
        }
        
        public void analyzeGrowthPattern() {
            if (usageHistory.size() < 3) return;
            
            // Calculate growth rate
            long oldUsage = usageHistory.get(0);
            long newUsage = usageHistory.get(usageHistory.size() - 1);
            long timeSpan = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
            
            if (timeSpan > 0) {
                double growthRate = (newUsage - oldUsage) / (timeSpan / 1000.0); // bytes per second
                
                if (growthRate > 1024 * 1024) { // More than 1MB per second growth
                    Logger.getLogger("PlayLand-AdvMemory").warning(
                        "⚠️ High memory growth rate detected for " + name + ": " + 
                        String.format("%.1f MB/s", growthRate / (1024.0 * 1024.0))
                    );
                }
            }
        }
    }
    
    /**
     * Memory region definition
     */
    private static class MemoryRegion {
        private final String name;
        private final String description;
        
        public MemoryRegion(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

}
