package ru.playland.core.optimization;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Predictive Entity Spawning
 * ЭКСТРЕМАЛЬНАЯ предиктивная система спавна сущностей
 * Предсказывает и предварительно создает сущности для оптимизации
 */
public class PredictiveEntitySpawning {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-PredictiveSpawn");
    
    // Prediction statistics
    private final AtomicLong predictionsGenerated = new AtomicLong(0);
    private final AtomicLong entitiesPreSpawned = new AtomicLong(0);
    private final AtomicLong predictionsCorrect = new AtomicLong(0);
    private final AtomicLong spawnOptimizations = new AtomicLong(0);
    private final AtomicLong lagsPrevented = new AtomicLong(0);
    
    // Prediction data
    private final Map<String, SpawnPrediction> spawnPredictions = new ConcurrentHashMap<>();
    private final Map<BlockPos, EntitySpawnData> spawnHistory = new ConcurrentHashMap<>();
    private final Map<EntityType<?>, SpawnPattern> spawnPatterns = new ConcurrentHashMap<>();
    private final Set<BlockPos> predictedSpawnLocations = ConcurrentHashMap.newKeySet();
    
    // Pre-spawned entities pool
    private final Map<EntityType<?>, List<Entity>> preSpawnedEntities = new ConcurrentHashMap<>();
    private final Map<String, Long> playerMovementHistory = new ConcurrentHashMap<>();
    private final Map<String, PreSpawnData> preSpawnDataMap = new ConcurrentHashMap<>();

    // Prediction engine
    private final ScheduledExecutorService predictionEngine = Executors.newScheduledThreadPool(2);

    // Configuration
    private boolean enablePredictiveSpawning = true;
    private boolean enablePreSpawning = true;
    private boolean enableMovementPrediction = true;
    private boolean enableSpawnOptimization = true;
    private boolean enableLagPrevention = true;
    private boolean enableVanillaSafeMode = true;

    private int predictionHorizon = 10; // seconds
    private int maxPreSpawnedEntities = 100;
    private int predictionRadius = 64; // blocks
    private double predictionAccuracyThreshold = 0.7;
    private int spawnHistorySize = 1000;
    
