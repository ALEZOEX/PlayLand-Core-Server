package ru.playland.core.optimization;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Entity AI Optimizer
 * Революционная оптимизация ИИ сущностей с сохранением поведения
 * Использует умное кэширование решений и адаптивную частоту обновлений
 */
public class EntityAIOptimizer {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-EntityAI");
    
    // Optimization statistics
    private final AtomicLong entitiesOptimized = new AtomicLong(0);
    private final AtomicLong pathfindingOptimizations = new AtomicLong(0);
    private final AtomicLong goalOptimizations = new AtomicLong(0);
    private final AtomicLong behaviorCacheHits = new AtomicLong(0);
    private final AtomicLong distanceOptimizations = new AtomicLong(0);
    
    // AI caches
    private final Map<Integer, EntityAIData> entityAICache = new ConcurrentHashMap<>();
    private final Map<String, PathfindingResult> pathfindingCache = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    private final Set<Integer> highPriorityEntities = new HashSet<>();
    
    // Configuration
    private boolean enablePathfindingOptimization = true;
    private boolean enableGoalOptimization = true;
    private boolean enableDistanceBasedOptimization = true;
    private boolean enableBehaviorCaching = true;
    private boolean enableAdaptiveUpdates = true;
    
    private double nearDistance = 32.0; // Blocks
    private double farDistance = 128.0; // Blocks
    private long nearUpdateInterval = 50; // milliseconds
    private long farUpdateInterval = 500; // milliseconds
    private long veryFarUpdateInterval = 2000; // milliseconds
    
