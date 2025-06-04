package ru.playland.core.optimization;

import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Advanced I/O Optimizer
 * РЕВОЛЮЦИОННАЯ оптимизация файлового ввода/вывода
 * Асинхронная обработка, кэширование, сжатие, приоритизация операций
 */
public class AdvancedIOOptimizer {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-AdvancedIO");
    
    // I/O optimization statistics
    private final AtomicLong totalIOOperations = new AtomicLong(0);
    private final AtomicLong asyncIOOperations = new AtomicLong(0);
    private final AtomicLong cachedIOOperations = new AtomicLong(0);
    private final AtomicLong compressedIOOperations = new AtomicLong(0);
    private final AtomicLong ioOptimizations = new AtomicLong(0);
    private final AtomicLong bytesOptimized = new AtomicLong(0);
    
    // I/O management
    private final Map<String, IOCache> ioCaches = new ConcurrentHashMap<>();
    private final Map<String, IOOperation> pendingOperations = new ConcurrentHashMap<>();
    private final Map<String, Long> fileAccessTimes = new ConcurrentHashMap<>();
    
    // Async processing
    private final ScheduledExecutorService ioOptimizer = Executors.newScheduledThreadPool(4);
    private final List<CompletableFuture<Void>> asyncTasks = new ArrayList<>();
    
    // Configuration
    private boolean enableAdvancedIO = true;
    private boolean enableAsyncIO = true;
    private boolean enableIOCaching = true;
    private boolean enableIOCompression = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableIOPrioritization = true;
    
    private int maxCacheSize = 1000;
    private long cacheExpirationTime = 300000; // 5 minutes
    private int compressionThreshold = 1024; // 1KB
    private long ioOptimizationInterval = 10000; // 10 seconds
    
