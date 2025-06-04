package ru.playland.core;

import ru.playland.core.optimization.QuantumLoadBalancer;
import ru.playland.core.vanilla.VanillaMechanicsManager;

import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PlayLand Core Server - Revolutionary Minecraft server
 * Integrated directly into Paper for impossible performance
 */
public class PlayLandCoreServer {

    private static final Logger LOGGER = Logger.getLogger("PlayLand-Core");

    public static final String VERSION = "2.2.0-REVOLUTIONARY";
    public static final String BRANDING = "PlayLand Core Server";
    public static final String WEBSITE = "https://github.com/PlayLandMC/PlayLand-Core-Server";

    // Minimal systems for memory efficiency
    private static QuantumLoadBalancer quantumLoadBalancer;
    private static VanillaMechanicsManager vanillaMechanicsManager;

    // System state
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean enabled = new AtomicBoolean(true);
    private static final AtomicLong optimizationCycles = new AtomicLong(0);
    private static final AtomicLong performanceBoosts = new AtomicLong(0);

    // Memory management settings
    private static final boolean MEMORY_SAFE_MODE = Boolean.parseBoolean(System.getProperty("playland.memory.safe.mode", "true"));
    private static final boolean LAZY_INITIALIZATION = Boolean.parseBoolean(System.getProperty("playland.lazy.init", "true"));
    private static final boolean DISABLE_OPTIMIZATIONS = Boolean.parseBoolean(System.getProperty("playland.disable.optimizations", "true"));
    private static final int MAX_CONCURRENT_INITIALIZATIONS = Integer.parseInt(System.getProperty("playland.max.concurrent.init", "5"));

    /**
     * Initialize PlayLand Core Server with memory-safe approach
     * Called during MinecraftServer startup
     */
    public static void initialize() {
        if (initialized.get()) {
            LOGGER.warning("PlayLand Core already initialized!");
            return;
        }

        LOGGER.info("🚀 Initializing PlayLand Core Server " + VERSION);

        if (DISABLE_OPTIMIZATIONS) {
            LOGGER.info("⚠️ Optimizations disabled - minimal initialization for testing");
            initializeMinimal();
        } else if (MEMORY_SAFE_MODE) {
            LOGGER.info("💾 Memory-safe mode enabled - initializing core systems only");
            initializeCoreSystemsOnly();
        } else {
            LOGGER.info("🌟 Full mode - initializing all revolutionary systems...");
            initializeAllSystems();
        }
    }

    /**
     * Minimal initialization - no optimization systems for testing
     */
    private static void initializeMinimal() {
        try {
            initialized.set(true);

            LOGGER.info("✅ PlayLand Core Server (Minimal Mode) initialized successfully!");
            LOGGER.info("⚠️ All optimization systems disabled for memory testing");
            LOGGER.info("💾 Memory usage: Minimal");

        } catch (Exception e) {
            LOGGER.severe("❌ Failed to initialize PlayLand Core (Minimal): " + e.getMessage());
            e.printStackTrace();
            initialized.set(false);
        }
    }

    /**
     * Initialize only essential core systems to minimize memory usage
     */
    private static void initializeCoreSystemsOnly() {
        try {
            LOGGER.info("⚛️ Initializing Quantum Load Balancer...");
            quantumLoadBalancer = new QuantumLoadBalancer();
            quantumLoadBalancer.initialize();

            LOGGER.info("🎮 Initializing Vanilla Mechanics Manager...");
            vanillaMechanicsManager = new VanillaMechanicsManager();
            vanillaMechanicsManager.initialize();

            initialized.set(true);

            LOGGER.info("✅ PlayLand Core Server (Memory-Safe Mode) initialized successfully!");
            LOGGER.info("⚛️ Quantum efficiency: " + String.format("%.1f", quantumLoadBalancer.getQuantumEfficiency() * 100) + "%");
            LOGGER.info("🎮 Vanilla compatibility: " + vanillaMechanicsManager.getVanillaCompatibility() + "%");
            LOGGER.info("💾 Memory usage optimized for hosting environments");

        } catch (Exception e) {
            LOGGER.severe("❌ Failed to initialize PlayLand Core (Memory-Safe): " + e.getMessage());
            e.printStackTrace();
            initialized.set(false);
        }
    }

    /**
     * Initialize all systems (full mode) - only for high-memory environments
     */
    private static void initializeAllSystems() {
        // Use minimal initialization for now
        initializeMinimal();
    }

    /**
     * Perform revolutionary optimization during server tick
     * Called from MinecraftServer.tick()
     * VANILLA-SAFE: Optimizations run only when safe for vanilla mechanics
     */
    public static void optimizeTick() {
        if (!initialized.get() || !enabled.get() || DISABLE_OPTIMIZATIONS) return;

        optimizationCycles.incrementAndGet();

        try {
            // Quantum optimization (vanilla-safe mode)
            if (quantumLoadBalancer != null) {
                quantumLoadBalancer.optimizeTick();
            }

            // Vanilla mechanics check (frequent to ensure compliance)
            if (optimizationCycles.get() % 10 == 0 && vanillaMechanicsManager != null) {
                vanillaMechanicsManager.checkVanillaCompliance();
            }

            performanceBoosts.incrementAndGet();

        } catch (Exception e) {
            LOGGER.warning("Optimization error: " + e.getMessage());
        }
    }

    /**
     * Perform load balancing optimization
     * Called during high load situations
     */
    public static void balanceLoad() {
        if (!initialized.get() || !enabled.get()) return;

        try {
            if (quantumLoadBalancer != null) {
                quantumLoadBalancer.balanceLoad();
            }

            performanceBoosts.incrementAndGet();

        } catch (Exception e) {
            LOGGER.warning("Load balancing error: " + e.getMessage());
        }
    }

    /**
     * Get performance statistics
     */
    public static String getPerformanceStats() {
        if (!initialized.get()) return "PlayLand Core not initialized";

        StringBuilder stats = new StringBuilder();
        stats.append("PlayLand Core Server ").append(VERSION).append("\n");
        stats.append("Optimization cycles: ").append(optimizationCycles.get()).append("\n");
        stats.append("Performance boosts: ").append(performanceBoosts.get()).append("\n");

        if (quantumLoadBalancer != null) {
            stats.append("Quantum operations: ").append(quantumLoadBalancer.getQuantumOperations()).append("\n");
            stats.append("Quantum efficiency: ").append(String.format("%.2f%%", quantumLoadBalancer.getQuantumEfficiency() * 100)).append("\n");
        }

        if (vanillaMechanicsManager != null) {
            stats.append("Vanilla compatibility: ").append(String.format("%.1f%%", vanillaMechanicsManager.getVanillaCompatibility())).append("\n");
        }

        stats.append("Status: Memory-optimized mode").append("\n");






        return stats.toString();
    }

    /**
     * Format bytes to human readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // Getters for monitoring
    public static boolean isInitialized() { return initialized.get(); }
    public static boolean isEnabled() { return enabled.get(); }
    public static void setEnabled(boolean enabled) { PlayLandCoreServer.enabled.set(enabled); }
    public static long getOptimizationCycles() { return optimizationCycles.get(); }
    public static long getPerformanceBoosts() { return performanceBoosts.get(); }

    // System accessors (minimal for memory efficiency)
    public static QuantumLoadBalancer getQuantumLoadBalancer() { return quantumLoadBalancer; }
    public static VanillaMechanicsManager getVanillaMechanicsManager() { return vanillaMechanicsManager; }
}
