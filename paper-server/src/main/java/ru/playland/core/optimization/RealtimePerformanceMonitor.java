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

/**
 * Real-time Performance Monitor
 * РЕВОЛЮЦИОННЫЙ мониторинг производительности в реальном времени
 * Предупреждения, автооптимизация, анализ трендов, предсказание проблем
 */
public class RealtimePerformanceMonitor {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-RealtimeMonitor");
    
    // Performance monitoring statistics
    private final AtomicLong performanceChecks = new AtomicLong(0);
    private final AtomicLong warningsGenerated = new AtomicLong(0);
    private final AtomicLong autoOptimizationsTriggered = new AtomicLong(0);
    private final AtomicLong performanceAnomaliesDetected = new AtomicLong(0);
    private final AtomicLong trendAnalysisPerformed = new AtomicLong(0);
    private final AtomicLong predictiveActionsExecuted = new AtomicLong(0);
    
    // Performance tracking
    private final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    private final Map<String, PerformanceThreshold> thresholds = new ConcurrentHashMap<>();
    private final Map<String, PerformanceTrend> trends = new ConcurrentHashMap<>();
    private final List<PerformanceAlert> activeAlerts = new ArrayList<>();
    
    // Monitoring system
    private final ScheduledExecutorService performanceMonitor = Executors.newScheduledThreadPool(3);
    private final Map<String, Long> lastAlertTimes = new ConcurrentHashMap<>();
    private final List<PerformanceSnapshot> performanceHistory = new ArrayList<>();
    
    // Configuration
    private boolean enableRealtimeMonitoring = true;
    private boolean enableAutoOptimization = true;
    private boolean enablePredictiveAnalysis = true;
    private boolean enableTrendAnalysis = true;
    private boolean enableVanillaSafeMode = true;
    private boolean enablePerformanceAlerts = true;
    
    private long monitoringInterval = 1000; // 1 second
    private long alertCooldown = 30000; // 30 seconds
    private int maxHistorySize = 3600; // 1 hour of data
    private double criticalThreshold = 90.0; // 90%
    private double warningThreshold = 75.0; // 75%
    