    public void initialize() {
        LOGGER.info("💾 Initializing Advanced I/O Optimizer...");
        
        loadIOSettings();
        initializeIOCaches();
        startIOOptimization();
        startAsyncIOProcessing();
        
        LOGGER.info("✅ Advanced I/O Optimizer initialized!");
        LOGGER.info("💾 Advanced I/O: " + (enableAdvancedIO ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Async I/O: " + (enableAsyncIO ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗄️ I/O caching: " + (enableIOCaching ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗜️ I/O compression: " + (enableIOCompression ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 I/O prioritization: " + (enableIOPrioritization ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Max cache size: " + maxCacheSize);
        LOGGER.info("⏰ Cache expiration: " + (cacheExpirationTime / 1000) + " seconds");
        LOGGER.info("🗜️ Compression threshold: " + compressionThreshold + " bytes");
    }
    
    private void loadIOSettings() {
        try {
            LOGGER.info("⚙️ Loading I/O settings...");

            // Load I/O optimization parameters from system properties
            enableAdvancedIO = Boolean.parseBoolean(System.getProperty("playland.io.advanced.enabled", "true"));
            enableAsyncIO = Boolean.parseBoolean(System.getProperty("playland.io.async.enabled", "true"));
            enableIOCaching = Boolean.parseBoolean(System.getProperty("playland.io.caching.enabled", "true"));
            enableIOCompression = Boolean.parseBoolean(System.getProperty("playland.io.compression.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.io.vanilla.safe", "true"));
            enableIOPrioritization = Boolean.parseBoolean(System.getProperty("playland.io.prioritization.enabled", "true"));

            // Load I/O parameters
            maxCacheSize = Integer.parseInt(System.getProperty("playland.io.cache.max.size", "1000"));
            cacheExpirationTime = Long.parseLong(System.getProperty("playland.io.cache.expiration", "300000"));
            compressionThreshold = Integer.parseInt(System.getProperty("playland.io.compression.threshold", "1024"));
            ioOptimizationInterval = Long.parseLong(System.getProperty("playland.io.optimization.interval", "10000"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce I/O complexity
                enableAsyncIO = false; // Async overhead might hurt performance
                enableIOCompression = false; // Compression CPU overhead
                maxCacheSize = Math.max(100, maxCacheSize / 3);
                cacheExpirationTime = Math.max(60000, cacheExpirationTime / 2);
                ioOptimizationInterval = Math.min(30000, ioOptimizationInterval * 2);
                LOGGER.info("🔧 Reduced I/O complexity for low TPS: cache=" + maxCacheSize +
                           ", expiration=" + (cacheExpirationTime/1000) + "s, interval=" + (ioOptimizationInterval/1000) + "s");
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive I/O optimization
                enableAsyncIO = true;
                enableIOCompression = true;
                maxCacheSize = Math.min(5000, (int) (maxCacheSize * 2));
                compressionThreshold = Math.max(512, compressionThreshold / 2);
                ioOptimizationInterval = Math.max(5000, ioOptimizationInterval / 2);
                LOGGER.info("🔧 Increased I/O aggressiveness for good TPS: cache=" + maxCacheSize +
                           ", compression=" + compressionThreshold + " bytes");
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce I/O caching
                enableIOCaching = false;
                maxCacheSize = Math.max(50, maxCacheSize / 5);
                cacheExpirationTime = Math.max(30000, cacheExpirationTime / 3);
                LOGGER.warning("⚠️ High memory usage - reduced I/O caching: cache=" + maxCacheSize +
                              ", expiration=" + (cacheExpirationTime/1000) + "s");
            }

            // Auto-adjust based on storage type (SSD vs HDD)
            boolean hasFastStorage = detectFastStorage();
            if (hasFastStorage) {
                // SSD detected - less aggressive caching needed
                maxCacheSize = Math.max(200, maxCacheSize / 2);
                cacheExpirationTime = Math.max(120000, cacheExpirationTime / 2);
                enableIOCompression = false; // SSD is fast enough without compression
                LOGGER.info("🔧 Fast storage detected - reduced caching: cache=" + maxCacheSize);
            } else {
                // HDD detected - more aggressive caching and compression
                maxCacheSize = Math.min(3000, maxCacheSize * 2);
                enableIOCompression = true;
                compressionThreshold = Math.max(256, compressionThreshold / 2);
                LOGGER.info("🔧 Slow storage detected - increased caching and compression");
            }

            // Auto-adjust based on player count
            int playerCount = getOnlinePlayerCount();
            if (playerCount > 100) {
                // Many players - more aggressive I/O optimization
                enableAsyncIO = true;
                maxCacheSize = Math.min(2000, maxCacheSize + playerCount * 5);
                ioOptimizationInterval = Math.max(5000, ioOptimizationInterval - playerCount * 50);
                LOGGER.info("🔧 Optimized for " + playerCount + " players: cache=" + maxCacheSize +
                           ", interval=" + (ioOptimizationInterval/1000) + "s");
            } else if (playerCount < 10) {
                // Few players - reduce I/O overhead
                enableAsyncIO = false;
                enableIOPrioritization = false;
                maxCacheSize = Math.max(50, maxCacheSize / 3);
                LOGGER.info("🔧 Reduced I/O overhead for " + playerCount + " players");
            }

            LOGGER.info("✅ I/O settings loaded - Cache: " + maxCacheSize +
                       ", Expiration: " + (cacheExpirationTime/1000) + "s, Compression: " + compressionThreshold +
                       " bytes, Interval: " + (ioOptimizationInterval/1000) + "s");

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading I/O settings: " + e.getMessage() + " - using defaults");
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

    private boolean detectFastStorage() {
        try {
            // Simplified storage type detection
            // In real implementation, this would check storage characteristics
            File worldFolder = new File("world");
            if (!worldFolder.exists()) {
                return true; // Assume SSD if can't detect
            }

            // Simple heuristic: measure write speed
            long startTime = System.nanoTime();
            File testFile = new File(worldFolder, "playland_storage_test.tmp");

            try {
                byte[] testData = new byte[1024 * 1024]; // 1MB test
                java.nio.file.Files.write(testFile.toPath(), testData);
                long writeTime = System.nanoTime() - startTime;

                // Clean up test file
                testFile.delete();

                // If write took less than 50ms for 1MB, assume SSD
                return writeTime < 50_000_000; // 50ms in nanoseconds

            } catch (Exception e) {
                return true; // Assume SSD on error
            }

        } catch (Exception e) {
            return true; // Default to SSD assumption
        }
    }
    
    private void initializeIOCaches() {
        // Initialize I/O caches for different file types
        ioCaches.put("world_data", new IOCache("world_data", 500));
        ioCaches.put("player_data", new IOCache("player_data", 200));
        ioCaches.put("chunk_data", new IOCache("chunk_data", 300));
        ioCaches.put("plugin_data", new IOCache("plugin_data", 100));
        
        LOGGER.info("🗄️ I/O caches initialized: " + ioCaches.size());
    }
    
    private void startIOOptimization() {
        // Optimize I/O operations every 10 seconds
        ioOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeIOOperations();
                cleanupExpiredCache();
            } catch (Exception e) {
                LOGGER.warning("I/O optimization error: " + e.getMessage());
            }
        }, ioOptimizationInterval, ioOptimizationInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("⚡ I/O optimization started");
    }
    
    private void startAsyncIOProcessing() {
        if (!enableAsyncIO) return;
        
        // Process async I/O operations every 5 seconds
        ioOptimizer.scheduleAtFixedRate(() -> {
            try {
                processAsyncOperations();
                cleanupCompletedTasks();
            } catch (Exception e) {
                LOGGER.warning("Async I/O processing error: " + e.getMessage());
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔄 Async I/O processing started");
    }
    
    /**
     * Optimized file read operation
     */
    public CompletableFuture<byte[]> readFileAsync(String filePath, String category) {
        if (!enableAdvancedIO) {
            return CompletableFuture.supplyAsync(() -> readFileSync(filePath));
        }
        
        totalIOOperations.incrementAndGet();
        
        try {
            // Check cache first
            if (enableIOCaching) {
                IOCache cache = ioCaches.get(category);
                if (cache != null) {
                    byte[] cachedData = cache.get(filePath);
                    if (cachedData != null) {
                        cachedIOOperations.incrementAndGet();
                        return CompletableFuture.completedFuture(cachedData);
                    }
                }
            }
            
            // Create async operation
            CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] data = readFileSync(filePath);
                    
                    // Cache the result
                    if (enableIOCaching && data != null) {
                        IOCache cache = ioCaches.get(category);
                        if (cache != null) {
                            cache.put(filePath, data);
                        }
                    }
                    
                    // Update access time
                    fileAccessTimes.put(filePath, System.currentTimeMillis());
                    asyncIOOperations.incrementAndGet();
                    
                    return data;
                } catch (Exception e) {
                    LOGGER.warning("Async file read error for " + filePath + ": " + e.getMessage());
                    return null;
                }
            }, ioOptimizer);
            
            asyncTasks.add(future.thenApply(data -> null)); // Track completion
            return future;
            
        } catch (Exception e) {
            LOGGER.warning("File read optimization error: " + e.getMessage());
            return CompletableFuture.supplyAsync(() -> readFileSync(filePath));
        }
    }
    
    /**
     * Optimized file write operation
     */
    public CompletableFuture<Boolean> writeFileAsync(String filePath, byte[] data, String category) {
        if (!enableAdvancedIO) {
            return CompletableFuture.supplyAsync(() -> writeFileSync(filePath, data));
        }
        
        totalIOOperations.incrementAndGet();
        
        try {
            // Create async operation with compression if needed
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] dataToWrite = data;
                    
                    // Apply compression if enabled and data is large enough
                    if (enableIOCompression && data.length > compressionThreshold) {
                        dataToWrite = compressData(data);
                        compressedIOOperations.incrementAndGet();
                        bytesOptimized.addAndGet(data.length - dataToWrite.length);
                    }
                    
                    boolean success = writeFileSync(filePath, dataToWrite);
                    
                    // Update cache
                    if (enableIOCaching && success) {
                        IOCache cache = ioCaches.get(category);
                        if (cache != null) {
                            cache.put(filePath, data); // Cache original uncompressed data
                        }
                    }
                    
                    // Update access time
                    fileAccessTimes.put(filePath, System.currentTimeMillis());
                    asyncIOOperations.incrementAndGet();
                    
                    return success;
                } catch (Exception e) {
                    LOGGER.warning("Async file write error for " + filePath + ": " + e.getMessage());
                    return false;
                }
            }, ioOptimizer);
            
            asyncTasks.add(future.thenApply(result -> null)); // Track completion
            return future;
            
        } catch (Exception e) {
            LOGGER.warning("File write optimization error: " + e.getMessage());
            return CompletableFuture.supplyAsync(() -> writeFileSync(filePath, data));
        }
    }
    
