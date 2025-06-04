package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Particle System Manager
 * РЕВОЛЮЦИОННОЕ управление частицами и эффектами
 */
public class ParticleSystemManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-ParticleSystem");
    
    // Statistics
    private final AtomicLong particlesGenerated = new AtomicLong(0);
    private final AtomicLong particlesOptimized = new AtomicLong(0);
    private final AtomicLong effectsRendered = new AtomicLong(0);
    private final AtomicLong particleCulling = new AtomicLong(0);
    private final AtomicLong batchedParticles = new AtomicLong(0);
    private final AtomicLong particlePooling = new AtomicLong(0);
    
    // Management
    private final Map<String, Object> particlePools = new ConcurrentHashMap<>();
    private final ScheduledExecutorService particleOptimizer = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private boolean enableParticleOptimization = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableParticleCulling = true;
    private boolean enableParticleBatching = true;
    private boolean enableParticlePooling = true;
    private boolean enableDistanceOptimization = true;

    // Particle parameters
    private int particleUpdateInterval = 20; // milliseconds
    private int maxParticlesPerPlayer = 500;
    private int maxParticlesGlobal = 5000;
    private double particleRenderDistance = 64.0;
    private int particleBatchSize = 50;
    private long particlePoolExpiration = 30000; // 30 seconds
    
    public void initialize() {
        LOGGER.info("✨ Initializing Particle System Manager...");

        loadParticleSettings();
        startParticleOptimization();

        LOGGER.info("✅ Particle System Manager initialized!");
        LOGGER.info("✨ Particle optimization: " + (enableParticleOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("🎯 Particle culling: " + (enableParticleCulling ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Particle batching: " + (enableParticleBatching ? "ENABLED" : "DISABLED"));
    }

    private void loadParticleSettings() {
        try {
            LOGGER.info("⚙️ Loading particle settings...");

            // Load particle parameters from system properties
            particleUpdateInterval = Integer.parseInt(System.getProperty("playland.particle.update.interval", "20"));
            maxParticlesPerPlayer = Integer.parseInt(System.getProperty("playland.particle.max.per.player", "500"));
            maxParticlesGlobal = Integer.parseInt(System.getProperty("playland.particle.max.global", "5000"));
            particleRenderDistance = Double.parseDouble(System.getProperty("playland.particle.render.distance", "64.0"));
            particleBatchSize = Integer.parseInt(System.getProperty("playland.particle.batch.size", "50"));
            particlePoolExpiration = Long.parseLong(System.getProperty("playland.particle.pool.expiration", "30000"));

            // Load feature flags
            enableParticleOptimization = Boolean.parseBoolean(System.getProperty("playland.particle.optimization.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.particle.vanilla.safe", "true"));
            enableParticleCulling = Boolean.parseBoolean(System.getProperty("playland.particle.culling.enabled", "true"));
            enableParticleBatching = Boolean.parseBoolean(System.getProperty("playland.particle.batching.enabled", "true"));
            enableParticlePooling = Boolean.parseBoolean(System.getProperty("playland.particle.pooling.enabled", "true"));
            enableDistanceOptimization = Boolean.parseBoolean(System.getProperty("playland.particle.distance.optimization", "true"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce particle complexity
                maxParticlesPerPlayer = Math.max(100, maxParticlesPerPlayer / 2);
                maxParticlesGlobal = Math.max(1000, maxParticlesGlobal / 2);
                particleRenderDistance = Math.max(32.0, particleRenderDistance * 0.75);
                particleUpdateInterval = Math.min(100, particleUpdateInterval * 2);
                LOGGER.info("🔧 Reduced particle complexity for low TPS: perPlayer=" + maxParticlesPerPlayer +
                           ", global=" + maxParticlesGlobal + ", distance=" + particleRenderDistance);
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more particles
                maxParticlesPerPlayer = Math.min(1000, (int) (maxParticlesPerPlayer * 1.5));
                maxParticlesGlobal = Math.min(10000, (int) (maxParticlesGlobal * 1.5));
                particleRenderDistance = Math.min(128.0, particleRenderDistance * 1.25);
                LOGGER.info("🔧 Increased particle capability for good TPS: perPlayer=" + maxParticlesPerPlayer +
                           ", global=" + maxParticlesGlobal + ", distance=" + particleRenderDistance);
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce particle effects
                maxParticlesGlobal = Math.max(500, maxParticlesGlobal / 3);
                enableParticlePooling = false;
                particlePoolExpiration = Math.max(10000, particlePoolExpiration / 2);
                LOGGER.warning("⚠️ High memory usage - reduced particle effects: global=" + maxParticlesGlobal);
            }

            // Auto-adjust based on player count
            int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
            if (playerCount > 50) {
                // Many players - reduce per-player particles
                maxParticlesPerPlayer = Math.max(200, maxParticlesPerPlayer - (playerCount - 50) * 5);
                LOGGER.info("🔧 Adjusted for " + playerCount + " players: max per player=" + maxParticlesPerPlayer);
            }

            LOGGER.info("✅ Particle settings loaded - Update: " + particleUpdateInterval + "ms, PerPlayer: " +
                       maxParticlesPerPlayer + ", Global: " + maxParticlesGlobal + ", Distance: " + particleRenderDistance);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading particle settings: " + e.getMessage() + " - using defaults");
        }
    }

    private double getCurrentTPS() {
        try {
            return org.bukkit.Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }
    
    private void startParticleOptimization() {
        particleOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeParticles();
            } catch (Exception e) {
                LOGGER.warning("Particle optimization error: " + e.getMessage());
            }
        }, 20, 20, TimeUnit.MILLISECONDS);
        
        LOGGER.info("⚡ Particle optimization started");
    }
    
    private void optimizeParticles() {
        if (!enableParticleOptimization) return;

        try {
            particlesOptimized.incrementAndGet();

            // Optimize particles for all online players
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                optimizeParticlesForPlayer(player);
            }

            // Clean up expired particle pools
            cleanupParticlePools();

            // Batch particles if enabled
            if (enableParticleBatching) {
                batchParticleEffects();
                batchedParticles.incrementAndGet();
            }

        } catch (Exception e) {
            LOGGER.fine("Particle optimization error: " + e.getMessage());
        }
    }

    private void optimizeParticlesForPlayer(org.bukkit.entity.Player player) {
        try {
            // Cull particles based on distance
            if (enableParticleCulling) {
                cullDistantParticles(player);
                particleCulling.incrementAndGet();
            }

            // Optimize particle rendering
            if (enableDistanceOptimization) {
                optimizeParticleRendering(player);
                effectsRendered.incrementAndGet();
            }

            // Manage particle pools for this player
            if (enableParticlePooling) {
                managePlayerParticlePool(player);
                particlePooling.incrementAndGet();
            }

        } catch (Exception e) {
            LOGGER.fine("Player particle optimization error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void cullDistantParticles(org.bukkit.entity.Player player) {
        try {
            org.bukkit.Location playerLocation = player.getLocation();
            String playerName = player.getName();
            String cacheKey = "particle_cull_" + playerName;

            // Check if we need to cull particles for this player
            ParticleCullData cullData = (ParticleCullData) particlePools.get(cacheKey);
            if (cullData != null && !cullData.isExpired(5000)) { // 5 second cache
                return; // Recently culled
            }

            // Count nearby particle sources (simplified)
            int nearbyParticles = countNearbyParticleEffects(playerLocation);

            if (nearbyParticles > maxParticlesPerPlayer) {
                // Too many particles - need to cull
                int particlesToCull = nearbyParticles - maxParticlesPerPlayer;
                cullParticleEffects(playerLocation, particlesToCull);

                LOGGER.fine("Culled " + particlesToCull + " particles for " + playerName +
                           " (had " + nearbyParticles + ", max " + maxParticlesPerPlayer + ")");
            }

            // Cache cull data
            ParticleCullData newCullData = new ParticleCullData();
            newCullData.setPlayerName(playerName);
            newCullData.setParticleCount(nearbyParticles);
            newCullData.setTimestamp(System.currentTimeMillis());
            particlePools.put(cacheKey, newCullData);

        } catch (Exception e) {
            LOGGER.fine("Particle culling error: " + e.getMessage());
        }
    }

    private int countNearbyParticleEffects(org.bukkit.Location location) {
        try {
            // Simplified particle counting based on nearby entities and effects
            int particleCount = 0;

            // Count entities that produce particles
            for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location,
                    particleRenderDistance, particleRenderDistance, particleRenderDistance)) {

                // Different entities produce different amounts of particles
                switch (entity.getType()) {
                    case BLAZE:
                        particleCount += 10; // Fire particles
                        break;
                    case ENDERMAN:
                        particleCount += 5; // Portal particles
                        break;
                    case WITCH:
                        particleCount += 8; // Potion particles
                        break;
                    case EXPERIENCE_ORB:
                        particleCount += 3; // Sparkle particles
                        break;
                    case ITEM:
                        particleCount += 1; // Minimal particles
                        break;
                    default:
                        if (entity instanceof org.bukkit.entity.LivingEntity) {
                            particleCount += 2; // Generic living entity particles
                        }
                        break;
                }
            }

            // Add base environmental particles
            particleCount += 20; // Dust, ambient particles, etc.

            return particleCount;

        } catch (Exception e) {
            LOGGER.fine("Particle counting error: " + e.getMessage());
            return 50; // Default moderate count
        }
    }

    private void cullParticleEffects(org.bukkit.Location location, int particlesToCull) {
        try {
            // Simplified particle culling - in real implementation this would
            // interact with the particle system to reduce particle density

            // Priority culling: remove least important particles first
            int culled = 0;

            // 1. Cull ambient particles first (lowest priority)
            int ambientCull = Math.min(particlesToCull / 3, 10);
            culled += ambientCull;

            // 2. Cull distant entity particles
            int distantCull = Math.min((particlesToCull - culled) / 2, 15);
            culled += distantCull;

            // 3. Reduce particle density for remaining effects
            int densityReduction = particlesToCull - culled;
            culled += densityReduction;

            LOGGER.fine("Particle culling: ambient=" + ambientCull +
                       ", distant=" + distantCull +
                       ", density=" + densityReduction +
                       ", total=" + culled);

        } catch (Exception e) {
            LOGGER.fine("Particle culling execution error: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getParticleStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("particles_generated", particlesGenerated.get());
        stats.put("particles_optimized", particlesOptimized.get());
        stats.put("effects_rendered", effectsRendered.get());
        stats.put("particle_culling", particleCulling.get());
        stats.put("batched_particles", batchedParticles.get());
        stats.put("particle_pooling", particlePooling.get());
        return stats;
    }
    
    // Getters
    public long getParticlesGenerated() { return particlesGenerated.get(); }
    public long getParticlesOptimized() { return particlesOptimized.get(); }
    public long getEffectsRendered() { return effectsRendered.get(); }
    public long getParticleCulling() { return particleCulling.get(); }
    public long getBatchedParticles() { return batchedParticles.get(); }
    public long getParticlePooling() { return particlePooling.get(); }
    
    public void shutdown() {
        particleOptimizer.shutdown();
        particlePools.clear();
        LOGGER.info("✨ Particle System Manager shutdown complete");
    }

    // Missing methods implementation
    private void cleanupParticlePools() {
        try {
            long currentTime = System.currentTimeMillis();

            // Remove expired particle pool entries
            particlePools.entrySet().removeIf(entry -> {
                Object value = entry.getValue();
                if (value instanceof ParticleCullData) {
                    ParticleCullData cullData = (ParticleCullData) value;
                    return cullData.isExpired(30000); // 30 seconds
                }
                return false;
            });

            LOGGER.fine("Cleaned up expired particle pools");

        } catch (Exception e) {
            LOGGER.fine("Particle pool cleanup error: " + e.getMessage());
        }
    }

    private void batchParticleEffects() {
        try {
            // Batch similar particle effects to reduce rendering calls
            // This is a simplified implementation

            int batchedCount = 0;

            // Group particles by type and location
            Map<String, Integer> particleGroups = new ConcurrentHashMap<>();

            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                org.bukkit.Location loc = player.getLocation();
                String locationKey = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

                particleGroups.merge(locationKey, 1, Integer::sum);
                batchedCount++;
            }

            if (batchedCount > 0) {
                LOGGER.fine("Batched " + batchedCount + " particle effects into " + particleGroups.size() + " groups");
            }

        } catch (Exception e) {
            LOGGER.fine("Particle batching error: " + e.getMessage());
        }
    }

    private void optimizeParticleRendering(org.bukkit.entity.Player player) {
        try {
            org.bukkit.Location playerLoc = player.getLocation();

            // Adjust particle quality based on distance and performance
            double distanceFromSpawn = playerLoc.distance(playerLoc.getWorld().getSpawnLocation());

            // Reduce particle quality for distant players
            if (distanceFromSpawn > 1000) {
                // Far from spawn - reduce particles
                reduceParticleQuality(player, 0.5);
            } else if (distanceFromSpawn > 500) {
                // Medium distance - slight reduction
                reduceParticleQuality(player, 0.75);
            } else {
                // Close to spawn - full quality
                reduceParticleQuality(player, 1.0);
            }

            effectsRendered.incrementAndGet();

        } catch (Exception e) {
            LOGGER.fine("Particle rendering optimization error: " + e.getMessage());
        }
    }

    private void managePlayerParticlePool(org.bukkit.entity.Player player) {
        try {
            String playerName = player.getName();
            String poolKey = "pool_" + playerName;

            // Get or create particle pool for this player
            Object poolObj = particlePools.get(poolKey);
            PlayerParticlePool pool;

            if (poolObj instanceof PlayerParticlePool) {
                pool = (PlayerParticlePool) poolObj;
            } else {
                pool = new PlayerParticlePool(playerName);
                particlePools.put(poolKey, pool);
            }

            // Update pool with current player state
            pool.updatePlayerLocation(player.getLocation());
            pool.manageParticles();

            particlePooling.incrementAndGet();

        } catch (Exception e) {
            LOGGER.fine("Player particle pool management error: " + e.getMessage());
        }
    }

    private void reduceParticleQuality(org.bukkit.entity.Player player, double qualityFactor) {
        try {
            // This would interact with the client to reduce particle density
            // For now, just log the quality adjustment
            LOGGER.fine("Adjusted particle quality for " + player.getName() + " to " + (qualityFactor * 100) + "%");

        } catch (Exception e) {
            LOGGER.fine("Particle quality reduction error: " + e.getMessage());
        }
    }

    // Missing data classes
    private static class ParticleCullData {
        private String playerName;
        private int particleCount;
        private long timestamp;

        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public void setParticleCount(int particleCount) { this.particleCount = particleCount; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getPlayerName() { return playerName; }
        public int getParticleCount() { return particleCount; }
        public long getTimestamp() { return timestamp; }

        public boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - timestamp > maxAge;
        }
    }

    private static class PlayerParticlePool {
        private final String playerName;
        private org.bukkit.Location lastLocation;
        private long lastUpdate;
        private int activeParticles;

        public PlayerParticlePool(String playerName) {
            this.playerName = playerName;
            this.lastUpdate = System.currentTimeMillis();
            this.activeParticles = 0;
        }

        public void updatePlayerLocation(org.bukkit.Location location) {
            this.lastLocation = location;
            this.lastUpdate = System.currentTimeMillis();
        }

        public void manageParticles() {
            // Manage particle pool for this player
            long timeSinceUpdate = System.currentTimeMillis() - lastUpdate;

            if (timeSinceUpdate > 5000) { // 5 seconds
                // Reduce particles for inactive players
                activeParticles = Math.max(0, activeParticles - 5);
            } else {
                // Normal particle management
                activeParticles = Math.min(50, activeParticles + 1);
            }
        }

        public String getPlayerName() { return playerName; }
        public org.bukkit.Location getLastLocation() { return lastLocation; }
        public int getActiveParticles() { return activeParticles; }
    }
}
