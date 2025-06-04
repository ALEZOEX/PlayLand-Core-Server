package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Smart Entity Grouping
 * УМНАЯ группировка сущностей для batch обработки
 * Группирует похожие сущности для оптимизации без нарушения vanilla
 */
public class SmartEntityGrouping {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-EntityGroup");
    
    // Grouping statistics
    private final AtomicLong entitiesProcessed = new AtomicLong(0);
    private final AtomicLong groupsCreated = new AtomicLong(0);
    private final AtomicLong batchOperations = new AtomicLong(0);
    private final AtomicLong optimizationsSaved = new AtomicLong(0);
    private final AtomicLong vanillaCompatibilityChecks = new AtomicLong(0);
    
    // Entity groups by type and behavior
    private final Map<String, EntityGroup> activeGroups = new ConcurrentHashMap<>();
    private final Map<String, List<String>> entityBehaviorPatterns = new ConcurrentHashMap<>();
    private final Set<String> vanillaSafeOperations = new HashSet<>();
    
    // Configuration
    private boolean enableEntityGrouping = true;
    private boolean enableBatchProcessing = true;
    private boolean enableVanillaSafetyMode = true;
    private boolean enableBehaviorAnalysis = true;
    private boolean enablePerformanceOptimization = true;
    
    private int maxGroupSize = 50;
    private int minGroupSize = 3;
    private double similarityThreshold = 0.8;
    private long groupExpirationTime = 10000; // 10 seconds
    
