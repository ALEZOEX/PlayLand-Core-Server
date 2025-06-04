package ru.playland.core.optimization;

import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Advanced Collision Optimizer
 * ПРОДВИНУТАЯ оптимизация коллизий
 * Оптимизирует проверки коллизий без нарушения vanilla механик
 */
public class AdvancedCollisionOptimizer {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-Collision");
    
    // Collision optimization statistics
    private final AtomicLong totalCollisionChecks = new AtomicLong(0);
    private final AtomicLong optimizedCollisionChecks = new AtomicLong(0);
    private final AtomicLong collisionCacheHits = new AtomicLong(0);
    private final AtomicLong collisionCacheMisses = new AtomicLong(0);
    private final AtomicLong spatialOptimizations = new AtomicLong(0);
    private final AtomicLong broadPhaseOptimizations = new AtomicLong(0);
    
    // Collision caching
    private final Map<String, CollisionResult> collisionCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Spatial optimization
    private final Map<String, SpatialGrid> spatialGrids = new ConcurrentHashMap<>();
    private final Map<String, EntityBounds> entityBounds = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableCollisionOptimization = true;
    private boolean enableCollisionCaching = true;
    private boolean enableSpatialOptimization = true;
    private boolean enableBroadPhaseOptimization = true;
    private boolean enableVanillaSafeMode = true;
    
    private long cacheExpirationTime = 100; // 100ms
    private int spatialGridSize = 16; // 16x16 blocks per grid cell
    private int maxCacheSize = 10000;
    private double collisionThreshold = 0.1; // Minimum distance for collision check
    
