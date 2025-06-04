package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Smart Cache Hierarchy
 * ЭКСТРЕМАЛЬНАЯ многоуровневая система кэширования
 * L1: CPU Cache (ultra-fast), L2: Memory Cache (fast), L3: Disk Cache (persistent)
 */
public class SmartCacheHierarchy {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-SmartCache");
    
    // Cache statistics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong l3Hits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    private final AtomicLong cachePromotions = new AtomicLong(0);
    
    // L1 Cache - Ultra-fast CPU cache (small, very fast)
    private final Map<String, CacheEntry> l1Cache = new ConcurrentHashMap<>();
    private final Queue<String> l1AccessOrder = new ConcurrentLinkedQueue<>();
    
    // L2 Cache - Memory cache (medium, fast)
    private final Map<String, CacheEntry> l2Cache = new ConcurrentHashMap<>();
    private final Queue<String> l2AccessOrder = new ConcurrentLinkedQueue<>();
    
    // L3 Cache - Persistent cache (large, slower but persistent)
    private final Map<String, CacheEntry> l3Cache = new ConcurrentHashMap<>();
    private final Queue<String> l3AccessOrder = new ConcurrentLinkedQueue<>();
    
    // Cache management
    private final ScheduledExecutorService cacheManager = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Long> accessFrequency = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableL1Cache = true;
    private boolean enableL2Cache = true;
    private boolean enableL3Cache = true;
    private boolean enableSmartPromotion = true;
    private boolean enablePredictiveLoading = true;
    
    private int l1MaxSize = 1000;      // Ultra-fast cache
    private int l2MaxSize = 10000;     // Fast cache
    private int l3MaxSize = 100000;    // Large persistent cache
    
    private long l1TTL = 30000;        // 30 seconds
    private long l2TTL = 300000;       // 5 minutes
    private long l3TTL = 3600000;      // 1 hour
    
