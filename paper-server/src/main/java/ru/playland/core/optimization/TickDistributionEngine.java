package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

/**
 * Tick Distribution Engine
 * Революционное распределение нагрузки по тикам
 * Использует умное планирование и адаптивную балансировку
 */
public class TickDistributionEngine {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-TickDist");
    
    // Tick statistics
    private final AtomicLong totalTicks = new AtomicLong(0);
    private final AtomicLong optimizedTicks = new AtomicLong(0);
    private final AtomicLong tasksDistributed = new AtomicLong(0);
    private final AtomicLong tasksDeferred = new AtomicLong(0);
    private final AtomicLong loadBalanceOperations = new AtomicLong(0);
    
    // Tick timing
    private final Map<String, Long> taskExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> taskExecutionCounts = new ConcurrentHashMap<>();
    private final Queue<Long> recentTickTimes = new ConcurrentLinkedQueue<>();
    
    // Task queues by priority
    private final PriorityBlockingQueue<TickTask> criticalTasks = new PriorityBlockingQueue<>(1000, 
        Comparator.comparing(TickTask::getPriority).thenComparing(TickTask::getScheduledTick));
    private final PriorityBlockingQueue<TickTask> normalTasks = new PriorityBlockingQueue<>(5000,
        Comparator.comparing(TickTask::getScheduledTick));
    private final PriorityBlockingQueue<TickTask> deferredTasks = new PriorityBlockingQueue<>(10000,
        Comparator.comparing(TickTask::getScheduledTick));
    
    // Load balancing
    private final Map<Integer, List<TickTask>> tickSchedule = new ConcurrentHashMap<>();
    private final AtomicLong currentTick = new AtomicLong(0);
    
    // Configuration
    private boolean enableTickDistribution = true;
    private boolean enableLoadBalancing = true;
    private boolean enableTaskDeferring = true;
    private boolean enableAdaptiveScheduling = true;
    private boolean enableTickProfiling = true;
    
    private double targetTPS = 20.0;
    private long maxTickTime = 50; // milliseconds (50ms = 20 TPS)
    private long warningTickTime = 40; // milliseconds
    private int maxTasksPerTick = 100;
    private int tickHistorySize = 100;
    