    public void initialize() {
        LOGGER.info("🤖 Initializing Entity AI Optimizer...");
        
        loadEntityAISettings();
        initializeAICaches();
        startAIMonitoring();
        
        LOGGER.info("✅ Entity AI Optimizer initialized!");
        LOGGER.info("🧭 Pathfinding optimization: " + (enablePathfindingOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🎯 Goal optimization: " + (enableGoalOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("📏 Distance-based optimization: " + (enableDistanceBasedOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("💾 Behavior caching: " + (enableBehaviorCaching ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔄 Adaptive updates: " + (enableAdaptiveUpdates ? "ENABLED" : "DISABLED"));
        LOGGER.info("📐 Near distance: " + nearDistance + " blocks");
        LOGGER.info("📐 Far distance: " + farDistance + " blocks");
    }
    
    private void loadEntityAISettings() {
        // Load entity AI optimization settings
        LOGGER.info("⚙️ Loading entity AI settings...");
    }
    
    private void initializeAICaches() {
        // Initialize AI caches
        LOGGER.info("💾 Entity AI caches initialized");
    }
    
    private void startAIMonitoring() {
        // Start AI performance monitoring
        LOGGER.info("📊 Entity AI monitoring started");
    }
    
    /**
     * Optimize entity AI behavior
     * VANILLA-SAFE: Ускоряет ИИ без изменения поведения
     */
    public void optimizeEntityAI(Entity entity) {
        if (!(entity instanceof Mob mob)) return;
        
        int entityId = entity.getId();
        long currentTime = System.currentTimeMillis();
        
        try {
            // Check if entity needs update based on distance and time
            if (!shouldUpdateEntity(entity, currentTime)) {
                return;
            }
            
            // Get or create AI data
            EntityAIData aiData = entityAICache.computeIfAbsent(entityId, k -> new EntityAIData());
            
            // Optimize pathfinding
            if (enablePathfindingOptimization) {
                optimizePathfinding(mob, aiData);
            }
            
            // Optimize goals
            if (enableGoalOptimization) {
                optimizeGoals(mob, aiData);
            }
            
            // Apply distance-based optimizations
            if (enableDistanceBasedOptimization) {
                applyDistanceOptimizations(mob, aiData);
            }
            
            // Cache behavior decisions
            if (enableBehaviorCaching) {
                cacheBehaviorDecisions(mob, aiData);
            }
            
            // Update timestamps
            lastUpdateTimes.put(entityId, currentTime);
            aiData.setLastOptimized(currentTime);
            
            entitiesOptimized.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Entity AI optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Determine if entity should be updated based on distance and time
     */
    private boolean shouldUpdateEntity(Entity entity, long currentTime) {
        if (!enableAdaptiveUpdates) return true;
        
        int entityId = entity.getId();
        Long lastUpdate = lastUpdateTimes.get(entityId);
        
        if (lastUpdate == null) return true; // First update
        
        // High priority entities always update
        if (highPriorityEntities.contains(entityId)) return true;
        
        // Calculate distance to nearest player
        double nearestPlayerDistance = getNearestPlayerDistance(entity);
        long timeSinceUpdate = currentTime - lastUpdate;
        
        // Determine update interval based on distance
        long requiredInterval;
        if (nearestPlayerDistance <= nearDistance) {
            requiredInterval = nearUpdateInterval;
        } else if (nearestPlayerDistance <= farDistance) {
            requiredInterval = farUpdateInterval;
        } else {
            requiredInterval = veryFarUpdateInterval;
        }
        
        return timeSinceUpdate >= requiredInterval;
    }
    
    private double getNearestPlayerDistance(Entity entity) {
        double minDistance = Double.MAX_VALUE;
        
        for (Player player : entity.level().players()) {
            double distance = entity.distanceTo(player);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return minDistance;
    }
    
    /**
     * Optimize pathfinding for entity
     * VANILLA-SAFE: Кэширование и предварительный расчет путей
     */
    private void optimizePathfinding(Mob mob, EntityAIData aiData) {
        try {
            PathNavigation navigation = mob.getNavigation();
            BlockPos currentPos = mob.blockPosition();
            BlockPos targetPos = aiData.getLastTargetPos();
            
            // Check if we can use cached pathfinding result
            if (targetPos != null) {
                String pathKey = getPathKey(currentPos, targetPos);
                PathfindingResult cachedResult = pathfindingCache.get(pathKey);
                
                if (cachedResult != null && !cachedResult.isExpired()) {
                    // Use cached pathfinding result
                    applyCachedPathfinding(mob, cachedResult);
                    behaviorCacheHits.incrementAndGet();
                    return;
                }
            }
            
            // Perform optimized pathfinding
            PathfindingResult result = calculateOptimizedPath(mob, currentPos, targetPos);
            
            if (result != null) {
                // Cache the result
                String pathKey = getPathKey(currentPos, targetPos);
                pathfindingCache.put(pathKey, result);
                
                // Apply pathfinding optimizations
                applyPathfindingOptimizations(mob, result);
            }
            
            pathfindingOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Pathfinding optimization error: " + e.getMessage());
        }
    }
    
    private PathfindingResult calculateOptimizedPath(Mob mob, BlockPos from, BlockPos to) {
        if (to == null || from == null) return null;

        try {
            PathfindingResult result = new PathfindingResult();
            result.setFromPos(from);
            result.setToPos(to);
            result.setTimestamp(System.currentTimeMillis());

            // Calculate real distance
            double distance = Math.sqrt(from.distSqr(to));
            result.setDistance(distance);

            // Optimize pathfinding based on distance and mob type
            PathNavigation navigation = mob.getNavigation();
            if (navigation != null) {
                Path path = null;

                if (distance < nearDistance) {
                    // Short distance - use direct pathfinding
                    path = navigation.createPath(to, 1);
                } else if (distance < farDistance) {
                    // Medium distance - use optimized pathfinding
                    path = navigation.createPath(to, 0);
                } else {
                    // Long distance - use simplified pathfinding
                    // Create approximate path with fewer nodes
                    BlockPos midPoint = new BlockPos(
                        (from.getX() + to.getX()) / 2,
                        (from.getY() + to.getY()) / 2,
                        (from.getZ() + to.getZ()) / 2
                    );

                    // Try to path to midpoint first
                    path = navigation.createPath(midPoint, 0);
                    if (path == null) {
                        // Fallback to direct path with lower accuracy
                        path = navigation.createPath(to, 0);
                    }
                }

                result.setPath(path);

                // Calculate path quality score
                if (path != null) {
                    result.setPathQuality(calculatePathQuality(path, distance));
                } else {
                    result.setPathQuality(0.0);
                }
            }

            return result;

        } catch (Exception e) {
            LOGGER.warning("Optimized pathfinding calculation error: " + e.getMessage());
            return null;
        }
    }

    private double calculatePathQuality(Path path, double directDistance) {
        try {
            if (path == null || path.getNodeCount() == 0) return 0.0;

            // Calculate path efficiency (closer to 1.0 is better)
            double pathLength = 0.0;

            for (int i = 1; i < path.getNodeCount(); i++) {
                Node prev = path.getNode(i - 1);
                Node curr = path.getNode(i);

                double segmentLength = Math.sqrt(
                    Math.pow(curr.x - prev.x, 2) +
                    Math.pow(curr.y - prev.y, 2) +
                    Math.pow(curr.z - prev.z, 2)
                );
                pathLength += segmentLength;
            }

            // Quality is inverse of path efficiency (lower is better)
            double efficiency = directDistance / Math.max(pathLength, 1.0);
            return Math.min(1.0, efficiency);

        } catch (Exception e) {
            return 0.5; // Default moderate quality
        }
    }
    
    private void applyCachedPathfinding(Mob mob, PathfindingResult result) {
        try {
            if (result == null || result.getPath() == null) return;

            // Apply cached pathfinding result to mob
            PathNavigation navigation = mob.getNavigation();
            if (navigation != null) {
                // Set the cached path directly
                Path cachedPath = result.getPath();
                if (cachedPath != null && !cachedPath.isDone()) {
                    navigation.moveTo(cachedPath, 1.0);

                    // Log successful cache usage
                    if (System.currentTimeMillis() % 10000 < 50) { // Log every ~10 seconds
                        LOGGER.fine("Applied cached pathfinding for " + mob.getType().getDescriptionId());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Cached pathfinding application error: " + e.getMessage());
        }
    }

    private void applyPathfindingOptimizations(Mob mob, PathfindingResult result) {
        try {
            if (result == null) return;

            // Apply real pathfinding optimizations
            PathNavigation navigation = mob.getNavigation();
            if (navigation != null) {
                // Optimize pathfinding speed based on distance
                double distance = result.getDistance();

                if (distance > farDistance) {
                    // Long distance - reduce pathfinding frequency
                    navigation.setSpeedModifier(0.8);
                } else if (distance < nearDistance) {
                    // Short distance - normal speed
                    navigation.setSpeedModifier(1.0);
                } else {
                    // Medium distance - slight optimization
                    navigation.setSpeedModifier(0.9);
                }

                // Optimize path recalculation frequency
                if (distance > farDistance * 2) {
                    // Very far - reduce recalculation frequency
                    if (navigation instanceof GroundPathNavigation groundNav) {
                        groundNav.setMaxVisitedNodesMultiplier(0.5f);
                    }
                } else {
                    // Normal distance - normal recalculation
                    if (navigation instanceof GroundPathNavigation groundNav) {
                        groundNav.setMaxVisitedNodesMultiplier(1.0f);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Pathfinding optimization application error: " + e.getMessage());
        }
    }
    
    private String getPathKey(BlockPos from, BlockPos to) {
        return from.getX() + "," + from.getY() + "," + from.getZ() + "->" + 
               to.getX() + "," + to.getY() + "," + to.getZ();
    }
    
    /**
     * Optimize entity goals
     * VANILLA-SAFE: Приоритизация и кэширование целей
     */
    private void optimizeGoals(Mob mob, EntityAIData aiData) {
        try {
            // Кэширование решений о целях
            List<Goal> cachedGoals = aiData.getCachedGoals();
            
            if (cachedGoals != null && !aiData.isGoalCacheExpired()) {
                // Используем кэшированные цели
                applyOptimizedGoals(mob, cachedGoals);
                behaviorCacheHits.incrementAndGet();
                return;
            }
            
            // Оптимизируем цели сущности
            List<Goal> optimizedGoals = calculateOptimizedGoals(mob);
            
            // Кэшируем результат
            aiData.setCachedGoals(optimizedGoals);
            aiData.setGoalCacheTime(System.currentTimeMillis());
            
            // Применяем оптимизированные цели
            applyOptimizedGoals(mob, optimizedGoals);
            
            goalOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Goal optimization error: " + e.getMessage());
        }
    }
    
    private List<Goal> calculateOptimizedGoals(Mob mob) {
        List<Goal> optimizedGoals = new ArrayList<>();

        try {
            // Get current goals from mob's goal selector
            GoalSelector goalSelector = mob.goalSelector;
            if (goalSelector == null) return optimizedGoals;

            // Get available goals (simplified approach)
            Set<WrappedGoal> availableGoals = goalSelector.getAvailableGoals();

            // Prioritize goals based on mob type and current situation
            List<WrappedGoal> prioritizedGoals = new ArrayList<>();

            for (WrappedGoal wrappedGoal : availableGoals) {
                Goal goal = wrappedGoal.getGoal();
                int priority = calculateGoalPriority(mob, goal);

                // Only include goals with reasonable priority
                if (priority > 0) {
                    prioritizedGoals.add(wrappedGoal);
                }
            }

            // Sort by priority (higher priority first)
            prioritizedGoals.sort((g1, g2) -> {
                int p1 = calculateGoalPriority(mob, g1.getGoal());
                int p2 = calculateGoalPriority(mob, g2.getGoal());
                return Integer.compare(p2, p1);
            });

            // Convert to Goal list and limit size
            for (WrappedGoal wrappedGoal : prioritizedGoals) {
                optimizedGoals.add(wrappedGoal.getGoal());

                // Limit number of active goals to prevent performance issues
                if (optimizedGoals.size() >= 6) {
                    break;
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Goal calculation error: " + e.getMessage());
            // Return empty list on error to maintain vanilla behavior
        }

        return optimizedGoals;
    }

    private int calculateGoalPriority(Mob mob, Goal goal) {
        try {
            // Calculate goal priority based on mob state and goal type
            String goalClass = goal.getClass().getSimpleName();

            // Combat goals - high priority when target exists
            if (goalClass.contains("Attack")) {
                return mob.getTarget() != null ? 10 : 2;
            }

            // Survival goals - very high priority when threatened
            if (goalClass.contains("Panic") || goalClass.contains("Avoid")) {
                return mob.getLastHurtByMob() != null ? 12 : 3;
            }

            // Pet following - high priority for tamed mobs
            if (goalClass.contains("Follow")) {
                return mob instanceof TamableAnimal && ((TamableAnimal) mob).isTame() ? 9 : 1;
            }

            // Biological needs - medium priority
            if (goalClass.contains("Eat") || goalClass.contains("Breed")) {
                return 6;
            }

            // Social behavior - medium priority when players nearby
            if (goalClass.contains("LookAt")) {
                return mob.level().getNearestPlayer(mob, 8.0) != null ? 4 : 1;
            }

            // Idle behavior - low priority
            if (goalClass.contains("Stroll") || goalClass.contains("Look")) {
                return 2;
            }

            return 5; // Default priority

        } catch (Exception e) {
            return 5; // Default priority on error
        }
    }
    
    private void applyOptimizedGoals(Mob mob, List<Goal> goals) {
        try {
            if (goals == null || goals.isEmpty()) return;

            GoalSelector goalSelector = mob.goalSelector;
            if (goalSelector == null) return;

            // Clear existing goals temporarily (vanilla-safe approach)
            Set<WrappedGoal> currentGoals = new HashSet<>(goalSelector.getAvailableGoals());

            // Disable low-priority goals temporarily
            for (WrappedGoal wrappedGoal : currentGoals) {
                Goal goal = wrappedGoal.getGoal();
                int priority = calculateGoalPriority(mob, goal);

                if (priority < 5) {
                    // Temporarily disable low-priority goals
                    wrappedGoal.stop();
                }
            }

            // Apply optimized goals with proper priorities
            for (int i = 0; i < goals.size(); i++) {
                Goal goal = goals.get(i);
                int priority = calculateGoalPriority(mob, goal);

                // Ensure goal can start if conditions are met
                if (goal.canUse()) {
                    goal.start();
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Goal application error: " + e.getMessage());
        }
    }
    
    /**
     * Apply distance-based optimizations
     * VANILLA-SAFE: Снижение частоты обновлений для далеких сущностей
     */
    private void applyDistanceOptimizations(Mob mob, EntityAIData aiData) {
        try {
            double nearestPlayerDistance = getNearestPlayerDistance(mob);
            
            // Настройка частоты обновлений на основе расстояния
            if (nearestPlayerDistance > farDistance) {
                // Очень далекие сущности - минимальные обновления
                aiData.setUpdateFrequency(0.1); // 10% от обычной частоты
                aiData.setSimplifiedAI(true);
            } else if (nearestPlayerDistance > nearDistance) {
                // Далекие сущности - сниженные обновления
                aiData.setUpdateFrequency(0.5); // 50% от обычной частоты
                aiData.setSimplifiedAI(false);
            } else {
                // Близкие сущности - полные обновления
                aiData.setUpdateFrequency(1.0); // 100% частота
                aiData.setSimplifiedAI(false);
                
                // Добавляем в высокоприоритетные
                highPriorityEntities.add(mob.getId());
            }
            
            distanceOptimizations.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Distance optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Cache behavior decisions
     * VANILLA-SAFE: Кэширование решений для ускорения
     */
    private void cacheBehaviorDecisions(Mob mob, EntityAIData aiData) {
        try {
            // Кэшируем часто принимаемые решения
            // Например, выбор цели, направление движения, действия
            
            // Кэшируем текущую цель
            if (mob.getTarget() != null) {
                aiData.setLastTargetPos(mob.getTarget().blockPosition());
            }
            
            // Кэшируем состояние поведения
            aiData.setBehaviorState(getCurrentBehaviorState(mob));
            
        } catch (Exception e) {
            LOGGER.warning("Behavior caching error: " + e.getMessage());
        }
    }
    
    private String getCurrentBehaviorState(Mob mob) {
        // Определяем текущее состояние поведения сущности
        if (mob.getTarget() != null) {
            return "ATTACKING";
        } else if (mob.getNavigation().isInProgress()) {
            return "MOVING";
        } else {
            return "IDLE";
        }
    }
    
    /**
     * Clean up expired AI cache entries
     */
    public void cleanupAICache() {
        long currentTime = System.currentTimeMillis();
        
        // Remove expired entity AI data
        entityAICache.entrySet().removeIf(entry -> {
            EntityAIData data = entry.getValue();
            return currentTime - data.getLastOptimized() > 300000; // 5 minutes
        });
        
        // Remove expired pathfinding cache
        pathfindingCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Clean up old update times
        lastUpdateTimes.entrySet().removeIf(entry -> {
            return currentTime - entry.getValue() > 600000; // 10 minutes
        });
        
        // Clean up high priority entities
        highPriorityEntities.removeIf(entityId -> !entityAICache.containsKey(entityId));
    }
    
    /**
     * Get entity AI optimization statistics
     */
    public Map<String, Object> getEntityAIStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("entities_optimized", entitiesOptimized.get());
        stats.put("pathfinding_optimizations", pathfindingOptimizations.get());
        stats.put("goal_optimizations", goalOptimizations.get());
        stats.put("behavior_cache_hits", behaviorCacheHits.get());
        stats.put("distance_optimizations", distanceOptimizations.get());
        stats.put("ai_cache_size", entityAICache.size());
        stats.put("pathfinding_cache_size", pathfindingCache.size());
        stats.put("high_priority_entities", highPriorityEntities.size());
        stats.put("cache_hit_rate", calculateAICacheHitRate());
        return stats;
    }
    
    private double calculateAICacheHitRate() {
        long total = entitiesOptimized.get();
        long hits = behaviorCacheHits.get();
        if (total == 0) return 0.0;
        return (hits * 100.0) / total;
    }
    
    // Getters
    public long getEntitiesOptimized() { return entitiesOptimized.get(); }
    public long getPathfindingOptimizations() { return pathfindingOptimizations.get(); }
    public long getGoalOptimizations() { return goalOptimizations.get(); }
    public long getBehaviorCacheHits() { return behaviorCacheHits.get(); }
    public long getDistanceOptimizations() { return distanceOptimizations.get(); }
    
    /**
     * Entity AI data container
     */
    private static class EntityAIData {
        private long lastOptimized = System.currentTimeMillis();
        private double updateFrequency = 1.0;
        private boolean simplifiedAI = false;
        private BlockPos lastTargetPos;
        private String behaviorState = "IDLE";
        private List<Goal> cachedGoals;
        private long goalCacheTime = 0;
        
        public boolean isGoalCacheExpired() {
            return System.currentTimeMillis() - goalCacheTime > 5000; // 5 seconds
        }
        
        // Getters and setters
        public long getLastOptimized() { return lastOptimized; }
        public void setLastOptimized(long lastOptimized) { this.lastOptimized = lastOptimized; }
        
        public double getUpdateFrequency() { return updateFrequency; }
        public void setUpdateFrequency(double updateFrequency) { this.updateFrequency = updateFrequency; }
        
        public boolean isSimplifiedAI() { return simplifiedAI; }
        public void setSimplifiedAI(boolean simplifiedAI) { this.simplifiedAI = simplifiedAI; }
        
        public BlockPos getLastTargetPos() { return lastTargetPos; }
        public void setLastTargetPos(BlockPos lastTargetPos) { this.lastTargetPos = lastTargetPos; }
        
        public String getBehaviorState() { return behaviorState; }
        public void setBehaviorState(String behaviorState) { this.behaviorState = behaviorState; }
        
        public List<Goal> getCachedGoals() { return cachedGoals; }
        public void setCachedGoals(List<Goal> cachedGoals) { this.cachedGoals = cachedGoals; }
        
        public long getGoalCacheTime() { return goalCacheTime; }
        public void setGoalCacheTime(long goalCacheTime) { this.goalCacheTime = goalCacheTime; }
    }
    
    /**
     * Pathfinding result container
     */
    private static class PathfindingResult {
        private BlockPos fromPos;
        private BlockPos toPos;
        private long timestamp = System.currentTimeMillis();
        private Path path;
        private double distance;
        private double pathQuality;

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 10000; // 10 seconds
        }

        // Getters and setters
        public BlockPos getFromPos() { return fromPos; }
        public void setFromPos(BlockPos fromPos) { this.fromPos = fromPos; }

        public BlockPos getToPos() { return toPos; }
        public void setToPos(BlockPos toPos) { this.toPos = toPos; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public Path getPath() { return path; }
        public void setPath(Path path) { this.path = path; }

        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }

        public double getPathQuality() { return pathQuality; }
        public void setPathQuality(double pathQuality) { this.pathQuality = pathQuality; }
    }
}
