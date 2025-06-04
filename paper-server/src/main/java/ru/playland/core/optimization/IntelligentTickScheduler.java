package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;

/**
 * Intelligent Tick Scheduler
 * VANILLA-SAFE планировщик тиков для однопоточного Minecraft
 * Оптимизирует порядок выполнения без нарушения vanilla механик
 */
public class IntelligentTickScheduler {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-TickScheduler");
    
    // Scheduling statistics
    private final AtomicLong totalTicks = new AtomicLong(0);
    private final AtomicLong optimizedTicks = new AtomicLong(0);
    private final AtomicLong timesSaved = new AtomicLong(0);
    private final AtomicLong tasksScheduled = new AtomicLong(0);
    private final AtomicLong tasksOptimized = new AtomicLong(0);
    
    // Tick timing analysis
    private final Map<String, TickProfile> tickProfiles = new ConcurrentHashMap<>();
    private final Queue<Long> recentTickTimes = new PriorityQueue<>();
    private final Map<String, Long> lastExecutionTime = new ConcurrentHashMap<>();
    
    // Task scheduling
    private final List<ScheduledTask> currentTickTasks = new ArrayList<>();
    private final List<ScheduledTask> nextTickTasks = new ArrayList<>();
    private final Map<String, Integer> taskPriorities = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enableIntelligentScheduling = true;
    private boolean enableTaskGrouping = true;
    private boolean enablePriorityOptimization = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableTickProfiling = true;
    private boolean enableAdaptiveScheduling = true;
    private boolean enableTaskPrioritization = true;
    private boolean enablePredictiveScheduling = true;

    private long targetTickTime = 50; // 50ms = 20 TPS
    private long warningTickTime = 40; // Warn at 40ms
    private int maxTasksPerTick = 1000;
    private int tickHistorySize = 100;
    private double adaptiveThreshold = 0.8;
    