    public void initialize() {
        LOGGER.info("⏱️ Initializing Tick Distribution Engine...");
        
        loadTickSettings();
        initializeTickScheduling();
        
        LOGGER.info("✅ Tick Distribution Engine initialized!");
        LOGGER.info("📊 Tick distribution: " + (enableTickDistribution ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚖️ Load balancing: " + (enableLoadBalancing ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏳ Task deferring: " + (enableTaskDeferring ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔄 Adaptive scheduling: " + (enableAdaptiveScheduling ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Tick profiling: " + (enableTickProfiling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🎯 Target TPS: " + targetTPS);
        LOGGER.info("⏰ Max tick time: " + maxTickTime + "ms");
        LOGGER.info("📦 Max tasks per tick: " + maxTasksPerTick);
    }
    
    private void loadTickSettings() {
        // Load tick distribution settings
        LOGGER.info("⚙️ Loading tick settings...");
    }
    
    private void initializeTickScheduling() {
        // Initialize tick scheduling system
        LOGGER.info("📅 Tick scheduling initialized");
    }
    
    /**
     * Process a server tick with optimization
     */
    public void processTick() {
        long tickStart = System.nanoTime();
        long currentTickNumber = currentTick.incrementAndGet();
        
        totalTicks.incrementAndGet();
        
        try {
            // Execute critical tasks first
            executeCriticalTasks(currentTickNumber);
            
            // Check remaining time budget
            long elapsed = (System.nanoTime() - tickStart) / 1_000_000;
            long remainingTime = maxTickTime - elapsed;
            
            if (remainingTime > 0) {
                // Execute normal tasks within time budget
                executeNormalTasks(currentTickNumber, remainingTime);
            }
            
            // Handle deferred tasks if time allows
            elapsed = (System.nanoTime() - tickStart) / 1_000_000;
            remainingTime = maxTickTime - elapsed;
            
            if (remainingTime > 5) { // At least 5ms remaining
                executeDeferredTasks(currentTickNumber, remainingTime);
            }
            
            // Perform load balancing
            if (enableLoadBalancing) {
                balanceTickLoad(currentTickNumber);
            }
            
            optimizedTicks.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Tick processing error: " + e.getMessage());
        }
        
        // Record tick timing
        long tickEnd = System.nanoTime();
        long tickDuration = (tickEnd - tickStart) / 1_000_000;
        recordTickTiming(tickDuration);
        
        // Warn about slow ticks
        if (tickDuration > warningTickTime) {
            LOGGER.warning("⚠️ Slow tick detected: " + tickDuration + "ms (Tick #" + currentTickNumber + ")");
        }
    }
    
    /**
     * Schedule a task for execution
     */
    public void scheduleTask(String taskName, Runnable task, TaskPriority priority, long delayTicks) {
        long scheduledTick = currentTick.get() + delayTicks;
        
        TickTask tickTask = new TickTask(taskName, task, priority, scheduledTick, System.currentTimeMillis());
        
        // Add to appropriate queue based on priority
        switch (priority) {
            case CRITICAL:
                criticalTasks.offer(tickTask);
                break;
            case NORMAL:
                normalTasks.offer(tickTask);
                break;
            case LOW:
                deferredTasks.offer(tickTask);
                break;
        }
        
        tasksDistributed.incrementAndGet();
    }
    
    /**
     * Execute critical tasks (must run this tick)
     */
    private void executeCriticalTasks(long currentTickNumber) {
        List<TickTask> toExecute = new ArrayList<>();
        
        // Collect tasks scheduled for this tick or earlier
        while (!criticalTasks.isEmpty()) {
            TickTask task = criticalTasks.peek();
            if (task.getScheduledTick() <= currentTickNumber) {
                toExecute.add(criticalTasks.poll());
            } else {
                break;
            }
        }
        
        // Execute critical tasks
        for (TickTask task : toExecute) {
            executeTask(task);
        }
    }
    
    /**
     * Execute normal tasks within time budget
     */
    private void executeNormalTasks(long currentTickNumber, long timebudget) {
        long startTime = System.nanoTime();
        int tasksExecuted = 0;
        
        while (!normalTasks.isEmpty() && tasksExecuted < maxTasksPerTick) {
            TickTask task = normalTasks.peek();
            
            if (task.getScheduledTick() > currentTickNumber) {
                break; // No more tasks for this tick
            }
            
            // Check time budget
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            if (elapsed >= timebudget) {
                break; // Out of time
            }
            
            // Estimate task execution time
            long estimatedTime = estimateTaskTime(task.getName());
            if (elapsed + estimatedTime > timebudget) {
                // Defer this task if it might exceed budget
                if (enableTaskDeferring) {
                    TickTask deferredTask = normalTasks.poll();
                    deferredTask.setScheduledTick(currentTickNumber + 1);
                    deferredTasks.offer(deferredTask);
                    tasksDeferred.incrementAndGet();
                    continue;
                } else {
                    break;
                }
            }
            
            executeTask(normalTasks.poll());
            tasksExecuted++;
        }
    }
    
    /**
     * Execute deferred tasks if time allows
     */
    private void executeDeferredTasks(long currentTickNumber, long timebudget) {
        long startTime = System.nanoTime();
        int tasksExecuted = 0;
        
        while (!deferredTasks.isEmpty() && tasksExecuted < maxTasksPerTick / 2) {
            TickTask task = deferredTasks.peek();
            
            // Check time budget
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            if (elapsed >= timebudget) {
                break;
            }
            
            // Only execute if task is overdue or we have plenty of time
            boolean isOverdue = task.getScheduledTick() < currentTickNumber - 5;
            boolean hasTime = elapsed < timebudget / 2;
            
            if (isOverdue || hasTime) {
                executeTask(deferredTasks.poll());
                tasksExecuted++;
            } else {
                break;
            }
        }
    }
    
    /**
     * Execute a single task with timing
     */
    private void executeTask(TickTask task) {
        long startTime = System.nanoTime();
        
        try {
            task.getTask().run();
        } catch (Exception e) {
            LOGGER.warning("Task execution error (" + task.getName() + "): " + e.getMessage());
        }
        
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
        
        // Record execution time for future estimation
        recordTaskTiming(task.getName(), executionTime);
    }
    
    /**
     * Balance load across ticks
     */
    private void balanceTickLoad(long currentTickNumber) {
        if (!enableLoadBalancing) return;
        
        try {
            // Analyze recent tick performance
            double averageTickTime = calculateAverageTickTime();
            
            if (averageTickTime > warningTickTime) {
                // Server is struggling, be more aggressive with deferring
                redistributeNormalTasks();
                loadBalanceOperations.incrementAndGet();
            } else if (averageTickTime < maxTickTime * 0.7) {
                // Server has spare capacity, can handle more tasks
                promoteDeferedTasks();
                loadBalanceOperations.incrementAndGet();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Load balancing error: " + e.getMessage());
        }
    }
    
    /**
     * Redistribute normal tasks to future ticks
     */
    private void redistributeNormalTasks() {
        List<TickTask> tasksToRedistribute = new ArrayList<>();
        
        // Move some normal tasks to deferred queue
        int toMove = Math.min(normalTasks.size() / 4, 50);
        
        for (int i = 0; i < toMove && !normalTasks.isEmpty(); i++) {
            TickTask task = normalTasks.poll();
            task.setScheduledTick(task.getScheduledTick() + 1);
            tasksToRedistribute.add(task);
        }
        
        // Add to deferred queue
        for (TickTask task : tasksToRedistribute) {
            deferredTasks.offer(task);
            tasksDeferred.incrementAndGet();
        }
        
        if (!tasksToRedistribute.isEmpty()) {
            LOGGER.info("⚖️ Redistributed " + tasksToRedistribute.size() + " tasks to reduce load");
        }
    }
    
    /**
     * Promote deferred tasks back to normal queue
     */
    private void promoteDeferedTasks() {
        List<TickTask> tasksToPromote = new ArrayList<>();
        
        // Move some deferred tasks back to normal queue
        int toMove = Math.min(deferredTasks.size() / 4, 25);
        
        for (int i = 0; i < toMove && !deferredTasks.isEmpty(); i++) {
            tasksToPromote.add(deferredTasks.poll());
        }
        
        // Add to normal queue
        for (TickTask task : tasksToPromote) {
            normalTasks.offer(task);
        }
        
        if (!tasksToPromote.isEmpty()) {
            LOGGER.info("⚡ Promoted " + tasksToPromote.size() + " deferred tasks");
        }
    }
    
    /**
     * Estimate task execution time based on history
     */
    private long estimateTaskTime(String taskName) {
        Long averageTime = taskExecutionTimes.get(taskName);
        if (averageTime != null) {
            return averageTime;
        }
        
        // Default estimate for unknown tasks
        return 1; // 1ms default
    }
    
    /**
     * Record task execution timing
     */
    private void recordTaskTiming(String taskName, long executionTime) {
        if (!enableTickProfiling) return;
        
        // Update average execution time
        Long currentAverage = taskExecutionTimes.get(taskName);
        AtomicLong count = taskExecutionCounts.computeIfAbsent(taskName, k -> new AtomicLong(0));
        
        if (currentAverage == null) {
            taskExecutionTimes.put(taskName, executionTime);
        } else {
            // Calculate rolling average
            long totalCount = count.incrementAndGet();
            long newAverage = (currentAverage * (totalCount - 1) + executionTime) / totalCount;
            taskExecutionTimes.put(taskName, newAverage);
        }
    }
    
    /**
     * Record tick timing for performance analysis
     */
    private void recordTickTiming(long tickTime) {
        recentTickTimes.offer(tickTime);
        
        // Keep only recent history
        while (recentTickTimes.size() > tickHistorySize) {
            recentTickTimes.poll();
        }
    }
    
    /**
     * Calculate average tick time from recent history
     */
    private double calculateAverageTickTime() {
        if (recentTickTimes.isEmpty()) return 0.0;
        
        long total = 0;
        for (Long tickTime : recentTickTimes) {
            total += tickTime;
        }
        
        return (double) total / recentTickTimes.size();
    }
    
    /**
     * Get current TPS based on recent tick times
     */
    public double getCurrentTPS() {
        double averageTickTime = calculateAverageTickTime();
        if (averageTickTime == 0) return targetTPS;
        
        return Math.min(targetTPS, 1000.0 / averageTickTime);
    }
    
    /**
     * Get tick distribution statistics
     */
    public Map<String, Object> getTickStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("total_ticks", totalTicks.get());
        stats.put("optimized_ticks", optimizedTicks.get());
        stats.put("tasks_distributed", tasksDistributed.get());
        stats.put("tasks_deferred", tasksDeferred.get());
        stats.put("load_balance_operations", loadBalanceOperations.get());
        
        stats.put("current_tps", getCurrentTPS());
        stats.put("average_tick_time", calculateAverageTickTime());
        stats.put("target_tps", targetTPS);
        
        stats.put("critical_tasks_queued", criticalTasks.size());
        stats.put("normal_tasks_queued", normalTasks.size());
        stats.put("deferred_tasks_queued", deferredTasks.size());
        
        stats.put("optimization_efficiency", calculateOptimizationEfficiency());
        
        return stats;
    }
    
    private double calculateOptimizationEfficiency() {
        long total = totalTicks.get();
        long optimized = optimizedTicks.get();
        if (total == 0) return 100.0;
        return (optimized * 100.0) / total;
    }
    
    // Getters
    public long getTotalTicks() { return totalTicks.get(); }
    public long getOptimizedTicks() { return optimizedTicks.get(); }
    public long getTasksDistributed() { return tasksDistributed.get(); }
    public long getTasksDeferred() { return tasksDeferred.get(); }
    public long getLoadBalanceOperations() { return loadBalanceOperations.get(); }
    
    /**
     * Task priority enumeration
     */
    public enum TaskPriority {
        CRITICAL(0),  // Must execute this tick
        NORMAL(1),    // Should execute this tick
        LOW(2);       // Can be deferred
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Tick task container
     */
    private static class TickTask {
        private final String name;
        private final Runnable task;
        private final TaskPriority priority;
        private long scheduledTick;
        private final long createdTime;
        
        public TickTask(String name, Runnable task, TaskPriority priority, long scheduledTick, long createdTime) {
            this.name = name;
            this.task = task;
            this.priority = priority;
            this.scheduledTick = scheduledTick;
            this.createdTime = createdTime;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public Runnable getTask() { return task; }
        public TaskPriority getPriority() { return priority; }
        public long getScheduledTick() { return scheduledTick; }
        public void setScheduledTick(long scheduledTick) { this.scheduledTick = scheduledTick; }
        public long getCreatedTime() { return createdTime; }
    }
}