    public void initialize() {
        LOGGER.info("📊 Initializing Real-time Performance Monitor...");
        
        loadMonitoringSettings();
        initializePerformanceMetrics();
        initializeThresholds();
        startRealtimeMonitoring();
        startTrendAnalysis();
        
        LOGGER.info("✅ Real-time Performance Monitor initialized!");
        LOGGER.info("📊 Realtime monitoring: " + (enableRealtimeMonitoring ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔧 Auto optimization: " + (enableAutoOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔮 Predictive analysis: " + (enablePredictiveAnalysis ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Trend analysis: " + (enableTrendAnalysis ? "ENABLED" : "DISABLED"));
        LOGGER.info("🛡️ Vanilla-safe mode: " + (enableVanillaSafeMode ? "ENABLED" : "DISABLED"));
        LOGGER.info("🚨 Performance alerts: " + (enablePerformanceAlerts ? "ENABLED" : "DISABLED"));
        LOGGER.info("⏰ Monitoring interval: " + monitoringInterval + "ms");
        LOGGER.info("🔔 Alert cooldown: " + (alertCooldown / 1000) + " seconds");
        LOGGER.info("📊 Max history size: " + maxHistorySize + " snapshots");
    }
    
    private void loadMonitoringSettings() {
        // Load real-time monitoring settings
        LOGGER.info("⚙️ Loading monitoring settings...");
    }
    
    private void initializePerformanceMetrics() {
        // Initialize performance metrics to track
        metrics.put("tps", new PerformanceMetric("tps", "Ticks Per Second", 20.0, 0.0, 20.0));
        metrics.put("memory_usage", new PerformanceMetric("memory_usage", "Memory Usage %", 0.0, 0.0, 100.0));
        metrics.put("cpu_usage", new PerformanceMetric("cpu_usage", "CPU Usage %", 0.0, 0.0, 100.0));
        metrics.put("player_count", new PerformanceMetric("player_count", "Online Players", 0.0, 0.0, 1000.0));
        metrics.put("chunk_count", new PerformanceMetric("chunk_count", "Loaded Chunks", 0.0, 0.0, 10000.0));
        metrics.put("entity_count", new PerformanceMetric("entity_count", "Total Entities", 0.0, 0.0, 50000.0));
        
        LOGGER.info("📊 Performance metrics initialized: " + metrics.size());
    }
    
    private void initializeThresholds() {
        // Initialize performance thresholds for alerts
        thresholds.put("tps", new PerformanceThreshold("tps", 15.0, 10.0, false)); // Lower is worse
        thresholds.put("memory_usage", new PerformanceThreshold("memory_usage", warningThreshold, criticalThreshold, true));
        thresholds.put("cpu_usage", new PerformanceThreshold("cpu_usage", warningThreshold, criticalThreshold, true));
        thresholds.put("player_count", new PerformanceThreshold("player_count", 800.0, 950.0, true));
        thresholds.put("chunk_count", new PerformanceThreshold("chunk_count", 8000.0, 9500.0, true));
        thresholds.put("entity_count", new PerformanceThreshold("entity_count", 40000.0, 47500.0, true));
        
        LOGGER.info("🚨 Performance thresholds initialized: " + thresholds.size());
    }
    
    private void startRealtimeMonitoring() {
        // Monitor performance every second
        performanceMonitor.scheduleAtFixedRate(() -> {
            try {
                collectPerformanceData();
                analyzePerformance();
                checkThresholds();
            } catch (Exception e) {
                LOGGER.warning("Real-time monitoring error: " + e.getMessage());
            }
        }, monitoringInterval, monitoringInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📊 Real-time monitoring started");
    }
    
    private void startTrendAnalysis() {
        if (!enableTrendAnalysis) return;
        
        // Analyze trends every 30 seconds
        performanceMonitor.scheduleAtFixedRate(() -> {
            try {
                analyzeTrends();
                performPredictiveAnalysis();
                cleanupOldData();
            } catch (Exception e) {
                LOGGER.warning("Trend analysis error: " + e.getMessage());
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📈 Trend analysis started");
    }
    
    /**
     * Collect current performance data
     */
    private void collectPerformanceData() {
        try {
            performanceChecks.incrementAndGet();
            
            // Collect current metrics
            updateMetric("tps", getCurrentTPS());
            updateMetric("memory_usage", getCurrentMemoryUsage());
            updateMetric("cpu_usage", getCurrentCPUUsage());
            updateMetric("player_count", getCurrentPlayerCount());
            updateMetric("chunk_count", getCurrentChunkCount());
            updateMetric("entity_count", getCurrentEntityCount());
            
            // Create performance snapshot
            PerformanceSnapshot snapshot = new PerformanceSnapshot(System.currentTimeMillis(), new ConcurrentHashMap<>(metrics));
            
            // Add to history
            synchronized (performanceHistory) {
                performanceHistory.add(snapshot);
                
                // Limit history size
                if (performanceHistory.size() > maxHistorySize) {
                    performanceHistory.remove(0);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Performance data collection error: " + e.getMessage());
        }
    }
    
    /**
     * Update performance metric value
     */
    private void updateMetric(String name, double value) {
        PerformanceMetric metric = metrics.get(name);
        if (metric != null) {
            metric.updateValue(value);
        }
    }
    
    /**
     * Get current TPS
     */
    private double getCurrentTPS() {
        // Simplified TPS calculation
        // In real implementation, this would get actual server TPS
        return 20.0 - (Math.random() * 2.0); // Simulate TPS between 18-20
    }
    
    /**
     * Get current memory usage percentage
     */
    private double getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (used * 100.0) / max;
    }
    
    /**
     * Get current CPU usage percentage
     */
    private double getCurrentCPUUsage() {
        // Simplified CPU usage estimation
        double memoryUsage = getCurrentMemoryUsage();
        return Math.min(100.0, memoryUsage * 1.2 + (Math.random() * 10.0));
    }
    
    /**
     * Get current player count
     */
    private double getCurrentPlayerCount() {
        // Placeholder - would get actual player count
        return Math.random() * 50; // Simulate 0-50 players
    }
    
    /**
     * Get current chunk count
     */
    private double getCurrentChunkCount() {
        // Placeholder - would get actual chunk count
        return 1000 + (Math.random() * 2000); // Simulate 1000-3000 chunks
    }
    
    /**
     * Get current entity count
     */
    private double getCurrentEntityCount() {
        // Placeholder - would get actual entity count
        return 5000 + (Math.random() * 10000); // Simulate 5000-15000 entities
    }
    
    /**
     * Analyze current performance
     */
    private void analyzePerformance() {
        try {
            // Check for performance anomalies
            detectPerformanceAnomalies();
            
            // Update performance trends
            updatePerformanceTrends();
            
            // Trigger auto-optimization if needed
            if (enableAutoOptimization) {
                checkAutoOptimizationTriggers();
            }
            
        } catch (Exception e) {
            LOGGER.warning("Performance analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Detect performance anomalies
     */
    private void detectPerformanceAnomalies() {
        try {
            for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
                String metricName = entry.getKey();
                PerformanceMetric metric = entry.getValue();
                
                if (metric.hasAnomaly()) {
                    performanceAnomaliesDetected.incrementAndGet();
                    LOGGER.warning("🚨 Performance anomaly detected in " + metricName + ": " + 
                                 String.format("%.2f", metric.getCurrentValue()));
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Anomaly detection error: " + e.getMessage());
        }
    }
    
    /**
     * Update performance trends
     */
    private void updatePerformanceTrends() {
        try {
            for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
                String metricName = entry.getKey();
                PerformanceMetric metric = entry.getValue();
                
                PerformanceTrend trend = trends.computeIfAbsent(metricName, k -> new PerformanceTrend(metricName));
                trend.addDataPoint(metric.getCurrentValue());
            }
            
        } catch (Exception e) {
            LOGGER.warning("Trend update error: " + e.getMessage());
        }
    }
    
    /**
     * Check thresholds and generate alerts
     */
    private void checkThresholds() {
        if (!enablePerformanceAlerts) return;
        
        try {
            long currentTime = System.currentTimeMillis();
            
            for (Map.Entry<String, PerformanceThreshold> entry : thresholds.entrySet()) {
                String metricName = entry.getKey();
                PerformanceThreshold threshold = entry.getValue();
                PerformanceMetric metric = metrics.get(metricName);
                
                if (metric != null) {
                    AlertLevel alertLevel = threshold.checkThreshold(metric.getCurrentValue());
                    
                    if (alertLevel != AlertLevel.NORMAL) {
                        // Check cooldown
                        Long lastAlert = lastAlertTimes.get(metricName);
                        if (lastAlert == null || currentTime - lastAlert > alertCooldown) {
                            generateAlert(metricName, alertLevel, metric.getCurrentValue());
                            lastAlertTimes.put(metricName, currentTime);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Threshold checking error: " + e.getMessage());
        }
    }
    
    /**
     * Generate performance alert
     */
    private void generateAlert(String metricName, AlertLevel level, double value) {
        try {
            PerformanceAlert alert = new PerformanceAlert(metricName, level, value, System.currentTimeMillis());
            
            synchronized (activeAlerts) {
                activeAlerts.add(alert);
                
                // Limit active alerts
                if (activeAlerts.size() > 100) {
                    activeAlerts.remove(0);
                }
            }
            
            warningsGenerated.incrementAndGet();
            
            String levelStr = level == AlertLevel.CRITICAL ? "🔴 CRITICAL" : "🟡 WARNING";
            LOGGER.warning(levelStr + " Performance Alert - " + metricName + ": " + String.format("%.2f", value));
            
            // Trigger auto-optimization for critical alerts
            if (level == AlertLevel.CRITICAL && enableAutoOptimization) {
                triggerAutoOptimization(metricName, value);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Alert generation error: " + e.getMessage());
        }
    }
    
    /**
     * Check auto-optimization triggers
     */
    private void checkAutoOptimizationTriggers() {
        try {
            // Check if multiple metrics are in warning state
            int warningCount = 0;
            
            for (Map.Entry<String, PerformanceThreshold> entry : thresholds.entrySet()) {
                String metricName = entry.getKey();
                PerformanceThreshold threshold = entry.getValue();
                PerformanceMetric metric = metrics.get(metricName);
                
                if (metric != null && threshold.checkThreshold(metric.getCurrentValue()) != AlertLevel.NORMAL) {
                    warningCount++;
                }
            }
            
            // Trigger optimization if multiple metrics are problematic
            if (warningCount >= 3) {
                triggerAutoOptimization("multiple_metrics", warningCount);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Auto-optimization trigger check error: " + e.getMessage());
        }
    }
    
    /**
     * Trigger automatic optimization
     */
    private void triggerAutoOptimization(String reason, double value) {
        try {
            autoOptimizationsTriggered.incrementAndGet();
            
            LOGGER.info("🔧 Auto-optimization triggered for: " + reason + " (value: " + String.format("%.2f", value) + ")");
            
            // Implement specific optimizations based on the metric
            switch (reason) {
                case "memory_usage":
                    optimizeMemoryUsage();
                    break;
                case "cpu_usage":
                    optimizeCPUUsage();
                    break;
                case "tps":
                    optimizeTPS();
                    break;
                case "entity_count":
                    optimizeEntityCount();
                    break;
                default:
                    performGeneralOptimization();
                    break;
            }
            
        } catch (Exception e) {
            LOGGER.warning("Auto-optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize memory usage
     */
    private void optimizeMemoryUsage() {
        LOGGER.info("🧹 Triggering memory optimization...");
        System.gc(); // Suggest garbage collection
    }
    
    /**
     * Optimize CPU usage
     */
    private void optimizeCPUUsage() {
        LOGGER.info("⚡ Triggering CPU optimization...");
        // Would implement CPU optimization strategies
    }
    
    /**
     * Optimize TPS
     */
    private void optimizeTPS() {
        LOGGER.info("🎯 Triggering TPS optimization...");
        // Would implement TPS optimization strategies
    }
    
    /**
     * Optimize entity count
     */
    private void optimizeEntityCount() {
        LOGGER.info("🐄 Triggering entity optimization...");
        // Would implement entity optimization strategies
    }
    
    /**
     * Perform general optimization
     */
    private void performGeneralOptimization() {
        LOGGER.info("🔧 Triggering general optimization...");
        // Would implement general optimization strategies
    }
    
    /**
     * Analyze performance trends
     */
    private void analyzeTrends() {
        if (!enableTrendAnalysis) return;
        
        try {
            trendAnalysisPerformed.incrementAndGet();
            
            for (PerformanceTrend trend : trends.values()) {
                trend.analyzeTrend();
                
                if (trend.hasSignificantTrend()) {
                    LOGGER.info("📈 Trend detected in " + trend.getMetricName() + ": " + trend.getTrendDirection());
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Trend analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Perform predictive analysis
     */
    private void performPredictiveAnalysis() {
        if (!enablePredictiveAnalysis) return;
        
        try {
            for (PerformanceTrend trend : trends.values()) {
                if (trend.canPredict()) {
                    double predictedValue = trend.predictNextValue();
                    String metricName = trend.getMetricName();
                    
                    PerformanceThreshold threshold = thresholds.get(metricName);
                    if (threshold != null) {
                        AlertLevel predictedLevel = threshold.checkThreshold(predictedValue);
                        
                        if (predictedLevel == AlertLevel.CRITICAL) {
                            LOGGER.warning("🔮 Predicted critical issue in " + metricName + 
                                         " (predicted: " + String.format("%.2f", predictedValue) + ")");
                            
                            // Take predictive action
                            takePredictiveAction(metricName, predictedValue);
                            predictiveActionsExecuted.incrementAndGet();
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Predictive analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Take predictive action to prevent issues
     */
    private void takePredictiveAction(String metricName, double predictedValue) {
        LOGGER.info("🔮 Taking predictive action for " + metricName + " (predicted: " + String.format("%.2f", predictedValue) + ")");
        
        // Implement predictive actions based on metric
        switch (metricName) {
            case "memory_usage":
                LOGGER.info("🔮 Preemptive memory cleanup...");
                System.gc();
                break;
            case "tps":
                LOGGER.info("🔮 Preemptive TPS optimization...");
                break;
            default:
                LOGGER.info("🔮 Preemptive general optimization...");
                break;
        }
    }
    
    /**
     * Clean up old performance data
     */
    private void cleanupOldData() {
        try {
            // Clean up old alerts
            synchronized (activeAlerts) {
                long cutoffTime = System.currentTimeMillis() - 3600000; // 1 hour
                activeAlerts.removeIf(alert -> alert.getTimestamp() < cutoffTime);
            }
            
            // Clean up old alert times
            long alertCutoffTime = System.currentTimeMillis() - alertCooldown * 10;
            lastAlertTimes.entrySet().removeIf(entry -> entry.getValue() < alertCutoffTime);
            
        } catch (Exception e) {
            LOGGER.warning("Data cleanup error: " + e.getMessage());
        }
    }
    
    /**
     * Get performance monitoring statistics
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("performance_checks", performanceChecks.get());
        stats.put("warnings_generated", warningsGenerated.get());
        stats.put("auto_optimizations_triggered", autoOptimizationsTriggered.get());
        stats.put("performance_anomalies_detected", performanceAnomaliesDetected.get());
        stats.put("trend_analysis_performed", trendAnalysisPerformed.get());
        stats.put("predictive_actions_executed", predictiveActionsExecuted.get());
        
        stats.put("tracked_metrics", metrics.size());
        stats.put("performance_thresholds", thresholds.size());
        stats.put("performance_trends", trends.size());
        
        synchronized (activeAlerts) {
            stats.put("active_alerts", activeAlerts.size());
        }
        
        synchronized (performanceHistory) {
            stats.put("performance_history_size", performanceHistory.size());
        }
        
        // Current metric values
        Map<String, Double> currentValues = new ConcurrentHashMap<>();
        for (Map.Entry<String, PerformanceMetric> entry : metrics.entrySet()) {
            currentValues.put(entry.getKey(), entry.getValue().getCurrentValue());
        }
        stats.put("current_metrics", currentValues);
        
        return stats;
    }
    
    // Getters
    public long getPerformanceChecks() { return performanceChecks.get(); }
    public long getWarningsGenerated() { return warningsGenerated.get(); }
    public long getAutoOptimizationsTriggered() { return autoOptimizationsTriggered.get(); }
    public long getPerformanceAnomaliesDetected() { return performanceAnomaliesDetected.get(); }
    public long getTrendAnalysisPerformed() { return trendAnalysisPerformed.get(); }
    public long getPredictiveActionsExecuted() { return predictiveActionsExecuted.get(); }
    
    public void shutdown() {
        performanceMonitor.shutdown();
        
        // Clear all data
        metrics.clear();
        thresholds.clear();
        trends.clear();
        activeAlerts.clear();
        lastAlertTimes.clear();
        performanceHistory.clear();
        
        LOGGER.info("📊 Real-time Performance Monitor shutdown complete");
    }
    
    // Helper classes and enums
    public enum AlertLevel {
        NORMAL, WARNING, CRITICAL
    }
    
    private static class PerformanceMetric {
        private final String name;
        private final String description;
        private double currentValue;
        private double minValue;
        private double maxValue;
        private final List<Double> recentValues = new ArrayList<>();
        
        public PerformanceMetric(String name, String description, double initialValue, double minValue, double maxValue) {
            this.name = name;
            this.description = description;
            this.currentValue = initialValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        public void updateValue(double value) {
            this.currentValue = value;
            
            synchronized (recentValues) {
                recentValues.add(value);
                if (recentValues.size() > 60) { // Keep last 60 values
                    recentValues.remove(0);
                }
            }
        }
        
        public boolean hasAnomaly() {
            synchronized (recentValues) {
                if (recentValues.size() < 10) return false;
                
                // Simple anomaly detection: value is significantly different from recent average
                double average = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(currentValue);
                double deviation = Math.abs(currentValue - average);
                double threshold = Math.abs(maxValue - minValue) * 0.2; // 20% of range
                
                return deviation > threshold;
            }
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getCurrentValue() { return currentValue; }
        public double getMinValue() { return minValue; }
        public double getMaxValue() { return maxValue; }
    }
    
    private static class PerformanceThreshold {
        private final String metricName;
        private final double warningThreshold;
        private final double criticalThreshold;
        private final boolean higherIsWorse;
        
        public PerformanceThreshold(String metricName, double warningThreshold, double criticalThreshold, boolean higherIsWorse) {
            this.metricName = metricName;
            this.warningThreshold = warningThreshold;
            this.criticalThreshold = criticalThreshold;
            this.higherIsWorse = higherIsWorse;
        }
        
        public AlertLevel checkThreshold(double value) {
            if (higherIsWorse) {
                if (value >= criticalThreshold) return AlertLevel.CRITICAL;
                if (value >= warningThreshold) return AlertLevel.WARNING;
            } else {
                if (value <= criticalThreshold) return AlertLevel.CRITICAL;
                if (value <= warningThreshold) return AlertLevel.WARNING;
            }
            return AlertLevel.NORMAL;
        }
        
        public String getMetricName() { return metricName; }
    }
    
    private static class PerformanceTrend {
        private final String metricName;
        private final List<Double> dataPoints = new ArrayList<>();
        private String trendDirection = "STABLE";
        
        public PerformanceTrend(String metricName) {
            this.metricName = metricName;
        }
        
        public void addDataPoint(double value) {
            synchronized (dataPoints) {
                dataPoints.add(value);
                if (dataPoints.size() > 120) { // Keep last 2 minutes of data
                    dataPoints.remove(0);
                }
            }
        }
        
        public void analyzeTrend() {
            synchronized (dataPoints) {
                if (dataPoints.size() < 10) return;
                
                // Simple trend analysis
                double firstHalf = dataPoints.subList(0, dataPoints.size() / 2).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                double secondHalf = dataPoints.subList(dataPoints.size() / 2, dataPoints.size()).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                
                double change = secondHalf - firstHalf;
                double changePercent = Math.abs(change) / firstHalf * 100;
                
                if (changePercent > 10) { // Significant change
                    trendDirection = change > 0 ? "INCREASING" : "DECREASING";
                } else {
                    trendDirection = "STABLE";
                }
            }
        }
        
        public boolean hasSignificantTrend() {
            return !trendDirection.equals("STABLE");
        }
        
        public boolean canPredict() {
            synchronized (dataPoints) {
                return dataPoints.size() >= 30 && hasSignificantTrend();
            }
        }
        
        public double predictNextValue() {
            synchronized (dataPoints) {
                if (dataPoints.size() < 10) return 0.0;
                
                // Simple linear prediction
                double recent = dataPoints.get(dataPoints.size() - 1);
                double previous = dataPoints.get(dataPoints.size() - 10);
                double trend = (recent - previous) / 10;
                
                return recent + trend * 5; // Predict 5 steps ahead
            }
        }
        
        public String getMetricName() { return metricName; }
        public String getTrendDirection() { return trendDirection; }
    }
    
    private static class PerformanceAlert {
        private final String metricName;
        private final AlertLevel level;
        private final double value;
        private final long timestamp;
        
        public PerformanceAlert(String metricName, AlertLevel level, double value, long timestamp) {
            this.metricName = metricName;
            this.level = level;
            this.value = value;
            this.timestamp = timestamp;
        }
        
        public String getMetricName() { return metricName; }
        public AlertLevel getLevel() { return level; }
        public double getValue() { return value; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class PerformanceSnapshot {
        private final long timestamp;
        private final Map<String, PerformanceMetric> metrics;
        
        public PerformanceSnapshot(long timestamp, Map<String, PerformanceMetric> metrics) {
            this.timestamp = timestamp;
            this.metrics = new ConcurrentHashMap<>(metrics);
        }
        
        public long getTimestamp() { return timestamp; }
        public Map<String, PerformanceMetric> getMetrics() { return metrics; }
    }
}
