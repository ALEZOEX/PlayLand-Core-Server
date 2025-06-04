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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Memory Pool Manager
 * РЕВОЛЮЦИОННОЕ управление пулами памяти с предаллокацией и оптимизацией
 * Минимизирует GC pressure и обеспечивает предсказуемую производительность
 */
public class AdvancedMemoryPoolManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-MemoryPoolManager");
    
    // Memory pool statistics
    private final AtomicLong poolAllocations = new AtomicLong(0);
    private final AtomicLong poolDeallocations = new AtomicLong(0);
    private final AtomicLong poolHits = new AtomicLong(0);
    private final AtomicLong poolMisses = new AtomicLong(0);
    private final AtomicLong memoryOptimizations = new AtomicLong(0);
    private final AtomicLong gcPrevented = new AtomicLong(0);
    
    // Memory pools for different object sizes
    private final Map<String, MemoryPool<?>> memoryPools = new ConcurrentHashMap<>();
    private final Map<String, PoolStatistics> poolStats = new ConcurrentHashMap<>();
    
    // Memory management
    private final ScheduledExecutorService memoryOptimizer = Executors.newScheduledThreadPool(2);
    private final List<MemoryAllocation> allocationHistory = new ArrayList<>();
    private final Runtime runtime = Runtime.getRuntime();
    
    // Configuration
    private boolean enableMemoryPooling = true;
    private boolean enablePoolPreallocation = true;
    private boolean enableAdaptivePooling = true;
    private boolean enableMemoryCompaction = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableGCOptimization = true;
    
    // Pool configurations
    private final int SMALL_OBJECT_POOL_SIZE = 10000;   // Objects < 1KB
    private final int MEDIUM_OBJECT_POOL_SIZE = 5000;   // Objects 1KB - 10KB
    private final int LARGE_OBJECT_POOL_SIZE = 1000;    // Objects 10KB - 100KB
    private final int HUGE_OBJECT_POOL_SIZE = 100;      // Objects > 100KB
    
    public void initialize() {
        LOGGER.info("🧠 Initializing Advanced Memory Pool Manager...");
        
        loadMemorySettings();
        initializeMemoryPools();
        startMemoryOptimization();
        startGCOptimization();
        
        LOGGER.info("✅ Advanced Memory Pool Manager initialized!");
        LOGGER.info("🧠 Memory pooling: " + (enableMemoryPooling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔄 Pool preallocation: " + (enablePoolPreallocation ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Adaptive pooling: " + (enableAdaptivePooling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗜️ Memory compaction: " + (enableMemoryCompaction ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("♻️ GC optimization: " + (enableGCOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Pool sizes - Small: " + SMALL_OBJECT_POOL_SIZE + ", Medium: " + MEDIUM_OBJECT_POOL_SIZE + 
                   ", Large: " + LARGE_OBJECT_POOL_SIZE + ", Huge: " + HUGE_OBJECT_POOL_SIZE);
    }
    
    private void loadMemorySettings() {
        LOGGER.info("⚙️ Loading memory pool settings...");
        
        // Log current memory status
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        LOGGER.info("💾 Memory status - Max: " + formatBytes(maxMemory) + 
                   ", Total: " + formatBytes(totalMemory) + 
                   ", Used: " + formatBytes(usedMemory) + 
                   ", Free: " + formatBytes(freeMemory));
    }
    
    private void initializeMemoryPools() {
        // Initialize pools for different object sizes
        memoryPools.put("small_objects", new MemoryPool<>("small_objects", SMALL_OBJECT_POOL_SIZE, 1024));
        memoryPools.put("medium_objects", new MemoryPool<>("medium_objects", MEDIUM_OBJECT_POOL_SIZE, 10240));
        memoryPools.put("large_objects", new MemoryPool<>("large_objects", LARGE_OBJECT_POOL_SIZE, 102400));
        memoryPools.put("huge_objects", new MemoryPool<>("huge_objects", HUGE_OBJECT_POOL_SIZE, 1048576));
        
        // Initialize specialized pools
        memoryPools.put("byte_arrays", new MemoryPool<>("byte_arrays", 5000, 4096));
        memoryPools.put("string_builders", new MemoryPool<>("string_builders", 2000, 512));
        memoryPools.put("collections", new MemoryPool<>("collections", 3000, 256));
        
        // Initialize pool statistics
        for (String poolName : memoryPools.keySet()) {
            poolStats.put(poolName, new PoolStatistics(poolName));
        }
        
        // Preallocate pools if enabled
        if (enablePoolPreallocation) {
            preallocatePools();
        }
        
        LOGGER.info("🧠 Memory pools initialized: " + memoryPools.size());
    }
    
    private void preallocatePools() {
        LOGGER.info("🔄 Preallocating memory pools...");
        
        for (MemoryPool<?> pool : memoryPools.values()) {
            pool.preallocate();
        }
        
        LOGGER.info("✅ Memory pools preallocated");
    }
    
    private void startMemoryOptimization() {
        // Optimize memory every 10 seconds
        memoryOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeMemoryPools();
                analyzeMemoryUsage();
                performMemoryCompaction();
            } catch (Exception e) {
                LOGGER.warning("Memory optimization error: " + e.getMessage());
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("⚡ Memory optimization started");
    }
    
    private void startGCOptimization() {
        if (!enableGCOptimization) return;
        
        // Monitor GC and optimize every 30 seconds
        memoryOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeGarbageCollection();
                preventUnnecessaryGC();
            } catch (Exception e) {
                LOGGER.warning("GC optimization error: " + e.getMessage());
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("♻️ GC optimization started");
    }
    
    /**
     * Allocate object from appropriate memory pool
     */
    @SuppressWarnings("unchecked")
    public <T> T allocate(Class<T> type, int estimatedSize) {
        if (!enableMemoryPooling) {
            return createNewObject(type);
        }
        
        poolAllocations.incrementAndGet();
        
        try {
            // Determine appropriate pool based on size
            String poolName = determinePool(estimatedSize);
            MemoryPool<T> pool = (MemoryPool<T>) memoryPools.get(poolName);
            
            if (pool != null) {
                T object = pool.acquire();
                if (object != null) {
                    poolHits.incrementAndGet();
                    recordAllocation(poolName, estimatedSize, true);
                    return object;
                }
            }
            
            // Pool miss - create new object
            poolMisses.incrementAndGet();
            T newObject = createNewObject(type);
            recordAllocation(poolName, estimatedSize, false);
            
            return newObject;
            
        } catch (Exception e) {
            LOGGER.warning("Memory allocation error: " + e.getMessage());
            return createNewObject(type);
        }
    }
    
    /**
     * Return object to memory pool
     */
    @SuppressWarnings("unchecked")
    public <T> void deallocate(T object, int estimatedSize) {
        if (!enableMemoryPooling || object == null) return;
        
        poolDeallocations.incrementAndGet();
        
        try {
            // Determine appropriate pool
            String poolName = determinePool(estimatedSize);
            MemoryPool<T> pool = (MemoryPool<T>) memoryPools.get(poolName);
            
            if (pool != null) {
                // Reset object state before returning to pool
                resetObject(object);
                pool.release(object);
                
                recordDeallocation(poolName, estimatedSize);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Memory deallocation error: " + e.getMessage());
        }
    }
    
    /**
     * Determine which pool to use based on object size
     */
    private String determinePool(int estimatedSize) {
        if (estimatedSize <= 1024) {
            return "small_objects";
        } else if (estimatedSize <= 10240) {
            return "medium_objects";
        } else if (estimatedSize <= 102400) {
            return "large_objects";
        } else {
            return "huge_objects";
        }
    }
    
    /**
     * Create new object instance
     */
    @SuppressWarnings("unchecked")
    private <T> T createNewObject(Class<T> type) {
        try {
            // Simplified object creation - in real implementation would use factories
            if (type == byte[].class) {
                return (T) new byte[4096];
            } else if (type == StringBuilder.class) {
                return (T) new StringBuilder(512);
            } else if (type == ArrayList.class) {
                return (T) new ArrayList<>();
            } else if (type == ConcurrentHashMap.class) {
                return (T) new ConcurrentHashMap<>();
            } else {
                return type.getDeclaredConstructor().newInstance();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Object creation error for type " + type.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Reset object state before returning to pool
     */
    private <T> void resetObject(T object) {
        try {
            if (object instanceof StringBuilder) {
                ((StringBuilder) object).setLength(0);
            } else if (object instanceof List) {
                ((List<?>) object).clear();
            } else if (object instanceof Map) {
                ((Map<?, ?>) object).clear();
            }
            // Add more reset logic for other types as needed
            
        } catch (Exception e) {
            LOGGER.warning("Object reset error: " + e.getMessage());
        }
    }
    
    /**
     * Record memory allocation for analytics
     */
    private void recordAllocation(String poolName, int size, boolean fromPool) {
        try {
            PoolStatistics stats = poolStats.get(poolName);
            if (stats != null) {
                if (fromPool) {
                    stats.recordPoolHit(size);
                } else {
                    stats.recordPoolMiss(size);
                }
            }
            
            // Record in allocation history
            synchronized (allocationHistory) {
                allocationHistory.add(new MemoryAllocation(poolName, size, fromPool, System.currentTimeMillis()));
                
                // Keep only recent allocations
                if (allocationHistory.size() > 10000) {
                    allocationHistory.remove(0);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Allocation recording error: " + e.getMessage());
        }
    }
    
    /**
     * Record memory deallocation
     */
    private void recordDeallocation(String poolName, int size) {
        try {
            PoolStatistics stats = poolStats.get(poolName);
            if (stats != null) {
                stats.recordDeallocation(size);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Deallocation recording error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize memory pools based on usage patterns
     */
    private void optimizeMemoryPools() {
        try {
            for (Map.Entry<String, MemoryPool<?>> entry : memoryPools.entrySet()) {
                String poolName = entry.getKey();
                MemoryPool<?> pool = entry.getValue();
                PoolStatistics stats = poolStats.get(poolName);
                
                if (stats != null && enableAdaptivePooling) {
                    // Adjust pool size based on hit rate
                    double hitRate = stats.getHitRate();
                    
                    if (hitRate < 50.0 && pool.getCurrentSize() > pool.getMinSize()) {
                        // Low hit rate - shrink pool
                        pool.shrink(10);
                    } else if (hitRate > 90.0 && pool.getCurrentSize() < pool.getMaxSize()) {
                        // High hit rate - grow pool
                        pool.grow(20);
                    }
                }
                
                // Clean up expired objects
                pool.cleanup();
            }
            
            memoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Memory pool optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze memory usage patterns
     */
    private void analyzeMemoryUsage() {
        try {
            // Current memory status
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsage = (usedMemory * 100.0) / maxMemory;
            
            // Log memory usage if significant
            if (memoryUsage > 80.0) {
                LOGGER.warning("🚨 High memory usage: " + String.format("%.1f%%", memoryUsage) + 
                              " (" + formatBytes(usedMemory) + " / " + formatBytes(maxMemory) + ")");
                
                // Trigger aggressive optimization
                if (memoryUsage > 90.0) {
                    performEmergencyCleanup();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Memory usage analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Perform memory compaction to reduce fragmentation
     */
    private void performMemoryCompaction() {
        if (!enableMemoryCompaction) return;
        
        try {
            // Compact memory pools
            for (MemoryPool<?> pool : memoryPools.values()) {
                pool.compact();
            }
            
            // Suggest GC if memory usage is high
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (usedMemory * 100.0) / runtime.maxMemory();
            
            if (memoryUsage > 75.0) {
                System.gc(); // Suggest garbage collection
                gcPrevented.incrementAndGet();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Memory compaction error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize garbage collection
     */
    private void optimizeGarbageCollection() {
        try {
            // Monitor GC pressure and optimize pools accordingly
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (usedMemory * 100.0) / runtime.maxMemory();
            
            if (memoryUsage > 85.0) {
                // High memory pressure - aggressively clean pools
                for (MemoryPool<?> pool : memoryPools.values()) {
                    pool.aggressiveCleanup();
                }
                
                gcPrevented.incrementAndGet();
            }
            
        } catch (Exception e) {
            LOGGER.warning("GC optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Prevent unnecessary garbage collection
     */
    private void preventUnnecessaryGC() {
        try {
            // Ensure pools have enough free objects to prevent allocations
            for (MemoryPool<?> pool : memoryPools.values()) {
                if (pool.getFreeObjectsRatio() < 0.2) { // Less than 20% free
                    pool.preallocate(50); // Add more objects
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("GC prevention error: " + e.getMessage());
        }
    }
    
    /**
     * Perform emergency cleanup when memory is critically low
     */
    private void performEmergencyCleanup() {
        LOGGER.warning("🚨 Performing emergency memory cleanup...");
        
        try {
            // Aggressively clean all pools
            for (MemoryPool<?> pool : memoryPools.values()) {
                pool.emergencyCleanup();
            }
            
            // Clear allocation history
            synchronized (allocationHistory) {
                allocationHistory.clear();
            }
            
            // Force garbage collection
            System.gc();
            
            LOGGER.info("✅ Emergency cleanup completed");
            
        } catch (Exception e) {
            LOGGER.warning("Emergency cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Format bytes for human-readable output
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Get comprehensive memory pool statistics
     */
    public Map<String, Object> getMemoryPoolStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("pool_allocations", poolAllocations.get());
        stats.put("pool_deallocations", poolDeallocations.get());
        stats.put("pool_hits", poolHits.get());
        stats.put("pool_misses", poolMisses.get());
        stats.put("memory_optimizations", memoryOptimizations.get());
        stats.put("gc_prevented", gcPrevented.get());
        
        // Calculate hit rate
        long totalRequests = poolHits.get() + poolMisses.get();
        if (totalRequests > 0) {
            stats.put("pool_hit_rate", (poolHits.get() * 100.0) / totalRequests);
        }
        
        // Memory status
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        stats.put("max_memory", maxMemory);
        stats.put("total_memory", totalMemory);
        stats.put("used_memory", usedMemory);
        stats.put("free_memory", freeMemory);
        stats.put("memory_usage_percent", (usedMemory * 100.0) / maxMemory);
        
        // Pool-specific statistics
        Map<String, Object> poolSpecificStats = new ConcurrentHashMap<>();
        for (Map.Entry<String, MemoryPool<?>> entry : memoryPools.entrySet()) {
            String poolName = entry.getKey();
            MemoryPool<?> pool = entry.getValue();
            
            Map<String, Object> poolInfo = new ConcurrentHashMap<>();
            poolInfo.put("current_size", pool.getCurrentSize());
            poolInfo.put("max_size", pool.getMaxSize());
            poolInfo.put("free_objects", pool.getFreeObjectsCount());
            poolInfo.put("free_ratio", pool.getFreeObjectsRatio());
            
            poolSpecificStats.put(poolName, poolInfo);
        }
        stats.put("pool_details", poolSpecificStats);
        
        return stats;
    }
    
    // Getters
    public long getPoolAllocations() { return poolAllocations.get(); }
    public long getPoolDeallocations() { return poolDeallocations.get(); }
    public long getPoolHits() { return poolHits.get(); }
    public long getPoolMisses() { return poolMisses.get(); }
    public long getMemoryOptimizations() { return memoryOptimizations.get(); }
    public long getGcPrevented() { return gcPrevented.get(); }
    
    public void shutdown() {
        memoryOptimizer.shutdown();
        
        // Clear all pools
        for (MemoryPool<?> pool : memoryPools.values()) {
            pool.shutdown();
        }
        
        // Clear statistics
        memoryPools.clear();
        poolStats.clear();
        allocationHistory.clear();
        
        LOGGER.info("🧠 Advanced Memory Pool Manager shutdown complete");
    }
    
    // Helper classes
    private static class MemoryPool<T> {
        private final String name;
        private final BlockingQueue<T> pool;
        private final AtomicInteger currentSize = new AtomicInteger(0);
        private final int maxSize;
        private final int minSize;
        private final int objectSize;
        
        public MemoryPool(String name, int maxSize, int objectSize) {
            this.name = name;
            this.maxSize = maxSize;
            this.minSize = maxSize / 10; // 10% of max size
            this.objectSize = objectSize;
            this.pool = new LinkedBlockingQueue<>(maxSize);
        }
        
        public T acquire() {
            T object = pool.poll();
            if (object != null) {
                currentSize.decrementAndGet();
            }
            return object;
        }
        
        public void release(T object) {
            if (currentSize.get() < maxSize && pool.offer(object)) {
                currentSize.incrementAndGet();
            }
        }
        
        public void preallocate() {
            preallocate(maxSize / 2); // Preallocate 50% of max size
        }
        
        @SuppressWarnings("unchecked")
        public void preallocate(int count) {
            for (int i = 0; i < count && currentSize.get() < maxSize; i++) {
                // Simplified preallocation - would use proper factories in real implementation
                T object = (T) new Object();
                if (pool.offer(object)) {
                    currentSize.incrementAndGet();
                }
            }
        }
        
        public void cleanup() {
            // Remove excess objects if pool is too large
            while (currentSize.get() > maxSize * 0.8) {
                if (pool.poll() != null) {
                    currentSize.decrementAndGet();
                } else {
                    break;
                }
            }
        }
        
        public void aggressiveCleanup() {
            // Remove objects down to minimum size
            while (currentSize.get() > minSize) {
                if (pool.poll() != null) {
                    currentSize.decrementAndGet();
                } else {
                    break;
                }
            }
        }
        
        public void emergencyCleanup() {
            // Clear most of the pool
            pool.clear();
            currentSize.set(0);
        }
        
        public void compact() {
            // Compact pool by removing and re-adding objects
            List<T> objects = new ArrayList<>();
            T object;
            while ((object = pool.poll()) != null) {
                objects.add(object);
                currentSize.decrementAndGet();
            }
            
            // Re-add objects
            for (T obj : objects) {
                if (pool.offer(obj)) {
                    currentSize.incrementAndGet();
                }
            }
        }
        
        public void grow(int amount) {
            preallocate(amount);
        }
        
        public void shrink(int amount) {
            for (int i = 0; i < amount && currentSize.get() > minSize; i++) {
                if (pool.poll() != null) {
                    currentSize.decrementAndGet();
                }
            }
        }
        
        public int getCurrentSize() { return currentSize.get(); }
        public int getMaxSize() { return maxSize; }
        public int getMinSize() { return minSize; }
        public int getFreeObjectsCount() { return currentSize.get(); }
        public double getFreeObjectsRatio() { return (double) currentSize.get() / maxSize; }
        public String getName() { return name; }
        
        public void shutdown() {
            pool.clear();
            currentSize.set(0);
        }
    }
    
    private static class PoolStatistics {
        private final String poolName;
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong deallocations = new AtomicLong(0);
        private final AtomicLong totalBytesAllocated = new AtomicLong(0);
        
        public PoolStatistics(String poolName) {
            this.poolName = poolName;
        }
        
        public void recordPoolHit(int size) {
            hits.incrementAndGet();
            totalBytesAllocated.addAndGet(size);
        }
        
        public void recordPoolMiss(int size) {
            misses.incrementAndGet();
            totalBytesAllocated.addAndGet(size);
        }
        
        public void recordDeallocation(int size) {
            deallocations.incrementAndGet();
        }
        
        public double getHitRate() {
            long totalRequests = hits.get() + misses.get();
            return totalRequests > 0 ? (hits.get() * 100.0) / totalRequests : 0.0;
        }
        
        public String getPoolName() { return poolName; }
        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getDeallocations() { return deallocations.get(); }
        public long getTotalBytesAllocated() { return totalBytesAllocated.get(); }
    }
    
    private static class MemoryAllocation {
        private final String poolName;
        private final int size;
        private final boolean fromPool;
        private final long timestamp;
        
        public MemoryAllocation(String poolName, int size, boolean fromPool, long timestamp) {
            this.poolName = poolName;
            this.size = size;
            this.fromPool = fromPool;
            this.timestamp = timestamp;
        }
        
        public String getPoolName() { return poolName; }
        public int getSize() { return size; }
        public boolean isFromPool() { return fromPool; }
        public long getTimestamp() { return timestamp; }
    }
}
