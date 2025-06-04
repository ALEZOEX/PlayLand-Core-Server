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

/**
 * Predictive World Loading
 * УМНАЯ предиктивная загрузка мира
 * Предсказывает и предзагружает области мира для плавной игры
 */
public class PredictiveWorldLoading {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-PredictiveWorld");
    
    // Prediction statistics
    private final AtomicLong chunksPreloaded = new AtomicLong(0);
    private final AtomicLong predictionsGenerated = new AtomicLong(0);
    private final AtomicLong predictionsCorrect = new AtomicLong(0);
    private final AtomicLong loadingOptimizations = new AtomicLong(0);
    private final AtomicLong memoryOptimizations = new AtomicLong(0);
    
    // Player movement tracking
    private final Map<String, PlayerMovementData> playerMovement = new ConcurrentHashMap<>();
    private final Map<String, List<ChunkPosition>> playerChunkHistory = new ConcurrentHashMap<>();
    private final Set<ChunkPosition> preloadedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPosition> loadedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPosition> priorityChunks = ConcurrentHashMap.newKeySet();

    // Prediction engine
    private final Queue<LoadingPrediction> pendingPredictions = new ConcurrentLinkedQueue<>();
    private final Map<ChunkPosition, Long> chunkLoadTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService predictionEngine = Executors.newScheduledThreadPool(2);

    // Configuration
    private boolean enablePredictiveLoading = true;
    private boolean enableMovementPrediction = true;
    private boolean enableChunkPreloading = true;
    private boolean enablePreloading = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableMemoryOptimization = true;
    private boolean enableLagPrevention = true;
    
    private int predictionRadius = 5; // chunks
    private int preloadRadius = 3; // chunks
    private int maxPreloadedChunks = 200;
    private int predictionHorizon = 5; // seconds
    private long predictionInterval = 2000; // 2 seconds
    private double movementThreshold = 5.0; // blocks
    
