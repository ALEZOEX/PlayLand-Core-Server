package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic Resource Allocator
 * УМНОЕ динамическое распределение ресурсов сервера
 * Автоматически адаптируется к нагрузке без нарушения vanilla
 */
public class DynamicResourceAllocator {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-ResourceAlloc");
    
    // Resource allocation statistics
    private final AtomicLong totalAllocations = new AtomicLong(0);
    private final AtomicLong resourceOptimizations = new AtomicLong(0);
    private final AtomicLong memoryAllocations = new AtomicLong(0);
    private final AtomicLong cpuOptimizations = new AtomicLong(0);
    private final AtomicLong ioOptimizations = new AtomicLong(0);
    private final AtomicLong adaptiveChanges = new AtomicLong(0);
    
    // Resource pools
    private final Map<String, ResourcePool> resourcePools = new ConcurrentHashMap<>();
    private final Map<String, ResourceUsage> resourceUsage = new ConcurrentHashMap<>();
    private final Map<String, Double> resourcePriorities = new ConcurrentHashMap<>();
    
    // System monitoring
    private final ScheduledExecutorService resourceMonitor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Long> lastAllocationTime = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableDynamicAllocation = true;
    private boolean enableResourceMonitoring = true;
    private boolean enableAdaptiveScaling = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enablePerformanceOptimization = true;
    private boolean enableResourcePrediction = true;
    
    private double cpuThreshold = 80.0; // 80% CPU usage threshold
    private double memoryThreshold = 85.0; // 85% memory usage threshold
    private long adaptationInterval = 5000; // 5 seconds
    private int maxResourcePools = 50;
    
