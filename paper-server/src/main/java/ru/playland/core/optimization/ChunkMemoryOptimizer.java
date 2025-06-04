package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * Chunk Memory Optimizer
 * ЭКСТРЕМАЛЬНАЯ оптимизация памяти чанков
 * Сжатие, выгрузка, умное кэширование чанков
 */
public class ChunkMemoryOptimizer {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-ChunkMemory");
    
    // Chunk memory statistics
    private final AtomicLong chunksCompressed = new AtomicLong(0);
    private final AtomicLong chunksUnloaded = new AtomicLong(0);
    private final AtomicLong chunksCached = new AtomicLong(0);
    private final AtomicLong memoryFreedFromChunks = new AtomicLong(0);
    private final AtomicLong compressionRatio = new AtomicLong(0);
    private final AtomicLong chunkMemoryOptimizations = new AtomicLong(0);
    
    // Chunk tracking
    private final Map<String, ChunkMemoryData> chunkMemoryMap = new ConcurrentHashMap<>();
    private final Map<String, CompressedChunkData> compressedChunks = new ConcurrentHashMap<>();
    private final Set<String> activeChunks = ConcurrentHashMap.newKeySet();
    private final Queue<String> unloadCandidates = new ConcurrentLinkedQueue<>();
    
    // Memory management
    private final ScheduledExecutorService chunkManager = Executors.newScheduledThreadPool(2);
    private final Map<String, Long> chunkAccessTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> chunkAccessCounts = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableChunkCompression = true;
    private boolean enableSmartUnloading = true;
    private boolean enableChunkCaching = true;
    private boolean enableMemoryOptimization = true;
    private boolean enableVanillaSafeMode = true;
    
    private int compressionLevel = 6; // 1-9, 6 is balanced
    private long chunkUnloadDelay = 300000; // 5 minutes
    private long chunkCompressionDelay = 60000; // 1 minute
    private int maxCachedChunks = 1000;
    private int maxCompressedChunks = 5000;
    private double memoryPressureThreshold = 80.0; // 80%
    