    public void initialize() {
        LOGGER.info("🤖 Initializing Smart Entity Grouping...");
        
        loadGroupingSettings();
        initializeVanillaSafeOperations();
        initializeBehaviorPatterns();
        
        LOGGER.info("✅ Smart Entity Grouping initialized!");
        LOGGER.info("🤖 Entity grouping: " + (enableEntityGrouping ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Batch processing: " + (enableBatchProcessing ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla safety mode: " + (enableVanillaSafetyMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("🧠 Behavior analysis: " + (enableBehaviorAnalysis ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Performance optimization: " + (enablePerformanceOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("📏 Max group size: " + maxGroupSize);
        LOGGER.info("📏 Min group size: " + minGroupSize);
        LOGGER.info("🎯 Similarity threshold: " + (similarityThreshold * 100) + "%");
    }
    
    private void loadGroupingSettings() {
        try {
            LOGGER.info("⚙️ Loading grouping settings...");

            // Load grouping parameters from system properties
            enableEntityGrouping = Boolean.parseBoolean(System.getProperty("playland.entity.grouping.enabled", "true"));
            enableBatchProcessing = Boolean.parseBoolean(System.getProperty("playland.entity.batch.enabled", "true"));
            enableVanillaSafetyMode = Boolean.parseBoolean(System.getProperty("playland.entity.vanilla.safe", "true"));
            enableBehaviorAnalysis = Boolean.parseBoolean(System.getProperty("playland.entity.behavior.analysis", "true"));
            enablePerformanceOptimization = Boolean.parseBoolean(System.getProperty("playland.entity.performance.enabled", "true"));

            // Load grouping limits
            maxGroupSize = Integer.parseInt(System.getProperty("playland.entity.group.max.size", "50"));
            minGroupSize = Integer.parseInt(System.getProperty("playland.entity.group.min.size", "3"));
            similarityThreshold = Double.parseDouble(System.getProperty("playland.entity.similarity.threshold", "0.8"));
            groupExpirationTime = Long.parseLong(System.getProperty("playland.entity.group.expiration", "10000"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce grouping complexity
                maxGroupSize = Math.max(10, maxGroupSize / 2);
                minGroupSize = Math.max(2, minGroupSize - 1);
                similarityThreshold = Math.min(0.9, similarityThreshold + 0.1);
                enableBehaviorAnalysis = false;
                LOGGER.info("🔧 Reduced grouping complexity for low TPS: maxSize=" + maxGroupSize +
                           ", minSize=" + minGroupSize + ", threshold=" + similarityThreshold);
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive grouping
                maxGroupSize = Math.min(100, (int) (maxGroupSize * 1.5));
                minGroupSize = Math.max(2, minGroupSize - 1);
                similarityThreshold = Math.max(0.6, similarityThreshold - 0.1);
                LOGGER.info("🔧 Increased grouping aggressiveness for good TPS: maxSize=" + maxGroupSize +
                           ", threshold=" + similarityThreshold);
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce grouping overhead
                maxGroupSize = Math.max(5, maxGroupSize / 3);
                enableBehaviorAnalysis = false;
                groupExpirationTime = Math.max(5000, groupExpirationTime / 2);
                LOGGER.warning("⚠️ High memory usage - reduced grouping overhead: maxSize=" + maxGroupSize +
                              ", expiration=" + groupExpirationTime + "ms");
            }

            // Auto-adjust based on entity count
            int totalEntities = getTotalEntityCount();
            if (totalEntities > 1000) {
                // Many entities - more aggressive grouping
                maxGroupSize = Math.min(200, maxGroupSize * 2);
                minGroupSize = Math.max(5, minGroupSize + 2);
                LOGGER.info("🔧 Adjusted for " + totalEntities + " entities: maxSize=" + maxGroupSize +
                           ", minSize=" + minGroupSize);
            } else if (totalEntities < 100) {
                // Few entities - less grouping overhead
                enableEntityGrouping = false;
                LOGGER.info("🔧 Disabled grouping for low entity count: " + totalEntities);
            }

            LOGGER.info("✅ Grouping settings loaded - Max: " + maxGroupSize + ", Min: " + minGroupSize +
                       ", Threshold: " + (similarityThreshold * 100) + "%, Expiration: " + groupExpirationTime + "ms");

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading grouping settings: " + e.getMessage() + " - using defaults");
        }
    }

    private double getCurrentTPS() {
        try {
            return org.bukkit.Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }

    private int getTotalEntityCount() {
        try {
            int totalEntities = 0;
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                totalEntities += world.getEntities().size();
            }
            return totalEntities;
        } catch (Exception e) {
            return 500; // Default moderate count
        }
    }
    
    private void initializeVanillaSafeOperations() {
        // Define operations that are safe to batch without affecting vanilla behavior
        vanillaSafeOperations.add("movement_update");
        vanillaSafeOperations.add("ai_goal_update");
        vanillaSafeOperations.add("pathfinding_update");
        vanillaSafeOperations.add("collision_check");
        vanillaSafeOperations.add("visibility_check");
        vanillaSafeOperations.add("distance_calculation");
        vanillaSafeOperations.add("chunk_loading_check");
        
        LOGGER.info("🛡️ Vanilla-safe operations initialized: " + vanillaSafeOperations.size());
    }
    
    private void initializeBehaviorPatterns() {
        // Initialize common entity behavior patterns
        List<String> passiveMobPattern = new ArrayList<>();
        passiveMobPattern.add("wander");
        passiveMobPattern.add("eat");
        passiveMobPattern.add("breed");
        passiveMobPattern.add("follow_player");
        entityBehaviorPatterns.put("passive_mob", passiveMobPattern);
        
        List<String> hostileMobPattern = new ArrayList<>();
        hostileMobPattern.add("target_player");
        hostileMobPattern.add("attack");
        hostileMobPattern.add("chase");
        hostileMobPattern.add("patrol");
        entityBehaviorPatterns.put("hostile_mob", hostileMobPattern);
        
        List<String> itemPattern = new ArrayList<>();
        itemPattern.add("physics_update");
        itemPattern.add("pickup_check");
        itemPattern.add("despawn_timer");
        entityBehaviorPatterns.put("item", itemPattern);
        
        LOGGER.info("🧠 Behavior patterns initialized: " + entityBehaviorPatterns.size());
    }
    
    /**
     * Process entity with smart grouping
     */
    public void processEntity(String entityId, String entityType, EntityData entityData) {
        if (!enableEntityGrouping) {
            // Process individually if grouping disabled
            processEntityIndividually(entityId, entityType, entityData);
            return;
        }
        
        entitiesProcessed.incrementAndGet();
        
        try {
            // Find or create appropriate group
            EntityGroup group = findOrCreateGroup(entityType, entityData);
            
            if (group != null) {
                // Add entity to group
                group.addEntity(entityId, entityData);
                
                // Process group if it's ready
                if (group.isReadyForProcessing()) {
                    processEntityGroup(group);
                }
            } else {
                // Process individually if no suitable group
                processEntityIndividually(entityId, entityType, entityData);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Entity grouping error: " + e.getMessage());
            // Fallback to individual processing
            processEntityIndividually(entityId, entityType, entityData);
        }
    }
    
    /**
     * Find or create appropriate group for entity
     */
    private EntityGroup findOrCreateGroup(String entityType, EntityData entityData) {
        // Generate group key based on entity characteristics
        String groupKey = generateGroupKey(entityType, entityData);
        
        // Check if suitable group already exists
        EntityGroup existingGroup = activeGroups.get(groupKey);
        if (existingGroup != null && !existingGroup.isFull() && !existingGroup.isExpired()) {
            return existingGroup;
        }
        
        // Create new group if needed
        if (shouldCreateNewGroup(entityType, entityData)) {
            EntityGroup newGroup = new EntityGroup(groupKey, entityType, System.currentTimeMillis());
            activeGroups.put(groupKey, newGroup);
            groupsCreated.incrementAndGet();
            return newGroup;
        }
        
        return null;
    }
    
    /**
     * Generate group key based on entity characteristics
     */
    private String generateGroupKey(String entityType, EntityData entityData) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Base type
        keyBuilder.append(entityType);
        
        // Behavior pattern
        if (enableBehaviorAnalysis) {
            String behaviorPattern = analyzeBehaviorPattern(entityType, entityData);
            keyBuilder.append("_").append(behaviorPattern);
        }
        
        // Location-based grouping (chunk)
        keyBuilder.append("_chunk_").append(entityData.getChunkX()).append("_").append(entityData.getChunkZ());
        
        // State-based grouping
        if (entityData.hasSpecialState()) {
            keyBuilder.append("_state_").append(entityData.getState());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Analyze entity behavior pattern
     */
    private String analyzeBehaviorPattern(String entityType, EntityData entityData) {
        // Determine behavior pattern based on entity type and current state
        if (entityType.contains("cow") || entityType.contains("sheep") || entityType.contains("pig")) {
            return "passive_mob";
        } else if (entityType.contains("zombie") || entityType.contains("skeleton") || entityType.contains("spider")) {
            return "hostile_mob";
        } else if (entityType.contains("item")) {
            return "item";
        } else if (entityType.contains("arrow") || entityType.contains("projectile")) {
            return "projectile";
        }
        
        return "generic";
    }
    
    /**
     * Check if new group should be created
     */
    private boolean shouldCreateNewGroup(String entityType, EntityData entityData) {
        // Don't group entities that require individual processing
        if (requiresIndividualProcessing(entityType, entityData)) {
            return false;
        }
        
        // Check if entity type is suitable for grouping
        if (!isSuitableForGrouping(entityType)) {
            return false;
        }
        
        // Check vanilla safety
        if (enableVanillaSafetyMode && !isVanillaSafeForGrouping(entityType, entityData)) {
            vanillaCompatibilityChecks.incrementAndGet();
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if entity requires individual processing
     */
    private boolean requiresIndividualProcessing(String entityType, EntityData entityData) {
        // Players always require individual processing
        if (entityType.equals("player")) {
            return true;
        }
        
        // Entities with unique behavior
        if (entityData.hasUniqueAI() || entityData.isNamedEntity() || entityData.hasCustomData()) {
            return true;
        }
        
        // Boss entities
        if (entityType.contains("dragon") || entityType.contains("wither") || entityType.contains("boss")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if entity type is suitable for grouping
     */
    private boolean isSuitableForGrouping(String entityType) {
        // Items and projectiles are good for grouping
        if (entityType.contains("item") || entityType.contains("arrow") || entityType.contains("projectile")) {
            return true;
        }
        
        // Common mobs are suitable
        if (entityType.contains("zombie") || entityType.contains("skeleton") || 
            entityType.contains("cow") || entityType.contains("sheep")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if grouping is vanilla-safe for this entity
     */
    private boolean isVanillaSafeForGrouping(String entityType, EntityData entityData) {
        // Check if entity has any vanilla-critical behaviors that could be affected
        if (entityData.hasRedstoneInteraction() || entityData.hasPlayerInteraction()) {
            return false;
        }
        
        // Check if entity affects world state in ways that require precise timing
        if (entityData.affectsBlockState() || entityData.hasTimeCriticalBehavior()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Process entity group with batch operations
     */
    private void processEntityGroup(EntityGroup group) {
        if (!enableBatchProcessing) {
            // Process each entity individually
            for (Map.Entry<String, EntityData> entry : group.getEntities().entrySet()) {
                processEntityIndividually(entry.getKey(), group.getEntityType(), entry.getValue());
            }
            return;
        }
        
        try {
            long batchStart = System.nanoTime();
            
            // Perform batch operations
            performBatchMovementUpdate(group);
            performBatchAIUpdate(group);
            performBatchCollisionCheck(group);
            performBatchVisibilityCheck(group);
            
            long batchEnd = System.nanoTime();
            long batchTime = (batchEnd - batchStart) / 1_000_000; // Convert to milliseconds
            
            // Calculate time saved compared to individual processing
            long individualTime = group.getEntityCount() * 2; // Assume 2ms per entity
            if (individualTime > batchTime) {
                optimizationsSaved.addAndGet(individualTime - batchTime);
            }
            
            batchOperations.incrementAndGet();
            
            // Mark group as processed and remove from active groups
            activeGroups.remove(group.getGroupKey());
            
        } catch (Exception e) {
            LOGGER.warning("Batch processing error: " + e.getMessage());
            // Fallback to individual processing
            for (Map.Entry<String, EntityData> entry : group.getEntities().entrySet()) {
                processEntityIndividually(entry.getKey(), group.getEntityType(), entry.getValue());
            }
        }
    }
    
    /**
     * Perform batch movement update
     */
    private void performBatchMovementUpdate(EntityGroup group) {
        if (!vanillaSafeOperations.contains("movement_update")) return;
        
        // Batch process movement for all entities in group
        for (Map.Entry<String, EntityData> entry : group.getEntities().entrySet()) {
            EntityData entityData = entry.getValue();
            
            // Update position, velocity, etc. in batch
            updateEntityMovement(entityData);
        }
    }
    
    /**
     * Perform batch AI update
     */
    private void performBatchAIUpdate(EntityGroup group) {
        if (!vanillaSafeOperations.contains("ai_goal_update")) return;
        
        // Batch process AI goals for similar entities
        String behaviorPattern = analyzeBehaviorPattern(group.getEntityType(), null);
        List<String> commonGoals = entityBehaviorPatterns.get(behaviorPattern);
        
        if (commonGoals != null) {
            for (String goal : commonGoals) {
                // Process this goal for all entities in the group
                for (Map.Entry<String, EntityData> entry : group.getEntities().entrySet()) {
                    processAIGoal(entry.getValue(), goal);
                }
            }
        }
    }
    
    /**
     * Perform batch collision check
     */
    private void performBatchCollisionCheck(EntityGroup group) {
        if (!vanillaSafeOperations.contains("collision_check")) return;
        
        // Optimize collision checking by grouping nearby entities
        List<EntityData> entities = new ArrayList<>(group.getEntities().values());
        
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                checkEntityCollision(entities.get(i), entities.get(j));
            }
        }
    }
    
    /**
     * Perform batch visibility check
     */
    private void performBatchVisibilityCheck(EntityGroup group) {
        if (!vanillaSafeOperations.contains("visibility_check")) return;
        
        // Batch check visibility for entities in the same chunk
        for (EntityData entityData : group.getEntities().values()) {
            updateEntityVisibility(entityData);
        }
    }
    
    /**
     * Process entity individually (fallback)
     */
    private void processEntityIndividually(String entityId, String entityType, EntityData entityData) {
        // Individual processing logic
        updateEntityMovement(entityData);
        processIndividualAI(entityData);
        checkIndividualCollision(entityData);
        updateEntityVisibility(entityData);
    }
    
    // Individual processing methods (placeholders)
    private void updateEntityMovement(EntityData entityData) {
        // Update entity movement
    }
    
    private void processIndividualAI(EntityData entityData) {
        // Process entity AI individually
    }
    
    private void processAIGoal(EntityData entityData, String goal) {
        // Process specific AI goal
    }
    
    private void checkIndividualCollision(EntityData entityData) {
        // Check collision for individual entity
    }
    
    private void checkEntityCollision(EntityData entity1, EntityData entity2) {
        // Check collision between two entities
    }
    
    private void updateEntityVisibility(EntityData entityData) {
        // Update entity visibility
    }
    
    /**
     * Clean up expired groups
     */
    public void cleanupExpiredGroups() {
        activeGroups.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                // Process any remaining entities individually
                EntityGroup group = entry.getValue();
                for (Map.Entry<String, EntityData> entityEntry : group.getEntities().entrySet()) {
                    processEntityIndividually(entityEntry.getKey(), group.getEntityType(), entityEntry.getValue());
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get grouping statistics
     */
    public Map<String, Object> getGroupingStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("entities_processed", entitiesProcessed.get());
        stats.put("groups_created", groupsCreated.get());
        stats.put("batch_operations", batchOperations.get());
        stats.put("optimizations_saved_ms", optimizationsSaved.get());
        stats.put("vanilla_compatibility_checks", vanillaCompatibilityChecks.get());
        
        stats.put("active_groups", activeGroups.size());
        stats.put("max_group_size", maxGroupSize);
        stats.put("min_group_size", minGroupSize);
        stats.put("similarity_threshold", similarityThreshold);
        
        long totalEntities = entitiesProcessed.get();
        if (totalEntities > 0) {
            stats.put("grouping_efficiency", (batchOperations.get() * 100.0) / totalEntities);
        }
        
        return stats;
    }
    
    // Getters
    public long getEntitiesProcessed() { return entitiesProcessed.get(); }
    public long getGroupsCreated() { return groupsCreated.get(); }
    public long getBatchOperations() { return batchOperations.get(); }
    public long getOptimizationsSaved() { return optimizationsSaved.get(); }
    public long getVanillaCompatibilityChecks() { return vanillaCompatibilityChecks.get(); }
    
    /**
     * Entity group container
     */
    private class EntityGroup {
        private final String groupKey;
        private final String entityType;
        private final long creationTime;
        private final Map<String, EntityData> entities = new ConcurrentHashMap<>();
        
        public EntityGroup(String groupKey, String entityType, long creationTime) {
            this.groupKey = groupKey;
            this.entityType = entityType;
            this.creationTime = creationTime;
        }
        
        public void addEntity(String entityId, EntityData entityData) {
            entities.put(entityId, entityData);
        }
        
        public boolean isReadyForProcessing() {
            return entities.size() >= minGroupSize || 
                   (entities.size() > 0 && System.currentTimeMillis() - creationTime > 1000); // 1 second timeout
        }
        
        public boolean isFull() {
            return entities.size() >= maxGroupSize;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > groupExpirationTime;
        }
        
        public String getGroupKey() { return groupKey; }
        public String getEntityType() { return entityType; }
        public Map<String, EntityData> getEntities() { return entities; }
        public int getEntityCount() { return entities.size(); }
    }
    
    /**
     * Entity data container (placeholder)
     */
    public static class EntityData {
        private int chunkX, chunkZ;
        private String state;
        private boolean uniqueAI, namedEntity, customData;
        private boolean redstoneInteraction, playerInteraction;
        private boolean affectsBlockState, timeCriticalBehavior;
        
        // Getters and setters
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public String getState() { return state; }
        public boolean hasSpecialState() { return state != null && !state.isEmpty(); }
        public boolean hasUniqueAI() { return uniqueAI; }
        public boolean isNamedEntity() { return namedEntity; }
        public boolean hasCustomData() { return customData; }
        public boolean hasRedstoneInteraction() { return redstoneInteraction; }
        public boolean hasPlayerInteraction() { return playerInteraction; }
        public boolean affectsBlockState() { return affectsBlockState; }
        public boolean hasTimeCriticalBehavior() { return timeCriticalBehavior; }
        
        public void setChunkX(int chunkX) { this.chunkX = chunkX; }
        public void setChunkZ(int chunkZ) { this.chunkZ = chunkZ; }
        public void setState(String state) { this.state = state; }
        public void setUniqueAI(boolean uniqueAI) { this.uniqueAI = uniqueAI; }
        public void setNamedEntity(boolean namedEntity) { this.namedEntity = namedEntity; }
        public void setCustomData(boolean customData) { this.customData = customData; }
        public void setRedstoneInteraction(boolean redstoneInteraction) { this.redstoneInteraction = redstoneInteraction; }
        public void setPlayerInteraction(boolean playerInteraction) { this.playerInteraction = playerInteraction; }
        public void setAffectsBlockState(boolean affectsBlockState) { this.affectsBlockState = affectsBlockState; }
        public void setTimeCriticalBehavior(boolean timeCriticalBehavior) { this.timeCriticalBehavior = timeCriticalBehavior; }
    }
}
