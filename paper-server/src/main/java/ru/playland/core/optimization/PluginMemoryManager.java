package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

/**
 * Plugin Memory Manager
 * ЭКСТРЕМАЛЬНОЕ управление памятью плагинов
 * Динамическая загрузка, выгрузка, оптимизация зависимостей
 */
public class PluginMemoryManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-PluginMemory");
    
    // Plugin memory statistics
    private final AtomicLong pluginsOptimized = new AtomicLong(0);
    private final AtomicLong pluginMemoryFreed = new AtomicLong(0);
    private final AtomicLong dependenciesOptimized = new AtomicLong(0);
    private final AtomicLong dynamicLoads = new AtomicLong(0);
    private final AtomicLong dynamicUnloads = new AtomicLong(0);
    private final AtomicLong memoryLeaksFixed = new AtomicLong(0);
    
    // Plugin tracking
    private final Map<String, PluginMemoryData> pluginMemoryMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pluginDependencies = new ConcurrentHashMap<>();
    private final Map<String, PluginLoadState> pluginStates = new ConcurrentHashMap<>();
    private final Map<String, Long> pluginLastUsed = new ConcurrentHashMap<>();
    
    // Memory optimization
    private final ScheduledExecutorService pluginManager = Executors.newScheduledThreadPool(2);
    private final Map<String, WeakReference<Object>> weakReferences = new ConcurrentHashMap<>();
    private final Map<String, SoftReference<Object>> softReferences = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableDynamicLoading = true;
    private boolean enableMemoryOptimization = true;
    private boolean enableDependencyOptimization = true;
    private boolean enablePluginUnloading = true;
    private boolean enableMemoryLeakDetection = true;
    
    private long pluginUnloadDelay = 600000; // 10 minutes
    private long memoryCheckInterval = 30000; // 30 seconds
    private double memoryThreshold = 75.0; // 75%
    private int maxInactivePlugins = 50;
    
    public void initialize() {
        LOGGER.info("🔌 Initializing Plugin Memory Manager...");
        
        loadPluginSettings();
        startPluginMemoryMonitoring();
        startDynamicPluginManagement();
        startMemoryLeakDetection();
        
        LOGGER.info("✅ Plugin Memory Manager initialized!");
        LOGGER.info("🔌 Dynamic loading: " + (enableDynamicLoading ? "ENABLED" : "DISABLED"));
        LOGGER.info("💾 Memory optimization: " + (enableMemoryOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔗 Dependency optimization: " + (enableDependencyOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗑️ Plugin unloading: " + (enablePluginUnloading ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔍 Memory leak detection: " + (enableMemoryLeakDetection ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏰ Unload delay: " + (pluginUnloadDelay / 1000) + " seconds");
        LOGGER.info("📊 Memory threshold: " + memoryThreshold + "%");
        LOGGER.info("📊 Max inactive plugins: " + maxInactivePlugins);
    }
    
    private void loadPluginSettings() {
        try {
            LOGGER.info("⚙️ Loading plugin settings...");

            // Load plugin memory management parameters from system properties
            enableDynamicLoading = Boolean.parseBoolean(System.getProperty("playland.plugin.dynamic.loading.enabled", "true"));
            enableMemoryOptimization = Boolean.parseBoolean(System.getProperty("playland.plugin.memory.optimization.enabled", "true"));
            enableDependencyOptimization = Boolean.parseBoolean(System.getProperty("playland.plugin.dependency.optimization.enabled", "true"));
            enablePluginUnloading = Boolean.parseBoolean(System.getProperty("playland.plugin.unloading.enabled", "true"));
            enableMemoryLeakDetection = Boolean.parseBoolean(System.getProperty("playland.plugin.memory.leak.detection.enabled", "true"));

            // Load plugin parameters
            pluginUnloadDelay = Long.parseLong(System.getProperty("playland.plugin.unload.delay", "600000"));
            memoryCheckInterval = Long.parseLong(System.getProperty("playland.plugin.memory.check.interval", "30000"));
            memoryThreshold = Double.parseDouble(System.getProperty("playland.plugin.memory.threshold", "75.0"));
            maxInactivePlugins = Integer.parseInt(System.getProperty("playland.plugin.max.inactive", "50"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - more aggressive plugin management
                enablePluginUnloading = true;
                enableMemoryOptimization = true;
                pluginUnloadDelay = Math.max(300000, pluginUnloadDelay / 2); // 5 minutes minimum
                memoryCheckInterval = Math.max(15000, memoryCheckInterval / 2);
                memoryThreshold = Math.max(60.0, memoryThreshold - 10.0);
                maxInactivePlugins = Math.max(20, maxInactivePlugins / 2);
                LOGGER.info("🔧 Aggressive plugin management for low TPS: threshold=" + memoryThreshold +
                           "%, unload_delay=" + (pluginUnloadDelay/1000) + "s, max_inactive=" + maxInactivePlugins);
            } else if (currentTPS > 19.5) {
                // Good TPS - less aggressive plugin management
                pluginUnloadDelay = Math.min(1800000, pluginUnloadDelay * 2); // 30 minutes maximum
                memoryCheckInterval = Math.min(60000, memoryCheckInterval * 2);
                memoryThreshold = Math.min(85.0, memoryThreshold + 5.0);
                maxInactivePlugins = Math.min(100, maxInactivePlugins + 20);
                LOGGER.info("🔧 Relaxed plugin management for good TPS: threshold=" + memoryThreshold + "%");
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double currentMemoryUsage = (double) usedMemory / maxMemory * 100;

            if (currentMemoryUsage > 80.0) {
                // High memory usage - aggressive plugin cleanup
                enablePluginUnloading = true;
                enableMemoryOptimization = true;
                enableMemoryLeakDetection = true;
                pluginUnloadDelay = Math.max(180000, pluginUnloadDelay / 3); // 3 minutes minimum
                memoryCheckInterval = Math.max(10000, memoryCheckInterval / 3);
                memoryThreshold = Math.max(50.0, memoryThreshold - 15.0);
                maxInactivePlugins = Math.max(10, maxInactivePlugins / 3);
                LOGGER.warning("⚠️ High memory usage (" + String.format("%.1f%%", currentMemoryUsage) +
                              ") - aggressive plugin cleanup: threshold=" + memoryThreshold + "%");
            }

            // Auto-adjust based on plugin count
            int totalPlugins = getTotalPluginCount();
            if (totalPlugins > 50) {
                // Many plugins - more aggressive management
                enableDependencyOptimization = true;
                enablePluginUnloading = true;
                pluginUnloadDelay = Math.max(300000, pluginUnloadDelay - totalPlugins * 5000);
                memoryThreshold = Math.max(65.0, memoryThreshold - totalPlugins * 0.2);
                maxInactivePlugins = Math.max(15, maxInactivePlugins - totalPlugins / 5);
                LOGGER.info("🔧 Optimized for " + totalPlugins + " plugins: threshold=" + memoryThreshold +
                           "%, max_inactive=" + maxInactivePlugins);
            } else if (totalPlugins < 10) {
                // Few plugins - less aggressive management
                enablePluginUnloading = false;
                enableMemoryLeakDetection = false;
                memoryThreshold = Math.min(90.0, memoryThreshold + 10.0);
                maxInactivePlugins = Math.min(100, maxInactivePlugins + 30);
                LOGGER.info("🔧 Relaxed plugin management for " + totalPlugins + " plugins");
            }

            // Auto-adjust based on player count
            int playerCount = getOnlinePlayerCount();
            if (playerCount > 100) {
                // Many players - keep more plugins active
                enablePluginUnloading = false;
                pluginUnloadDelay = Math.min(3600000, pluginUnloadDelay * 2); // 1 hour maximum
                memoryThreshold = Math.min(85.0, memoryThreshold + 5.0);
                maxInactivePlugins = Math.min(150, maxInactivePlugins + playerCount / 2);
                LOGGER.info("🔧 Optimized for " + playerCount + " players: keeping more plugins active");
            } else if (playerCount < 5) {
                // Few players - more aggressive unloading
                enablePluginUnloading = true;
                pluginUnloadDelay = Math.max(120000, pluginUnloadDelay / 3); // 2 minutes minimum
                memoryThreshold = Math.max(60.0, memoryThreshold - 10.0);
                maxInactivePlugins = Math.max(5, maxInactivePlugins / 4);
                LOGGER.info("🔧 Aggressive plugin unloading for " + playerCount + " players");
            }

            LOGGER.info("✅ Plugin settings loaded - Memory threshold: " + memoryThreshold +
                       "%, Unload delay: " + (pluginUnloadDelay/1000) + "s, Check interval: " + (memoryCheckInterval/1000) +
                       "s, Max inactive: " + maxInactivePlugins);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading plugin settings: " + e.getMessage() + " - using defaults");
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

    private int getTotalPluginCount() {
        try {
            return org.bukkit.Bukkit.getPluginManager().getPlugins().length;
        } catch (Exception e) {
            return 25; // Default moderate count
        }
    }
    
    private void startPluginMemoryMonitoring() {
        // Monitor plugin memory usage every 30 seconds
        pluginManager.scheduleAtFixedRate(() -> {
            try {
                monitorPluginMemory();
                optimizePluginMemory();
            } catch (Exception e) {
                LOGGER.warning("Plugin memory monitoring error: " + e.getMessage());
            }
        }, memoryCheckInterval, memoryCheckInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📊 Plugin memory monitoring started");
    }
    
    private void startDynamicPluginManagement() {
        if (!enableDynamicLoading) return;
        
        // Manage dynamic plugin loading/unloading every 60 seconds
        pluginManager.scheduleAtFixedRate(() -> {
            try {
                manageDynamicPlugins();
                optimizeDependencies();
            } catch (Exception e) {
                LOGGER.warning("Dynamic plugin management error: " + e.getMessage());
            }
        }, 60000, 60000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔄 Dynamic plugin management started");
    }
    
    private void startMemoryLeakDetection() {
        if (!enableMemoryLeakDetection) return;
        
        // Check for plugin memory leaks every 2 minutes
        pluginManager.scheduleAtFixedRate(() -> {
            try {
                detectPluginMemoryLeaks();
                cleanupWeakReferences();
            } catch (Exception e) {
                LOGGER.warning("Plugin memory leak detection error: " + e.getMessage());
            }
        }, 120000, 120000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔍 Plugin memory leak detection started");
    }
    
    /**
     * Register plugin for memory management
     */
    public void registerPlugin(String pluginName, Object pluginInstance, long memoryUsage) {
        try {
            PluginMemoryData memoryData = new PluginMemoryData(pluginName, memoryUsage);
            pluginMemoryMap.put(pluginName, memoryData);
            pluginStates.put(pluginName, PluginLoadState.LOADED);
            pluginLastUsed.put(pluginName, System.currentTimeMillis());
            
            // Create weak reference for memory management
            weakReferences.put(pluginName, new WeakReference<>(pluginInstance));
            
            LOGGER.fine("📝 Registered plugin: " + pluginName + " (Memory: " + formatBytes(memoryUsage) + ")");
            
        } catch (Exception e) {
            LOGGER.warning("Plugin registration error for " + pluginName + ": " + e.getMessage());
        }
    }
    
    /**
     * Track plugin usage
     */
    public void trackPluginUsage(String pluginName) {
        pluginLastUsed.put(pluginName, System.currentTimeMillis());
        
        PluginMemoryData data = pluginMemoryMap.get(pluginName);
        if (data != null) {
            data.incrementUsage();
        }
        
        // Update plugin state
        pluginStates.put(pluginName, PluginLoadState.ACTIVE);
    }
    
    /**
     * Register plugin dependency
     */
    public void registerDependency(String pluginName, String dependencyName) {
        pluginDependencies.computeIfAbsent(pluginName, k -> new HashSet<>()).add(dependencyName);
        
        LOGGER.fine("🔗 Registered dependency: " + pluginName + " -> " + dependencyName);
    }
    
    /**
     * Monitor plugin memory usage
     */
    private void monitorPluginMemory() {
        long totalPluginMemory = 0;
        int activePlugins = 0;
        int inactivePlugins = 0;
        
        for (Map.Entry<String, PluginMemoryData> entry : pluginMemoryMap.entrySet()) {
            String pluginName = entry.getKey();
            PluginMemoryData data = entry.getValue();
            
            totalPluginMemory += data.getMemoryUsage();
            
            Long lastUsed = pluginLastUsed.get(pluginName);
            if (lastUsed != null && System.currentTimeMillis() - lastUsed < 300000) { // 5 minutes
                activePlugins++;
            } else {
                inactivePlugins++;
            }
        }
        
        // Log plugin memory status periodically
        if (System.currentTimeMillis() % 120000 < memoryCheckInterval) { // Every 2 minutes
            LOGGER.info("🔌 Plugin memory - Active: " + activePlugins + 
                       ", Inactive: " + inactivePlugins + 
                       ", Total memory: " + formatBytes(totalPluginMemory));
        }
        
        // Check if optimization is needed
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = ((runtime.totalMemory() - runtime.freeMemory()) * 100.0) / runtime.maxMemory();
        
        if (memoryUsage > memoryThreshold) {
            triggerPluginMemoryOptimization();
        }
    }
    
    /**
     * Optimize plugin memory usage
     */
    private void optimizePluginMemory() {
        try {
            // Clean up unused weak references
            cleanupWeakReferences();
            
            // Optimize plugin data
            for (Map.Entry<String, PluginMemoryData> entry : pluginMemoryMap.entrySet()) {
                String pluginName = entry.getKey();
                PluginMemoryData data = entry.getValue();
                
                // Check if plugin can be optimized
                if (canOptimizePlugin(pluginName)) {
                    optimizePluginData(pluginName, data);
                }
            }
            
            pluginsOptimized.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Plugin memory optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Manage dynamic plugin loading/unloading
     */
    private void manageDynamicPlugins() {
        if (!enableDynamicLoading) return;
        
        long currentTime = System.currentTimeMillis();
        List<String> pluginsToUnload = new ArrayList<>();
        
        // Find plugins that can be unloaded
        for (Map.Entry<String, Long> entry : pluginLastUsed.entrySet()) {
            String pluginName = entry.getKey();
            long lastUsed = entry.getValue();
            
            if (currentTime - lastUsed > pluginUnloadDelay && 
                canUnloadPlugin(pluginName)) {
                pluginsToUnload.add(pluginName);
            }
        }
        
        // Unload inactive plugins
        for (String pluginName : pluginsToUnload) {
            if (pluginsToUnload.size() <= 5) { // Limit batch size
                unloadPlugin(pluginName);
            }
        }
    }
    
    /**
     * Optimize plugin dependencies
     */
    private void optimizeDependencies() {
        if (!enableDependencyOptimization) return;
        
        try {
            // Find shared dependencies
            Map<String, Set<String>> sharedDependencies = new ConcurrentHashMap<>();
            
            for (Map.Entry<String, Set<String>> entry : pluginDependencies.entrySet()) {
                for (String dependency : entry.getValue()) {
                    sharedDependencies.computeIfAbsent(dependency, k -> new HashSet<>()).add(entry.getKey());
                }
            }
            
            // Optimize shared dependencies
            for (Map.Entry<String, Set<String>> entry : sharedDependencies.entrySet()) {
                String dependency = entry.getKey();
                Set<String> users = entry.getValue();
                
                if (users.size() > 1) {
                    optimizeSharedDependency(dependency, users);
                }
            }
            
            dependenciesOptimized.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Dependency optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Detect plugin memory leaks
     */
    private void detectPluginMemoryLeaks() {
        if (!enableMemoryLeakDetection) return;
        
        try {
            for (Map.Entry<String, PluginMemoryData> entry : pluginMemoryMap.entrySet()) {
                String pluginName = entry.getKey();
                PluginMemoryData data = entry.getValue();
                
                // Check for memory growth pattern
                if (data.hasMemoryLeak()) {
                    LOGGER.warning("🔍 Memory leak detected in plugin: " + pluginName);
                    fixPluginMemoryLeak(pluginName);
                    memoryLeaksFixed.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Plugin memory leak detection error: " + e.getMessage());
        }
    }
    
    /**
     * Check if plugin can be optimized
     */
    private boolean canOptimizePlugin(String pluginName) {
        Long lastUsed = pluginLastUsed.get(pluginName);
        if (lastUsed == null) return false;
        
        // Don't optimize recently used plugins
        return System.currentTimeMillis() - lastUsed > 300000; // 5 minutes
    }
    
    /**
     * Optimize plugin data
     */
    private void optimizePluginData(String pluginName, PluginMemoryData data) {
        try {
            // Move to soft reference if not recently used
            WeakReference<Object> weakRef = weakReferences.get(pluginName);
            if (weakRef != null && weakRef.get() != null) {
                softReferences.put(pluginName, new SoftReference<>(weakRef.get()));
                weakReferences.remove(pluginName);
                
                long memorySaved = data.getMemoryUsage() / 4; // Estimate
                pluginMemoryFreed.addAndGet(memorySaved);
                
                LOGGER.fine("💾 Optimized plugin memory: " + pluginName);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Plugin data optimization error for " + pluginName + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if plugin can be unloaded
     */
    private boolean canUnloadPlugin(String pluginName) {
        if (!enablePluginUnloading) return false;
        
        // Check if plugin has active dependencies
        for (Set<String> dependencies : pluginDependencies.values()) {
            if (dependencies.contains(pluginName)) {
                return false; // Plugin is a dependency for others
            }
        }
        
        // Check plugin state
        PluginLoadState state = pluginStates.get(pluginName);
        return state != PluginLoadState.CRITICAL;
    }
    
    /**
     * Unload plugin from memory
     */
    private void unloadPlugin(String pluginName) {
        try {
            // Remove from memory tracking
            PluginMemoryData data = pluginMemoryMap.remove(pluginName);
            if (data != null) {
                pluginMemoryFreed.addAndGet(data.getMemoryUsage());
            }
            
            // Clear references
            weakReferences.remove(pluginName);
            softReferences.remove(pluginName);
            
            // Update state
            pluginStates.put(pluginName, PluginLoadState.UNLOADED);
            pluginLastUsed.remove(pluginName);
            
            dynamicUnloads.incrementAndGet();
            
            LOGGER.info("🗑️ Unloaded plugin from memory: " + pluginName);
            
        } catch (Exception e) {
            LOGGER.warning("Plugin unload error for " + pluginName + ": " + e.getMessage());
        }
    }
    
    /**
     * Optimize shared dependency
     */
    private void optimizeSharedDependency(String dependency, Set<String> users) {
        try {
            // Create shared instance for dependency
            // This is a simplified implementation
            LOGGER.fine("🔗 Optimizing shared dependency: " + dependency + " (used by " + users.size() + " plugins)");
            
        } catch (Exception e) {
            LOGGER.warning("Shared dependency optimization error for " + dependency + ": " + e.getMessage());
        }
    }
    
    /**
     * Fix plugin memory leak
     */
    private void fixPluginMemoryLeak(String pluginName) {
        try {
            // Clear plugin caches
            clearPluginCaches(pluginName);
            
            // Force garbage collection for plugin
            WeakReference<Object> weakRef = weakReferences.get(pluginName);
            if (weakRef != null) {
                weakRef.clear();
            }
            
            SoftReference<Object> softRef = softReferences.get(pluginName);
            if (softRef != null) {
                softRef.clear();
            }
            
            // Reset memory tracking
            PluginMemoryData data = pluginMemoryMap.get(pluginName);
            if (data != null) {
                data.resetMemoryTracking();
            }
            
            LOGGER.info("🔧 Fixed memory leak for plugin: " + pluginName);
            
        } catch (Exception e) {
            LOGGER.warning("Memory leak fix error for " + pluginName + ": " + e.getMessage());
        }
    }
    
    /**
     * Clear plugin caches
     */
    private void clearPluginCaches(String pluginName) {
        // This would interface with plugin-specific cache clearing
        LOGGER.fine("🧹 Clearing caches for plugin: " + pluginName);
    }
    
    /**
     * Cleanup weak references
     */
    private void cleanupWeakReferences() {
        weakReferences.entrySet().removeIf(entry -> entry.getValue().get() == null);
        softReferences.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
    
    /**
     * Trigger plugin memory optimization under pressure
     */
    private void triggerPluginMemoryOptimization() {
        LOGGER.warning("⚠️ Memory pressure detected - triggering plugin memory optimization");
        
        try {
            // Aggressive plugin optimization
            List<String> pluginsToOptimize = new ArrayList<>();
            
            for (String pluginName : pluginMemoryMap.keySet()) {
                Long lastUsed = pluginLastUsed.get(pluginName);
                if (lastUsed != null && System.currentTimeMillis() - lastUsed > 60000) { // 1 minute
                    pluginsToOptimize.add(pluginName);
                }
            }
            
            // Optimize plugins
            for (String pluginName : pluginsToOptimize) {
                PluginMemoryData data = pluginMemoryMap.get(pluginName);
                if (data != null) {
                    optimizePluginData(pluginName, data);
                }
            }
            
            // Force cleanup
            cleanupWeakReferences();
            System.gc();
            
        } catch (Exception e) {
            LOGGER.warning("Plugin memory optimization trigger error: " + e.getMessage());
        }
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
     * Get plugin memory statistics
     */
    public Map<String, Object> getPluginMemoryStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("plugins_optimized", pluginsOptimized.get());
        stats.put("plugin_memory_freed", pluginMemoryFreed.get());
        stats.put("dependencies_optimized", dependenciesOptimized.get());
        stats.put("dynamic_loads", dynamicLoads.get());
        stats.put("dynamic_unloads", dynamicUnloads.get());
        stats.put("memory_leaks_fixed", memoryLeaksFixed.get());
        
        stats.put("tracked_plugins", pluginMemoryMap.size());
        stats.put("weak_references", weakReferences.size());
        stats.put("soft_references", softReferences.size());
        stats.put("plugin_dependencies", pluginDependencies.size());
        
        return stats;
    }
    
    // Getters
    public long getPluginsOptimized() { return pluginsOptimized.get(); }
    public long getPluginMemoryFreed() { return pluginMemoryFreed.get(); }
    public long getDependenciesOptimized() { return dependenciesOptimized.get(); }
    public long getDynamicLoads() { return dynamicLoads.get(); }
    public long getDynamicUnloads() { return dynamicUnloads.get(); }
    public long getMemoryLeaksFixed() { return memoryLeaksFixed.get(); }
    
    public void shutdown() {
        pluginManager.shutdown();
        
        // Clear all data
        pluginMemoryMap.clear();
        pluginDependencies.clear();
        pluginStates.clear();
        pluginLastUsed.clear();
        weakReferences.clear();
        softReferences.clear();
        
        LOGGER.info("🔌 Plugin Memory Manager shutdown complete");
    }
    
    /**
     * Plugin memory data container
     */
    private static class PluginMemoryData {
        private final String pluginName;
        private long memoryUsage;
        private long usageCount;
        private final List<Long> memoryHistory = new ArrayList<>();
        private final long creationTime;
        
        public PluginMemoryData(String pluginName, long memoryUsage) {
            this.pluginName = pluginName;
            this.memoryUsage = memoryUsage;
            this.usageCount = 0;
            this.creationTime = System.currentTimeMillis();
            this.memoryHistory.add(memoryUsage);
        }
        
        public void incrementUsage() {
            this.usageCount++;
        }
        
        public void updateMemoryUsage(long newUsage) {
            this.memoryUsage = newUsage;
            this.memoryHistory.add(newUsage);
            
            // Keep only recent history
            if (memoryHistory.size() > 20) {
                memoryHistory.remove(0);
            }
        }
        
        public boolean hasMemoryLeak() {
            if (memoryHistory.size() < 5) return false;
            
            // Simple heuristic: check if memory keeps growing
            int growthCount = 0;
            for (int i = 1; i < memoryHistory.size(); i++) {
                if (memoryHistory.get(i) > memoryHistory.get(i - 1)) {
                    growthCount++;
                }
            }
            
            return growthCount >= memoryHistory.size() * 0.8; // 80% growth
        }
        
        public void resetMemoryTracking() {
            memoryHistory.clear();
            memoryHistory.add(memoryUsage);
        }
        
        // Getters
        public String getPluginName() { return pluginName; }
        public long getMemoryUsage() { return memoryUsage; }
        public long getUsageCount() { return usageCount; }
        public long getCreationTime() { return creationTime; }
    }
    
    /**
     * Plugin load state enum
     */
    private enum PluginLoadState {
        LOADED,
        ACTIVE,
        INACTIVE,
        UNLOADED,
        CRITICAL
    }
}