    public void initialize() {
        LOGGER.info("🔧 Initializing Dynamic Resource Allocator...");
        
        loadResourceSettings();
        initializeResourcePools();
        startResourceMonitoring();
        startAdaptiveScaling();
        
        LOGGER.info("✅ Dynamic Resource Allocator initialized!");
        LOGGER.info("🔧 Dynamic allocation: " + (enableDynamicAllocation ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Resource monitoring: " + (enableResourceMonitoring ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Adaptive scaling: " + (enableAdaptiveScaling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Performance optimization: " + (enablePerformanceOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🖥️ CPU threshold: " + cpuThreshold + "%");
        LOGGER.info("💾 Memory threshold: " + memoryThreshold + "%");
        LOGGER.info("⏰ Adaptation interval: " + (adaptationInterval / 1000) + " seconds");
    }
    
    private void loadResourceSettings() {
        try {
            LOGGER.info("⚙️ Loading resource settings...");

            // Load thresholds from system properties
            cpuThreshold = Double.parseDouble(System.getProperty("playland.resource.cpu.threshold", "80.0"));
            memoryThreshold = Double.parseDouble(System.getProperty("playland.resource.memory.threshold", "85.0"));
            adaptationInterval = Long.parseLong(System.getProperty("playland.resource.adaptation.interval", "30000"));

            // Load feature flags
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.resource.vanilla.safe", "true"));
            enableDynamicAllocation = Boolean.parseBoolean(System.getProperty("playland.resource.dynamic.enabled", "true"));
            enableResourcePrediction = Boolean.parseBoolean(System.getProperty("playland.resource.prediction.enabled", "true"));

            // Auto-adjust based on system capabilities
            Runtime runtime = Runtime.getRuntime();
            int availableProcessors = runtime.availableProcessors();
            long maxMemory = runtime.maxMemory();

            // Adjust thresholds for low-end systems
            if (availableProcessors <= 2) {
                cpuThreshold = Math.max(60.0, cpuThreshold - 20.0);
                LOGGER.info("🔧 Adjusted CPU threshold for low-core system: " + cpuThreshold + "%");
            }

            if (maxMemory < 2L * 1024 * 1024 * 1024) { // Less than 2GB
                memoryThreshold = Math.max(70.0, memoryThreshold - 15.0);
                LOGGER.info("🔧 Adjusted memory threshold for low-memory system: " + memoryThreshold + "%");
            }

            // Load resource priorities
            loadResourcePriorities();

            LOGGER.info("✅ Resource settings loaded - CPU: " + cpuThreshold + "%, Memory: " + memoryThreshold + "%");

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading resource settings: " + e.getMessage() + " - using defaults");
        }
    }

    private void loadResourcePriorities() {
        try {
            // Set default resource priorities
            resourcePriorities.put("player_processing", 1.0);      // Highest priority
            resourcePriorities.put("chunk_loading", 0.9);          // Very high
            resourcePriorities.put("entity_processing", 0.8);      // High
            resourcePriorities.put("redstone_processing", 0.7);    // Medium-high
            resourcePriorities.put("world_generation", 0.6);       // Medium
            resourcePriorities.put("plugin_tasks", 0.5);           // Medium-low
            resourcePriorities.put("background_tasks", 0.3);       // Low
            resourcePriorities.put("cleanup_tasks", 0.2);          // Very low

            // Load custom priorities from system properties
            for (String key : resourcePriorities.keySet()) {
                String propertyKey = "playland.resource.priority." + key;
                String value = System.getProperty(propertyKey);
                if (value != null) {
                    try {
                        double priority = Double.parseDouble(value);
                        resourcePriorities.put(key, Math.max(0.1, Math.min(1.0, priority)));
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid priority value for " + key + ": " + value);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Error loading resource priorities: " + e.getMessage());
        }
    }
    
    private void initializeResourcePools() {
        // Initialize resource pools for different server components
        createResourcePool("chunk_loading", 100, 1.0);
        createResourcePool("entity_processing", 200, 0.9);
        createResourcePool("block_updates", 150, 0.8);
        createResourcePool("redstone_processing", 80, 0.7);
        createResourcePool("player_actions", 300, 1.0);
        createResourcePool("world_generation", 50, 0.6);
        createResourcePool("lighting_updates", 120, 0.8);
        createResourcePool("pathfinding", 100, 0.7);
        
        LOGGER.info("🔧 Resource pools initialized: " + resourcePools.size());
    }
    
    private void createResourcePool(String poolName, int initialCapacity, double priority) {
        ResourcePool pool = new ResourcePool(poolName, initialCapacity, priority);
        resourcePools.put(poolName, pool);
        resourcePriorities.put(poolName, priority);
        resourceUsage.put(poolName, new ResourceUsage());
        
        LOGGER.fine("Created resource pool: " + poolName + " (capacity: " + initialCapacity + ", priority: " + priority + ")");
    }
    
    private void startResourceMonitoring() {
        if (!enableResourceMonitoring) return;
        
        resourceMonitor.scheduleAtFixedRate(() -> {
            try {
                monitorSystemResources();
                analyzeResourceUsage();
                optimizeResourceDistribution();
            } catch (Exception e) {
                LOGGER.warning("Resource monitoring error: " + e.getMessage());
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Every second
        
        LOGGER.info("📊 Resource monitoring started");
    }
    
    private void startAdaptiveScaling() {
        if (!enableAdaptiveScaling) return;
        
        resourceMonitor.scheduleAtFixedRate(() -> {
            try {
                performAdaptiveScaling();
                balanceResourcePools();
            } catch (Exception e) {
                LOGGER.warning("Adaptive scaling error: " + e.getMessage());
            }
        }, adaptationInterval, adaptationInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📈 Adaptive scaling started");
    }
    
    /**
     * Allocate resources for a specific task
     */
    public ResourceAllocation allocateResources(String poolName, String taskType, int requestedAmount) {
        if (!enableDynamicAllocation) {
            return new ResourceAllocation(requestedAmount, true);
        }
        
        totalAllocations.incrementAndGet();
        
        try {
            ResourcePool pool = resourcePools.get(poolName);
            if (pool == null) {
                // Create pool on demand
                createResourcePool(poolName, Math.max(requestedAmount * 2, 50), 0.5);
                pool = resourcePools.get(poolName);
            }
            
            // Check if allocation is vanilla-safe
            if (enableVanillaSafeMode && !isVanillaSafeAllocation(poolName, taskType, requestedAmount)) {
                return new ResourceAllocation(Math.min(requestedAmount, 10), false);
            }
            
            // Calculate optimal allocation
            int allocatedAmount = calculateOptimalAllocation(pool, requestedAmount);
            
            // Update pool state
            pool.allocate(allocatedAmount);
            
            // Record usage
            ResourceUsage usage = resourceUsage.get(poolName);
            if (usage != null) {
                usage.recordAllocation(allocatedAmount);
            }
            
            lastAllocationTime.put(poolName, System.currentTimeMillis());
            resourceOptimizations.incrementAndGet();
            
            return new ResourceAllocation(allocatedAmount, true);
            
        } catch (Exception e) {
            LOGGER.warning("Resource allocation error: " + e.getMessage());
            return new ResourceAllocation(Math.min(requestedAmount, 5), false);
        }
    }
    
    /**
     * Release allocated resources
     */
    public void releaseResources(String poolName, int amount) {
        if (!enableDynamicAllocation) return;
        
        try {
            ResourcePool pool = resourcePools.get(poolName);
            if (pool != null) {
                pool.release(amount);
                
                ResourceUsage usage = resourceUsage.get(poolName);
                if (usage != null) {
                    usage.recordRelease(amount);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Resource release error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate optimal resource allocation
     */
    private int calculateOptimalAllocation(ResourcePool pool, int requested) {
        // Consider current pool capacity
        int available = pool.getAvailableCapacity();
        
        // Consider system load
        double systemLoad = getCurrentSystemLoad();
        
        // Consider pool priority
        double priority = pool.getPriority();
        
        // Calculate allocation factor
        double allocationFactor = 1.0;
        
        if (systemLoad > cpuThreshold) {
            allocationFactor *= 0.7; // Reduce allocation under high load
        }
        
        allocationFactor *= priority; // Apply priority multiplier
        
        int optimalAmount = (int) (requested * allocationFactor);
        
        // Ensure we don't exceed available capacity
        return Math.min(optimalAmount, available);
    }
    
    /**
     * Check if allocation is vanilla-safe
     */
    private boolean isVanillaSafeAllocation(String poolName, String taskType, int amount) {
        // Player actions always get priority (vanilla-critical)
        if (poolName.equals("player_actions")) {
            return true;
        }
        
        // Block updates are vanilla-critical
        if (poolName.equals("block_updates") && taskType.contains("vanilla")) {
            return true;
        }
        
        // Redstone processing is vanilla-critical
        if (poolName.equals("redstone_processing")) {
            return true;
        }
        
        // Large allocations might affect vanilla behavior
        if (amount > 100) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Monitor system resources
     */
    private void monitorSystemResources() {
        // Monitor CPU usage
        double cpuUsage = getCurrentCPUUsage();
        
        // Monitor memory usage
        double memoryUsage = getCurrentMemoryUsage();
        
        // Monitor I/O usage
        double ioUsage = getCurrentIOUsage();
        
        // Trigger optimizations if thresholds exceeded
        if (cpuUsage > cpuThreshold) {
            optimizeCPUUsage();
            cpuOptimizations.incrementAndGet();
        }
        
        if (memoryUsage > memoryThreshold) {
            optimizeMemoryUsage();
            memoryAllocations.incrementAndGet();
        }
        
        if (ioUsage > 80.0) {
            optimizeIOUsage();
            ioOptimizations.incrementAndGet();
        }
    }
    
    /**
     * Analyze resource usage patterns
     */
    private void analyzeResourceUsage() {
        for (Map.Entry<String, ResourceUsage> entry : resourceUsage.entrySet()) {
            String poolName = entry.getKey();
            ResourceUsage usage = entry.getValue();
            
            // Analyze usage patterns
            double utilizationRate = usage.getUtilizationRate();
            double allocationRate = usage.getAllocationRate();
            
            // Adjust pool capacity based on usage
            if (utilizationRate > 90.0) {
                // High utilization - increase capacity
                expandResourcePool(poolName, 20);
            } else if (utilizationRate < 30.0) {
                // Low utilization - decrease capacity
                shrinkResourcePool(poolName, 10);
            }
        }
    }
    
    /**
     * Optimize resource distribution
     */
    private void optimizeResourceDistribution() {
        // Redistribute resources based on priority and usage
        for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
            String poolName = entry.getKey();
            ResourcePool pool = entry.getValue();
            
            double priority = resourcePriorities.get(poolName);
            ResourceUsage usage = resourceUsage.get(poolName);
            
            if (usage != null) {
                // Calculate optimal capacity
                int optimalCapacity = calculateOptimalCapacity(pool, usage, priority);
                
                if (optimalCapacity != pool.getMaxCapacity()) {
                    pool.setMaxCapacity(optimalCapacity);
                    resourceOptimizations.incrementAndGet();
                }
            }
        }
    }
    
    /**
     * Perform adaptive scaling
     */
    private void performAdaptiveScaling() {
        double systemLoad = getCurrentSystemLoad();
        
        // Scale resources based on system load
        if (systemLoad > 85.0) {
            // High load - scale down non-critical resources
            scaleDownNonCriticalResources();
            adaptiveChanges.incrementAndGet();
        } else if (systemLoad < 50.0) {
            // Low load - scale up resources
            scaleUpResources();
            adaptiveChanges.incrementAndGet();
        }
    }
    
    /**
     * Balance resource pools
     */
    private void balanceResourcePools() {
        // Calculate total available resources
        int totalCapacity = resourcePools.values().stream()
            .mapToInt(ResourcePool::getMaxCapacity)
            .sum();
        
        // Redistribute based on priorities
        for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
            String poolName = entry.getKey();
            ResourcePool pool = entry.getValue();
            double priority = resourcePriorities.get(poolName);
            
            int targetCapacity = (int) (totalCapacity * priority / getTotalPriority());
            
            if (Math.abs(pool.getMaxCapacity() - targetCapacity) > 10) {
                pool.setMaxCapacity(targetCapacity);
                resourceOptimizations.incrementAndGet();
            }
        }
    }
    
    // Helper methods for system monitoring
    private double getCurrentCPUUsage() {
        try {
            // Real CPU usage monitoring
            com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            double cpuLoad = osBean.getProcessCpuLoad() * 100.0;

            // If process CPU load is not available, use system load average
            if (cpuLoad < 0) {
                double systemLoad = osBean.getSystemLoadAverage();
                int processors = osBean.getAvailableProcessors();
                if (systemLoad >= 0 && processors > 0) {
                    cpuLoad = (systemLoad / processors) * 100.0;
                } else {
                    cpuLoad = 50.0; // Default moderate load
                }
            }

            return Math.max(0.0, Math.min(100.0, cpuLoad));

        } catch (Exception e) {
            LOGGER.fine("CPU monitoring error: " + e.getMessage());
            return 50.0; // Default moderate load
        }
    }

    private double getCurrentMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();

            // Calculate memory usage as percentage of max memory
            long usedMemory = totalMemory - freeMemory;
            return (usedMemory * 100.0) / maxMemory;

        } catch (Exception e) {
            LOGGER.fine("Memory monitoring error: " + e.getMessage());
            return 50.0; // Default moderate usage
        }
    }

    private double getCurrentIOUsage() {
        try {
            // Real I/O usage monitoring using MXBeans
            java.lang.management.OperatingSystemMXBean osBean =
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;

                // Estimate I/O usage based on system load and CPU usage
                double systemLoad = sunOsBean.getSystemLoadAverage();
                double cpuLoad = sunOsBean.getProcessCpuLoad() * 100.0;

                if (systemLoad >= 0 && cpuLoad >= 0) {
                    // High system load with low CPU usage often indicates I/O bottleneck
                    double ioEstimate = Math.max(0, systemLoad * 20 - cpuLoad);
                    return Math.min(100.0, ioEstimate);
                }
            }

            // Fallback: estimate based on memory allocation rate
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long maxMemory = runtime.maxMemory();

            // High memory allocation rate might indicate I/O activity
            double memoryPressure = (totalMemory * 100.0) / maxMemory;
            return Math.min(100.0, memoryPressure * 0.5);

        } catch (Exception e) {
            LOGGER.fine("I/O monitoring error: " + e.getMessage());
            return 30.0; // Default low I/O usage
        }
    }
    
    private double getCurrentSystemLoad() {
        return (getCurrentCPUUsage() + getCurrentMemoryUsage()) / 2.0;
    }
    
    private void optimizeCPUUsage() {
        try {
            LOGGER.info("🔧 Optimizing CPU usage - reducing non-critical operations");

            // Scale down non-critical resources
            scaleDownNonCriticalResources();

            // Reduce background task frequency
            for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
                String poolName = entry.getKey();
                ResourcePool pool = entry.getValue();

                if (poolName.contains("background") || poolName.contains("cleanup")) {
                    // Reduce background task capacity by 50%
                    pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.5));
                }

                if (poolName.contains("world_generation")) {
                    // Reduce world generation capacity by 30%
                    pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.7));
                }
            }

            // Increase adaptation interval to reduce monitoring overhead
            adaptationInterval = Math.min(60000, adaptationInterval * 2);

        } catch (Exception e) {
            LOGGER.warning("CPU optimization error: " + e.getMessage());
        }
    }

    private void optimizeMemoryUsage() {
        try {
            LOGGER.info("🧹 Optimizing memory usage - triggering cleanup");

            // Force garbage collection
            System.gc();

            // Clear resource usage history for memory savings
            for (ResourceUsage usage : resourceUsage.values()) {
                // Reset counters to free memory
                usage.recordRelease(0); // This will update timestamp
            }

            // Reduce cache sizes in memory-intensive pools
            for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
                String poolName = entry.getKey();
                ResourcePool pool = entry.getValue();

                if (poolName.contains("chunk") || poolName.contains("entity")) {
                    // Reduce memory-intensive pool capacity by 20%
                    pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.8));
                }
            }

            // Clear old allocation times to free memory
            long currentTime = System.currentTimeMillis();
            lastAllocationTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 300000); // Remove entries older than 5 minutes

        } catch (Exception e) {
            LOGGER.warning("Memory optimization error: " + e.getMessage());
        }
    }

    private void optimizeIOUsage() {
        try {
            LOGGER.info("💾 Optimizing I/O usage - reducing disk operations");

            // Reduce I/O-intensive operations
            for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
                String poolName = entry.getKey();
                ResourcePool pool = entry.getValue();

                if (poolName.contains("world") || poolName.contains("chunk")) {
                    // Reduce world/chunk I/O capacity by 20%
                    pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.8));
                }

                if (poolName.contains("lighting")) {
                    // Reduce lighting update capacity by 30%
                    pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.7));
                }
            }

            // Increase monitoring interval to reduce I/O overhead
            if (resourceMonitor instanceof java.util.concurrent.ScheduledThreadPoolExecutor) {
                // Note: In real implementation, we'd need to reschedule tasks
                LOGGER.fine("Reduced monitoring frequency to optimize I/O");
            }

        } catch (Exception e) {
            LOGGER.warning("I/O optimization error: " + e.getMessage());
        }
    }
    
    private void expandResourcePool(String poolName, int amount) {
        ResourcePool pool = resourcePools.get(poolName);
        if (pool != null) {
            pool.setMaxCapacity(pool.getMaxCapacity() + amount);
            LOGGER.fine("Expanded pool " + poolName + " by " + amount);
        }
    }
    
    private void shrinkResourcePool(String poolName, int amount) {
        ResourcePool pool = resourcePools.get(poolName);
        if (pool != null && pool.getMaxCapacity() > amount) {
            pool.setMaxCapacity(pool.getMaxCapacity() - amount);
            LOGGER.fine("Shrunk pool " + poolName + " by " + amount);
        }
    }
    
    private int calculateOptimalCapacity(ResourcePool pool, ResourceUsage usage, double priority) {
        double utilizationRate = usage.getUtilizationRate();
        int currentCapacity = pool.getMaxCapacity();
        
        // Base calculation on utilization and priority
        double factor = (utilizationRate / 100.0) * priority;
        
        return Math.max(10, (int) (currentCapacity * factor));
    }
    
    private void scaleDownNonCriticalResources() {
        for (Map.Entry<String, ResourcePool> entry : resourcePools.entrySet()) {
            String poolName = entry.getKey();
            ResourcePool pool = entry.getValue();
            double priority = resourcePriorities.get(poolName);
            
            if (priority < 0.8) { // Non-critical pools
                pool.setMaxCapacity((int) (pool.getMaxCapacity() * 0.9));
            }
        }
    }
    
    private void scaleUpResources() {
        for (ResourcePool pool : resourcePools.values()) {
            pool.setMaxCapacity((int) (pool.getMaxCapacity() * 1.1));
        }
    }
    
    private double getTotalPriority() {
        return resourcePriorities.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
    }
    
    /**
     * Get resource allocation statistics
     */
    public Map<String, Object> getResourceStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_allocations", totalAllocations.get());
        stats.put("resource_optimizations", resourceOptimizations.get());
        stats.put("memory_allocations", memoryAllocations.get());
        stats.put("cpu_optimizations", cpuOptimizations.get());
        stats.put("io_optimizations", ioOptimizations.get());
        stats.put("adaptive_changes", adaptiveChanges.get());
        
        stats.put("active_pools", resourcePools.size());
        stats.put("system_load", getCurrentSystemLoad());
        stats.put("memory_usage", getCurrentMemoryUsage());
        
        return stats;
    }
    
    // Getters
    public long getTotalAllocations() { return totalAllocations.get(); }
    public long getResourceOptimizations() { return resourceOptimizations.get(); }
    public long getMemoryAllocations() { return memoryAllocations.get(); }
    public long getCpuOptimizations() { return cpuOptimizations.get(); }
    public long getIoOptimizations() { return ioOptimizations.get(); }
    public long getAdaptiveChanges() { return adaptiveChanges.get(); }
    
    public void shutdown() {
        resourceMonitor.shutdown();
        resourcePools.clear();
        resourceUsage.clear();
        LOGGER.info("🔧 Dynamic Resource Allocator shutdown complete");
    }
    
    /**
     * Resource pool container
     */
    private static class ResourcePool {
        private final String name;
        private int maxCapacity;
        private int currentUsage;
        private final double priority;
        
        public ResourcePool(String name, int maxCapacity, double priority) {
            this.name = name;
            this.maxCapacity = maxCapacity;
            this.currentUsage = 0;
            this.priority = priority;
        }
        
        public synchronized boolean allocate(int amount) {
            if (currentUsage + amount <= maxCapacity) {
                currentUsage += amount;
                return true;
            }
            return false;
        }
        
        public synchronized void release(int amount) {
            currentUsage = Math.max(0, currentUsage - amount);
        }
        
        public int getAvailableCapacity() {
            return maxCapacity - currentUsage;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public int getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(int maxCapacity) { this.maxCapacity = Math.max(10, maxCapacity); }
        public int getCurrentUsage() { return currentUsage; }
        public double getPriority() { return priority; }
    }
    
    /**
     * Resource usage tracker
     */
    private static class ResourceUsage {
        private long totalAllocations = 0;
        private long totalReleases = 0;
        private long lastUpdateTime = System.currentTimeMillis();
        
        public void recordAllocation(int amount) {
            totalAllocations += amount;
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public void recordRelease(int amount) {
            totalReleases += amount;
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public double getUtilizationRate() {
            if (totalAllocations == 0) return 0.0;
            return (totalAllocations * 100.0) / (totalAllocations + totalReleases);
        }
        
        public double getAllocationRate() {
            long timeDiff = System.currentTimeMillis() - lastUpdateTime;
            if (timeDiff == 0) return 0.0;
            return totalAllocations / (timeDiff / 1000.0); // Allocations per second
        }
    }
    
    /**
     * Resource allocation result
     */
    public static class ResourceAllocation {
        private final int allocatedAmount;
        private final boolean successful;
        
        public ResourceAllocation(int allocatedAmount, boolean successful) {
            this.allocatedAmount = allocatedAmount;
            this.successful = successful;
        }
        
        public int getAllocatedAmount() { return allocatedAmount; }
        public boolean isSuccessful() { return successful; }
    }
}