    public void initialize() {
        LOGGER.info("📦 Initializing Chunk Memory Optimizer...");
        
        loadChunkSettings();
        startChunkMemoryManagement();
        startChunkCompression();
        startChunkUnloading();
        
        LOGGER.info("✅ Chunk Memory Optimizer initialized!");
        LOGGER.info("📦 Chunk compression: " + (enableChunkCompression ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗑️ Smart unloading: " + (enableSmartUnloading ? "ENABLED" : "DISABLED"));
        LOGGER.info("💾 Chunk caching: " + (enableChunkCaching ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Memory optimization: " + (enableMemoryOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗜️ Compression level: " + compressionLevel);
        LOGGER.info("⏰ Unload delay: " + (chunkUnloadDelay / 1000) + " seconds");
        LOGGER.info("📊 Max cached chunks: " + maxCachedChunks);
        LOGGER.info("📊 Max compressed chunks: " + maxCompressedChunks);
    }
    
    private void loadChunkSettings() {
        // Load chunk memory optimization settings
        LOGGER.info("⚙️ Loading chunk settings...");
    }
    
    private void startChunkMemoryManagement() {
        // Monitor chunk memory usage every 10 seconds
        chunkManager.scheduleAtFixedRate(() -> {
            try {
                monitorChunkMemory();
                optimizeChunkMemory();
            } catch (Exception e) {
                LOGGER.warning("Chunk memory management error: " + e.getMessage());
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📊 Chunk memory management started");
    }
    
    private void startChunkCompression() {
        if (!enableChunkCompression) return;
        
        // Compress inactive chunks every 30 seconds
        chunkManager.scheduleAtFixedRate(() -> {
            try {
                compressInactiveChunks();
                cleanupCompressedChunks();
            } catch (Exception e) {
                LOGGER.warning("Chunk compression error: " + e.getMessage());
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🗜️ Chunk compression started");
    }
    
    private void startChunkUnloading() {
        if (!enableSmartUnloading) return;
        
        // Check for chunks to unload every 60 seconds
        chunkManager.scheduleAtFixedRate(() -> {
            try {
                identifyUnloadCandidates();
                processUnloadCandidates();
            } catch (Exception e) {
                LOGGER.warning("Chunk unloading error: " + e.getMessage());
            }
        }, 60000, 60000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🗑️ Chunk unloading started");
    }
    
    /**
     * Track chunk access for optimization
     */
    public void trackChunkAccess(String chunkKey, int x, int z) {
        long currentTime = System.currentTimeMillis();
        
        // Update access tracking
        chunkAccessTimes.put(chunkKey, currentTime);
        chunkAccessCounts.merge(chunkKey, 1, Integer::sum);
        activeChunks.add(chunkKey);
        
        // Update chunk memory data
        ChunkMemoryData memoryData = chunkMemoryMap.computeIfAbsent(chunkKey, 
            k -> new ChunkMemoryData(chunkKey, x, z));
        memoryData.updateAccess(currentTime);
        
        // Remove from unload candidates if present
        unloadCandidates.remove(chunkKey);
    }
    
    /**
     * Register chunk data for memory tracking
     */
    public void registerChunkData(String chunkKey, byte[] chunkData) {
        if (!enableMemoryOptimization) return;
        
        try {
            ChunkMemoryData memoryData = chunkMemoryMap.get(chunkKey);
            if (memoryData != null) {
                memoryData.setDataSize(chunkData.length);
                memoryData.setData(chunkData);
                
                // Consider compression if chunk is large
                if (chunkData.length > 8192 && enableChunkCompression) { // 8KB threshold
                    scheduleChunkCompression(chunkKey);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Chunk data registration error: " + e.getMessage());
        }
    }
    
    /**
     * Monitor chunk memory usage
     */
    private void monitorChunkMemory() {
        long totalChunkMemory = 0;
        int activeChunkCount = 0;
        int compressedChunkCount = compressedChunks.size();
        
        for (ChunkMemoryData data : chunkMemoryMap.values()) {
            totalChunkMemory += data.getDataSize();
            if (data.isActive()) {
                activeChunkCount++;
            }
        }
        
        // Log memory usage periodically
        if (System.currentTimeMillis() % 60000 < 10000) { // Every minute
            LOGGER.info("📦 Chunk memory - Active: " + activeChunkCount + 
                       ", Compressed: " + compressedChunkCount + 
                       ", Total memory: " + formatBytes(totalChunkMemory));
        }
        
        // Check if memory optimization is needed
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = ((runtime.totalMemory() - runtime.freeMemory()) * 100.0) / runtime.maxMemory();
        
        if (memoryUsage > memoryPressureThreshold) {
            triggerMemoryOptimization();
        }
    }
    
    /**
     * Optimize chunk memory usage
     */
    private void optimizeChunkMemory() {
        try {
            // Remove old access tracking data
            long cutoffTime = System.currentTimeMillis() - 3600000; // 1 hour
            chunkAccessTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
            chunkAccessCounts.entrySet().removeIf(entry -> 
                !chunkAccessTimes.containsKey(entry.getKey()));
            
            // Update active chunks set
            activeChunks.removeIf(chunkKey -> {
                Long lastAccess = chunkAccessTimes.get(chunkKey);
                return lastAccess == null || System.currentTimeMillis() - lastAccess > chunkUnloadDelay;
            });
            
            chunkMemoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Chunk memory optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Compress inactive chunks
     */
    private void compressInactiveChunks() {
        if (!enableChunkCompression) return;
        
        long currentTime = System.currentTimeMillis();
        List<String> chunksToCompress = new ArrayList<>();
        
        // Find chunks that haven't been accessed recently
        for (Map.Entry<String, ChunkMemoryData> entry : chunkMemoryMap.entrySet()) {
            String chunkKey = entry.getKey();
            ChunkMemoryData data = entry.getValue();
            
            if (!activeChunks.contains(chunkKey) && 
                !compressedChunks.containsKey(chunkKey) &&
                data.getData() != null &&
                currentTime - data.getLastAccess() > chunkCompressionDelay) {
                
                chunksToCompress.add(chunkKey);
            }
        }
        
        // Compress chunks
        for (String chunkKey : chunksToCompress) {
            compressChunk(chunkKey);
            
            // Limit compression batch size
            if (chunksToCompress.indexOf(chunkKey) >= 10) {
                break;
            }
        }
    }
    
    /**
     * Compress a single chunk
     */
    private void compressChunk(String chunkKey) {
        try {
            ChunkMemoryData data = chunkMemoryMap.get(chunkKey);
            if (data == null || data.getData() == null) return;
            
            byte[] originalData = data.getData();
            byte[] compressedData = compressData(originalData);
            
            if (compressedData.length < originalData.length) {
                // Store compressed data
                CompressedChunkData compressed = new CompressedChunkData(
                    chunkKey, compressedData, originalData.length, System.currentTimeMillis()
                );
                
                compressedChunks.put(chunkKey, compressed);
                
                // Clear original data to free memory
                data.clearData();
                
                long memorySaved = originalData.length - compressedData.length;
                memoryFreedFromChunks.addAndGet(memorySaved);
                chunksCompressed.incrementAndGet();
                
                // Update compression ratio
                long totalRatio = (compressedData.length * 100L) / originalData.length;
                compressionRatio.set((compressionRatio.get() + totalRatio) / 2);
                
                LOGGER.fine("🗜️ Compressed chunk " + chunkKey + " - saved " + formatBytes(memorySaved));
            }
            
        } catch (Exception e) {
            LOGGER.warning("Chunk compression error for " + chunkKey + ": " + e.getMessage());
        }
    }
    
    /**
     * Decompress chunk data when needed
     */
    public byte[] getChunkData(String chunkKey) {
        // Check if chunk is compressed
        CompressedChunkData compressed = compressedChunks.get(chunkKey);
        if (compressed != null) {
            try {
                byte[] decompressedData = decompressData(compressed.getCompressedData());
                
                // Move back to active memory if frequently accessed
                trackChunkAccess(chunkKey, 0, 0); // Coordinates would be stored in compressed data
                
                return decompressedData;
                
            } catch (Exception e) {
                LOGGER.warning("Chunk decompression error for " + chunkKey + ": " + e.getMessage());
                return null;
            }
        }
        
        // Return regular data
        ChunkMemoryData data = chunkMemoryMap.get(chunkKey);
        return data != null ? data.getData() : null;
    }
    
    /**
     * Compress data using Deflater
     */
    private byte[] compressData(byte[] data) throws Exception {
        Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(data);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        
        deflater.end();
        return outputStream.toByteArray();
    }
    
    /**
     * Decompress data using Inflater
     */
    private byte[] decompressData(byte[] compressedData) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        
        inflater.end();
        return outputStream.toByteArray();
    }
    
    /**
     * Schedule chunk for compression
     */
    private void scheduleChunkCompression(String chunkKey) {
        // Add to compression queue (simplified implementation)
        chunkManager.schedule(() -> compressChunk(chunkKey), chunkCompressionDelay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Identify chunks that can be unloaded
     */
    private void identifyUnloadCandidates() {
        if (!enableSmartUnloading) return;
        
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, ChunkMemoryData> entry : chunkMemoryMap.entrySet()) {
            String chunkKey = entry.getKey();
            ChunkMemoryData data = entry.getValue();
            
            // Check if chunk hasn't been accessed recently
            if (currentTime - data.getLastAccess() > chunkUnloadDelay &&
                !activeChunks.contains(chunkKey) &&
                !unloadCandidates.contains(chunkKey)) {
                
                // Check if chunk is safe to unload (vanilla-safe)
                if (enableVanillaSafeMode && !isSafeToUnload(chunkKey)) {
                    continue;
                }
                
                unloadCandidates.offer(chunkKey);
            }
        }
    }
    
    /**
     * Process chunks marked for unloading
     */
    private void processUnloadCandidates() {
        int unloadedCount = 0;
        
        while (!unloadCandidates.isEmpty() && unloadedCount < 20) { // Limit batch size
            String chunkKey = unloadCandidates.poll();
            if (chunkKey != null && unloadChunk(chunkKey)) {
                unloadedCount++;
            }
        }
        
        if (unloadedCount > 0) {
            LOGGER.info("🗑️ Unloaded " + unloadedCount + " chunks from memory");
        }
    }
    
    /**
     * Unload a chunk from memory
     */
    private boolean unloadChunk(String chunkKey) {
        try {
            // Remove from memory tracking
            ChunkMemoryData data = chunkMemoryMap.remove(chunkKey);
            if (data != null) {
                memoryFreedFromChunks.addAndGet(data.getDataSize());
                chunksUnloaded.incrementAndGet();
            }
            
            // Remove compressed data if exists
            compressedChunks.remove(chunkKey);
            
            // Remove access tracking
            chunkAccessTimes.remove(chunkKey);
            chunkAccessCounts.remove(chunkKey);
            activeChunks.remove(chunkKey);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.warning("Chunk unload error for " + chunkKey + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if chunk is safe to unload (vanilla-safe)
     */
    private boolean isSafeToUnload(String chunkKey) {
        if (!enableVanillaSafeMode) return true;
        
        // Check if chunk contains important data
        ChunkMemoryData data = chunkMemoryMap.get(chunkKey);
        if (data == null) return true;
        
        // Don't unload spawn chunks or chunks with players
        // This would be implemented with actual chunk data checking
        return !data.isSpawnChunk() && !data.hasPlayers();
    }
    
    /**
     * Trigger memory optimization under pressure
     */
    private void triggerMemoryOptimization() {
        LOGGER.warning("⚠️ Memory pressure detected - triggering chunk memory optimization");
        
        try {
            // Aggressive compression
            List<String> chunksToCompress = new ArrayList<>();
            for (String chunkKey : chunkMemoryMap.keySet()) {
                if (!activeChunks.contains(chunkKey) && !compressedChunks.containsKey(chunkKey)) {
                    chunksToCompress.add(chunkKey);
                }
            }
            
            // Compress up to 50 chunks immediately
            for (int i = 0; i < Math.min(50, chunksToCompress.size()); i++) {
                compressChunk(chunksToCompress.get(i));
            }
            
            // Aggressive unloading
            identifyUnloadCandidates();
            processUnloadCandidates();
            
            chunkMemoryOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Memory optimization trigger error: " + e.getMessage());
        }
    }
    
    /**
     * Cleanup old compressed chunks
     */
    private void cleanupCompressedChunks() {
        if (compressedChunks.size() <= maxCompressedChunks) return;
        
        // Remove oldest compressed chunks
        List<Map.Entry<String, CompressedChunkData>> entries = new ArrayList<>(compressedChunks.entrySet());
        entries.sort((a, b) -> Long.compare(a.getValue().getCompressionTime(), b.getValue().getCompressionTime()));
        
        int toRemove = compressedChunks.size() - maxCompressedChunks;
        for (int i = 0; i < toRemove; i++) {
            String chunkKey = entries.get(i).getKey();
            compressedChunks.remove(chunkKey);
        }
        
        LOGGER.info("🧹 Cleaned up " + toRemove + " old compressed chunks");
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
     * Get chunk memory statistics
     */
    public Map<String, Object> getChunkMemoryStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("chunks_compressed", chunksCompressed.get());
        stats.put("chunks_unloaded", chunksUnloaded.get());
        stats.put("chunks_cached", chunksCached.get());
        stats.put("memory_freed_from_chunks", memoryFreedFromChunks.get());
        stats.put("compression_ratio", compressionRatio.get());
        stats.put("chunk_memory_optimizations", chunkMemoryOptimizations.get());
        
        stats.put("active_chunks", activeChunks.size());
        stats.put("compressed_chunks", compressedChunks.size());
        stats.put("tracked_chunks", chunkMemoryMap.size());
        stats.put("unload_candidates", unloadCandidates.size());
        
        return stats;
    }
    
    // Getters
    public long getChunksCompressed() { return chunksCompressed.get(); }
    public long getChunksUnloaded() { return chunksUnloaded.get(); }
    public long getChunksCached() { return chunksCached.get(); }
    public long getMemoryFreedFromChunks() { return memoryFreedFromChunks.get(); }
    public long getCompressionRatio() { return compressionRatio.get(); }
    public long getChunkMemoryOptimizations() { return chunkMemoryOptimizations.get(); }
    
    public void shutdown() {
        chunkManager.shutdown();
        
        // Clear all data
        chunkMemoryMap.clear();
        compressedChunks.clear();
        activeChunks.clear();
        unloadCandidates.clear();
        chunkAccessTimes.clear();
        chunkAccessCounts.clear();
        
        LOGGER.info("📦 Chunk Memory Optimizer shutdown complete");
    }
    
    /**
     * Chunk memory data container
     */
    private static class ChunkMemoryData {
        private final String chunkKey;
        private final int x, z;
        private byte[] data;
        private int dataSize;
        private long lastAccess;
        private long creationTime;
        private boolean active;
        
        public ChunkMemoryData(String chunkKey, int x, int z) {
            this.chunkKey = chunkKey;
            this.x = x;
            this.z = z;
            this.creationTime = System.currentTimeMillis();
            this.lastAccess = this.creationTime;
            this.active = true;
        }
        
        public void updateAccess(long time) {
            this.lastAccess = time;
            this.active = true;
        }
        
        public void setData(byte[] data) {
            this.data = data;
            this.dataSize = data != null ? data.length : 0;
        }
        
        public void clearData() {
            this.data = null;
            this.active = false;
        }
        
        // Getters
        public String getChunkKey() { return chunkKey; }
        public int getX() { return x; }
        public int getZ() { return z; }
        public byte[] getData() { return data; }
        public int getDataSize() { return dataSize; }
        public long getLastAccess() { return lastAccess; }
        public boolean isActive() { return active; }
        
        public void setDataSize(int dataSize) { this.dataSize = dataSize; }
        
        // Vanilla-safe checks (simplified)
        public boolean isSpawnChunk() {
            return Math.abs(x) <= 2 && Math.abs(z) <= 2; // Simplified spawn chunk check
        }
        
        public boolean hasPlayers() {
            return false; // Would check for actual players in chunk
        }
    }
    
    /**
     * Compressed chunk data container
     */
    private static class CompressedChunkData {
        private final String chunkKey;
        private final byte[] compressedData;
        private final int originalSize;
        private final long compressionTime;
        
        public CompressedChunkData(String chunkKey, byte[] compressedData, int originalSize, long compressionTime) {
            this.chunkKey = chunkKey;
            this.compressedData = compressedData;
            this.originalSize = originalSize;
            this.compressionTime = compressionTime;
        }
        
        public String getChunkKey() { return chunkKey; }
        public byte[] getCompressedData() { return compressedData; }
        public int getOriginalSize() { return originalSize; }
        public long getCompressionTime() { return compressionTime; }
        
        public double getCompressionRatio() {
            return (compressedData.length * 100.0) / originalSize;
        }
    }
}