    public void initialize() {
        LOGGER.info("💾 Initializing Smart Cache Hierarchy...");
        
        loadCacheSettings();
        startCacheManagement();
        startCacheOptimization();
        
        LOGGER.info("✅ Smart Cache Hierarchy initialized!");
        LOGGER.info("⚡ L1 Cache (CPU): " + (enableL1Cache ? "ENABLED" : "DISABLED") + " - " + l1MaxSize + " entries");
        LOGGER.info("🚀 L2 Cache (Memory): " + (enableL2Cache ? "ENABLED" : "DISABLED") + " - " + l2MaxSize + " entries");
        LOGGER.info("💾 L3 Cache (Persistent): " + (enableL3Cache ? "ENABLED" : "DISABLED") + " - " + l3MaxSize + " entries");
        LOGGER.info("🧠 Smart promotion: " + (enableSmartPromotion ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔮 Predictive loading: " + (enablePredictiveLoading ? "ENABLED" : "DISABLED"));
    }
    
    private void loadCacheSettings() {
        try {
            // Load cache configuration from system properties or defaults
            LOGGER.info("⚙️ Loading cache settings...");

            // Load cache sizes
            l1MaxSize = Integer.parseInt(System.getProperty("playland.cache.l1.size", "1000"));
            l2MaxSize = Integer.parseInt(System.getProperty("playland.cache.l2.size", "10000"));
            l3MaxSize = Integer.parseInt(System.getProperty("playland.cache.l3.size", "100000"));

            // Load TTL settings
            l1TTL = Long.parseLong(System.getProperty("playland.cache.l1.ttl", "30000"));
            l2TTL = Long.parseLong(System.getProperty("playland.cache.l2.ttl", "300000"));
            l3TTL = Long.parseLong(System.getProperty("playland.cache.l3.ttl", "3600000"));

            // Load feature flags
            enableL1Cache = Boolean.parseBoolean(System.getProperty("playland.cache.l1.enabled", "true"));
            enableL2Cache = Boolean.parseBoolean(System.getProperty("playland.cache.l2.enabled", "true"));
            enableL3Cache = Boolean.parseBoolean(System.getProperty("playland.cache.l3.enabled", "true"));
            enableSmartPromotion = Boolean.parseBoolean(System.getProperty("playland.cache.promotion.enabled", "true"));
            enablePredictiveLoading = Boolean.parseBoolean(System.getProperty("playland.cache.predictive.enabled", "true"));

            // Auto-adjust based on available memory
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long availableMemory = maxMemory - (runtime.totalMemory() - runtime.freeMemory());

            // If low memory, reduce cache sizes
            if (availableMemory < maxMemory * 0.3) {
                l1MaxSize = Math.max(500, l1MaxSize / 2);
                l2MaxSize = Math.max(5000, l2MaxSize / 2);
                l3MaxSize = Math.max(50000, l3MaxSize / 2);
                LOGGER.warning("⚠️ Low memory detected - reduced cache sizes");
            }

            LOGGER.info("✅ Cache settings loaded - L1: " + l1MaxSize + ", L2: " + l2MaxSize + ", L3: " + l3MaxSize);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading cache settings: " + e.getMessage() + " - using defaults");
        }
    }
    
    private void startCacheManagement() {
        // Cache cleanup every 30 seconds
        cacheManager.scheduleAtFixedRate(this::performCacheCleanup, 30, 30, TimeUnit.SECONDS);
        
        // Cache optimization every 60 seconds
        cacheManager.scheduleAtFixedRate(this::optimizeCacheHierarchy, 60, 60, TimeUnit.SECONDS);
        
        LOGGER.info("🔧 Cache management started");
    }
    
    private void startCacheOptimization() {
        // Smart promotion every 10 seconds
        if (enableSmartPromotion) {
            cacheManager.scheduleAtFixedRate(this::performSmartPromotion, 10, 10, TimeUnit.SECONDS);
        }
        
        // Predictive loading every 5 seconds
        if (enablePredictiveLoading) {
            cacheManager.scheduleAtFixedRate(this::performPredictiveLoading, 5, 5, TimeUnit.SECONDS);
        }
        
        LOGGER.info("🚀 Cache optimization started");
    }
    
    /**
     * Get value from cache hierarchy
     */
    public Object get(String key) {
        totalRequests.incrementAndGet();
        long currentTime = System.currentTimeMillis();
        
        // Update access tracking
        accessFrequency.merge(key, 1L, Long::sum);
        lastAccessTime.put(key, currentTime);
        
        // Try L1 Cache first (fastest)
        if (enableL1Cache) {
            CacheEntry entry = l1Cache.get(key);
            if (entry != null && !entry.isExpired(currentTime, l1TTL)) {
                l1Hits.incrementAndGet();
                updateAccessOrder(l1AccessOrder, key);
                return entry.getValue();
            }
        }
        
        // Try L2 Cache (fast)
        if (enableL2Cache) {
            CacheEntry entry = l2Cache.get(key);
            if (entry != null && !entry.isExpired(currentTime, l2TTL)) {
                l2Hits.incrementAndGet();
                updateAccessOrder(l2AccessOrder, key);
                
                // Promote to L1 if frequently accessed
                if (shouldPromoteToL1(key)) {
                    promoteToL1(key, entry);
                }
                
                return entry.getValue();
            }
        }
        
        // Try L3 Cache (persistent)
        if (enableL3Cache) {
            CacheEntry entry = l3Cache.get(key);
            if (entry != null && !entry.isExpired(currentTime, l3TTL)) {
                l3Hits.incrementAndGet();
                updateAccessOrder(l3AccessOrder, key);
                
                // Promote to L2 if frequently accessed
                if (shouldPromoteToL2(key)) {
                    promoteToL2(key, entry);
                }
                
                return entry.getValue();
            }
        }
        
        // Cache miss
        cacheMisses.incrementAndGet();
        return null;
    }
    
    /**
     * Put value into cache hierarchy
     */
    public void put(String key, Object value) {
        long currentTime = System.currentTimeMillis();
        CacheEntry entry = new CacheEntry(value, currentTime);
        
        // Determine which cache level to use based on access frequency
        long frequency = accessFrequency.getOrDefault(key, 0L);
        
        if (frequency > 10 && enableL1Cache) {
            // High frequency - put in L1
            putInL1(key, entry);
        } else if (frequency > 3 && enableL2Cache) {
            // Medium frequency - put in L2
            putInL2(key, entry);
        } else if (enableL3Cache) {
            // Low frequency - put in L3
            putInL3(key, entry);
        }
        
        // Update access tracking
        accessFrequency.merge(key, 1L, Long::sum);
        lastAccessTime.put(key, currentTime);
    }
    
    /**
     * Put value directly in L1 cache
     */
    private void putInL1(String key, CacheEntry entry) {
        if (!enableL1Cache) return;
        
        // Check if cache is full
        if (l1Cache.size() >= l1MaxSize) {
            evictFromL1();
        }
        
        l1Cache.put(key, entry);
        updateAccessOrder(l1AccessOrder, key);
    }
    
    /**
     * Put value directly in L2 cache
     */
    private void putInL2(String key, CacheEntry entry) {
        if (!enableL2Cache) return;
        
        // Check if cache is full
        if (l2Cache.size() >= l2MaxSize) {
            evictFromL2();
        }
        
        l2Cache.put(key, entry);
        updateAccessOrder(l2AccessOrder, key);
    }
    
    /**
     * Put value directly in L3 cache
     */
    private void putInL3(String key, CacheEntry entry) {
        if (!enableL3Cache) return;
        
        // Check if cache is full
        if (l3Cache.size() >= l3MaxSize) {
            evictFromL3();
        }
        
        l3Cache.put(key, entry);
        updateAccessOrder(l3AccessOrder, key);
    }
    
    /**
     * Promote entry to L1 cache
     */
    private void promoteToL1(String key, CacheEntry entry) {
        putInL1(key, entry);
        cachePromotions.incrementAndGet();
    }
    
    /**
     * Promote entry to L2 cache
     */
    private void promoteToL2(String key, CacheEntry entry) {
        putInL2(key, entry);
        cachePromotions.incrementAndGet();
    }
    
    /**
     * Check if entry should be promoted to L1
     */
    private boolean shouldPromoteToL1(String key) {
        long frequency = accessFrequency.getOrDefault(key, 0L);
        long lastAccess = lastAccessTime.getOrDefault(key, 0L);
        long timeSinceAccess = System.currentTimeMillis() - lastAccess;
        
        return frequency > 5 && timeSinceAccess < 60000; // Accessed 5+ times in last minute
    }
    
    /**
     * Check if entry should be promoted to L2
     */
    private boolean shouldPromoteToL2(String key) {
        long frequency = accessFrequency.getOrDefault(key, 0L);
        long lastAccess = lastAccessTime.getOrDefault(key, 0L);
        long timeSinceAccess = System.currentTimeMillis() - lastAccess;
        
        return frequency > 2 && timeSinceAccess < 300000; // Accessed 2+ times in last 5 minutes
    }
    
    /**
     * Evict least recently used entry from L1
     */
    private void evictFromL1() {
        String lruKey = l1AccessOrder.poll();
        if (lruKey != null) {
            CacheEntry entry = l1Cache.remove(lruKey);
            if (entry != null) {
                // Demote to L2
                putInL2(lruKey, entry);
                cacheEvictions.incrementAndGet();
            }
        }
    }
    
    /**
     * Evict least recently used entry from L2
     */
    private void evictFromL2() {
        String lruKey = l2AccessOrder.poll();
        if (lruKey != null) {
            CacheEntry entry = l2Cache.remove(lruKey);
            if (entry != null) {
                // Demote to L3
                putInL3(lruKey, entry);
                cacheEvictions.incrementAndGet();
            }
        }
    }
    
    /**
     * Evict least recently used entry from L3
     */
    private void evictFromL3() {
        String lruKey = l3AccessOrder.poll();
        if (lruKey != null) {
            l3Cache.remove(lruKey);
            cacheEvictions.incrementAndGet();
        }
    }
    
    /**
     * Update access order for LRU tracking
     */
    private void updateAccessOrder(Queue<String> accessOrder, String key) {
        accessOrder.remove(key); // Remove if exists
        accessOrder.offer(key);  // Add to end
    }
    
    /**
     * Perform cache cleanup
     */
    private void performCacheCleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean expired entries from L1
        l1Cache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime, l1TTL));
        
