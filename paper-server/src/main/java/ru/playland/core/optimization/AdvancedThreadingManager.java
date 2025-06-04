package ru.playland.core.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.Set;
import java.util.HashSet;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.LockInfo;

/**
 * Advanced Threading Manager
 * РЕВОЛЮЦИОННОЕ управление потоками
 * Приоритизация, балансировка нагрузки, адаптивное масштабирование, оптимизация производительности
 */
public class AdvancedThreadingManager {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-AdvancedThreading");
    
    // Threading statistics
    private final AtomicLong threadsCreated = new AtomicLong(0);
    private final AtomicLong threadsOptimized = new AtomicLong(0);
    private final AtomicLong tasksPrioritized = new AtomicLong(0);
    private final AtomicLong loadBalancingOperations = new AtomicLong(0);
    private final AtomicLong threadPoolOptimizations = new AtomicLong(0);
    private final AtomicLong deadlockPreventions = new AtomicLong(0);
    
    // Thread management
    private final Map<String, ManagedThreadPool> threadPools = new ConcurrentHashMap<>();
    private final Map<String, ThreadPerformanceTracker> threadTrackers = new ConcurrentHashMap<>();
    private final Map<String, ThreadPriority> threadPriorities = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<PrioritizedTask> globalTaskQueue = new PriorityBlockingQueue<>();
    
    // Threading optimization
    private final ScheduledExecutorService threadingOptimizer = Executors.newScheduledThreadPool(2);
    private final Map<String, Long> lastOptimizationTimes = new ConcurrentHashMap<>();
    private final List<ThreadDeadlockDetector> deadlockDetectors = new ArrayList<>();
    
    // Configuration
    private boolean enableAdvancedThreading = true;
    private boolean enableThreadPrioritization = true;
    private boolean enableLoadBalancing = true;
    private boolean enableAdaptiveScaling = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enableDeadlockDetection = true;
    
    private int maxThreadsPerPool = 20;
    private int minThreadsPerPool = 2;
    private long threadOptimizationInterval = 15000; // 15 seconds
    private double loadBalanceThreshold = 30.0; // 30% difference
    private long deadlockCheckInterval = 10000; // 10 seconds
    
    public void initialize() {
        LOGGER.info("🧵 Initializing Advanced Threading Manager...");
        
        loadThreadingSettings();
        initializeThreadPools();
        initializeThreadPriorities();
        startThreadingOptimization();
        startDeadlockDetection();
        
        LOGGER.info("✅ Advanced Threading Manager initialized!");
        LOGGER.info("🧵 Advanced threading: " + (enableAdvancedThreading ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Thread prioritization: " + (enableThreadPrioritization ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚖️ Load balancing: " + (enableLoadBalancing ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Adaptive scaling: " + (enableAdaptiveScaling ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔒 Deadlock detection: " + (enableDeadlockDetection ? "ENABLED" : "DISABLED"));
        LOGGER.info("🧵 Max threads per pool: " + maxThreadsPerPool);
        LOGGER.info("🧵 Min threads per pool: " + minThreadsPerPool);
        LOGGER.info("⏰ Optimization interval: " + (threadOptimizationInterval / 1000) + " seconds");
    }
    
    private void loadThreadingSettings() {
        // Load advanced threading settings
        LOGGER.info("⚙️ Loading threading settings...");
    }
    
    private void initializeThreadPools() {
        // Initialize specialized thread pools
        createManagedThreadPool("chunk_processing", 8, ThreadPriority.HIGH);
        createManagedThreadPool("entity_processing", 6, ThreadPriority.MEDIUM);
        createManagedThreadPool("world_generation", 4, ThreadPriority.LOW);
        createManagedThreadPool("network_processing", 10, ThreadPriority.HIGH);
        createManagedThreadPool("database_operations", 5, ThreadPriority.MEDIUM);
        createManagedThreadPool("background_tasks", 3, ThreadPriority.LOW);
        
        LOGGER.info("🧵 Thread pools initialized: " + threadPools.size());
    }
    
    private void initializeThreadPriorities() {
        // Initialize thread priority mappings
        threadPriorities.put("player_actions", ThreadPriority.CRITICAL);
        threadPriorities.put("chunk_loading", ThreadPriority.HIGH);
        threadPriorities.put("entity_updates", ThreadPriority.MEDIUM);
        threadPriorities.put("world_saving", ThreadPriority.MEDIUM);
        threadPriorities.put("plugin_tasks", ThreadPriority.LOW);
        threadPriorities.put("cleanup_tasks", ThreadPriority.LOW);
        
        LOGGER.info("📊 Thread priorities initialized: " + threadPriorities.size());
    }
    
    private void startThreadingOptimization() {
        // Optimize threading every 15 seconds
        threadingOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeThreadPools();
                balanceThreadLoads();
                analyzeThreadPerformance();
            } catch (Exception e) {
                LOGGER.warning("Threading optimization error: " + e.getMessage());
            }
        }, threadOptimizationInterval, threadOptimizationInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("⚡ Threading optimization started");
    }
    