    public void initialize() {
        LOGGER.info("🧠 Initializing Intelligent Tick Scheduler...");
        
        loadSchedulingSettings();
        initializeTaskPriorities();
        
        LOGGER.info("✅ Intelligent Tick Scheduler initialized!");
        LOGGER.info("🧠 Intelligent scheduling: " + (enableIntelligentScheduling ? "ENABLED" : "DISABLED"));
        LOGGER.info("📦 Task grouping: " + (enableTaskGrouping ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Priority optimization: " + (enablePriorityOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🎮 Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Tick profiling: " + (enableTickProfiling ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏰ Target tick time: " + targetTickTime + "ms");
        LOGGER.info("📦 Max tasks per tick: " + maxTasksPerTick);
    }
    
    private void loadSchedulingSettings() {
        try {
            LOGGER.info("⚙️ Loading scheduling settings...");

            // Load timing settings from system properties
            targetTickTime = Long.parseLong(System.getProperty("playland.tick.target.time", "50"));
            maxTasksPerTick = Integer.parseInt(System.getProperty("playland.tick.max.tasks", "100"));
            adaptiveThreshold = Double.parseDouble(System.getProperty("playland.tick.adaptive.threshold", "0.8"));

            // Load feature flags
            enableAdaptiveScheduling = Boolean.parseBoolean(System.getProperty("playland.tick.adaptive.enabled", "true"));
            enableTaskPrioritization = Boolean.parseBoolean(System.getProperty("playland.tick.prioritization.enabled", "true"));
            enableVanillaSafeMode = Boolean.parseBoolean(System.getProperty("playland.tick.vanilla.safe", "true"));
            enablePredictiveScheduling = Boolean.parseBoolean(System.getProperty("playland.tick.predictive.enabled", "true"));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - be more aggressive with optimization
                adaptiveThreshold = Math.max(0.6, adaptiveThreshold - 0.2);
                maxTasksPerTick = Math.max(50, maxTasksPerTick - 25);
                LOGGER.info("🔧 Adjusted settings for low TPS: threshold=" + adaptiveThreshold + ", maxTasks=" + maxTasksPerTick);
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more tasks
                maxTasksPerTick = Math.min(150, maxTasksPerTick + 25);
                LOGGER.info("🔧 Increased task limit for good TPS: " + maxTasksPerTick);
            }

            // Auto-adjust based on available CPU cores
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            if (availableProcessors >= 4) {
                // Multi-core system - can handle more tasks
                maxTasksPerTick = Math.min(200, (int) (maxTasksPerTick * 1.5));
            } else if (availableProcessors <= 2) {
                // Low-core system - reduce task load
                maxTasksPerTick = Math.max(30, (int) (maxTasksPerTick * 0.7));
            }

            LOGGER.info("✅ Scheduling settings loaded - Target: " + targetTickTime + "ms, Max tasks: " + maxTasksPerTick);

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading scheduling settings: " + e.getMessage() + " - using defaults");
        }
    }


    
    private void initializeTaskPriorities() {
        // Initialize task priorities for vanilla-safe optimization
        taskPriorities.put("player_movement", 100);      // Highest priority
        taskPriorities.put("player_interaction", 95);
        taskPriorities.put("redstone_update", 90);
        taskPriorities.put("entity_movement", 80);
        taskPriorities.put("block_update", 75);
        taskPriorities.put("chunk_loading", 70);
        taskPriorities.put("entity_ai", 60);
        taskPriorities.put("world_generation", 50);
        taskPriorities.put("cleanup", 30);               // Lowest priority
        
        LOGGER.info("📋 Task priorities initialized");
    }
    
    /**
     * Start tick optimization
     */
    public void startTick() {
        long tickStart = System.nanoTime();
        long currentTick = totalTicks.incrementAndGet();
        
        if (!enableIntelligentScheduling) return;
        
        try {
            // Analyze previous tick performance
            analyzePreviousTickPerformance();
            
            // Prepare tasks for this tick
            prepareTickTasks(currentTick);
            
            // Optimize task order
            if (enablePriorityOptimization) {
                optimizeTaskOrder();
            }
            
            // Group similar tasks
            if (enableTaskGrouping) {
                groupSimilarTasks();
            }
            
            optimizedTicks.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Tick start optimization error: " + e.getMessage());
        }
    }
    
    /**
     * End tick optimization
     */
    public void endTick() {
        if (!enableIntelligentScheduling) return;
        
        try {
            long tickEnd = System.nanoTime();
            
            // Record tick timing
            recordTickTiming(tickEnd);
            
            // Prepare for next tick
            prepareNextTick();
            
        } catch (Exception e) {
            LOGGER.warning("Tick end optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Schedule a task with intelligent optimization
     */
    public void scheduleTask(String taskType, Runnable task, int priority) {
        if (!enableIntelligentScheduling) {
            // Execute immediately if optimization disabled
            task.run();
            return;
        }
        
        try {
            ScheduledTask scheduledTask = new ScheduledTask(
                taskType, 
                task, 
                priority, 
                System.nanoTime()
            );
            
            // Determine optimal execution time
            if (shouldExecuteThisTick(taskType)) {
                currentTickTasks.add(scheduledTask);
            } else {
                nextTickTasks.add(scheduledTask);
            }
            
            tasksScheduled.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Task scheduling error: " + e.getMessage());
            // Fallback to immediate execution
            task.run();
        }
    }
    
    /**
     * Execute scheduled tasks for current tick
     */
    public void executeScheduledTasks() {
        if (!enableIntelligentScheduling || currentTickTasks.isEmpty()) return;
        
        long tickStart = System.nanoTime();
        int tasksExecuted = 0;
        
        try {
            // Execute tasks in optimized order
            for (ScheduledTask task : currentTickTasks) {
                if (tasksExecuted >= maxTasksPerTick) {
                    // Move remaining tasks to next tick
                    for (int i = tasksExecuted; i < currentTickTasks.size(); i++) {
                        nextTickTasks.add(currentTickTasks.get(i));
                    }
                    break;
                }
                
                // Check time budget
                long elapsed = (System.nanoTime() - tickStart) / 1_000_000;
                if (elapsed > targetTickTime * 0.8) { // Use 80% of tick time
                    // Move remaining tasks to next tick
                    for (int i = tasksExecuted; i < currentTickTasks.size(); i++) {
                        nextTickTasks.add(currentTickTasks.get(i));
                    }
                    break;
                }
                
                // Execute task
                executeTask(task);
                tasksExecuted++;
                tasksOptimized.incrementAndGet();
            }
            
            // Clear executed tasks
            currentTickTasks.clear();
            
        } catch (Exception e) {
            LOGGER.warning("Task execution error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze previous tick performance
     */
    private void analyzePreviousTickPerformance() {
        // Analyze recent tick times to adjust optimization strategy
        if (recentTickTimes.size() > 10) {
            double averageTickTime = recentTickTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(targetTickTime);
            
            // Adjust optimization aggressiveness based on performance
            if (averageTickTime > targetTickTime) {
                // Server is struggling, be more aggressive
                maxTasksPerTick = Math.max(100, maxTasksPerTick - 50);
            } else if (averageTickTime < targetTickTime * 0.7) {
                // Server has spare capacity
                maxTasksPerTick = Math.min(2000, maxTasksPerTick + 50);
            }
        }
    }
    
    /**
     * Prepare tasks for current tick
     */
    private void prepareTickTasks(long currentTick) {
        // Move next tick tasks to current tick
        currentTickTasks.addAll(nextTickTasks);
        nextTickTasks.clear();
        
        // Add any pending vanilla tasks
        addVanillaTasks(currentTick);
    }
    
    /**
     * Optimize task execution order
     */
    private void optimizeTaskOrder() {
        // Sort tasks by priority and vanilla-safety
        currentTickTasks.sort((a, b) -> {
            // First priority: vanilla-critical tasks
            boolean aVanillaCritical = isVanillaCritical(a.getType());
            boolean bVanillaCritical = isVanillaCritical(b.getType());
            
            if (aVanillaCritical != bVanillaCritical) {
                return aVanillaCritical ? -1 : 1;
            }
            
            // Second priority: task priority
            int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            
            // Third priority: execution time (faster tasks first)
            return Long.compare(getEstimatedExecutionTime(a.getType()), 
                               getEstimatedExecutionTime(b.getType()));
        });
    }
    
    /**
     * Group similar tasks for batch execution
     */
    private void groupSimilarTasks() {
        Map<String, List<ScheduledTask>> taskGroups = new ConcurrentHashMap<>();
        
        // Group tasks by type
        for (ScheduledTask task : currentTickTasks) {
            taskGroups.computeIfAbsent(task.getType(), k -> new ArrayList<>()).add(task);
        }
        
        // Reorder tasks to group similar ones together
        List<ScheduledTask> optimizedOrder = new ArrayList<>();
        
        // Add vanilla-critical tasks first
        for (Map.Entry<String, List<ScheduledTask>> entry : taskGroups.entrySet()) {
            if (isVanillaCritical(entry.getKey())) {
                optimizedOrder.addAll(entry.getValue());
            }
        }
        
        // Add other tasks in groups
        for (Map.Entry<String, List<ScheduledTask>> entry : taskGroups.entrySet()) {
            if (!isVanillaCritical(entry.getKey())) {
                optimizedOrder.addAll(entry.getValue());
            }
        }
        
        currentTickTasks.clear();
        currentTickTasks.addAll(optimizedOrder);
    }
    
    /**
     * Execute a single task with profiling
     */
    private void executeTask(ScheduledTask task) {
        long startTime = System.nanoTime();
        
        try {
            // Execute the task
            task.getTask().run();
            
            // Record execution time
            long endTime = System.nanoTime();
            long executionTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            recordTaskExecution(task.getType(), executionTime);
            
        } catch (Exception e) {
            LOGGER.warning("Task execution error (" + task.getType() + "): " + e.getMessage());
        }
    }
    
    /**
     * Check if task should execute this tick
     */
    private boolean shouldExecuteThisTick(String taskType) {
        if (!enableVanillaSafeMode) return true;
        
        // Always execute vanilla-critical tasks immediately
        if (isVanillaCritical(taskType)) {
            return true;
        }
        
        // Check if we have time budget for non-critical tasks
        long lastExecution = lastExecutionTime.getOrDefault(taskType, 0L);
        long timeSinceLastExecution = System.nanoTime() - lastExecution;
        
        // Don't execute too frequently
        if (timeSinceLastExecution < 1_000_000) { // 1ms minimum interval
            return false;
        }
        
        // Check current tick load
        return currentTickTasks.size() < maxTasksPerTick / 2;
    }
    
    /**
     * Check if task type is vanilla-critical
     */
    private boolean isVanillaCritical(String taskType) {
        return taskType.equals("player_movement") ||
               taskType.equals("player_interaction") ||
               taskType.equals("redstone_update") ||
               taskType.equals("block_update");
    }
    
    /**
     * Get estimated execution time for task type
     */
    private long getEstimatedExecutionTime(String taskType) {
        TickProfile profile = tickProfiles.get(taskType);
        if (profile != null) {
            return profile.getAverageExecutionTime();
        }
        return 1; // Default 1ms estimate
    }
    
    /**
     * Add vanilla tasks that must execute this tick
     */
    private void addVanillaTasks(long currentTick) {
        // This would be called by vanilla systems to register critical tasks
        // For now, it's a placeholder
    }
    
    /**
     * Record task execution for profiling
     */
    private void recordTaskExecution(String taskType, long executionTime) {
        if (!enableTickProfiling) return;
        
        TickProfile profile = tickProfiles.computeIfAbsent(taskType, k -> new TickProfile(taskType));
        profile.recordExecution(executionTime);
        
        lastExecutionTime.put(taskType, System.nanoTime());
    }
    
    /**
     * Record tick timing
     */
    private void recordTickTiming(long tickEnd) {
        // This would be called with the actual tick timing
        // For now, we'll estimate based on task execution
        long estimatedTickTime = currentTickTasks.stream()
            .mapToLong(task -> getEstimatedExecutionTime(task.getType()))
            .sum();
        
        recentTickTimes.offer(estimatedTickTime);
        
        // Keep only recent history
        while (recentTickTimes.size() > tickHistorySize) {
            recentTickTimes.poll();
        }
        
        // Calculate time saved
        long baselineTime = currentTickTasks.size() * 2; // Assume 2ms per task baseline
        long actualTime = estimatedTickTime;
        if (baselineTime > actualTime) {
            timesSaved.addAndGet(baselineTime - actualTime);
        }
    }
    
    /**
     * Prepare for next tick
     */
    private void prepareNextTick() {
        // Clean up completed tasks
        currentTickTasks.clear();
        
        // Optimize next tick tasks
        if (nextTickTasks.size() > maxTasksPerTick) {
            // Prioritize most important tasks
            nextTickTasks.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            
            // Keep only the most important tasks
            List<ScheduledTask> prioritizedTasks = new ArrayList<>(
                nextTickTasks.subList(0, Math.min(maxTasksPerTick, nextTickTasks.size()))
            );
            
            nextTickTasks.clear();
            nextTickTasks.addAll(prioritizedTasks);
        }
    }
    
    /**
     * Get current TPS estimate
     */
    public double getCurrentTPS() {
        if (recentTickTimes.isEmpty()) return 20.0;
        
        double averageTickTime = recentTickTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(50.0);
        
        return Math.min(20.0, 1000.0 / averageTickTime);
    }
    
    /**
     * Get scheduling statistics
     */
    public Map<String, Object> getSchedulingStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_ticks", totalTicks.get());
        stats.put("optimized_ticks", optimizedTicks.get());
        stats.put("times_saved_ms", timesSaved.get());
        stats.put("tasks_scheduled", tasksScheduled.get());
        stats.put("tasks_optimized", tasksOptimized.get());
        
        stats.put("current_tps", getCurrentTPS());
        stats.put("target_tick_time", targetTickTime);
        stats.put("max_tasks_per_tick", maxTasksPerTick);
        
        stats.put("current_tick_tasks", currentTickTasks.size());
        stats.put("next_tick_tasks", nextTickTasks.size());
        stats.put("task_profiles", tickProfiles.size());
        
        long totalTicks = this.totalTicks.get();
        if (totalTicks > 0) {
            stats.put("optimization_efficiency", (optimizedTicks.get() * 100.0) / totalTicks);
        }
        
        return stats;
    }
    
    // Getters
    public long getTotalTicks() { return totalTicks.get(); }
    public long getOptimizedTicks() { return optimizedTicks.get(); }
    public long getTimesSaved() { return timesSaved.get(); }
    public long getTasksScheduled() { return tasksScheduled.get(); }
    public long getTasksOptimized() { return tasksOptimized.get(); }
    
    /**
     * Scheduled task container
     */
    private static class ScheduledTask {
        private final String type;
        private final Runnable task;
        private final int priority;
        private final long scheduledTime;
        
        public ScheduledTask(String type, Runnable task, int priority, long scheduledTime) {
            this.type = type;
            this.task = task;
            this.priority = priority;
            this.scheduledTime = scheduledTime;
        }
        
        public String getType() { return type; }
        public Runnable getTask() { return task; }
        public int getPriority() { return priority; }
        public long getScheduledTime() { return scheduledTime; }
    }
    
    /**
     * Tick profile for performance analysis
     */
    private static class TickProfile {
        private final String taskType;
        private long totalExecutions = 0;
        private long totalExecutionTime = 0;
        private long minExecutionTime = Long.MAX_VALUE;
        private long maxExecutionTime = 0;
        
        public TickProfile(String taskType) {
            this.taskType = taskType;
        }
        
        public void recordExecution(long executionTime) {
            totalExecutions++;
            totalExecutionTime += executionTime;
            minExecutionTime = Math.min(minExecutionTime, executionTime);
            maxExecutionTime = Math.max(maxExecutionTime, executionTime);
        }
        
        public long getAverageExecutionTime() {
            if (totalExecutions == 0) return 1;
            return totalExecutionTime / totalExecutions;
        }
        
        public String getTaskType() { return taskType; }
        public long getTotalExecutions() { return totalExecutions; }
        public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
    }
}