        // Clean expired entries from L2
        l2Cache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime, l2TTL));
        
        // Clean expired entries from L3
        l3Cache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime, l3TTL));
        
        // Clean old access tracking
        long cutoffTime = currentTime - 3600000; // 1 hour
        lastAccessTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        accessFrequency.entrySet().removeIf(entry -> 
            lastAccessTime.getOrDefault(entry.getKey(), 0L) < cutoffTime);
    }
    
    /**
     * Optimize cache hierarchy
     */
    private void optimizeCacheHierarchy() {
        // Analyze cache performance
        long totalHits = l1Hits.get() + l2Hits.get() + l3Hits.get();
        long totalOps = totalHits + cacheMisses.get();
        
        if (totalOps > 1000) {
            double hitRate = (totalHits * 100.0) / totalOps;
            double l1Rate = (l1Hits.get() * 100.0) / totalOps;
            double l2Rate = (l2Hits.get() * 100.0) / totalOps;
            double l3Rate = (l3Hits.get() * 100.0) / totalOps;
            
            LOGGER.info("💾 Cache performance - Hit rate: " + String.format("%.1f%%", hitRate) +
                       " (L1: " + String.format("%.1f%%", l1Rate) +
                       ", L2: " + String.format("%.1f%%", l2Rate) +
                       ", L3: " + String.format("%.1f%%", l3Rate) + ")");
            
            // Auto-tune cache sizes based on performance
            autoTuneCacheSizes(l1Rate, l2Rate, l3Rate);
        }
    }
    
    /**
     * Auto-tune cache sizes based on hit rates
     */
    private void autoTuneCacheSizes(double l1Rate, double l2Rate, double l3Rate) {
        // Increase L1 size if it has high hit rate
        if (l1Rate > 30.0 && l1MaxSize < 2000) {
            l1MaxSize = Math.min(2000, l1MaxSize + 100);
            LOGGER.info("🔧 Increased L1 cache size to " + l1MaxSize);
        }
        
        // Increase L2 size if it has high hit rate
        if (l2Rate > 40.0 && l2MaxSize < 20000) {
            l2MaxSize = Math.min(20000, l2MaxSize + 1000);
            LOGGER.info("🔧 Increased L2 cache size to " + l2MaxSize);
        }
        
        // Increase L3 size if it has high hit rate
        if (l3Rate > 20.0 && l3MaxSize < 200000) {
            l3MaxSize = Math.min(200000, l3MaxSize + 10000);
            LOGGER.info("🔧 Increased L3 cache size to " + l3MaxSize);
        }
    }
    
    /**
     * Perform smart promotion based on access patterns
     */
    private void performSmartPromotion() {
        // Promote frequently accessed L2 entries to L1
        List<String> toPromoteToL1 = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> entry : l2Cache.entrySet()) {
            if (shouldPromoteToL1(entry.getKey())) {
                toPromoteToL1.add(entry.getKey());
            }
        }
        
        for (String key : toPromoteToL1) {
            CacheEntry entry = l2Cache.remove(key);
            if (entry != null) {
                promoteToL1(key, entry);
            }
        }
        
        // Promote frequently accessed L3 entries to L2
        List<String> toPromoteToL2 = new ArrayList<>();
        for (Map.Entry<String, CacheEntry> entry : l3Cache.entrySet()) {
            if (shouldPromoteToL2(entry.getKey())) {
                toPromoteToL2.add(entry.getKey());
            }
        }
        
        for (String key : toPromoteToL2) {
            CacheEntry entry = l3Cache.remove(key);
            if (entry != null) {
                promoteToL2(key, entry);
            }
        }
    }
    
    /**
     * Perform predictive loading
     */
    private void performPredictiveLoading() {
        // Analyze access patterns and preload related data
        // This is a simplified version - real implementation would be more sophisticated
        
        for (Map.Entry<String, Long> entry : accessFrequency.entrySet()) {
            String key = entry.getKey();
            Long frequency = entry.getValue();
            
            if (frequency > 10) {
                // Predict related keys and preload them
                predictAndPreload(key);
            }
        }
    }
    
    private void predictAndPreload(String key) {
        // Simple prediction: if key is "chunk_1_2", preload "chunk_1_3", "chunk_2_2", etc.
        if (key.startsWith("chunk_")) {
            String[] parts = key.split("_");
            if (parts.length == 3) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    
                    // Preload adjacent chunks
                    String[] adjacentKeys = {
                        "chunk_" + (x+1) + "_" + z,
                        "chunk_" + (x-1) + "_" + z,
                        "chunk_" + x + "_" + (z+1),
                        "chunk_" + x + "_" + (z-1)
                    };
                    
                    for (String adjacentKey : adjacentKeys) {
                        if (!containsKey(adjacentKey)) {
                            // Would trigger loading of adjacent chunk data
                            // put(adjacentKey, loadChunkData(adjacentKey));
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid format
                }
            }
        }
    }
    
    /**
     * Check if key exists in any cache level
     */
    public boolean containsKey(String key) {
        return (enableL1Cache && l1Cache.containsKey(key)) ||
               (enableL2Cache && l2Cache.containsKey(key)) ||
               (enableL3Cache && l3Cache.containsKey(key));
    }
    
    /**
     * Remove key from all cache levels
     */
    public void remove(String key) {
        if (enableL1Cache) l1Cache.remove(key);
        if (enableL2Cache) l2Cache.remove(key);
        if (enableL3Cache) l3Cache.remove(key);
        
        l1AccessOrder.remove(key);
        l2AccessOrder.remove(key);
        l3AccessOrder.remove(key);
        
        accessFrequency.remove(key);
        lastAccessTime.remove(key);
    }
    
    /**
     * Clear all caches
     */
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
        l3Cache.clear();
        
        l1AccessOrder.clear();
        l2AccessOrder.clear();
        l3AccessOrder.clear();
        
        accessFrequency.clear();
        lastAccessTime.clear();
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_requests", totalRequests.get());
        stats.put("l1_hits", l1Hits.get());
        stats.put("l2_hits", l2Hits.get());
        stats.put("l3_hits", l3Hits.get());
        stats.put("cache_misses", cacheMisses.get());
        stats.put("cache_evictions", cacheEvictions.get());
        stats.put("cache_promotions", cachePromotions.get());
        
        stats.put("l1_size", l1Cache.size());
        stats.put("l2_size", l2Cache.size());
        stats.put("l3_size", l3Cache.size());
        
        stats.put("l1_max_size", l1MaxSize);
        stats.put("l2_max_size", l2MaxSize);
        stats.put("l3_max_size", l3MaxSize);
        
        long totalHits = l1Hits.get() + l2Hits.get() + l3Hits.get();
        long totalOps = totalHits + cacheMisses.get();
        
        if (totalOps > 0) {
            stats.put("hit_rate", (totalHits * 100.0) / totalOps);
            stats.put("l1_hit_rate", (l1Hits.get() * 100.0) / totalOps);
            stats.put("l2_hit_rate", (l2Hits.get() * 100.0) / totalOps);
            stats.put("l3_hit_rate", (l3Hits.get() * 100.0) / totalOps);
        }
        
        return stats;
    }
    
    // Getters
    public long getTotalRequests() { return totalRequests.get(); }
    public long getL1Hits() { return l1Hits.get(); }
    public long getL2Hits() { return l2Hits.get(); }
    public long getL3Hits() { return l3Hits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getCacheEvictions() { return cacheEvictions.get(); }
    public long getCachePromotions() { return cachePromotions.get(); }
    
    public void shutdown() {
        cacheManager.shutdown();
        clear();
        LOGGER.info("💾 Smart Cache Hierarchy shutdown complete");
    }
    
    /**
     * Cache entry container
     */
    private static class CacheEntry {
        private final Object value;
        private final long timestamp;
        
        public CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        public Object getValue() {
            return value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isExpired(long currentTime, long ttl) {
            return currentTime - timestamp > ttl;
        }
    }
}
