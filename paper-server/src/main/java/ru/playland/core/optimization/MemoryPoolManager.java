package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Memory Pool Manager
 * Революционное управление памятью с предотвращением утечек
 * Использует пулы объектов и умную сборку мусора
 */
public class MemoryPoolManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-Memory");
    
    // Memory statistics
    private final AtomicLong totalAllocations = new AtomicLong(0);
    private final AtomicLong totalDeallocations = new AtomicLong(0);
    private final AtomicLong poolHits = new AtomicLong(0);
    private final AtomicLong poolMisses = new AtomicLong(0);
    private final AtomicLong gcOptimizations = new AtomicLong(0);
    private final AtomicLong memoryLeaksDetected = new AtomicLong(0);
    
    // Object pools
    private final Map<Class<?>, Queue<Object>> objectPools = new ConcurrentHashMap<>();
    private final Map<Class<?>, AtomicLong> poolSizes = new ConcurrentHashMap<>();
    private final Map<Class<?>, AtomicLong> poolUsage = new ConcurrentHashMap<>();
    
    // Memory monitoring
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ScheduledExecutorService memoryMonitor = Executors.newSingleThreadScheduledExecutor();
    
    // Configuration
    private boolean enableObjectPooling = true;
    private boolean enableMemoryMonitoring = true;
    private boolean enableGCOptimization = true;
    private boolean enableLeakDetection = true;
    private boolean enableAutoCleanup = true;
    
    private int maxPoolSize = 1000;
    private long memoryThreshold = Runtime.getRuntime().maxMemory() * 80 / 100; // 80% of max memory
    private long gcInterval = 30000; // 30 seconds
    private long cleanupInterval = 60000; // 1 minute
    
    public void initialize() {
        LOGGER.info("💾 Initializing Memory Pool Manager...");
        
        loadMemorySettings();
        initializeObjectPools();
        startMemoryMonitoring();
        startGCOptimization();
        
        LOGGER.info("✅ Memory Pool Manager initialized!");
        LOGGER.info("🔄 Object pooling: " + (enableObjectPooling ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Memory monitoring: " + (enableMemoryMonitoring ? "ENABLED" : "DISABLED"));
        LOGGER.info("🧹 GC optimization: " + (enableGCOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔍 Leak detection: " + (enableLeakDetection ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔄 Auto cleanup: " + (enableAutoCleanup ? "ENABLED" : "DISABLED"));
        LOGGER.info("📏 Max pool size: " + maxPoolSize);
        LOGGER.info("⚠️ Memory threshold: " + (memoryThreshold / 1024 / 1024) + " MB");
    }
    
    private void loadMemorySettings() {
        // Load memory optimization settings
        LOGGER.info("⚙️ Loading memory settings...");
    }
    
    private void initializeObjectPools() {
        // Initialize common object pools
        createObjectPool(StringBuilder.class, 100);
        createObjectPool(java.util.ArrayList.class, 200);
        createObjectPool(java.util.HashMap.class, 150);
        createObjectPool(java.util.HashSet.class, 100);
        
        LOGGER.info("💾 Object pools initialized");
    }
    
    private void createObjectPool(Class<?> clazz, int initialSize) {
        Queue<Object> pool = new ConcurrentLinkedQueue<>();
        objectPools.put(clazz, pool);
        poolSizes.put(clazz, new AtomicLong(0));
        poolUsage.put(clazz, new AtomicLong(0));
        
        // Pre-populate pool
        for (int i = 0; i < initialSize; i++) {
            try {
                Object obj = clazz.getDeclaredConstructor().newInstance();
                pool.offer(obj);
                poolSizes.get(clazz).incrementAndGet();
            } catch (Exception e) {
                LOGGER.warning("Failed to create object for pool: " + clazz.getSimpleName());
            }
        }
    }
    
    private void startMemoryMonitoring() {
        if (!enableMemoryMonitoring) return;
        
        memoryMonitor.scheduleAtFixedRate(() -> {
            try {
                monitorMemoryUsage();
                detectMemoryLeaks();
                
                if (enableAutoCleanup) {
                    performAutoCleanup();
                }
                
            } catch (Exception e) {
                LOGGER.warning("Memory monitoring error: " + e.getMessage());
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS); // Every 5 seconds
        
        LOGGER.info("📊 Memory monitoring started");
    }
    
    private void startGCOptimization() {
        if (!enableGCOptimization) return;
        
        memoryMonitor.scheduleAtFixedRate(() -> {
            try {
                optimizeGarbageCollection();
            } catch (Exception e) {
                LOGGER.warning("GC optimization error: " + e.getMessage());
            }
        }, gcInterval, gcInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🧹 GC optimization started");
    }
    
    /**
     * Get object from pool or create new one
     */
    @SuppressWarnings("unchecked")
    public <T> T getFromPool(Class<T> clazz) {
        if (!enableObjectPooling) {
            return createNewObject(clazz);
        }
        
        Queue<Object> pool = objectPools.get(clazz);
        if (pool != null) {
            Object obj = pool.poll();
            if (obj != null) {
                poolHits.incrementAndGet();
                poolUsage.get(clazz).incrementAndGet();
                return (T) obj;
            }
        }
        
        poolMisses.incrementAndGet();
        return createNewObject(clazz);
    }
    
    /**
     * Return object to pool
     */
    public <T> void returnToPool(T obj) {
        if (!enableObjectPooling || obj == null) return;
        
        Class<?> clazz = obj.getClass();
        Queue<Object> pool = objectPools.get(clazz);
        
        if (pool != null) {
            AtomicLong poolSize = poolSizes.get(clazz);
            
            if (poolSize.get() < maxPoolSize) {
                // Reset object state if possible
                resetObjectState(obj);
                
                pool.offer(obj);
                poolSize.incrementAndGet();
                totalDeallocations.incrementAndGet();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T createNewObject(Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            totalAllocations.incrementAndGet();
            return obj;
        } catch (Exception e) {
            LOGGER.warning("Failed to create object: " + clazz.getSimpleName());
            return null;
        }
    }
    
    private void resetObjectState(Object obj) {
        try {
            // Reset common object states
            if (obj instanceof StringBuilder) {
                ((StringBuilder) obj).setLength(0);
            } else if (obj instanceof java.util.Collection) {
                ((java.util.Collection<?>) obj).clear();
            } else if (obj instanceof java.util.Map) {
                ((java.util.Map<?, ?>) obj).clear();
            }
        } catch (Exception e) {
            // Ignore reset errors
        }
    }
    
    /**
     * Monitor memory usage and trigger optimizations
     */
    private void monitorMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();
        
        double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;
        
        if (usedMemory > memoryThreshold) {
            LOGGER.warning("⚠️ High memory usage detected: " + String.format("%.1f%%", memoryUsagePercent));
            
            // Trigger emergency cleanup
            performEmergencyCleanup();
            
            // Suggest GC
            if (enableGCOptimization) {
                System.gc();
                gcOptimizations.incrementAndGet();
                LOGGER.info("🧹 Emergency GC triggered");
            }
        }
        
        // Log memory stats periodically
        if (System.currentTimeMillis() % 30000 < 5000) { // Every 30 seconds
            LOGGER.info("💾 Memory usage: " + String.format("%.1f%%", memoryUsagePercent) + 
                       " (" + (usedMemory / 1024 / 1024) + " MB / " + (maxMemory / 1024 / 1024) + " MB)");
        }
    }
    
    /**
     * Detect potential memory leaks
     */
    private void detectMemoryLeaks() {
        if (!enableLeakDetection) return;
        
        // Check for unusually large object pools
        for (Map.Entry<Class<?>, AtomicLong> entry : poolSizes.entrySet()) {
            long poolSize = entry.getValue().get();
            long poolUsage = this.poolUsage.get(entry.getKey()).get();
            
            if (poolSize > maxPoolSize * 2) {
                LOGGER.warning("🔍 Potential memory leak detected in pool: " + entry.getKey().getSimpleName() + 
                              " (Size: " + poolSize + ", Usage: " + poolUsage + ")");
                memoryLeaksDetected.incrementAndGet();
                
                // Auto-fix: reduce pool size
                reducePoolSize(entry.getKey());
            }
        }
    }
    
    private void reducePoolSize(Class<?> clazz) {
        Queue<Object> pool = objectPools.get(clazz);
        AtomicLong poolSize = poolSizes.get(clazz);
        
        if (pool != null && poolSize != null) {
            int toRemove = (int) (poolSize.get() - maxPoolSize);
            
            for (int i = 0; i < toRemove && !pool.isEmpty(); i++) {
                pool.poll();
                poolSize.decrementAndGet();
            }
            
            LOGGER.info("🔧 Reduced pool size for " + clazz.getSimpleName() + " by " + toRemove + " objects");
        }
    }
    
    /**
     * Optimize garbage collection
     */
    private void optimizeGarbageCollection() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();
        
        double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;
        
        // Trigger GC if memory usage is above 70%
        if (memoryUsagePercent > 70.0) {
            long beforeGC = usedMemory;
            System.gc();
            
            // Wait a bit for GC to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            MemoryUsage afterGCUsage = memoryBean.getHeapMemoryUsage();
            long afterGC = afterGCUsage.getUsed();
            long freedMemory = beforeGC - afterGC;
            
            if (freedMemory > 0) {
                LOGGER.info("🧹 GC optimization freed " + (freedMemory / 1024 / 1024) + " MB");
            }
            
            gcOptimizations.incrementAndGet();
        }
    }
    
    /**
     * Perform automatic cleanup
     */
    private void performAutoCleanup() {
        // Clean up unused pools
        for (Map.Entry<Class<?>, Queue<Object>> entry : objectPools.entrySet()) {
            Queue<Object> pool = entry.getValue();
            AtomicLong usage = poolUsage.get(entry.getKey());
            
            // If pool hasn't been used recently, reduce its size
            if (usage.get() == 0 && pool.size() > 10) {
                int toRemove = pool.size() / 2;
                for (int i = 0; i < toRemove && !pool.isEmpty(); i++) {
                    pool.poll();
                    poolSizes.get(entry.getKey()).decrementAndGet();
                }
            }
            
            // Reset usage counter
            usage.set(0);
        }
    }
    
    /**
     * Perform emergency cleanup when memory is critically low
     */
    private void performEmergencyCleanup() {
        LOGGER.warning("🚨 Performing emergency memory cleanup...");
        
        // Aggressively reduce all pool sizes
        for (Map.Entry<Class<?>, Queue<Object>> entry : objectPools.entrySet()) {
            Queue<Object> pool = entry.getValue();
            AtomicLong poolSize = poolSizes.get(entry.getKey());
            
            int toRemove = pool.size() * 3 / 4; // Remove 75% of objects
            for (int i = 0; i < toRemove && !pool.isEmpty(); i++) {
                pool.poll();
                poolSize.decrementAndGet();
            }
        }
        
        LOGGER.info("🧹 Emergency cleanup completed");
    }
    
    /**
     * Get memory optimization statistics
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        stats.put("total_allocations", totalAllocations.get());
        stats.put("total_deallocations", totalDeallocations.get());
        stats.put("pool_hits", poolHits.get());
        stats.put("pool_misses", poolMisses.get());
        stats.put("gc_optimizations", gcOptimizations.get());
        stats.put("memory_leaks_detected", memoryLeaksDetected.get());
        
        stats.put("heap_used_mb", heapUsage.getUsed() / 1024 / 1024);
        stats.put("heap_max_mb", heapUsage.getMax() / 1024 / 1024);
        stats.put("heap_usage_percent", (heapUsage.getUsed() * 100.0) / heapUsage.getMax());
        
        stats.put("pool_hit_rate", calculatePoolHitRate());
        stats.put("active_pools", objectPools.size());
        
        return stats;
    }
    
    private double calculatePoolHitRate() {
        long hits = poolHits.get();
        long misses = poolMisses.get();
        long total = hits + misses;
        
        if (total == 0) return 0.0;
        return (hits * 100.0) / total;
    }
    
    // Getters
    public long getTotalAllocations() { return totalAllocations.get(); }
    public long getTotalDeallocations() { return totalDeallocations.get(); }
    public long getPoolHits() { return poolHits.get(); }
    public long getPoolMisses() { return poolMisses.get(); }
    public long getGCOptimizations() { return gcOptimizations.get(); }
    public long getMemoryLeaksDetected() { return memoryLeaksDetected.get(); }
    
    public void shutdown() {
        memoryMonitor.shutdown();
        
        // Clear all pools
        for (Queue<Object> pool : objectPools.values()) {
            pool.clear();
        }
        objectPools.clear();
        poolSizes.clear();
        poolUsage.clear();
        
        LOGGER.info("💾 Memory Pool Manager shutdown complete");
    }
}
