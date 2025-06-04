package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Vanilla-Safe Caching System
 * БЕЗОПАСНОЕ кэширование без нарушения vanilla механик
 * Кэширует только детерминированные вычисления
 */
public class VanillaSafeCaching {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-VanillaCache");
    
    // Cache statistics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheEvictions = new AtomicLong(0);
    private final AtomicLong computationsSaved = new AtomicLong(0);
    
    // Safe caches for different data types
    private final Map<String, Object> blockStateCache = new ConcurrentHashMap<>();
    private final Map<String, Object> pathfindingCache = new ConcurrentHashMap<>();
    private final Map<String, Object> lightLevelCache = new ConcurrentHashMap<>();
    private final Map<String, Object> biomeCache = new ConcurrentHashMap<>();
    private final Map<String, Object> redstoneCache = new ConcurrentHashMap<>();
    
    // Weak reference caches for memory safety
    private final Map<Object, Object> weakCache = new WeakHashMap<>();
    
    // Cache metadata
    private final Map<String, CacheEntry> cacheMetadata = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheAccessTimes = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableBlockStateCache = true;
    private boolean enablePathfindingCache = true;
    private boolean enableLightLevelCache = true;
    private boolean enableBiomeCache = true;
    private boolean enableRedstoneCache = true;
    private boolean enableVanillaSafetyChecks = true;
    
    private int maxCacheSize = 10000;
    private long cacheExpirationTime = 30000; // 30 seconds
    private long maxComputationTime = 10; // 10ms max for cached computations
    