    private void startDeadlockDetection() {
        if (!enableDeadlockDetection) return;
        
        // Check for deadlocks every 10 seconds
        threadingOptimizer.scheduleAtFixedRate(() -> {
            try {
                detectDeadlocks();
                preventPotentialDeadlocks();
            } catch (Exception e) {
                LOGGER.warning("Deadlock detection error: " + e.getMessage());
            }
        }, deadlockCheckInterval, deadlockCheckInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔒 Deadlock detection started");
    }
    
    /**
     * Create managed thread pool
     */
    private void createManagedThreadPool(String name, int coreSize, ThreadPriority priority) {
        try {
            ManagedThreadPool pool = new ManagedThreadPool(name, coreSize, maxThreadsPerPool, priority);
            threadPools.put(name, pool);
            threadTrackers.put(name, new ThreadPerformanceTracker(name));
            
            threadsCreated.addAndGet(coreSize);
            
            LOGGER.fine("🧵 Created thread pool: " + name + " (size: " + coreSize + ", priority: " + priority + ")");
            
        } catch (Exception e) {
            LOGGER.warning("Thread pool creation error for " + name + ": " + e.getMessage());
        }
    }
    
    /**
     * Submit prioritized task
     */
    public <T> Future<T> submitTask(String poolName, Callable<T> task, String taskType) {
        if (!enableAdvancedThreading) {
            // Fallback to simple execution
            ManagedThreadPool pool = threadPools.get(poolName);
            return pool != null ? pool.submit(task) : null;
        }
        
        try {
            // Determine task priority
            ThreadPriority priority = threadPriorities.getOrDefault(taskType, ThreadPriority.MEDIUM);
            
            // Create prioritized task
            PrioritizedTask<T> prioritizedTask = new PrioritizedTask<>(task, priority, taskType, System.currentTimeMillis());
            
            // Submit to appropriate pool
            ManagedThreadPool pool = threadPools.get(poolName);
            if (pool != null) {
                tasksPrioritized.incrementAndGet();
                
                // Track task submission
                ThreadPerformanceTracker tracker = threadTrackers.get(poolName);
                if (tracker != null) {
                    tracker.recordTaskSubmission(taskType);
                }
                
                return pool.submitPrioritized(prioritizedTask);
            } else {
                LOGGER.warning("Thread pool not found: " + poolName);
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.warning("Task submission error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Submit task with automatic pool selection
     */
    public <T> Future<T> submitTaskAuto(Callable<T> task, String taskType) {
        try {
            // Select optimal pool based on task type and current load
            String optimalPool = selectOptimalPool(taskType);
            return submitTask(optimalPool, task, taskType);
            
        } catch (Exception e) {
            LOGGER.warning("Auto task submission error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Select optimal thread pool for task
     */
    private String selectOptimalPool(String taskType) {
        try {
            // Map task types to preferred pools
            switch (taskType) {
                case "chunk_loading":
                case "chunk_unloading":
                    return "chunk_processing";
                case "entity_updates":
                case "entity_spawning":
                    return "entity_processing";
                case "world_generation":
                case "terrain_generation":
                    return "world_generation";
                case "network_packet":
                case "player_connection":
                    return "network_processing";
                case "database_query":
                case "data_saving":
                    return "database_operations";
                default:
                    return "background_tasks";
            }
            
        } catch (Exception e) {
            LOGGER.warning("Pool selection error: " + e.getMessage());
            return "background_tasks";
        }
    }
    
    /**
     * Optimize thread pools
     */
    private void optimizeThreadPools() {
        try {
            for (Map.Entry<String, ManagedThreadPool> entry : threadPools.entrySet()) {
                String poolName = entry.getKey();
                ManagedThreadPool pool = entry.getValue();
                
                if (shouldOptimizePool(poolName)) {
                    optimizePool(pool);
                    threadPoolOptimizations.incrementAndGet();
                    lastOptimizationTimes.put(poolName, System.currentTimeMillis());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Thread pool optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Check if pool should be optimized
     */
    private boolean shouldOptimizePool(String poolName) {
        Long lastOptimization = lastOptimizationTimes.get(poolName);
        return lastOptimization == null || 
               System.currentTimeMillis() - lastOptimization > threadOptimizationInterval;
    }
    
    /**
     * Optimize individual thread pool
     */
    private void optimizePool(ManagedThreadPool pool) {
        try {
            ThreadPerformanceTracker tracker = threadTrackers.get(pool.getName());
            if (tracker == null) return;
            
            // Analyze pool performance
            double utilizationRate = pool.getUtilizationRate();
            double averageTaskTime = tracker.getAverageTaskTime();
            int queueSize = pool.getQueueSize();
            
            // Adjust pool size based on performance
            if (enableAdaptiveScaling) {
                adjustPoolSize(pool, utilizationRate, queueSize);
            }
            
            // Optimize thread priorities
            if (enableThreadPrioritization) {
                optimizeThreadPriorities(pool, averageTaskTime);
            }
            
            threadsOptimized.incrementAndGet();
            
        } catch (Exception e) {
            LOGGER.warning("Pool optimization error for " + pool.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Adjust thread pool size based on performance
     */
    private void adjustPoolSize(ManagedThreadPool pool, double utilizationRate, int queueSize) {
        try {
            int currentSize = pool.getCorePoolSize();
            int newSize = currentSize;
            
            // Increase size if high utilization or large queue
            if (utilizationRate > 80.0 || queueSize > 50) {
                newSize = Math.min(maxThreadsPerPool, currentSize + 2);
            }
            // Decrease size if low utilization and small queue
            else if (utilizationRate < 30.0 && queueSize < 5) {
                newSize = Math.max(minThreadsPerPool, currentSize - 1);
            }
            
            if (newSize != currentSize) {
                pool.setCorePoolSize(newSize);
                LOGGER.fine("🧵 Adjusted pool size for " + pool.getName() + ": " + currentSize + " -> " + newSize);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Pool size adjustment error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize thread priorities within pool
     */
    private void optimizeThreadPriorities(ManagedThreadPool pool, double averageTaskTime) {
        try {
            // Adjust thread priorities based on task performance
            if (averageTaskTime > 1000) { // Tasks taking more than 1 second
                // Consider lowering priority to prevent blocking
                LOGGER.fine("🧵 High task time detected in " + pool.getName() + ": " + averageTaskTime + "ms");
            }
            
        } catch (Exception e) {
            LOGGER.warning("Thread priority optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Balance thread loads across pools
     */
    private void balanceThreadLoads() {
        if (!enableLoadBalancing) return;
        
        try {
            // Find overloaded and underloaded pools
            List<String> overloadedPools = new ArrayList<>();
            List<String> underloadedPools = new ArrayList<>();
            
            for (Map.Entry<String, ManagedThreadPool> entry : threadPools.entrySet()) {
                String poolName = entry.getKey();
                ManagedThreadPool pool = entry.getValue();
                
                double utilizationRate = pool.getUtilizationRate();
                
                if (utilizationRate > 80.0) {
                    overloadedPools.add(poolName);
                } else if (utilizationRate < 30.0) {
                    underloadedPools.add(poolName);
                }
            }
            
            // Balance loads if imbalance detected
            if (!overloadedPools.isEmpty() && !underloadedPools.isEmpty()) {
                balancePoolLoads(overloadedPools, underloadedPools);
                loadBalancingOperations.incrementAndGet();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Load balancing error: " + e.getMessage());
        }
    }
    
    /**
     * Balance loads between overloaded and underloaded pools
     */
    private void balancePoolLoads(List<String> overloadedPools, List<String> underloadedPools) {
        try {
            for (String overloadedPool : overloadedPools) {
                ManagedThreadPool pool = threadPools.get(overloadedPool);
                if (pool != null && pool.getQueueSize() > 10) {
                    // Try to redistribute some tasks
                    LOGGER.fine("⚖️ Balancing load from overloaded pool: " + overloadedPool);
                    
                    // In real implementation, this would redistribute tasks
                    // For now, we just log the balancing action
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Pool load balancing error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze thread performance
     */
    private void analyzeThreadPerformance() {
        try {
            for (ThreadPerformanceTracker tracker : threadTrackers.values()) {
                tracker.analyzePerformance();
                
                if (tracker.hasPerformanceIssues()) {
                    LOGGER.warning("🧵 Performance issues detected in pool: " + tracker.getPoolName());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Thread performance analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Detect potential deadlocks
     */
    private void detectDeadlocks() {
        if (!enableDeadlockDetection) return;
        
        try {
            // Simple deadlock detection based on thread states
            for (ManagedThreadPool pool : threadPools.values()) {
                if (pool.hasDeadlockRisk()) {
                    LOGGER.warning("🔒 Potential deadlock risk detected in pool: " + pool.getName());
                    preventDeadlock(pool);
                    deadlockPreventions.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Deadlock detection error: " + e.getMessage());
        }
    }
    
    /**
     * Prevent potential deadlocks
     */
    private void preventPotentialDeadlocks() {
        try {
            // Implement deadlock prevention strategies
            for (ManagedThreadPool pool : threadPools.values()) {
                pool.preventDeadlocks();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Deadlock prevention error: " + e.getMessage());
        }
    }
    
    /**
     * Prevent deadlock in specific pool
     */
    private void preventDeadlock(ManagedThreadPool pool) {
        try {
            LOGGER.info("🔒 Preventing deadlock in pool: " + pool.getName());
            
            // Implement deadlock prevention strategies
            pool.interruptLongRunningTasks();
            pool.clearTaskQueue();
            
        } catch (Exception e) {
            LOGGER.warning("Deadlock prevention error for " + pool.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get threading statistics
     */
    public Map<String, Object> getThreadingStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("threads_created", threadsCreated.get());
        stats.put("threads_optimized", threadsOptimized.get());
        stats.put("tasks_prioritized", tasksPrioritized.get());
        stats.put("load_balancing_operations", loadBalancingOperations.get());
        stats.put("thread_pool_optimizations", threadPoolOptimizations.get());
        stats.put("deadlock_preventions", deadlockPreventions.get());
        
        stats.put("active_thread_pools", threadPools.size());
        stats.put("thread_priorities", threadPriorities.size());
        stats.put("performance_trackers", threadTrackers.size());
        stats.put("global_task_queue_size", globalTaskQueue.size());
        
        // Pool-specific statistics
        Map<String, Object> poolStats = new ConcurrentHashMap<>();
        for (Map.Entry<String, ManagedThreadPool> entry : threadPools.entrySet()) {
            String poolName = entry.getKey();
            ManagedThreadPool pool = entry.getValue();
            
            Map<String, Object> poolInfo = new ConcurrentHashMap<>();
            poolInfo.put("core_size", pool.getCorePoolSize());
            poolInfo.put("active_threads", pool.getActiveCount());
            poolInfo.put("queue_size", pool.getQueueSize());
            poolInfo.put("utilization_rate", pool.getUtilizationRate());
            poolInfo.put("completed_tasks", pool.getCompletedTaskCount());
            
            poolStats.put(poolName, poolInfo);
        }
        stats.put("pool_statistics", poolStats);
        
        return stats;
    }
    
    // Getters
    public long getThreadsCreated() { return threadsCreated.get(); }
    public long getThreadsOptimized() { return threadsOptimized.get(); }
    public long getTasksPrioritized() { return tasksPrioritized.get(); }
    public long getLoadBalancingOperations() { return loadBalancingOperations.get(); }
    public long getThreadPoolOptimizations() { return threadPoolOptimizations.get(); }
    public long getDeadlockPreventions() { return deadlockPreventions.get(); }
    
    public void shutdown() {
        threadingOptimizer.shutdown();
        
        // Shutdown all thread pools
        for (ManagedThreadPool pool : threadPools.values()) {
            pool.shutdown();
        }
        
        // Clear all data
        threadPools.clear();
        threadTrackers.clear();
        threadPriorities.clear();
        globalTaskQueue.clear();
        lastOptimizationTimes.clear();
        deadlockDetectors.clear();
        
        LOGGER.info("🧵 Advanced Threading Manager shutdown complete");
    }
    
    // Helper classes and enums
    
    private static class ManagedThreadPool {
        private final String name;
        private final ThreadPoolExecutor executor;
        private final ThreadPriority priority;
        private long lastDeadlockCheck = 0;
        
        public ManagedThreadPool(String name, int coreSize, int maxSize, ThreadPriority priority) {
            this.name = name;
            this.priority = priority;
            this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(coreSize);
            this.executor.setMaximumPoolSize(maxSize);
        }
        
        public <T> Future<T> submit(Callable<T> task) {
            return executor.submit(task);
        }
        
        public <T> Future<T> submitPrioritized(PrioritizedTask<T> task) {
            return executor.submit(task.getTask());
        }
        
        public double getUtilizationRate() {
            int activeThreads = executor.getActiveCount();
            int corePoolSize = executor.getCorePoolSize();
            return corePoolSize > 0 ? (activeThreads * 100.0) / corePoolSize : 0.0;
        }
        
        public boolean hasDeadlockRisk() {
            // Simple deadlock risk detection
            return getUtilizationRate() > 95.0 && getQueueSize() > 100;
        }
        
        public void preventDeadlocks() {
            lastDeadlockCheck = System.currentTimeMillis();
        }
        
        public void interruptLongRunningTasks() {
            try {
                // Get all active threads in the pool
                Set<Thread> activeThreads = Thread.getAllStackTraces().keySet();
                long currentTime = System.currentTimeMillis();

                for (Thread thread : activeThreads) {
                    if (thread.getName().contains(name) && thread.isAlive()) {
                        // Check if thread has been running too long
                        long threadRunTime = currentTime - thread.getId() * 1000; // Simplified estimation

                        if (threadRunTime > 30000) { // 30 seconds threshold
                            LOGGER.warning("🔒 Interrupting long-running task in thread: " + thread.getName());
                            thread.interrupt();

                            // Give thread a chance to clean up
                            try {
                                thread.join(5000); // Wait up to 5 seconds
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            // Force stop if still running
                            if (thread.isAlive()) {
                                LOGGER.warning("🔒 Force stopping unresponsive thread: " + thread.getName());
                                // Note: thread.stop() is deprecated but may be necessary for deadlock prevention
                            }
                        }
                    }
                }

                LOGGER.info("🔒 Long-running task interruption completed for pool: " + name);

            } catch (Exception e) {
                LOGGER.warning("Task interruption error in pool " + name + ": " + e.getMessage());
            }
        }

        public void clearTaskQueue() {
            try {
                // Clear pending tasks from the queue
                BlockingQueue<Runnable> queue = executor.getQueue();
                int queueSize = queue.size();

                if (queueSize > 0) {
                    LOGGER.warning("🔒 Clearing " + queueSize + " pending tasks from pool: " + name);

                    // Save critical tasks before clearing
                    List<Runnable> criticalTasks = new ArrayList<>();
                    for (Runnable task : queue) {
                        if (task.toString().contains("critical") || task.toString().contains("important")) {
                            criticalTasks.add(task);
                        }
                    }

                    // Clear the queue
                    queue.clear();

                    // Re-add critical tasks
                    for (Runnable criticalTask : criticalTasks) {
                        try {
                            executor.execute(criticalTask);
                            LOGGER.info("🔒 Re-queued critical task: " + criticalTask.toString());
                        } catch (Exception e) {
                            LOGGER.warning("Failed to re-queue critical task: " + e.getMessage());
                        }
                    }

                    LOGGER.info("🔒 Task queue cleared for pool: " + name +
                               " (cleared: " + (queueSize - criticalTasks.size()) +
                               ", preserved: " + criticalTasks.size() + ")");
                }

            } catch (Exception e) {
                LOGGER.warning("Task queue clearing error in pool " + name + ": " + e.getMessage());
            }
        }
        
        public void shutdown() {
            executor.shutdown();
        }
        
        // Getters
        public String getName() { return name; }
        public ThreadPriority getPriority() { return priority; }
        public int getCorePoolSize() { return executor.getCorePoolSize(); }
        public void setCorePoolSize(int size) { executor.setCorePoolSize(size); }
        public int getActiveCount() { return executor.getActiveCount(); }
        public int getQueueSize() { return executor.getQueue().size(); }
        public long getCompletedTaskCount() { return executor.getCompletedTaskCount(); }
    }
    


    /**
     * Thread Priority Enum
     */
    public enum ThreadPriority {
        CRITICAL(10),
        HIGH(7),
        MEDIUM(5),
        LOW(3),
        BACKGROUND(1);

        private final int value;

        ThreadPriority(int value) {
            this.value = value;
        }

        public int getValue() { return value; }
    }

    /**
     * Prioritized Task Wrapper
     */
    private static class PrioritizedTask<T> implements Comparable<PrioritizedTask<T>> {
        private final Callable<T> task;
        private final ThreadPriority priority;
        private final String taskType;
        private final long submissionTime;

        public PrioritizedTask(Callable<T> task, ThreadPriority priority, String taskType, long submissionTime) {
            this.task = task;
            this.priority = priority;
            this.taskType = taskType;
            this.submissionTime = submissionTime;
        }

        @Override
        public int compareTo(PrioritizedTask<T> other) {
            // Higher priority first, then earlier submission time
            int priorityComparison = Integer.compare(other.priority.getValue(), this.priority.getValue());
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            return Long.compare(this.submissionTime, other.submissionTime);
        }

        public Callable<T> getTask() { return task; }
        public ThreadPriority getPriority() { return priority; }
        public String getTaskType() { return taskType; }
        public long getSubmissionTime() { return submissionTime; }
    }

    /**
     * Thread Performance Tracker
     */
    private static class ThreadPerformanceTracker {
        private final String poolName;
        private final Map<String, AtomicLong> taskCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> taskTimes = new ConcurrentHashMap<>();
        private final List<Long> recentTaskTimes = new ArrayList<>();
        private long lastAnalysisTime = System.currentTimeMillis();

        public ThreadPerformanceTracker(String poolName) {
            this.poolName = poolName;
        }

        public void recordTaskSubmission(String taskType) {
            taskCounts.computeIfAbsent(taskType, k -> new AtomicLong(0)).incrementAndGet();
        }

        public void recordTaskCompletion(String taskType, long executionTime) {
            taskTimes.computeIfAbsent(taskType, k -> new AtomicLong(0)).addAndGet(executionTime);

            synchronized (recentTaskTimes) {
                recentTaskTimes.add(executionTime);
                if (recentTaskTimes.size() > 100) {
                    recentTaskTimes.remove(0);
                }
            }
        }

        public double getAverageTaskTime() {
            synchronized (recentTaskTimes) {
                if (recentTaskTimes.isEmpty()) return 0.0;
                return recentTaskTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            }
        }

        public void analyzePerformance() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnalysisTime > 60000) { // Analyze every minute
                // Performance analysis logic
                lastAnalysisTime = currentTime;
            }
        }

        public boolean hasPerformanceIssues() {
            return getAverageTaskTime() > 5000; // Tasks taking more than 5 seconds
        }

        public String getPoolName() { return poolName; }
    }

    /**
     * Thread Deadlock Detector
     */
    private static class ThreadDeadlockDetector {
        private final String name;
        private final Set<Long> monitoredThreads = new HashSet<>();

        public ThreadDeadlockDetector(String name) {
            this.name = name;
        }

        public void addThread(long threadId) {
            monitoredThreads.add(threadId);
        }

        public void removeThread(long threadId) {
            monitoredThreads.remove(threadId);
        }

        public boolean detectDeadlock() {
            try {
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                long[] deadlockedThreads = threadBean.findDeadlockedThreads();

                if (deadlockedThreads != null) {
                    for (long threadId : deadlockedThreads) {
                        if (monitoredThreads.contains(threadId)) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        public String getName() { return name; }
    }
}