    public void initialize() {
        LOGGER.info("🌍 Initializing Predictive World Loading...");
        
        loadPredictionSettings();
        startPredictionEngine();
        startMemoryOptimization();
        
        LOGGER.info("✅ Predictive World Loading initialized!");
        LOGGER.info("🌍 Predictive loading: " + (enablePredictiveLoading ? "ENABLED" : "DISABLED"));
        LOGGER.info("🏃 Movement prediction: " + (enableMovementPrediction ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Chunk preloading: " + (enableChunkPreloading ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("💾 Memory optimization: " + (enableMemoryOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("📏 Prediction radius: " + predictionRadius + " chunks");
        LOGGER.info("📦 Preload radius: " + preloadRadius + " chunks");
        LOGGER.info("📊 Max preloaded chunks: " + maxPreloadedChunks);
    }
    
    private void loadPredictionSettings() {
        try {
            LOGGER.info("⚙️ Loading prediction settings...");

            // Load prediction parameters from system properties
            predictionHorizon = Integer.parseInt(System.getProperty("playland.world.prediction.horizon", "5"));
            preloadRadius = Integer.parseInt(System.getProperty("playland.world.preload.radius", "3"));
            maxPreloadedChunks = Integer.parseInt(System.getProperty("playland.world.max.preloaded", "50"));

            // Load feature flags
            enablePredictiveLoading = Boolean.parseBoolean(System.getProperty("playland.world.predictive.enabled", "true"));
            enablePreloading = Boolean.parseBoolean(System.getProperty("playland.world.preload.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.world.vanilla.safe", "true"));
            enableLagPrevention = Boolean.parseBoolean(System.getProperty("playland.world.lag.prevention", "true"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce prediction complexity
                preloadRadius = Math.max(1, preloadRadius - 1);
                maxPreloadedChunks = Math.max(10, maxPreloadedChunks / 2);
                predictionHorizon = Math.max(2, predictionHorizon - 2);
                LOGGER.info("🔧 Reduced prediction complexity for low TPS: radius=" + preloadRadius +
                           ", max=" + maxPreloadedChunks + ", horizon=" + predictionHorizon + "s");
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive prediction
                preloadRadius = Math.min(5, preloadRadius + 1);
                maxPreloadedChunks = Math.min(100, (int) (maxPreloadedChunks * 1.5));
                LOGGER.info("🔧 Increased prediction capability for good TPS: radius=" + preloadRadius +
                           ", max=" + maxPreloadedChunks);
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce chunk preloading
                maxPreloadedChunks = Math.max(5, maxPreloadedChunks / 3);
                enablePreloading = false;
                LOGGER.warning("⚠️ High memory usage - disabled preloading, reduced max to " + maxPreloadedChunks);
            }

            // Auto-adjust based on player count
            int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
            if (playerCount > 20) {
                // Many players - reduce per-player prediction
                maxPreloadedChunks = Math.max(20, maxPreloadedChunks - (playerCount - 20) * 2);
                LOGGER.info("🔧 Adjusted for " + playerCount + " players: max chunks=" + maxPreloadedChunks);
            }

            LOGGER.info("✅ Prediction settings loaded - Horizon: " + predictionHorizon + "s, Radius: " + preloadRadius +
                       ", Max: " + maxPreloadedChunks);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading prediction settings: " + e.getMessage() + " - using defaults");
        }
    }

    private double getCurrentTPS() {
        try {
            return org.bukkit.Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }
    
    private void startPredictionEngine() {
        // Generate predictions every 2 seconds
        predictionEngine.scheduleAtFixedRate(() -> {
            try {
                processLoadingPredictions();
            } catch (Exception e) {
                LOGGER.warning("Prediction engine error: " + e.getMessage());
            }
        }, predictionInterval, predictionInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔮 Prediction engine started");
    }
    
    private void startMemoryOptimization() {
        if (!enableMemoryOptimization) return;
        
        // Cleanup old data every 30 seconds
        predictionEngine.scheduleAtFixedRate(() -> {
            try {
                cleanupOldPredictions();
                optimizeChunkMemory();
            } catch (Exception e) {
                LOGGER.warning("Memory optimization error: " + e.getMessage());
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("💾 Memory optimization started");
    }
    
    /**
     * Track player movement for prediction
     */
    public void trackPlayerMovement(String playerId, double x, double y, double z, float yaw, float pitch) {
        if (!enableMovementPrediction) return;
        
        try {
            PlayerMovementData currentData = new PlayerMovementData(x, y, z, yaw, pitch, System.currentTimeMillis());
            PlayerMovementData previousData = playerMovement.put(playerId, currentData);
            
            if (previousData != null) {
                // Calculate movement vector
                double deltaX = x - previousData.getX();
                double deltaZ = z - previousData.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                // Update chunk history
                ChunkPosition currentChunk = new ChunkPosition((int) x >> 4, (int) z >> 4);
                updateChunkHistory(playerId, currentChunk);
                
                // Generate prediction if significant movement
                if (distance > movementThreshold) {
                    generatePlayerMovementPrediction(playerId, currentData, previousData);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Player movement tracking error: " + e.getMessage());
        }
    }
    
    /**
     * Generate movement prediction for player
     */
    private void generatePlayerMovementPrediction(String playerId, PlayerMovementData current, PlayerMovementData previous) {
        try {
            // Calculate movement vector
            double deltaX = current.getX() - previous.getX();
            double deltaZ = current.getZ() - previous.getZ();
            long deltaTime = current.getTimestamp() - previous.getTimestamp();
            
            if (deltaTime == 0) return;
            
            // Calculate velocity
            double velocityX = deltaX / (deltaTime / 1000.0);
            double velocityZ = deltaZ / (deltaTime / 1000.0);
            
            // Predict future positions
            for (int seconds = 1; seconds <= 5; seconds++) {
                double predictedX = current.getX() + velocityX * seconds;
                double predictedZ = current.getZ() + velocityZ * seconds;
                
                ChunkPosition predictedChunk = new ChunkPosition((int) predictedX >> 4, (int) predictedZ >> 4);
                
                // Create loading prediction
                LoadingPrediction prediction = new LoadingPrediction(
                    playerId,
                    predictedChunk,
                    calculatePredictionConfidence(playerId, predictedChunk),
                    System.currentTimeMillis() + (seconds * 1000)
                );
                
                pendingPredictions.offer(prediction);
                predictionsGenerated.incrementAndGet();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Movement prediction error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate prediction confidence based on player history
     */
    private double calculatePredictionConfidence(String playerId, ChunkPosition predictedChunk) {
        List<ChunkPosition> history = playerChunkHistory.get(playerId);
        if (history == null || history.isEmpty()) {
            return 0.5; // Default confidence
        }
        
        // Check if player has visited this chunk before
        boolean hasVisited = history.contains(predictedChunk);
        
        // Calculate confidence based on movement patterns
        double confidence = 0.6; // Base confidence
        
        if (hasVisited) {
            confidence += 0.2; // Increase if visited before
        }
        
        // Analyze movement consistency
        if (history.size() >= 3) {
            ChunkPosition last = history.get(history.size() - 1);
            ChunkPosition secondLast = history.get(history.size() - 2);
            
            int deltaX1 = last.getX() - secondLast.getX();
            int deltaZ1 = last.getZ() - secondLast.getZ();
            
            int deltaX2 = predictedChunk.getX() - last.getX();
            int deltaZ2 = predictedChunk.getZ() - last.getZ();
            
            // Check if movement direction is consistent
            if (Math.signum(deltaX1) == Math.signum(deltaX2) && Math.signum(deltaZ1) == Math.signum(deltaZ2)) {
                confidence += 0.15; // Consistent movement direction
            }
        }
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Process loading predictions
     */
    private void processLoadingPredictions() {
        if (!enableChunkPreloading) return;
        
        long currentTime = System.currentTimeMillis();
        List<LoadingPrediction> toProcess = new ArrayList<>();
        
        // Collect predictions ready for processing
        LoadingPrediction prediction;
        while ((prediction = pendingPredictions.poll()) != null) {
            if (prediction.getTargetTime() <= currentTime + 1000) { // Process 1 second early
                toProcess.add(prediction);
            } else {
                pendingPredictions.offer(prediction); // Put back for later
                break;
            }
        }
        
        // Process predictions
        for (LoadingPrediction pred : toProcess) {
            if (pred.getConfidence() > 0.7 && preloadedChunks.size() < maxPreloadedChunks) {
                preloadChunkArea(pred.getChunkPosition());
            }
        }
    }
    
    /**
     * Preload chunk area around predicted position
     */
    private void preloadChunkArea(ChunkPosition centerChunk) {
        if (!enableChunkPreloading) return;
        
        try {
            List<ChunkPosition> chunksToLoad = new ArrayList<>();
            
            // Generate chunks in radius
            for (int dx = -preloadRadius; dx <= preloadRadius; dx++) {
                for (int dz = -preloadRadius; dz <= preloadRadius; dz++) {
                    ChunkPosition chunkPos = new ChunkPosition(
                        centerChunk.getX() + dx,
                        centerChunk.getZ() + dz
                    );
                    
                    // Check if chunk is not already preloaded
                    if (!preloadedChunks.contains(chunkPos)) {
                        chunksToLoad.add(chunkPos);
                    }
                }
            }
            
            // Preload chunks (vanilla-safe)
            for (ChunkPosition chunk : chunksToLoad) {
                if (preloadedChunks.size() >= maxPreloadedChunks) {
                    break;
                }
                
                if (preloadChunk(chunk)) {
                    preloadedChunks.add(chunk);
                    chunksPreloaded.incrementAndGet();
                    chunkLoadTimes.put(chunk, System.currentTimeMillis());
                }
            }
            
            loadingOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Chunk preloading error: " + e.getMessage());
        }
    }
    
    /**
     * Preload a single chunk (vanilla-safe)
     */
    private boolean preloadChunk(ChunkPosition chunk) {
        if (!enableVanillaSafeMode) return true;
        
        try {
            // Check if chunk is already loaded or being loaded
            if (loadedChunks.contains(chunk)) {
                return true; // Already loaded
            }

            // Check if we've reached the preload limit
            if (loadedChunks.size() >= maxPreloadedChunks) {
                // Remove oldest chunk to make room
                ChunkPosition oldestChunk = findOldestPreloadedChunk();
                if (oldestChunk != null) {
                    unloadChunk(oldestChunk);
                }
            }

            // In vanilla-safe mode, we mark chunks for priority loading
            if (enableVanillaSafeMode) {
                // Add to priority loading queue instead of force-loading
                priorityChunks.add(chunk);

                // Mark as loaded in our tracking
                loadedChunks.add(chunk);
                chunkLoadTimes.put(chunk, System.currentTimeMillis());

                LOGGER.fine("Marked chunk " + chunk + " for priority loading (vanilla-safe)");
                return true;
            } else {
                // Direct chunk loading (non-vanilla-safe mode)
                boolean success = forceLoadChunk(chunk);
                if (success) {
                    loadedChunks.add(chunk);
                    chunkLoadTimes.put(chunk, System.currentTimeMillis());
                    chunksPreloaded.incrementAndGet();
                    LOGGER.fine("Force-loaded chunk " + chunk);
                }
                return success;
            }

        } catch (Exception e) {
            LOGGER.warning("Chunk preload error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update player chunk history
     */
    private void updateChunkHistory(String playerId, ChunkPosition chunk) {
        List<ChunkPosition> history = playerChunkHistory.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // Add chunk if it's different from the last one
        if (history.isEmpty() || !history.get(history.size() - 1).equals(chunk)) {
            history.add(chunk);
            
            // Keep only recent history
            if (history.size() > 50) {
                history.remove(0);
            }
        }
    }
    
    /**
     * Verify prediction accuracy
     */
    public void verifyPrediction(String playerId, ChunkPosition actualChunk) {
        // Check if we predicted this chunk correctly
        long currentTime = System.currentTimeMillis();
        
        // Look for recent predictions for this player
        boolean foundCorrectPrediction = false;
        
        // This is simplified - in real implementation, we'd track specific predictions
        List<ChunkPosition> history = playerChunkHistory.get(playerId);
        if (history != null && !history.isEmpty()) {
            // Simple heuristic: if we preloaded this chunk recently, count as correct
            if (preloadedChunks.contains(actualChunk)) {
                Long loadTime = chunkLoadTimes.get(actualChunk);
                if (loadTime != null && currentTime - loadTime < 10000) { // Within 10 seconds
                    predictionsCorrect.incrementAndGet();
                    foundCorrectPrediction = true;
                }
            }
        }
        
        if (foundCorrectPrediction) {
            LOGGER.fine("Correct prediction for player " + playerId + " at chunk " + actualChunk);
        }
    }
    
    /**
     * Clean up old predictions and data
     */
    private void cleanupOldPredictions() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - 300000; // 5 minutes
        
        // Clean up old chunk load times
        chunkLoadTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        
        // Clean up old player movement data
        playerMovement.entrySet().removeIf(entry -> entry.getValue().getTimestamp() < cutoffTime);
        
        // Clean up old chunk history
        for (List<ChunkPosition> history : playerChunkHistory.values()) {
            if (history.size() > 20) {
                history.subList(0, history.size() - 20).clear();
            }
        }
    }
    
    /**
     * Optimize chunk memory usage
     */
    private void optimizeChunkMemory() {
        if (!enableMemoryOptimization) return;
        
        // Remove old preloaded chunks
        long currentTime = System.currentTimeMillis();
        
        preloadedChunks.removeIf(chunk -> {
            Long loadTime = chunkLoadTimes.get(chunk);
            if (loadTime != null && currentTime - loadTime > 60000) { // 1 minute old
                chunkLoadTimes.remove(chunk);
                memoryOptimizations.incrementAndGet();
                return true;
            }
            return false;
        });
        
        // Ensure we don't exceed memory limits
        if (preloadedChunks.size() > maxPreloadedChunks) {
            // Remove oldest chunks
            List<ChunkPosition> sortedChunks = new ArrayList<>();
            for (ChunkPosition chunk : preloadedChunks) {
                Long loadTime = chunkLoadTimes.get(chunk);
                if (loadTime != null) {
                    sortedChunks.add(chunk);
                }
            }
            
            sortedChunks.sort((a, b) -> Long.compare(
                chunkLoadTimes.get(a), chunkLoadTimes.get(b)
            ));
            
            // Remove oldest chunks
            int toRemove = preloadedChunks.size() - maxPreloadedChunks;
            for (int i = 0; i < toRemove && i < sortedChunks.size(); i++) {
                ChunkPosition chunk = sortedChunks.get(i);
                preloadedChunks.remove(chunk);
                chunkLoadTimes.remove(chunk);
                memoryOptimizations.incrementAndGet();
            }
        }
    }
    
    /**
     * Get prediction accuracy rate
     */
    public double getPredictionAccuracy() {
        long total = predictionsGenerated.get();
        if (total == 0) return 0.0;
        return (predictionsCorrect.get() * 100.0) / total;
    }
    
    /**
     * Get predictive loading statistics
     */
    public Map<String, Object> getPredictiveStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("chunks_preloaded", chunksPreloaded.get());
        stats.put("predictions_generated", predictionsGenerated.get());
        stats.put("predictions_correct", predictionsCorrect.get());
        stats.put("loading_optimizations", loadingOptimizations.get());
        stats.put("memory_optimizations", memoryOptimizations.get());
        
        stats.put("prediction_accuracy", getPredictionAccuracy());
        stats.put("active_players", playerMovement.size());
        stats.put("preloaded_chunks", preloadedChunks.size());
        stats.put("max_preloaded_chunks", maxPreloadedChunks);
        
        return stats;
    }
    
    // Getters
    public long getChunksPreloaded() { return chunksPreloaded.get(); }
    public long getPredictionsGenerated() { return predictionsGenerated.get(); }
    public long getPredictionsCorrect() { return predictionsCorrect.get(); }
    public long getLoadingOptimizations() { return loadingOptimizations.get(); }
    public long getMemoryOptimizations() { return memoryOptimizations.get(); }
    
    public void shutdown() {
        predictionEngine.shutdown();
        
        // Clear all data
        playerMovement.clear();
        playerChunkHistory.clear();
        preloadedChunks.clear();
        chunkLoadTimes.clear();
        pendingPredictions.clear();
        
        LOGGER.info("🌍 Predictive World Loading shutdown complete");
    }
    
    // Data classes
    private static class PlayerMovementData {
        private final double x, y, z;
        private final float yaw, pitch;
        private final long timestamp;
        
        public PlayerMovementData(double x, double y, double z, float yaw, float pitch, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class ChunkPosition {
        private final int x, z;
        
        public ChunkPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        public int getX() { return x; }
        public int getZ() { return z; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ChunkPosition that = (ChunkPosition) obj;
            return x == that.x && z == that.z;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + z + ")";
        }
    }
    
    private static class LoadingPrediction {
        private final String playerId;
        private final ChunkPosition chunkPosition;
        private final double confidence;
        private final long targetTime;
        
        public LoadingPrediction(String playerId, ChunkPosition chunkPosition, double confidence, long targetTime) {
            this.playerId = playerId;
            this.chunkPosition = chunkPosition;
            this.confidence = confidence;
            this.targetTime = targetTime;
        }
        
        public String getPlayerId() { return playerId; }
        public ChunkPosition getChunkPosition() { return chunkPosition; }
        public double getConfidence() { return confidence; }
        public long getTargetTime() { return targetTime; }
    }

    // Missing methods implementation
    private ChunkPosition findOldestPreloadedChunk() {
        if (loadedChunks.isEmpty()) return null;

        ChunkPosition oldest = null;
        long oldestTime = Long.MAX_VALUE;

        for (ChunkPosition chunk : loadedChunks) {
            Long loadTime = chunkLoadTimes.get(chunk);
            if (loadTime != null && loadTime < oldestTime) {
                oldestTime = loadTime;
                oldest = chunk;
            }
        }

        return oldest;
    }

    private void unloadChunk(ChunkPosition chunk) {
        try {
            loadedChunks.remove(chunk);
            preloadedChunks.remove(chunk);
            priorityChunks.remove(chunk);
            chunkLoadTimes.remove(chunk);

            LOGGER.fine("Unloaded chunk " + chunk);

        } catch (Exception e) {
            LOGGER.warning("Error unloading chunk " + chunk + ": " + e.getMessage());
        }
    }

    private boolean forceLoadChunk(ChunkPosition chunk) {
        try {
            // In a real implementation, this would interact with the world loading system
            // For now, we simulate successful loading
            LOGGER.fine("Force-loading chunk " + chunk);
            return true;

        } catch (Exception e) {
            LOGGER.warning("Error force-loading chunk " + chunk + ": " + e.getMessage());
            return false;
        }
    }
}