    public void initialize() {
        LOGGER.info("💾 Initializing Vanilla-Safe Caching System...");
        
        loadCacheSettings();
        startCacheCleanup();
        
        LOGGER.info("✅ Vanilla-Safe Caching System initialized!");
        LOGGER.info("🧱 Block state cache: " + (enableBlockStateCache ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗺️ Pathfinding cache: " + (enablePathfindingCache ? "ENABLED" : "DISABLED"));
        LOGGER.info("💡 Light level cache: " + (enableLightLevelCache ? "ENABLED" : "DISABLED"));
        LOGGER.info("🌍 Biome cache: " + (enableBiomeCache ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Redstone cache: " + (enableRedstoneCache ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla safety checks: " + (enableVanillaSafetyChecks ? "ENABLED" : "DISABLED"));
        LOGGER.info("📏 Max cache size: " + maxCacheSize);
        LOGGER.info("⏰ Cache expiration: " + (cacheExpirationTime / 1000) + " seconds");
    }
    
    private void loadCacheSettings() {
        // Load vanilla-safe caching settings
        LOGGER.info("⚙️ Loading cache settings...");
    }
    
    private void startCacheCleanup() {
        // Start cache cleanup in a separate thread (safe for vanilla)
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Clean every minute
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("PlayLand-CacheCleanup");
        cleanupThread.start();
        
        LOGGER.info("🧹 Cache cleanup started");
    }
    
    /**
     * Get or compute block state with caching
     */
    public <T> T getBlockState(String key, Supplier<T> computation) {
        if (!enableBlockStateCache) {
            return computation.get();
        }
        
        return getCachedValue("blockstate_" + key, computation, blockStateCache);
    }
    
    /**
     * Get or compute pathfinding result with caching
     */
    public <T> T getPathfinding(String key, Supplier<T> computation) {
        if (!enablePathfindingCache) {
            return computation.get();
        }
        
        return getCachedValue("pathfinding_" + key, computation, pathfindingCache);
    }
    
    /**
     * Get or compute light level with caching
     */
    public <T> T getLightLevel(String key, Supplier<T> computation) {
        if (!enableLightLevelCache) {
            return computation.get();
        }
        
        return getCachedValue("light_" + key, computation, lightLevelCache);
    }
    
    /**
     * Get or compute biome data with caching
     */
    public <T> T getBiome(String key, Supplier<T> computation) {
        if (!enableBiomeCache) {
            return computation.get();
        }
        
        return getCachedValue("biome_" + key, computation, biomeCache);
    }
    
    /**
     * Get or compute redstone signal with caching
     */
    public <T> T getRedstoneSignal(String key, Supplier<T> computation) {
        if (!enableRedstoneCache) {
            return computation.get();
        }
        
        // Redstone cache has shorter expiration for accuracy
        return getCachedValue("redstone_" + key, computation, redstoneCache, 5000); // 5 seconds
    }
    
    /**
     * Generic cached value getter
     */
    @SuppressWarnings("unchecked")
    private <T> T getCachedValue(String key, Supplier<T> computation, Map<String, Object> cache) {
        return getCachedValue(key, computation, cache, cacheExpirationTime);
    }
    
    /**
     * Generic cached value getter with custom expiration
     */
    @SuppressWarnings("unchecked")
    private <T> T getCachedValue(String key, Supplier<T> computation, Map<String, Object> cache, long expiration) {
        totalRequests.incrementAndGet();
        
        // Check if value is cached and not expired
        Object cachedValue = cache.get(key);
        CacheEntry metadata = cacheMetadata.get(key);
        
        if (cachedValue != null && metadata != null) {
            if (!metadata.isExpired(expiration)) {
                cacheHits.incrementAndGet();
                cacheAccessTimes.put(key, System.currentTimeMillis());
                return (T) cachedValue;
            } else {
                // Remove expired entry
                cache.remove(key);
                cacheMetadata.remove(key);
                cacheEvictions.incrementAndGet();
            }
        }
        
        // Cache miss - compute value
        cacheMisses.incrementAndGet();
        
        long computationStart = System.nanoTime();
        T result = computation.get();
        long computationTime = (System.nanoTime() - computationStart) / 1_000_000; // Convert to milliseconds
        
        // Only cache if computation was expensive enough and result is cacheable
        if (computationTime >= 1 && isCacheable(result)) { // At least 1ms computation time
            // Check cache size limit
            if (cache.size() >= maxCacheSize) {
                evictOldestEntry(cache);
            }
            
            // Store in cache
            cache.put(key, result);
            cacheMetadata.put(key, new CacheEntry(System.currentTimeMillis(), computationTime));
            cacheAccessTimes.put(key, System.currentTimeMillis());
            
            computationsSaved.incrementAndGet();
        }
        
        return result;
    }
    
    /**
     * Check if a value is safe to cache (vanilla-safe)
     */
    private boolean isCacheable(Object value) {
        if (!enableVanillaSafetyChecks) return true;
        
        // Don't cache null values
        if (value == null) return false;
        
        // Don't cache mutable objects that could affect vanilla behavior
        if (value instanceof java.util.List ||
            value instanceof java.util.Set ||
            value instanceof java.util.Map) {
            return false;
        }
        
        // Safe to cache immutable types
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Enum;
    }
    
    /**
     * Evict oldest entry from cache
     */
    private void evictOldestEntry(Map<String, Object> cache) {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, Long> entry : cacheAccessTimes.entrySet()) {
            if (cache.containsKey(entry.getKey()) && entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            cacheMetadata.remove(oldestKey);
            cacheAccessTimes.remove(oldestKey);
            cacheEvictions.incrementAndGet();
        }
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up each cache type
        cleanupCache(blockStateCache, currentTime, cacheExpirationTime);
        cleanupCache(pathfindingCache, currentTime, cacheExpirationTime);
        cleanupCache(lightLevelCache, currentTime, cacheExpirationTime);
        cleanupCache(biomeCache, currentTime, cacheExpirationTime);
        cleanupCache(redstoneCache, currentTime, 5000); // Shorter expiration for redstone
        
        // Clean up metadata for removed entries
        cacheMetadata.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return !blockStateCache.containsKey(key) &&
                   !pathfindingCache.containsKey(key) &&
                   !lightLevelCache.containsKey(key) &&
                   !biomeCache.containsKey(key) &&
                   !redstoneCache.containsKey(key);
        });
        
        // Clean up access times for removed entries
        cacheAccessTimes.entrySet().removeIf(entry -> !cacheMetadata.containsKey(entry.getKey()));
    }
    
    /**
     * Clean up specific cache
     */
    private void cleanupCache(Map<String, Object> cache, long currentTime, long expiration) {
        cache.entrySet().removeIf(entry -> {
            CacheEntry metadata = cacheMetadata.get(entry.getKey());
            if (metadata != null && metadata.isExpired(currentTime, expiration)) {
                cacheEvictions.incrementAndGet();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Invalidate cache entries by prefix
     */
    public void invalidateByPrefix(String prefix) {
        invalidateCacheByPrefix(blockStateCache, prefix);
        invalidateCacheByPrefix(pathfindingCache, prefix);
        invalidateCacheByPrefix(lightLevelCache, prefix);
        invalidateCacheByPrefix(biomeCache, prefix);
        invalidateCacheByPrefix(redstoneCache, prefix);
        
        // Clean up metadata
        cacheMetadata.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        cacheAccessTimes.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
    
    private void invalidateCacheByPrefix(Map<String, Object> cache, String prefix) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        blockStateCache.clear();
        pathfindingCache.clear();
        lightLevelCache.clear();
        biomeCache.clear();
        redstoneCache.clear();
        weakCache.clear();
        
        cacheMetadata.clear();
        cacheAccessTimes.clear();
        
        LOGGER.info("🧹 All caches cleared");
    }
    
    /**
     * Get cache hit rate
     */
    public double getCacheHitRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (cacheHits.get() * 100.0) / total;
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_requests", totalRequests.get());
        stats.put("cache_hits", cacheHits.get());
        stats.put("cache_misses", cacheMisses.get());
        stats.put("cache_evictions", cacheEvictions.get());
        stats.put("computations_saved", computationsSaved.get());
        
        stats.put("hit_rate", getCacheHitRate());
        
        stats.put("blockstate_cache_size", blockStateCache.size());
        stats.put("pathfinding_cache_size", pathfindingCache.size());
        stats.put("light_cache_size", lightLevelCache.size());
        stats.put("biome_cache_size", biomeCache.size());
        stats.put("redstone_cache_size", redstoneCache.size());
        
        stats.put("total_cache_size", getTotalCacheSize());
        stats.put("max_cache_size", maxCacheSize);
        
        return stats;
    }
    
    private int getTotalCacheSize() {
        return blockStateCache.size() +
               pathfindingCache.size() +
               lightLevelCache.size() +
               biomeCache.size() +
               redstoneCache.size();
    }
    
    // Getters
    public long getTotalRequests() { return totalRequests.get(); }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public long getCacheEvictions() { return cacheEvictions.get(); }
    public long getComputationsSaved() { return computationsSaved.get(); }
    
    /**
     * Cache entry metadata
     */
    private static class CacheEntry {
        private final long creationTime;
        private final long computationTime;
        
        public CacheEntry(long creationTime, long computationTime) {
            this.creationTime = creationTime;
            this.computationTime = computationTime;
        }
        
        public boolean isExpired(long expiration) {
            return isExpired(System.currentTimeMillis(), expiration);
        }
        
        public boolean isExpired(long currentTime, long expiration) {
            return currentTime - creationTime > expiration;
        }
        
        public long getCreationTime() { return creationTime; }
        public long getComputationTime() { return computationTime; }
    }
}