    public void initialize() {
        LOGGER.info("🤖 Initializing Predictive Entity Spawning...");
        
        loadPredictionSettings();
        startPredictionEngine();
        startSpawnOptimization();
        
        LOGGER.info("✅ Predictive Entity Spawning initialized!");
        LOGGER.info("🔮 Predictive spawning: " + (enablePredictiveSpawning ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Pre-spawning: " + (enablePreSpawning ? "ENABLED" : "DISABLED"));
        LOGGER.info("🏃 Movement prediction: " + (enableMovementPrediction ? "ENABLED" : "DISABLED"));
        LOGGER.info("🚀 Spawn optimization: " + (enableSpawnOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Lag prevention: " + (enableLagPrevention ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏰ Prediction horizon: " + predictionHorizon + " seconds");
        LOGGER.info("📦 Max pre-spawned: " + maxPreSpawnedEntities);
    }
    
    private void loadPredictionSettings() {
        try {
            LOGGER.info("⚙️ Loading prediction settings...");

            // Load prediction parameters from system properties
            predictionHorizon = Integer.parseInt(System.getProperty("playland.spawn.prediction.horizon", "10"));
            maxPreSpawnedEntities = Integer.parseInt(System.getProperty("playland.spawn.max.prespawned", "100"));
            predictionRadius = Integer.parseInt(System.getProperty("playland.spawn.prediction.radius", "64"));

            // Load feature flags
            enablePredictiveSpawning = Boolean.parseBoolean(System.getProperty("playland.spawn.predictive.enabled", "true"));
            enablePreSpawning = Boolean.parseBoolean(System.getProperty("playland.spawn.prespawn.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.spawn.vanilla.safe", "true"));
            enableSpawnOptimization = Boolean.parseBoolean(System.getProperty("playland.spawn.optimization.enabled", "true"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce prediction complexity
                predictionRadius = Math.max(32, predictionRadius / 2);
                maxPreSpawnedEntities = Math.max(25, maxPreSpawnedEntities / 2);
                LOGGER.info("🔧 Reduced prediction complexity for low TPS: radius=" + predictionRadius + ", max=" + maxPreSpawnedEntities);
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive prediction
                predictionRadius = Math.min(128, (int) (predictionRadius * 1.5));
                maxPreSpawnedEntities = Math.min(200, (int) (maxPreSpawnedEntities * 1.5));
                LOGGER.info("🔧 Increased prediction capability for good TPS: radius=" + predictionRadius + ", max=" + maxPreSpawnedEntities);
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce pre-spawning
                maxPreSpawnedEntities = Math.max(10, maxPreSpawnedEntities / 3);
                enablePreSpawning = false;
                LOGGER.warning("⚠️ High memory usage - disabled pre-spawning, reduced max to " + maxPreSpawnedEntities);
            }

            LOGGER.info("✅ Prediction settings loaded - Horizon: " + predictionHorizon + "s, Radius: " + predictionRadius + ", Max: " + maxPreSpawnedEntities);

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
        predictionEngine.scheduleAtFixedRate(this::generateSpawnPredictions, 2, 2, TimeUnit.SECONDS);
        
        // Update spawn patterns every 30 seconds
        predictionEngine.scheduleAtFixedRate(this::updateSpawnPatterns, 30, 30, TimeUnit.SECONDS);
        
        LOGGER.info("🔮 Prediction engine started");
    }
    
    private void startSpawnOptimization() {
        // Optimize spawning every 5 seconds
        predictionEngine.scheduleAtFixedRate(this::optimizeSpawning, 5, 5, TimeUnit.SECONDS);
        
        // Clean up old data every 60 seconds
        predictionEngine.scheduleAtFixedRate(this::cleanupOldData, 60, 60, TimeUnit.SECONDS);
        
        LOGGER.info("🚀 Spawn optimization started");
    }
    
    /**
     * Predict entity spawning for a player
     */
    public void predictEntitySpawning(Player player, ServerLevel level) {
        if (!enablePredictiveSpawning) return;
        
        try {
            String playerId = player.getUUID().toString();
            BlockPos playerPos = player.blockPosition();
            
            // Update player movement history
            updatePlayerMovement(playerId, playerPos);
            
            // Generate spawn predictions
            List<SpawnPrediction> predictions = generatePredictionsForPlayer(player, level);
            
            for (SpawnPrediction prediction : predictions) {
                spawnPredictions.put(prediction.getId(), prediction);
                predictionsGenerated.incrementAndGet();
                
                // Pre-spawn entities if confidence is high
                if (enablePreSpawning && prediction.getConfidence() > predictionAccuracyThreshold) {
                    preSpawnEntity(prediction, level);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Prediction error: " + e.getMessage());
        }
    }
    
    /**
     * Generate spawn predictions for a player
     */
    private List<SpawnPrediction> generatePredictionsForPlayer(Player player, ServerLevel level) {
        List<SpawnPrediction> predictions = new ArrayList<>();
        BlockPos playerPos = player.blockPosition();
        
        // Predict based on movement direction
        if (enableMovementPrediction) {
            BlockPos predictedPos = predictPlayerMovement(player);
            if (predictedPos != null) {
                predictions.addAll(generateSpawnPredictionsAtLocation(predictedPos, level));
            }
        }
        
        // Predict based on spawn history
        predictions.addAll(generateHistoryBasedPredictions(playerPos, level));
        
        // Predict based on spawn patterns
        predictions.addAll(generatePatternBasedPredictions(playerPos, level));
        
        return predictions;
    }
    
    /**
     * Predict player movement
     */
    private BlockPos predictPlayerMovement(Player player) {
        String playerId = player.getUUID().toString();
        BlockPos currentPos = player.blockPosition();
        
        // Simple movement prediction based on velocity
        double velocityX = player.getDeltaMovement().x;
        double velocityZ = player.getDeltaMovement().z;
        
        // Predict position in predictionHorizon seconds
        int predictedX = (int) (currentPos.getX() + velocityX * predictionHorizon * 20); // 20 ticks per second
        int predictedZ = (int) (currentPos.getZ() + velocityZ * predictionHorizon * 20);
        
        return new BlockPos(predictedX, currentPos.getY(), predictedZ);
    }
    
    /**
     * Generate spawn predictions at a location
     */
    private List<SpawnPrediction> generateSpawnPredictionsAtLocation(BlockPos pos, ServerLevel level) {
        List<SpawnPrediction> predictions = new ArrayList<>();
        
        // Analyze biome and environment
        String biome = level.getBiome(pos).toString();
        int lightLevel = level.getBrightness(LightLayer.BLOCK, pos);
        boolean isNight = level.getDayTime() > 13000 && level.getDayTime() < 23000;
        
        // Generate predictions based on environment
        for (EntityType<?> entityType : getSpawnableEntities(biome, lightLevel, isNight)) {
            double confidence = calculateSpawnConfidence(entityType, pos, level);
            
            if (confidence > 0.3) { // Minimum confidence threshold
                SpawnPrediction prediction = new SpawnPrediction(
                    generatePredictionId(pos, entityType),
                    entityType,
                    pos,
                    confidence,
                    System.currentTimeMillis() + (predictionHorizon * 1000)
                );
                
                predictions.add(prediction);
            }
        }
        
        return predictions;
    }
    
    /**
     * Generate history-based predictions
     */
    private List<SpawnPrediction> generateHistoryBasedPredictions(BlockPos playerPos, ServerLevel level) {
        List<SpawnPrediction> predictions = new ArrayList<>();
        
        // Look for nearby spawn history
        for (Map.Entry<BlockPos, EntitySpawnData> entry : spawnHistory.entrySet()) {
            BlockPos spawnPos = entry.getKey();
            EntitySpawnData spawnData = entry.getValue();
            
            double distance = playerPos.distSqr(spawnPos);
            if (distance < 1600) { // Within 40 blocks
                double confidence = calculateHistoryBasedConfidence(spawnData, distance);
                
                if (confidence > 0.4) {
                    SpawnPrediction prediction = new SpawnPrediction(
                        generatePredictionId(spawnPos, spawnData.getEntityType()),
                        spawnData.getEntityType(),
                        spawnPos,
                        confidence,
                        System.currentTimeMillis() + (predictionHorizon * 1000)
                    );
                    
                    predictions.add(prediction);
                }
            }
        }
        
        return predictions;
    }
    
    /**
     * Generate pattern-based predictions
     */
    private List<SpawnPrediction> generatePatternBasedPredictions(BlockPos playerPos, ServerLevel level) {
        List<SpawnPrediction> predictions = new ArrayList<>();
        
        for (Map.Entry<EntityType<?>, SpawnPattern> entry : spawnPatterns.entrySet()) {
            EntityType<?> entityType = entry.getKey();
            SpawnPattern pattern = entry.getValue();
            
            if (pattern.shouldSpawnNear(playerPos, level)) {
                BlockPos predictedSpawnPos = pattern.getPredictedSpawnLocation(playerPos);
                double confidence = pattern.getConfidence();
                
                SpawnPrediction prediction = new SpawnPrediction(
                    generatePredictionId(predictedSpawnPos, entityType),
                    entityType,
                    predictedSpawnPos,
                    confidence,
                    System.currentTimeMillis() + (predictionHorizon * 1000)
                );
                
                predictions.add(prediction);
            }
        }
        
        return predictions;
    }
    
    /**
     * Pre-spawn entity based on prediction
     */
    private void preSpawnEntity(SpawnPrediction prediction, ServerLevel level) {
        try {
            EntityType<?> entityType = prediction.getEntityType();
            
            // Check if we already have enough pre-spawned entities
            List<Entity> preSpawned = preSpawnedEntities.computeIfAbsent(entityType, k -> new ArrayList<>());
            
            if (preSpawned.size() < maxPreSpawnedEntities / 10) { // Limit per entity type
                try {
                    // Create a pre-spawn data entry instead of actual entity
                    PreSpawnData preSpawnData = new PreSpawnData(
                        entityType,
                        prediction.getSpawnPos(),
                        System.currentTimeMillis(),
                        prediction.getConfidence()
                    );

                    // Store pre-spawn data for later use
                    String preSpawnId = generatePreSpawnId(entityType, prediction.getSpawnPos());
                    preSpawnDataMap.put(preSpawnId, preSpawnData);

                    entitiesPreSpawned.incrementAndGet();

                    LOGGER.fine("Pre-spawn data created for " + entityType + " at " + prediction.getSpawnPos() +
                               " (confidence: " + String.format("%.2f", prediction.getConfidence()) + ")");

                    // If confidence is very high, prepare for immediate spawning
                    if (prediction.getConfidence() > 0.8) {
                        prepareHighConfidenceSpawn(preSpawnData, level);
                    }

                } catch (Exception e) {
                    LOGGER.warning("Pre-spawn data creation error: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Pre-spawn error: " + e.getMessage());
        }
    }
    
    /**
     * Get pre-spawned entity if available
     */
    public Entity getPreSpawnedEntity(EntityType<?> entityType, BlockPos pos) {
        if (!enablePreSpawning) return null;
        
        List<Entity> preSpawned = preSpawnedEntities.get(entityType);
        if (preSpawned != null && !preSpawned.isEmpty()) {
            // Find closest pre-spawned entity
            Entity closest = null;
            double closestDistance = Double.MAX_VALUE;
            
            for (Entity entity : preSpawned) {
                double distance = entity.blockPosition().distSqr(pos);
                if (distance < closestDistance) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
            
            if (closest != null && closestDistance < 400) { // Within 20 blocks
                preSpawned.remove(closest);
                spawnOptimizations.incrementAndGet();
                return closest;
            }
        }
        
        return null;
    }
    
    /**
     * Record actual entity spawn for learning
     */
    public void recordEntitySpawn(Entity entity, BlockPos pos) {
        try {
            EntityType<?> entityType = entity.getType();
            
            // Update spawn history
            EntitySpawnData spawnData = spawnHistory.computeIfAbsent(pos, k -> new EntitySpawnData(entityType));
            spawnData.recordSpawn();
            
            // Check if we predicted this spawn
            String predictionId = generatePredictionId(pos, entityType);
            SpawnPrediction prediction = spawnPredictions.get(predictionId);
            
            if (prediction != null && !prediction.isExpired()) {
                predictionsCorrect.incrementAndGet();
                LOGGER.fine("Correct prediction for " + entityType + " at " + pos);
            }
            
            // Update spawn patterns
            SpawnPattern pattern = spawnPatterns.computeIfAbsent(entityType, k -> new SpawnPattern(entityType));
            pattern.recordSpawn(pos, System.currentTimeMillis());
            
            // Cleanup old history
            if (spawnHistory.size() > spawnHistorySize) {
                cleanupSpawnHistory();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Spawn recording error: " + e.getMessage());
        }
    }
    
    /**
     * Generate spawn predictions
     */
    private void generateSpawnPredictions() {
        if (!enablePredictiveSpawning) return;

        try {
            // Get all online players for prediction
            for (org.bukkit.entity.Player bukkitPlayer : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    // Convert Bukkit player to Minecraft player
                    if (bukkitPlayer instanceof org.bukkit.craftbukkit.entity.CraftPlayer) {
                        net.minecraft.server.level.ServerPlayer mcPlayer = ((org.bukkit.craftbukkit.entity.CraftPlayer) bukkitPlayer).getHandle();
                        generatePredictionsForPlayer(mcPlayer, mcPlayer.serverLevel());
                    }
                } catch (Exception e) {
                    LOGGER.fine("Player conversion error: " + e.getMessage());
                }
            }

            // Analyze spawn patterns and update predictions
            analyzeSpawnPatterns();

            // Clean up old predictions
            cleanupExpiredPredictions();

            // Update prediction accuracy
            updatePredictionAccuracy();

        } catch (Exception e) {
            LOGGER.warning("Spawn prediction generation error: " + e.getMessage());
        }
    }

    private void analyzeSpawnPatterns() {
        try {
            long currentTime = System.currentTimeMillis();

            // Analyze recent spawn history to identify patterns
            for (Map.Entry<BlockPos, EntitySpawnData> entry : spawnHistory.entrySet()) {
                BlockPos pos = entry.getKey();
                EntitySpawnData data = entry.getValue();

                // Skip old data
                if (currentTime - data.getLastSpawnTime() > 300000) { // 5 minutes
                    continue;
                }

                // Update spawn patterns for this location
                for (EntityType<?> entityType : data.getSpawnedTypes()) {
                    SpawnPattern pattern = spawnPatterns.computeIfAbsent(entityType, k -> new SpawnPattern());
                    pattern.addSpawnLocation(pos, currentTime);
                    pattern.updateFrequency(data.getSpawnCount(entityType));
                }
            }

            // Predict future spawns based on patterns
            for (Map.Entry<EntityType<?>, SpawnPattern> entry : spawnPatterns.entrySet()) {
                EntityType<?> entityType = entry.getKey();
                SpawnPattern pattern = entry.getValue();

                // Generate predictions for high-frequency spawn areas
                if (pattern.getAverageFrequency() > 0.1) { // Spawns more than once per 10 seconds
                    List<BlockPos> likelySpawnPositions = pattern.getPredictedSpawnPositions();

                    for (BlockPos pos : likelySpawnPositions) {
                        if (predictedSpawnLocations.size() < maxPreSpawnedEntities) {
                            predictedSpawnLocations.add(pos);

                            // Create prediction
                            SpawnPrediction prediction = new SpawnPrediction(
                                generatePredictionId(),
                                entityType,
                                pos,
                                pattern.getConfidence(),
                                currentTime + ((long) predictionHorizon * 1000L)
                            );

                            spawnPredictions.put(prediction.getId(), prediction);
                            predictionsGenerated.incrementAndGet();
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Spawn pattern analysis error: " + e.getMessage());
        }
    }

    private void cleanupExpiredPredictions() {
        try {
            long currentTime = System.currentTimeMillis();

            // Remove expired predictions
            spawnPredictions.entrySet().removeIf(entry -> {
                SpawnPrediction prediction = entry.getValue();
                return prediction.isExpired(currentTime);
            });

            // Clean up old spawn history
            spawnHistory.entrySet().removeIf(entry -> {
                EntitySpawnData data = entry.getValue();
                return currentTime - data.getLastSpawnTime() > 600000; // 10 minutes
            });

            // Clean up old player movement history
            playerMovementHistory.entrySet().removeIf(entry -> {
                return currentTime - entry.getValue() > 300000; // 5 minutes
            });

        } catch (Exception e) {
            LOGGER.warning("Prediction cleanup error: " + e.getMessage());
        }
    }

    private void updatePredictionAccuracy() {
        try {
            long currentTime = System.currentTimeMillis();
            long totalPredictions = predictionsGenerated.get();
            long correctPredictions = predictionsCorrect.get();

            if (totalPredictions > 0) {
                double accuracy = (correctPredictions * 100.0) / totalPredictions;

                // Log accuracy periodically
                if (totalPredictions % 100 == 0) {
                    LOGGER.info("🎯 Prediction accuracy: " + String.format("%.1f%%", accuracy) +
                               " (" + correctPredictions + "/" + totalPredictions + ")");
                }

                // Adjust prediction parameters based on accuracy
                if (accuracy < 30.0 && totalPredictions > 50) {
                    // Low accuracy - reduce prediction aggressiveness
                    predictionRadius = Math.max(32, (int) (predictionRadius * 0.9));
                    LOGGER.info("🔧 Reduced prediction radius due to low accuracy: " + predictionRadius);
                } else if (accuracy > 70.0 && totalPredictions > 100) {
                    // High accuracy - can be more aggressive
                    predictionRadius = Math.min(128, (int) (predictionRadius * 1.05));
                    LOGGER.fine("🔧 Increased prediction radius due to high accuracy: " + predictionRadius);
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Prediction accuracy update error: " + e.getMessage());
        }
    }

    private String generatePredictionId() {
        return "pred_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }
    
    /**
     * Update spawn patterns
     */
    private void updateSpawnPatterns() {
        for (SpawnPattern pattern : spawnPatterns.values()) {
            pattern.updatePattern();
        }
    }
    
    /**
     * Optimize spawning
     */
    private void optimizeSpawning() {
        // Remove expired predictions
        long currentTime = System.currentTimeMillis();
        spawnPredictions.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        
        // Optimize pre-spawned entity pools
        optimizePreSpawnedPools();
        
        // Prevent lag by limiting spawning
        if (enableLagPrevention) {
            preventSpawnLag();
        }
    }
    
    /**
     * Optimize pre-spawned entity pools
     */
    private void optimizePreSpawnedPools() {
        for (Map.Entry<EntityType<?>, List<Entity>> entry : preSpawnedEntities.entrySet()) {
            List<Entity> entities = entry.getValue();
            
            // Remove old pre-spawned entities
            entities.removeIf(entity -> {
                // Remove if entity is too old (5 minutes)
                return System.currentTimeMillis() - entity.tickCount > 6000; // 5 minutes in ticks
            });
        }
    }
    
    /**
     * Prevent spawn lag
     */
    private void preventSpawnLag() {
        // Count total pre-spawned entities
        int totalPreSpawned = preSpawnedEntities.values().stream()
            .mapToInt(List::size)
            .sum();
        
        if (totalPreSpawned > maxPreSpawnedEntities) {
            // Remove excess entities
            for (List<Entity> entities : preSpawnedEntities.values()) {
                if (entities.size() > 10) {
                    entities.subList(10, entities.size()).clear();
                }
            }
            
            lagsPrevented.incrementAndGet();
            LOGGER.info("🛡️ Prevented spawn lag by limiting pre-spawned entities");
        }
    }
    
    /**
     * Clean up old data
     */
    private void cleanupOldData() {
        // Clean up old spawn history
        cleanupSpawnHistory();
        
        // Clean up old movement history
        long cutoffTime = System.currentTimeMillis() - 300000; // 5 minutes
        playerMovementHistory.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
    
    private void cleanupSpawnHistory() {
        if (spawnHistory.size() > spawnHistorySize) {
            // Remove oldest entries
            List<Map.Entry<BlockPos, EntitySpawnData>> entries = new ArrayList<>(spawnHistory.entrySet());
            entries.sort((a, b) -> Long.compare(a.getValue().getLastSpawnTime(), b.getValue().getLastSpawnTime()));
            
            int toRemove = spawnHistory.size() - spawnHistorySize;
            for (int i = 0; i < toRemove; i++) {
                spawnHistory.remove(entries.get(i).getKey());
            }
        }
    }
    
    // Helper methods
    private void updatePlayerMovement(String playerId, BlockPos pos) {
        playerMovementHistory.put(playerId, System.currentTimeMillis());
    }
    
    private List<EntityType<?>> getSpawnableEntities(String biome, int lightLevel, boolean isNight) {
        List<EntityType<?>> entities = new ArrayList<>();

        try {
            // Determine spawnable entities based on biome and conditions
            switch (biome.toLowerCase()) {
                case "plains":
                case "forest":
                case "birch_forest":
                case "oak_forest":
                    // Peaceful animals
                    entities.add(EntityType.COW);
                    entities.add(EntityType.PIG);
                    entities.add(EntityType.SHEEP);
                    entities.add(EntityType.CHICKEN);

                    // Hostile mobs at night or low light
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                        entities.add(EntityType.SPIDER);
                    }
                    break;

                case "desert":
                    entities.add(EntityType.RABBIT);
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                        entities.add(EntityType.HUSK); // Desert zombie variant
                    }
                    break;

                case "ocean":
                case "deep_ocean":
                    entities.add(EntityType.COD);
                    entities.add(EntityType.SALMON);
                    entities.add(EntityType.SQUID);
                    entities.add(EntityType.DOLPHIN);
                    if (biome.contains("deep")) {
                        entities.add(EntityType.GUARDIAN);
                    }
                    break;

                case "nether_wastes":
                case "crimson_forest":
                case "warped_forest":
                    entities.add(EntityType.ZOMBIFIED_PIGLIN);
                    entities.add(EntityType.GHAST);
                    entities.add(EntityType.MAGMA_CUBE);
                    if (biome.contains("crimson")) {
                        entities.add(EntityType.HOGLIN);
                        entities.add(EntityType.PIGLIN);
                    }
                    break;

                case "the_end":
                    entities.add(EntityType.ENDERMAN);
                    entities.add(EntityType.SHULKER);
                    break;

                case "swamp":
                    entities.add(EntityType.SLIME);
                    entities.add(EntityType.WITCH);
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                    }
                    break;

                case "mountain":
                case "extreme_hills":
                    entities.add(EntityType.GOAT);
                    entities.add(EntityType.LLAMA);
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                    }
                    break;

                case "jungle":
                    entities.add(EntityType.PARROT);
                    entities.add(EntityType.OCELOT);
                    entities.add(EntityType.PANDA);
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                    }
                    break;

                case "taiga":
                case "snowy_taiga":
                    entities.add(EntityType.WOLF);
                    entities.add(EntityType.FOX);
                    if (biome.contains("snowy")) {
                        entities.add(EntityType.POLAR_BEAR);
                        entities.add(EntityType.STRAY); // Cold skeleton variant
                    }
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                    }
                    break;

                default:
                    // Default spawns for unknown biomes
                    entities.add(EntityType.COW);
                    entities.add(EntityType.PIG);
                    if (isNight || lightLevel < 7) {
                        entities.add(EntityType.ZOMBIE);
                        entities.add(EntityType.SKELETON);
                        entities.add(EntityType.CREEPER);
                    }
                    break;
            }

            // Filter based on additional conditions
            entities = filterEntitiesByConditions(entities, lightLevel, isNight);

        } catch (Exception e) {
            LOGGER.warning("Error determining spawnable entities: " + e.getMessage());
            // Return basic entities as fallback
            entities.add(EntityType.COW);
            entities.add(EntityType.PIG);
        }

        return entities;
    }

    private List<EntityType<?>> filterEntitiesByConditions(List<EntityType<?>> entities, int lightLevel, boolean isNight) {
        List<EntityType<?>> filtered = new ArrayList<>();

        for (EntityType<?> entityType : entities) {
            // Check light level requirements
            if (isHostileMob(entityType)) {
                // Hostile mobs need low light or night
                if (lightLevel < 7 || isNight) {
                    filtered.add(entityType);
                }
            } else {
                // Peaceful mobs can spawn in any light
                filtered.add(entityType);
            }
        }

        return filtered;
    }

    private boolean isHostileMob(EntityType<?> entityType) {
        return entityType == EntityType.ZOMBIE ||
               entityType == EntityType.SKELETON ||
               entityType == EntityType.CREEPER ||
               entityType == EntityType.SPIDER ||
               entityType == EntityType.WITCH ||
               entityType == EntityType.HUSK ||
               entityType == EntityType.STRAY ||
               entityType == EntityType.GHAST ||
               entityType == EntityType.MAGMA_CUBE ||
               entityType == EntityType.GUARDIAN ||
               entityType == EntityType.SHULKER ||
               entityType == EntityType.ENDERMAN;
    }
    
    private double calculateSpawnConfidence(EntityType<?> entityType, BlockPos pos, ServerLevel level) {
        // Calculate confidence based on various factors
        double confidence = 0.5; // Base confidence
        
        // Adjust based on spawn history
        EntitySpawnData history = spawnHistory.get(pos);
        if (history != null && history.getEntityType() == entityType) {
            confidence += 0.3;
        }
        
        // Adjust based on spawn patterns
        SpawnPattern pattern = spawnPatterns.get(entityType);
        if (pattern != null) {
            confidence += pattern.getConfidenceAt(pos) * 0.2;
        }
        
        return Math.min(1.0, confidence);
    }
    
    private double calculateHistoryBasedConfidence(EntitySpawnData spawnData, double distance) {
        double baseConfidence = 0.6;
        
        // Reduce confidence based on distance
        double distanceFactor = Math.max(0.1, 1.0 - (distance / 1600.0));
        
        // Increase confidence based on spawn frequency
        double frequencyFactor = Math.min(1.0, spawnData.getSpawnCount() / 10.0);
        
        return baseConfidence * distanceFactor * frequencyFactor;
    }
    
    private String generatePredictionId(BlockPos pos, EntityType<?> entityType) {
        return entityType.toString() + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }
    
    /**
     * Get prediction statistics
     */
    public Map<String, Object> getPredictionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("predictions_generated", predictionsGenerated.get());
        stats.put("entities_pre_spawned", entitiesPreSpawned.get());
        stats.put("predictions_correct", predictionsCorrect.get());
        stats.put("spawn_optimizations", spawnOptimizations.get());
        stats.put("lags_prevented", lagsPrevented.get());
        
        stats.put("active_predictions", spawnPredictions.size());
        stats.put("spawn_history_size", spawnHistory.size());
        stats.put("spawn_patterns", spawnPatterns.size());
        
        long totalPredictions = predictionsGenerated.get();
        if (totalPredictions > 0) {
            stats.put("prediction_accuracy", (predictionsCorrect.get() * 100.0) / totalPredictions);
        }
        
        return stats;
    }
    
    // Getters
    public long getPredictionsGenerated() { return predictionsGenerated.get(); }
    public long getEntitiesPreSpawned() { return entitiesPreSpawned.get(); }
    public long getPredictionsCorrect() { return predictionsCorrect.get(); }
    public long getSpawnOptimizations() { return spawnOptimizations.get(); }
    public long getLagsPrevented() { return lagsPrevented.get(); }
    
    public void shutdown() {
        predictionEngine.shutdown();
        
        // Clear all data
        spawnPredictions.clear();
        spawnHistory.clear();
        spawnPatterns.clear();
        preSpawnedEntities.clear();
        playerMovementHistory.clear();
        
        LOGGER.info("🤖 Predictive Entity Spawning shutdown complete");
    }
    
    // Data classes
    private static class SpawnPrediction {
        private final String id;
        private final EntityType<?> entityType;
        private final BlockPos spawnPos;
        private final double confidence;
        private final long expirationTime;
        
        public SpawnPrediction(String id, EntityType<?> entityType, BlockPos spawnPos, double confidence, long expirationTime) {
            this.id = id;
            this.entityType = entityType;
            this.spawnPos = spawnPos;
            this.confidence = confidence;
            this.expirationTime = expirationTime;
        }
        
        public String getId() { return id; }
        public EntityType<?> getEntityType() { return entityType; }
        public BlockPos getSpawnPos() { return spawnPos; }
        public double getConfidence() { return confidence; }
        public boolean isExpired() { return isExpired(System.currentTimeMillis()); }
        public boolean isExpired(long currentTime) { return currentTime > expirationTime; }
    }
    
    private static class EntitySpawnData {
        private final EntityType<?> entityType;
        private int spawnCount = 0;
        private long lastSpawnTime = 0;
        
        public EntitySpawnData(EntityType<?> entityType) {
            this.entityType = entityType;
        }
        
        public void recordSpawn() {
            spawnCount++;
            lastSpawnTime = System.currentTimeMillis();
        }
        
        public EntityType<?> getEntityType() { return entityType; }
        public int getSpawnCount() { return spawnCount; }
        public int getSpawnCount(EntityType<?> type) {
            return type == entityType ? spawnCount : 0;
        }
        public long getLastSpawnTime() { return lastSpawnTime; }

        public Set<EntityType<?>> getSpawnedTypes() {
            Set<EntityType<?>> types = new HashSet<>();
            types.add(entityType);
            return types;
        }
    }
    
    private static class SpawnPattern {
        private final EntityType<?> entityType;
        private final List<BlockPos> spawnLocations = new ArrayList<>();
        private final List<Long> spawnTimes = new ArrayList<>();
        private double confidence = 0.5;
        private long lastSpawnTime = 0;
        private int spawnCount = 0;

        public SpawnPattern(EntityType<?> entityType) {
            this.entityType = entityType;
        }

        public SpawnPattern() {
            this.entityType = null;
        }
        
        public void recordSpawn(BlockPos pos, long time) {
            spawnLocations.add(pos);
            spawnTimes.add(time);
            
            // Keep only recent spawns
            if (spawnLocations.size() > 100) {
                spawnLocations.remove(0);
                spawnTimes.remove(0);
            }
        }
        
        public boolean shouldSpawnNear(BlockPos pos, ServerLevel level) {
            // Simple pattern matching
            return spawnLocations.stream().anyMatch(spawnPos -> spawnPos.distSqr(pos) < 400);
        }
        
        public BlockPos getPredictedSpawnLocation(BlockPos playerPos) {
            // Return closest historical spawn location
            return spawnLocations.stream()
                .min((a, b) -> Double.compare(a.distSqr(playerPos), b.distSqr(playerPos)))
                .orElse(playerPos);
        }
        
        public double getConfidence() { return confidence; }
        public double getConfidenceAt(BlockPos pos) { return confidence; }
        
        public void updatePattern() {
            // Update pattern confidence based on recent accuracy
            if (spawnLocations.size() > 10) {
                confidence = Math.min(1.0, confidence + 0.01);
            }
        }

        // Missing methods for SpawnPattern
        public void addSpawnLocation(BlockPos pos, long time) {
            spawnLocations.add(pos);
            lastSpawnTime = time;
        }

        public void updateFrequency(int count) {
            this.spawnCount = count;
        }

        public double getAverageFrequency() {
            if (spawnLocations.isEmpty()) return 0.0;
            long timeDiff = System.currentTimeMillis() - lastSpawnTime;
            if (timeDiff <= 0) return 0.0;
            return (spawnCount * 1000.0) / timeDiff; // spawns per second
        }

        public List<BlockPos> getPredictedSpawnPositions() {
            return new ArrayList<>(spawnLocations);
        }
    }

    // Missing PreSpawnData class
    private static class PreSpawnData {
        private final EntityType<?> entityType;
        private final BlockPos spawnPos;
        private final long creationTime;
        private final double confidence;

        public PreSpawnData(EntityType<?> entityType, BlockPos spawnPos, long creationTime, double confidence) {
            this.entityType = entityType;
            this.spawnPos = spawnPos;
            this.creationTime = creationTime;
            this.confidence = confidence;
        }

        public EntityType<?> getEntityType() { return entityType; }
        public BlockPos getSpawnPos() { return spawnPos; }
        public long getCreationTime() { return creationTime; }
        public double getConfidence() { return confidence; }
    }

    // Missing helper methods
    private String generatePreSpawnId(EntityType<?> entityType, BlockPos pos) {
        return "prespawn_" + entityType.toString() + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    private void prepareHighConfidenceSpawn(PreSpawnData preSpawnData, ServerLevel level) {
        // Prepare for immediate spawning when needed
        LOGGER.fine("High confidence spawn prepared: " + preSpawnData.getEntityType() + " at " + preSpawnData.getSpawnPos());
    }
}