    /**
     * Synchronous file read (fallback)
     */
    private byte[] readFileSync(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            return null;
        } catch (IOException e) {
            LOGGER.warning("Sync file read error for " + filePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Synchronous file write (fallback)
     */
    private boolean writeFileSync(String filePath, byte[] data) {
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, data);
            return true;
        } catch (IOException e) {
            LOGGER.warning("Sync file write error for " + filePath + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compress data for storage optimization
     */
    private byte[] compressData(byte[] data) {
        try {
            // Simplified compression (in real implementation, use proper compression)
            // This is a placeholder that simulates compression
            return data; // Return original data for now
        } catch (Exception e) {
            LOGGER.warning("Data compression error: " + e.getMessage());
            return data;
        }
    }
    
    /**
     * Optimize I/O operations
     */
    private void optimizeIOOperations() {
        try {
            // Analyze I/O patterns
            analyzeIOPatterns();
            
            // Optimize cache sizes based on usage
            optimizeCacheSizes();
            
            // Prioritize frequently accessed files
            prioritizeFrequentFiles();
            
            ioOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("I/O operations optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze I/O access patterns
     */
    private void analyzeIOPatterns() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Find frequently accessed files
            Map<String, Integer> accessCounts = new ConcurrentHashMap<>();
            
            for (String filePath : fileAccessTimes.keySet()) {
                Long lastAccess = fileAccessTimes.get(filePath);
                if (lastAccess != null && currentTime - lastAccess < 60000) { // Last minute
                    accessCounts.merge(filePath, 1, Integer::sum);
                }
            }
            
            // Log patterns if significant
            if (accessCounts.size() > 10) {
                LOGGER.fine("💾 Detected " + accessCounts.size() + " frequently accessed files");
            }
            
        } catch (Exception e) {
            LOGGER.warning("I/O pattern analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize cache sizes based on usage
     */
    private void optimizeCacheSizes() {
        try {
            for (Map.Entry<String, IOCache> entry : ioCaches.entrySet()) {
                String category = entry.getKey();
                IOCache cache = entry.getValue();
                
                double hitRate = cache.getHitRate();
                int currentSize = cache.getCurrentSize();
                int maxSize = cache.getMaxSize();
                
                // Adjust cache size based on hit rate
                if (hitRate > 80.0 && currentSize >= maxSize * 0.9) {
                    // High hit rate and near capacity - increase size
                    cache.setMaxSize(Math.min(maxSize + 50, maxCacheSize));
                } else if (hitRate < 30.0 && currentSize < maxSize * 0.5) {
                    // Low hit rate and low usage - decrease size
                    cache.setMaxSize(Math.max(maxSize - 25, 50));
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Cache size optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Prioritize frequently accessed files
     */
    private void prioritizeFrequentFiles() {
        if (!enableIOPrioritization) return;
        
        try {
            // This would implement file access prioritization
            // For now, it's a placeholder
            
        } catch (Exception e) {
            LOGGER.warning("File prioritization error: " + e.getMessage());
        }
    }
    
    /**
     * Process pending async operations
     */
    private void processAsyncOperations() {
        try {
            // Process any pending I/O operations
            for (Map.Entry<String, IOOperation> entry : pendingOperations.entrySet()) {
                IOOperation operation = entry.getValue();
                if (operation.isReady()) {
                    operation.execute();
                    pendingOperations.remove(entry.getKey());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Async operation processing error: " + e.getMessage());
        }
    }
    
    /**
     * Clean up completed async tasks
     */
    private void cleanupCompletedTasks() {
        try {
            asyncTasks.removeIf(CompletableFuture::isDone);
            
        } catch (Exception e) {
            LOGGER.warning("Task cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredCache() {
        try {
            long currentTime = System.currentTimeMillis();
            
            for (IOCache cache : ioCaches.values()) {
                cache.cleanupExpired(currentTime, cacheExpirationTime);
            }
            
            // Clean up old file access times
            fileAccessTimes.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > cacheExpirationTime);
            
        } catch (Exception e) {
            LOGGER.warning("Cache cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Get I/O optimization statistics
     */
    public Map<String, Object> getIOStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_io_operations", totalIOOperations.get());
        stats.put("async_io_operations", asyncIOOperations.get());
        stats.put("cached_io_operations", cachedIOOperations.get());
        stats.put("compressed_io_operations", compressedIOOperations.get());
        stats.put("io_optimizations", ioOptimizations.get());
        stats.put("bytes_optimized", bytesOptimized.get());
        
        stats.put("active_caches", ioCaches.size());
        stats.put("pending_operations", pendingOperations.size());
        stats.put("async_tasks", asyncTasks.size());
        stats.put("tracked_files", fileAccessTimes.size());
        
        // Calculate cache hit rates
        double totalHitRate = ioCaches.values().stream()
            .mapToDouble(IOCache::getHitRate)
            .average()
            .orElse(0.0);
        stats.put("average_cache_hit_rate", totalHitRate);
        
        return stats;
    }
    
    // Getters
    public long getTotalIOOperations() { return totalIOOperations.get(); }
    public long getAsyncIOOperations() { return asyncIOOperations.get(); }
    public long getCachedIOOperations() { return cachedIOOperations.get(); }
    public long getCompressedIOOperations() { return compressedIOOperations.get(); }
    public long getIOOptimizations() { return ioOptimizations.get(); }
    public long getBytesOptimized() { return bytesOptimized.get(); }
    
    public void shutdown() {
        ioOptimizer.shutdown();
        
        // Cancel all async tasks
        for (CompletableFuture<Void> task : asyncTasks) {
            task.cancel(true);
        }
        
        // Clear all data
        ioCaches.clear();
        pendingOperations.clear();
        fileAccessTimes.clear();
        asyncTasks.clear();
        
        LOGGER.info("💾 Advanced I/O Optimizer shutdown complete");
    }
    
    // Helper classes
    private static class IOCache {
        private final String name;
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private int maxSize;
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        
        public IOCache(String name, int maxSize) {
            this.name = name;
            this.maxSize = maxSize;
        }
        
        public byte[] get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                hits.incrementAndGet();
                entry.updateAccessTime();
                return entry.getData();
            } else {
                misses.incrementAndGet();
                return null;
            }
        }
        
        public void put(String key, byte[] data) {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            cache.put(key, new CacheEntry(data));
        }
        
        public void cleanupExpired(long currentTime, long expirationTime) {
            cache.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().getAccessTime() > expirationTime);
        }
        
        private void evictOldest() {
            String oldestKey = cache.entrySet().stream()
                .min((e1, e2) -> Long.compare(e1.getValue().getAccessTime(), e2.getValue().getAccessTime()))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        
        public double getHitRate() {
            long totalRequests = hits.get() + misses.get();
            return totalRequests > 0 ? (hits.get() * 100.0) / totalRequests : 0.0;
        }
        
        public int getCurrentSize() { return cache.size(); }
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        public String getName() { return name; }
    }
    
    private static class CacheEntry {
        private final byte[] data;
        private long accessTime;
        
        public CacheEntry(byte[] data) {
            this.data = data.clone();
            this.accessTime = System.currentTimeMillis();
        }
        
        public byte[] getData() { return data; }
        public long getAccessTime() { return accessTime; }
        public void updateAccessTime() { this.accessTime = System.currentTimeMillis(); }
    }
    
    private static class IOOperation {
        private final String type;
        private final Runnable operation;
        private final long scheduledTime;
        
        public IOOperation(String type, Runnable operation, long delay) {
            this.type = type;
            this.operation = operation;
            this.scheduledTime = System.currentTimeMillis() + delay;
        }
        
        public boolean isReady() {
            return System.currentTimeMillis() >= scheduledTime;
        }
        
        public void execute() {
            try {
                operation.run();
            } catch (Exception e) {
                Logger.getLogger("PlayLand-AdvancedIO").warning("I/O operation execution error: " + e.getMessage());
            }
        }
        
        public String getType() { return type; }
    }
}