    public void initialize() {
        LOGGER.info("⚡ Initializing Advanced Collision Optimizer...");
        
        loadCollisionSettings();
        initializeSpatialGrids();
        startCacheCleanup();
        
        LOGGER.info("✅ Advanced Collision Optimizer initialized!");
        LOGGER.info("⚡ Collision optimization: " + (enableCollisionOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("💾 Collision caching: " + (enableCollisionCaching ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗺️ Spatial optimization: " + (enableSpatialOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔍 Broad phase optimization: " + (enableBroadPhaseOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏰ Cache expiration: " + cacheExpirationTime + "ms");
        LOGGER.info("🗺️ Spatial grid size: " + spatialGridSize + "x" + spatialGridSize + " blocks");
        LOGGER.info("💾 Max cache size: " + maxCacheSize);
    }
    
    private void loadCollisionSettings() {
        try {
            LOGGER.info("⚙️ Loading collision settings...");

            // Load collision optimization parameters from system properties
            enableCollisionOptimization = Boolean.parseBoolean(System.getProperty("playland.collision.optimization.enabled", "true"));
            enableCollisionCaching = Boolean.parseBoolean(System.getProperty("playland.collision.caching.enabled", "true"));
            enableSpatialOptimization = Boolean.parseBoolean(System.getProperty("playland.collision.spatial.enabled", "true"));
            enableBroadPhaseOptimization = Boolean.parseBoolean(System.getProperty("playland.collision.broadphase.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.collision.vanilla.safe", "true"));

            // Load collision parameters
            cacheExpirationTime = Long.parseLong(System.getProperty("playland.collision.cache.expiration", "100"));
            spatialGridSize = Integer.parseInt(System.getProperty("playland.collision.spatial.grid.size", "16"));
            maxCacheSize = Integer.parseInt(System.getProperty("playland.collision.cache.max.size", "10000"));
            collisionThreshold = Double.parseDouble(System.getProperty("playland.collision.threshold", "0.1"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce collision complexity
                enableSpatialOptimization = false;
                enableBroadPhaseOptimization = true; // Keep broad phase for performance
                maxCacheSize = Math.max(1000, maxCacheSize / 2);
                cacheExpirationTime = Math.max(50, cacheExpirationTime / 2);
                spatialGridSize = Math.min(32, spatialGridSize * 2); // Larger grids = less precision but faster
                LOGGER.info("🔧 Reduced collision complexity for low TPS: cache=" + maxCacheSize +
                           ", expiration=" + cacheExpirationTime + "ms, grid=" + spatialGridSize);
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive collision optimization
                enableSpatialOptimization = true;
                enableBroadPhaseOptimization = true;
                maxCacheSize = Math.min(20000, (int) (maxCacheSize * 1.5));
                spatialGridSize = Math.max(8, spatialGridSize / 2); // Smaller grids = more precision
                LOGGER.info("🔧 Increased collision precision for good TPS: cache=" + maxCacheSize +
                           ", grid=" + spatialGridSize);
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce collision caching
                enableCollisionCaching = false;
                maxCacheSize = Math.max(500, maxCacheSize / 4);
                cacheExpirationTime = Math.max(25, cacheExpirationTime / 2);
                LOGGER.warning("⚠️ High memory usage - reduced collision caching: cache=" + maxCacheSize +
                              ", expiration=" + cacheExpirationTime + "ms");
            }

            // Auto-adjust based on entity count
            int totalEntities = getTotalEntityCount();
            if (totalEntities > 1000) {
                // Many entities - more aggressive spatial optimization
                enableSpatialOptimization = true;
                spatialGridSize = Math.max(4, spatialGridSize / 2);
                collisionThreshold = Math.max(0.05, collisionThreshold / 2);
                LOGGER.info("🔧 Optimized for " + totalEntities + " entities: grid=" + spatialGridSize +
                           ", threshold=" + collisionThreshold);
            } else if (totalEntities < 100) {
                // Few entities - disable spatial optimization overhead
                enableSpatialOptimization = false;
                enableCollisionCaching = false;
                LOGGER.info("🔧 Disabled spatial optimization for low entity count: " + totalEntities);
            }

            // Auto-adjust based on world size
            int loadedChunks = getLoadedChunkCount();
            if (loadedChunks > 500) {
                // Large world - larger spatial grids
                spatialGridSize = Math.min(64, spatialGridSize * 2);
                LOGGER.info("🔧 Adjusted for large world: " + loadedChunks + " chunks, grid=" + spatialGridSize);
            }

            LOGGER.info("✅ Collision settings loaded - Cache: " + maxCacheSize +
                       ", Expiration: " + cacheExpirationTime + "ms, Grid: " + spatialGridSize +
                       "x" + spatialGridSize + ", Threshold: " + collisionThreshold);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading collision settings: " + e.getMessage() + " - using defaults");
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

    private int getLoadedChunkCount() {
        try {
            int totalChunks = 0;
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                totalChunks += world.getLoadedChunks().length;
            }
            return totalChunks;
        } catch (Exception e) {
            return 200; // Default moderate count
        }
    }
    
    private void initializeSpatialGrids() {
        // Initialize spatial grids for different worlds
        createSpatialGrid("overworld");
        createSpatialGrid("nether");
        createSpatialGrid("end");
        
        LOGGER.info("🗺️ Spatial grids initialized: " + spatialGrids.size());
    }
    
    private void createSpatialGrid(String worldName) {
        SpatialGrid grid = new SpatialGrid(worldName, spatialGridSize);
        spatialGrids.put(worldName, grid);
    }
    
    private void startCacheCleanup() {
        // Start cache cleanup in a separate thread
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Clean every 10 seconds
                    cleanupExpiredCache();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("PlayLand-CollisionCleanup");
        cleanupThread.start();
        
        LOGGER.info("🧹 Cache cleanup started");
    }
    
    /**
     * Optimize collision check between two entities
     */
    public boolean checkCollision(String entity1Id, EntityBounds bounds1, String entity2Id, EntityBounds bounds2) {
        if (!enableCollisionOptimization) {
            return performVanillaCollisionCheck(bounds1, bounds2);
        }
        
        totalCollisionChecks.incrementAndGet();
        
        try {
            // Broad phase optimization
            if (enableBroadPhaseOptimization && !broadPhaseCollisionCheck(bounds1, bounds2)) {
                broadPhaseOptimizations.incrementAndGet();
                return false;
            }
            
            // Check cache first
            if (enableCollisionCaching) {
                String cacheKey = generateCacheKey(entity1Id, bounds1, entity2Id, bounds2);
                CollisionResult cachedResult = getCachedCollision(cacheKey);
                
                if (cachedResult != null) {
                    collisionCacheHits.incrementAndGet();
                    return cachedResult.isColliding();
                }
                
                collisionCacheMisses.incrementAndGet();
                
                // Perform collision check and cache result
                boolean result = performOptimizedCollisionCheck(bounds1, bounds2);
                cacheCollisionResult(cacheKey, result);
                
                optimizedCollisionChecks.incrementAndGet();
                return result;
            }
            
            // Direct optimized collision check
            boolean result = performOptimizedCollisionCheck(bounds1, bounds2);
            optimizedCollisionChecks.incrementAndGet();
            return result;
            
        } catch (Exception e) {
            LOGGER.warning("Collision optimization error: " + e.getMessage());
            // Fallback to vanilla collision check
            return performVanillaCollisionCheck(bounds1, bounds2);
        }
    }
    
    /**
     * Broad phase collision check (quick elimination)
     */
    private boolean broadPhaseCollisionCheck(EntityBounds bounds1, EntityBounds bounds2) {
        // Quick distance check
        double centerDistance = calculateCenterDistance(bounds1, bounds2);
        double maxRadius = Math.max(bounds1.getRadius(), bounds2.getRadius());
        
        // If centers are too far apart, no collision possible
        return centerDistance <= (maxRadius * 2 + collisionThreshold);
    }
    
    /**
     * Perform optimized collision check
     */
    private boolean performOptimizedCollisionCheck(EntityBounds bounds1, EntityBounds bounds2) {
        if (!enableVanillaSafeMode) {
            return performFastCollisionCheck(bounds1, bounds2);
        }
        
        // Vanilla-safe optimized collision check
        return performVanillaCollisionCheck(bounds1, bounds2);
    }
    
    /**
     * Fast collision check (non-vanilla)
     */
    private boolean performFastCollisionCheck(EntityBounds bounds1, EntityBounds bounds2) {
        // Simple sphere-sphere collision for fast checking
        double distance = calculateCenterDistance(bounds1, bounds2);
        double radiusSum = bounds1.getRadius() + bounds2.getRadius();
        
        return distance <= radiusSum;
    }
    
    /**
     * Vanilla collision check (maintains exact vanilla behavior)
     */
    private boolean performVanillaCollisionCheck(EntityBounds bounds1, EntityBounds bounds2) {
        // AABB (Axis-Aligned Bounding Box) collision check
        return bounds1.getMinX() < bounds2.getMaxX() &&
               bounds1.getMaxX() > bounds2.getMinX() &&
               bounds1.getMinY() < bounds2.getMaxY() &&
               bounds1.getMaxY() > bounds2.getMinY() &&
               bounds1.getMinZ() < bounds2.getMaxZ() &&
               bounds1.getMaxZ() > bounds2.getMinZ();
    }
    
    /**
     * Calculate distance between entity centers
     */
    private double calculateCenterDistance(EntityBounds bounds1, EntityBounds bounds2) {
        double dx = bounds1.getCenterX() - bounds2.getCenterX();
        double dy = bounds1.getCenterY() - bounds2.getCenterY();
        double dz = bounds1.getCenterZ() - bounds2.getCenterZ();
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Generate cache key for collision pair
     */
    private String generateCacheKey(String entity1Id, EntityBounds bounds1, String entity2Id, EntityBounds bounds2) {
        // Create deterministic key regardless of entity order
        String id1 = entity1Id.compareTo(entity2Id) < 0 ? entity1Id : entity2Id;
        String id2 = entity1Id.compareTo(entity2Id) < 0 ? entity2Id : entity1Id;
        
        EntityBounds b1 = entity1Id.compareTo(entity2Id) < 0 ? bounds1 : bounds2;
        EntityBounds b2 = entity1Id.compareTo(entity2Id) < 0 ? bounds2 : bounds1;
        
        return id1 + "_" + id2 + "_" + 
               (int)(b1.getCenterX() * 10) + "_" + (int)(b1.getCenterY() * 10) + "_" + (int)(b1.getCenterZ() * 10) + "_" +
               (int)(b2.getCenterX() * 10) + "_" + (int)(b2.getCenterY() * 10) + "_" + (int)(b2.getCenterZ() * 10);
    }
    
    /**
     * Get cached collision result
     */
    private CollisionResult getCachedCollision(String cacheKey) {
        CollisionResult result = collisionCache.get(cacheKey);
        if (result != null) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && System.currentTimeMillis() - timestamp < cacheExpirationTime) {
                return result;
            } else {
                // Expired cache entry
                collisionCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
            }
        }
        return null;
    }
    
    /**
     * Cache collision result
     */
    private void cacheCollisionResult(String cacheKey, boolean isColliding) {
        if (collisionCache.size() >= maxCacheSize) {
            // Remove oldest entries
            cleanupOldestCacheEntries();
        }
        
        collisionCache.put(cacheKey, new CollisionResult(isColliding));
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * Update entity position in spatial grid
     */
    public void updateEntityPosition(String worldName, String entityId, EntityBounds bounds) {
        if (!enableSpatialOptimization) return;
        
        try {
            SpatialGrid grid = spatialGrids.get(worldName);
            if (grid != null) {
                grid.updateEntity(entityId, bounds);
                entityBounds.put(entityId, bounds);
                spatialOptimizations.incrementAndGet();
            }
        } catch (Exception e) {
            LOGGER.warning("Spatial grid update error: " + e.getMessage());
        }
    }
    
    /**
     * Remove entity from spatial grid
     */
    public void removeEntity(String worldName, String entityId) {
        if (!enableSpatialOptimization) return;
        
        try {
            SpatialGrid grid = spatialGrids.get(worldName);
            if (grid != null) {
                grid.removeEntity(entityId);
                entityBounds.remove(entityId);
            }
        } catch (Exception e) {
            LOGGER.warning("Entity removal error: " + e.getMessage());
        }
    }
    
    /**
     * Get nearby entities for collision checking
     */
    public List<String> getNearbyEntities(String worldName, EntityBounds bounds, double radius) {
        if (!enableSpatialOptimization) {
            return new ArrayList<>(); // Return empty list if spatial optimization disabled
        }
        
        try {
            SpatialGrid grid = spatialGrids.get(worldName);
            if (grid != null) {
                return grid.getNearbyEntities(bounds, radius);
            }
        } catch (Exception e) {
            LOGGER.warning("Nearby entities query error: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        cacheTimestamps.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > cacheExpirationTime) {
                collisionCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Clean up oldest cache entries when cache is full
     */
    private void cleanupOldestCacheEntries() {
        if (cacheTimestamps.isEmpty()) return;
        
        // Find oldest entries
        List<Map.Entry<String, Long>> entries = new ArrayList<>(cacheTimestamps.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        
        // Remove oldest 25% of entries
        int toRemove = Math.max(1, entries.size() / 4);
        for (int i = 0; i < toRemove; i++) {
            String key = entries.get(i).getKey();
            collisionCache.remove(key);
            cacheTimestamps.remove(key);
        }
    }
    
    /**
     * Get collision cache hit rate
     */
    public double getCacheHitRate() {
        long hits = collisionCacheHits.get();
        long misses = collisionCacheMisses.get();
        long total = hits + misses;
        
        if (total == 0) return 0.0;
        return (hits * 100.0) / total;
    }
    
    /**
     * Get collision optimization statistics
     */
    public Map<String, Object> getCollisionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_collision_checks", totalCollisionChecks.get());
        stats.put("optimized_collision_checks", optimizedCollisionChecks.get());
        stats.put("collision_cache_hits", collisionCacheHits.get());
        stats.put("collision_cache_misses", collisionCacheMisses.get());
        stats.put("spatial_optimizations", spatialOptimizations.get());
        stats.put("broad_phase_optimizations", broadPhaseOptimizations.get());
        
        stats.put("cache_hit_rate", getCacheHitRate());
        stats.put("cache_size", collisionCache.size());
        stats.put("max_cache_size", maxCacheSize);
        stats.put("spatial_grids", spatialGrids.size());
        stats.put("tracked_entities", entityBounds.size());
        
        long totalChecks = totalCollisionChecks.get();
        if (totalChecks > 0) {
            stats.put("optimization_rate", (optimizedCollisionChecks.get() * 100.0) / totalChecks);
        }
        
        return stats;
    }
    
    // Getters
    public long getTotalCollisionChecks() { return totalCollisionChecks.get(); }
    public long getOptimizedCollisionChecks() { return optimizedCollisionChecks.get(); }
    public long getCollisionCacheHits() { return collisionCacheHits.get(); }
    public long getCollisionCacheMisses() { return collisionCacheMisses.get(); }
    public long getSpatialOptimizations() { return spatialOptimizations.get(); }
    public long getBroadPhaseOptimizations() { return broadPhaseOptimizations.get(); }
    
    public void shutdown() {
        // Clear all data
        collisionCache.clear();
        cacheTimestamps.clear();
        spatialGrids.clear();
        entityBounds.clear();
        
        LOGGER.info("⚡ Advanced Collision Optimizer shutdown complete");
    }
    
    /**
     * Collision result container
     */
    private static class CollisionResult {
        private final boolean colliding;
        
        public CollisionResult(boolean colliding) {
            this.colliding = colliding;
        }
        
        public boolean isColliding() { return colliding; }
    }
    
    /**
     * Entity bounds container
     */
    public static class EntityBounds {
        private final double minX, minY, minZ;
        private final double maxX, maxY, maxZ;
        private final double centerX, centerY, centerZ;
        private final double radius;
        
        public EntityBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            
            this.centerX = (minX + maxX) / 2.0;
            this.centerY = (minY + maxY) / 2.0;
            this.centerZ = (minZ + maxZ) / 2.0;
            
            double dx = maxX - minX;
            double dy = maxY - minY;
            double dz = maxZ - minZ;
            this.radius = Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0;
        }
        
        // Getters
        public double getMinX() { return minX; }
        public double getMinY() { return minY; }
        public double getMinZ() { return minZ; }
        public double getMaxX() { return maxX; }
        public double getMaxY() { return maxY; }
        public double getMaxZ() { return maxZ; }
        public double getCenterX() { return centerX; }
        public double getCenterY() { return centerY; }
        public double getCenterZ() { return centerZ; }
        public double getRadius() { return radius; }
    }
    
    /**
     * Spatial grid for efficient collision detection
     */
    private static class SpatialGrid {
        private final String worldName;
        private final int gridSize;
        private final Map<String, Set<String>> grid = new ConcurrentHashMap<>();
        private final Map<String, String> entityToCell = new ConcurrentHashMap<>();
        
        public SpatialGrid(String worldName, int gridSize) {
            this.worldName = worldName;
            this.gridSize = gridSize;
        }
        
        public void updateEntity(String entityId, EntityBounds bounds) {
            String newCell = getCellKey(bounds.getCenterX(), bounds.getCenterZ());
            String oldCell = entityToCell.get(entityId);
            
            if (!newCell.equals(oldCell)) {
                // Remove from old cell
                if (oldCell != null) {
                    Set<String> oldCellEntities = grid.get(oldCell);
                    if (oldCellEntities != null) {
                        oldCellEntities.remove(entityId);
                        if (oldCellEntities.isEmpty()) {
                            grid.remove(oldCell);
                        }
                    }
                }
                
                // Add to new cell
                grid.computeIfAbsent(newCell, k -> ConcurrentHashMap.newKeySet()).add(entityId);
                entityToCell.put(entityId, newCell);
            }
        }
        
        public void removeEntity(String entityId) {
            String cell = entityToCell.remove(entityId);
            if (cell != null) {
                Set<String> cellEntities = grid.get(cell);
                if (cellEntities != null) {
                    cellEntities.remove(entityId);
                    if (cellEntities.isEmpty()) {
                        grid.remove(cell);
                    }
                }
            }
        }
        
        public List<String> getNearbyEntities(EntityBounds bounds, double radius) {
            List<String> nearby = new ArrayList<>();
            
            // Calculate grid cells to check
            int radiusCells = (int) Math.ceil(radius / gridSize) + 1;
            int centerX = (int) (bounds.getCenterX() / gridSize);
            int centerZ = (int) (bounds.getCenterZ() / gridSize);
            
            for (int dx = -radiusCells; dx <= radiusCells; dx++) {
                for (int dz = -radiusCells; dz <= radiusCells; dz++) {
                    String cellKey = getCellKey((centerX + dx) * gridSize, (centerZ + dz) * gridSize);
                    Set<String> cellEntities = grid.get(cellKey);
                    
                    if (cellEntities != null) {
                        nearby.addAll(cellEntities);
                    }
                }
            }
            
            return nearby;
        }
        
        private String getCellKey(double x, double z) {
            int cellX = (int) Math.floor(x / gridSize);
            int cellZ = (int) Math.floor(z / gridSize);
            return cellX + "_" + cellZ;
        }
    }
}
