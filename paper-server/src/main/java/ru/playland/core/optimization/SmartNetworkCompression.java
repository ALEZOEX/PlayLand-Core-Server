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
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * Smart Network Compression
 * УМНОЕ сжатие сетевого трафика
 * Адаптивное сжатие, анализ паттернов, оптимизация пропускной способности
 */
public class SmartNetworkCompression {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-SmartNetwork");
    
    // Network compression statistics
    private final AtomicLong packetsCompressed = new AtomicLong(0);
    private final AtomicLong bytesCompressed = new AtomicLong(0);
    private final AtomicLong bytesSaved = new AtomicLong(0);
    private final AtomicLong compressionOptimizations = new AtomicLong(0);
    private final AtomicLong adaptiveChanges = new AtomicLong(0);
    private final AtomicLong patternOptimizations = new AtomicLong(0);
    
    // Compression tracking
    private final Map<String, CompressionProfile> playerProfiles = new ConcurrentHashMap<>();
    private final Map<String, PacketPattern> packetPatterns = new ConcurrentHashMap<>();
    private final Map<String, CompressionStats> compressionStats = new ConcurrentHashMap<>();
    
    // Network optimization
    private final ScheduledExecutorService networkOptimizer = Executors.newScheduledThreadPool(2);
    private final Map<String, Long> lastOptimization = new ConcurrentHashMap<>();
    private final List<CompressionAlgorithm> algorithms = new ArrayList<>();
    
    // Configuration
    private boolean enableSmartCompression = true;
    private boolean enableAdaptiveCompression = true;
    private boolean enablePatternAnalysis = true;
    private boolean enableBandwidthOptimization = true;
    private boolean enableRealTimeOptimization = true;
    
    private int baseCompressionLevel = 6; // 1-9
    private int maxCompressionLevel = 9;
    private int minPacketSize = 64; // bytes
    private double compressionThreshold = 0.8; // 80% compression ratio
    private long optimizationInterval = 10000; // 10 seconds
    
    public void initialize() {
        LOGGER.info("🌐 Initializing Smart Network Compression...");
        
        loadNetworkSettings();
        initializeCompressionAlgorithms();
        startNetworkOptimization();
        startPatternAnalysis();
        
        LOGGER.info("✅ Smart Network Compression initialized!");
        LOGGER.info("🌐 Smart compression: " + (enableSmartCompression ? "ENABLED" : "DISABLED"));
        LOGGER.info("📈 Adaptive compression: " + (enableAdaptiveCompression ? "ENABLED" : "DISABLED"));
        LOGGER.info("🔍 Pattern analysis: " + (enablePatternAnalysis ? "ENABLED" : "DISABLED"));
        LOGGER.info("📊 Bandwidth optimization: " + (enableBandwidthOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("⚡ Real-time optimization: " + (enableRealTimeOptimization ? "ENABLED" : "DISABLED"));
        LOGGER.info("🗜️ Base compression level: " + baseCompressionLevel);
        LOGGER.info("📏 Min packet size: " + minPacketSize + " bytes");
        LOGGER.info("📊 Compression threshold: " + (compressionThreshold * 100) + "%");
    }
    
    private void loadNetworkSettings() {
        try {
            LOGGER.info("⚙️ Loading network settings...");

            // Load network compression parameters from system properties
            enableSmartCompression = Boolean.parseBoolean(System.getProperty("playland.network.smart.enabled", "true"));
            enableAdaptiveCompression = Boolean.parseBoolean(System.getProperty("playland.network.adaptive.enabled", "true"));
            enablePatternAnalysis = Boolean.parseBoolean(System.getProperty("playland.network.pattern.analysis", "true"));
            enableBandwidthOptimization = Boolean.parseBoolean(System.getProperty("playland.network.bandwidth.optimization", "true"));
            enableRealTimeOptimization = Boolean.parseBoolean(System.getProperty("playland.network.realtime.optimization", "true"));

            // Load compression parameters
            baseCompressionLevel = Integer.parseInt(System.getProperty("playland.network.compression.level", "6"));
            maxCompressionLevel = Integer.parseInt(System.getProperty("playland.network.compression.max", "9"));
            minPacketSize = Integer.parseInt(System.getProperty("playland.network.packet.min.size", "64"));
            compressionThreshold = Double.parseDouble(System.getProperty("playland.network.compression.threshold", "0.8"));
            optimizationInterval = Long.parseLong(System.getProperty("playland.network.optimization.interval", "10000"));

            // Validate compression level
            baseCompressionLevel = Math.max(1, Math.min(9, baseCompressionLevel));
            maxCompressionLevel = Math.max(baseCompressionLevel, Math.min(9, maxCompressionLevel));

            // Auto-adjust based on server performance
            double currentTPS = getCurrentTPS();
            if (currentTPS < 18.0) {
                // Low TPS - reduce compression complexity
                baseCompressionLevel = Math.max(1, baseCompressionLevel - 2);
                maxCompressionLevel = Math.max(baseCompressionLevel, maxCompressionLevel - 1);
                minPacketSize = Math.min(256, minPacketSize * 2);
                compressionThreshold = Math.min(0.95, compressionThreshold + 0.1);
                enableRealTimeOptimization = false;
                LOGGER.info("🔧 Reduced compression complexity for low TPS: level=" + baseCompressionLevel +
                           ", threshold=" + (compressionThreshold * 100) + "%");
            } else if (currentTPS > 19.5) {
                // Good TPS - allow more aggressive compression
                baseCompressionLevel = Math.min(maxCompressionLevel, baseCompressionLevel + 1);
                minPacketSize = Math.max(32, minPacketSize / 2);
                compressionThreshold = Math.max(0.5, compressionThreshold - 0.1);
                LOGGER.info("🔧 Increased compression aggressiveness for good TPS: level=" + baseCompressionLevel +
                           ", threshold=" + (compressionThreshold * 100) + "%");
            }

            // Auto-adjust based on memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;

            if (memoryUsage > 0.8) {
                // High memory usage - reduce compression overhead
                enablePatternAnalysis = false;
                enableRealTimeOptimization = false;
                optimizationInterval = Math.max(30000, optimizationInterval * 2);
                LOGGER.warning("⚠️ High memory usage - reduced compression overhead: interval=" + optimizationInterval + "ms");
            }

            // Auto-adjust based on player count
            int playerCount = getOnlinePlayerCount();
            if (playerCount > 100) {
                // Many players - optimize for bandwidth
                baseCompressionLevel = Math.min(maxCompressionLevel, baseCompressionLevel + 1);
                minPacketSize = Math.max(32, minPacketSize - 16);
                enableBandwidthOptimization = true;
                LOGGER.info("🔧 Optimized for " + playerCount + " players: level=" + baseCompressionLevel +
                           ", minSize=" + minPacketSize);
            } else if (playerCount < 10) {
                // Few players - reduce compression overhead
                baseCompressionLevel = Math.max(1, baseCompressionLevel - 1);
                enablePatternAnalysis = false;
                LOGGER.info("🔧 Reduced overhead for " + playerCount + " players: level=" + baseCompressionLevel);
            }

            // Auto-adjust based on network conditions
            double networkLatency = estimateNetworkLatency();
            if (networkLatency > 100.0) {
                // High latency - prioritize compression ratio
                baseCompressionLevel = Math.min(maxCompressionLevel, baseCompressionLevel + 2);
                compressionThreshold = Math.max(0.4, compressionThreshold - 0.2);
                LOGGER.info("🔧 High latency detected (" + String.format("%.1f", networkLatency) + "ms) - prioritizing compression");
            } else if (networkLatency < 20.0) {
                // Low latency - prioritize speed
                baseCompressionLevel = Math.max(1, baseCompressionLevel - 1);
                compressionThreshold = Math.min(0.9, compressionThreshold + 0.1);
                LOGGER.info("🔧 Low latency detected (" + String.format("%.1f", networkLatency) + "ms) - prioritizing speed");
            }

            LOGGER.info("✅ Network settings loaded - Compression: " + baseCompressionLevel +
                       ", Min packet: " + minPacketSize + " bytes, Threshold: " + (compressionThreshold * 100) +
                       "%, Interval: " + optimizationInterval + "ms");

        } catch (Exception e) {
            LOGGER.warning("❌ Error loading network settings: " + e.getMessage() + " - using defaults");
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

    private double estimateNetworkLatency() {
        try {
            // Simplified network latency estimation
            // In real implementation, this would measure actual ping times
            double totalLatency = 0.0;
            int playerCount = 0;

            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    // Get player ping (Paper API)
                    int ping = player.getPing();
                    totalLatency += ping;
                    playerCount++;
                } catch (Exception e) {
                    // Fallback for players without ping data
                    totalLatency += 50.0; // Default moderate latency
                    playerCount++;
                }
            }

            return playerCount > 0 ? totalLatency / playerCount : 50.0;

        } catch (Exception e) {
            return 50.0; // Default moderate latency
        }
    }
    
    private void initializeCompressionAlgorithms() {
        // Initialize different compression algorithms
        algorithms.add(new CompressionAlgorithm("DEFLATE", Deflater.DEFLATED, 6));
        algorithms.add(new CompressionAlgorithm("BEST_SPEED", Deflater.DEFLATED, 1));
        algorithms.add(new CompressionAlgorithm("BEST_COMPRESSION", Deflater.DEFLATED, 9));
        algorithms.add(new CompressionAlgorithm("BALANCED", Deflater.DEFLATED, 6));
        
        LOGGER.info("🗜️ Compression algorithms initialized: " + algorithms.size());
    }
    
    private void startNetworkOptimization() {
        // Optimize network compression every 10 seconds
        networkOptimizer.scheduleAtFixedRate(() -> {
            try {
                optimizeCompressionProfiles();
                analyzeNetworkPerformance();
            } catch (Exception e) {
                LOGGER.warning("Network optimization error: " + e.getMessage());
            }
        }, optimizationInterval, optimizationInterval, TimeUnit.MILLISECONDS);
        
        LOGGER.info("📊 Network optimization started");
    }
    
    private void startPatternAnalysis() {
        if (!enablePatternAnalysis) return;
        
        // Analyze packet patterns every 30 seconds
        networkOptimizer.scheduleAtFixedRate(() -> {
            try {
                analyzePacketPatterns();
                optimizeCompressionStrategies();
            } catch (Exception e) {
                LOGGER.warning("Pattern analysis error: " + e.getMessage());
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("🔍 Pattern analysis started");
    }
    
    /**
     * Compress packet data with smart optimization
     */
    public byte[] compressPacket(String playerId, String packetType, byte[] data) {
        if (!enableSmartCompression || data.length < minPacketSize) {
            return data; // Don't compress small packets
        }
        
        try {
            // Get or create compression profile for player
            CompressionProfile profile = getCompressionProfile(playerId);
            
            // Select optimal compression algorithm
            CompressionAlgorithm algorithm = selectOptimalAlgorithm(profile, packetType, data);
            
            // Compress data
            byte[] compressedData = compressData(data, algorithm);
            
            // Check compression efficiency
            double compressionRatio = (double) compressedData.length / data.length;
            
            if (compressionRatio < compressionThreshold) {
                // Good compression - update statistics
                packetsCompressed.incrementAndGet();
                bytesCompressed.addAndGet(data.length);
                bytesSaved.addAndGet(data.length - compressedData.length);
                
                // Update profile
                profile.updateCompressionStats(packetType, data.length, compressedData.length);
                
                // Track packet pattern
                trackPacketPattern(playerId, packetType, data.length, compressionRatio);
                
                return compressedData;
            } else {
                // Poor compression - return original data
                return data;
            }
            
        } catch (Exception e) {
            LOGGER.warning("Packet compression error: " + e.getMessage());
            return data; // Return original data on error
        }
    }
    
    /**
     * Decompress packet data
     */
    public byte[] decompressPacket(String playerId, String packetType, byte[] compressedData) {
        try {
            CompressionProfile profile = getCompressionProfile(playerId);
            CompressionAlgorithm algorithm = profile.getPreferredAlgorithm(packetType);
            
            return decompressData(compressedData, algorithm);
            
        } catch (Exception e) {
            LOGGER.warning("Packet decompression error: " + e.getMessage());
            return compressedData; // Return original data on error
        }
    }
    
    /**
     * Get compression profile for player
     */
    private CompressionProfile getCompressionProfile(String playerId) {
        return playerProfiles.computeIfAbsent(playerId, k -> new CompressionProfile(playerId));
    }
    
    /**
     * Select optimal compression algorithm
     */
    private CompressionAlgorithm selectOptimalAlgorithm(CompressionProfile profile, String packetType, byte[] data) {
        if (!enableAdaptiveCompression) {
            return algorithms.get(0); // Use default algorithm
        }
        
        // Check if we have historical data for this packet type
        CompressionAlgorithm preferred = profile.getPreferredAlgorithm(packetType);
        if (preferred != null) {
            return preferred;
        }
        
        // Analyze data characteristics
        if (data.length > 1024) {
            // Large packets - use best compression
            return algorithms.stream()
                .filter(a -> a.getName().equals("BEST_COMPRESSION"))
                .findFirst()
                .orElse(algorithms.get(0));
        } else if (data.length < 256) {
            // Small packets - use fast compression
            return algorithms.stream()
                .filter(a -> a.getName().equals("BEST_SPEED"))
                .findFirst()
                .orElse(algorithms.get(0));
        } else {
            // Medium packets - use balanced compression
            return algorithms.stream()
                .filter(a -> a.getName().equals("BALANCED"))
                .findFirst()
                .orElse(algorithms.get(0));
        }
    }
    
    /**
     * Compress data using specified algorithm
     */
    private byte[] compressData(byte[] data, CompressionAlgorithm algorithm) throws Exception {
        Deflater deflater = new Deflater(algorithm.getLevel());
        deflater.setInput(data);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        
        deflater.end();
        return outputStream.toByteArray();
    }
    
    /**
     * Decompress data using specified algorithm
     */
    private byte[] decompressData(byte[] compressedData, CompressionAlgorithm algorithm) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        
        inflater.end();
        return outputStream.toByteArray();
    }
    
    /**
     * Track packet patterns for optimization
     */
    private void trackPacketPattern(String playerId, String packetType, int originalSize, double compressionRatio) {
        if (!enablePatternAnalysis) return;
        
        String patternKey = playerId + "_" + packetType;
        PacketPattern pattern = packetPatterns.computeIfAbsent(patternKey, k -> new PacketPattern(playerId, packetType));
        
        pattern.addSample(originalSize, compressionRatio);
    }
    
    /**
     * Optimize compression profiles
     */
    private void optimizeCompressionProfiles() {
        try {
            for (CompressionProfile profile : playerProfiles.values()) {
                if (profile.needsOptimization()) {
                    optimizeProfile(profile);
                    compressionOptimizations.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Profile optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize individual compression profile
     */
    private void optimizeProfile(CompressionProfile profile) {
        try {
            // Analyze compression statistics
            Map<String, CompressionStats> stats = profile.getCompressionStats();
            
            for (Map.Entry<String, CompressionStats> entry : stats.entrySet()) {
                String packetType = entry.getKey();
                CompressionStats stat = entry.getValue();
                
                // Find best algorithm for this packet type
                CompressionAlgorithm bestAlgorithm = findBestAlgorithm(stat);
                profile.setPreferredAlgorithm(packetType, bestAlgorithm);
            }
            
            profile.markOptimized();
            
        } catch (Exception e) {
            LOGGER.warning("Profile optimization error for " + profile.getPlayerId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Find best compression algorithm based on statistics
     */
    private CompressionAlgorithm findBestAlgorithm(CompressionStats stats) {
        double avgCompressionRatio = stats.getAverageCompressionRatio();
        int avgPacketSize = stats.getAveragePacketSize();
        
        if (avgCompressionRatio < 0.5 && avgPacketSize > 512) {
            // Good compression on large packets - use best compression
            return algorithms.stream()
                .filter(a -> a.getName().equals("BEST_COMPRESSION"))
                .findFirst()
                .orElse(algorithms.get(0));
        } else if (avgCompressionRatio > 0.8 || avgPacketSize < 128) {
            // Poor compression or small packets - use fast compression
            return algorithms.stream()
                .filter(a -> a.getName().equals("BEST_SPEED"))
                .findFirst()
                .orElse(algorithms.get(0));
        } else {
            // Balanced approach
            return algorithms.stream()
                .filter(a -> a.getName().equals("BALANCED"))
                .findFirst()
                .orElse(algorithms.get(0));
        }
    }
    
    /**
     * Analyze network performance
     */
    private void analyzeNetworkPerformance() {
        try {
            long totalPackets = packetsCompressed.get();
            long totalBytes = bytesCompressed.get();
            long totalSaved = bytesSaved.get();
            
            if (totalBytes > 0) {
                double overallCompressionRatio = (double) (totalBytes - totalSaved) / totalBytes;
                double bandwidthSavings = (double) totalSaved / totalBytes * 100;
                
                // Log performance metrics periodically
                if (System.currentTimeMillis() % 60000 < optimizationInterval) { // Every minute
                    LOGGER.info("🌐 Network compression - Packets: " + totalPackets + 
                               ", Compression ratio: " + String.format("%.1f%%", overallCompressionRatio * 100) +
                               ", Bandwidth saved: " + String.format("%.1f%%", bandwidthSavings));
                }
                
                // Trigger adaptive changes if needed
                if (enableAdaptiveCompression && shouldAdaptCompression(overallCompressionRatio)) {
                    adaptCompressionSettings(overallCompressionRatio);
                    adaptiveChanges.incrementAndGet();
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Network performance analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze packet patterns
     */
    private void analyzePacketPatterns() {
        if (!enablePatternAnalysis) return;
        
        try {
            for (PacketPattern pattern : packetPatterns.values()) {
                if (pattern.hasEnoughSamples()) {
                    analyzePattern(pattern);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Packet pattern analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze individual packet pattern
     */
    private void analyzePattern(PacketPattern pattern) {
        try {
            double avgCompressionRatio = pattern.getAverageCompressionRatio();
            int avgPacketSize = pattern.getAveragePacketSize();
            
            // Check if pattern suggests optimization opportunity
            if (avgCompressionRatio > 0.9) {
                // Poor compression - consider skipping compression for this pattern
                pattern.setShouldCompress(false);
                patternOptimizations.incrementAndGet();
                
                LOGGER.fine("🔍 Pattern optimization: Disabling compression for " + 
                           pattern.getPacketType() + " (poor compression ratio: " + 
                           String.format("%.1f%%", avgCompressionRatio * 100) + ")");
            } else if (avgCompressionRatio < 0.3) {
                // Excellent compression - prioritize this pattern
                pattern.setShouldCompress(true);
                pattern.setPriority(PacketPattern.Priority.HIGH);
                patternOptimizations.incrementAndGet();
                
                LOGGER.fine("🔍 Pattern optimization: Prioritizing compression for " + 
                           pattern.getPacketType() + " (excellent compression ratio: " + 
                           String.format("%.1f%%", avgCompressionRatio * 100) + ")");
            }
            
        } catch (Exception e) {
            LOGGER.warning("Pattern analysis error: " + e.getMessage());
        }
    }
    
    /**
     * Optimize compression strategies
     */
    private void optimizeCompressionStrategies() {
        try {
            // Global optimization based on all patterns
            double globalCompressionRatio = calculateGlobalCompressionRatio();
            
            if (globalCompressionRatio > 0.8) {
                // Overall poor compression - reduce compression level
                baseCompressionLevel = Math.max(1, baseCompressionLevel - 1);
                LOGGER.info("🔧 Reducing base compression level to " + baseCompressionLevel);
            } else if (globalCompressionRatio < 0.4) {
                // Overall excellent compression - increase compression level
                baseCompressionLevel = Math.min(maxCompressionLevel, baseCompressionLevel + 1);
                LOGGER.info("🔧 Increasing base compression level to " + baseCompressionLevel);
            }
            
        } catch (Exception e) {
            LOGGER.warning("Compression strategy optimization error: " + e.getMessage());
        }
    }
    
    /**
     * Check if compression settings should be adapted
     */
    private boolean shouldAdaptCompression(double compressionRatio) {
        return compressionRatio > 0.9 || compressionRatio < 0.3;
    }
    
    /**
     * Adapt compression settings based on performance
     */
    private void adaptCompressionSettings(double compressionRatio) {
        if (compressionRatio > 0.9) {
            // Poor compression - reduce compression level
            minPacketSize = Math.min(512, minPacketSize + 32);
            compressionThreshold = Math.min(0.95, compressionThreshold + 0.05);
        } else if (compressionRatio < 0.3) {
            // Excellent compression - be more aggressive
            minPacketSize = Math.max(32, minPacketSize - 16);
            compressionThreshold = Math.max(0.5, compressionThreshold - 0.05);
        }
        
        LOGGER.info("🔧 Adapted compression settings - Min packet size: " + minPacketSize + 
                   ", Threshold: " + String.format("%.1f%%", compressionThreshold * 100));
    }
    
    /**
     * Calculate global compression ratio
     */
    private double calculateGlobalCompressionRatio() {
        long totalOriginal = 0;
        long totalCompressed = 0;
        
        for (PacketPattern pattern : packetPatterns.values()) {
            totalOriginal += pattern.getTotalOriginalBytes();
            totalCompressed += pattern.getTotalCompressedBytes();
        }
        
        return totalOriginal > 0 ? (double) totalCompressed / totalOriginal : 1.0;
    }
    
    /**
     * Get network compression statistics
     */
    public Map<String, Object> getNetworkCompressionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("packets_compressed", packetsCompressed.get());
        stats.put("bytes_compressed", bytesCompressed.get());
        stats.put("bytes_saved", bytesSaved.get());
        stats.put("compression_optimizations", compressionOptimizations.get());
        stats.put("adaptive_changes", adaptiveChanges.get());
        stats.put("pattern_optimizations", patternOptimizations.get());
        
        stats.put("player_profiles", playerProfiles.size());
        stats.put("packet_patterns", packetPatterns.size());
        stats.put("compression_algorithms", algorithms.size());
        
        long totalBytes = bytesCompressed.get();
        if (totalBytes > 0) {
            double compressionRatio = (double) (totalBytes - bytesSaved.get()) / totalBytes;
            double bandwidthSavings = (double) bytesSaved.get() / totalBytes * 100;
            
            stats.put("compression_ratio", compressionRatio);
            stats.put("bandwidth_savings_percent", bandwidthSavings);
        }
        
        return stats;
    }
    
    // Getters
    public long getPacketsCompressed() { return packetsCompressed.get(); }
    public long getBytesCompressed() { return bytesCompressed.get(); }
    public long getBytesSaved() { return bytesSaved.get(); }
    public long getCompressionOptimizations() { return compressionOptimizations.get(); }
    public long getAdaptiveChanges() { return adaptiveChanges.get(); }
    public long getPatternOptimizations() { return patternOptimizations.get(); }
    
    public void shutdown() {
        networkOptimizer.shutdown();
        
        // Clear all data
        playerProfiles.clear();
        packetPatterns.clear();
        compressionStats.clear();
        lastOptimization.clear();
        
        LOGGER.info("🌐 Smart Network Compression shutdown complete");
    }
    
    // Helper classes
    private static class CompressionProfile {
        private final String playerId;
        private final Map<String, CompressionAlgorithm> preferredAlgorithms = new ConcurrentHashMap<>();
        private final Map<String, CompressionStats> compressionStats = new ConcurrentHashMap<>();
        private long lastOptimization = 0;
        
        public CompressionProfile(String playerId) {
            this.playerId = playerId;
        }
        
        public void updateCompressionStats(String packetType, int originalSize, int compressedSize) {
            CompressionStats stats = compressionStats.computeIfAbsent(packetType, k -> new CompressionStats());
            stats.addSample(originalSize, compressedSize);
        }
        
        public boolean needsOptimization() {
            return System.currentTimeMillis() - lastOptimization > 300000; // 5 minutes
        }
        
        public void markOptimized() {
            lastOptimization = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getPlayerId() { return playerId; }
        public CompressionAlgorithm getPreferredAlgorithm(String packetType) { return preferredAlgorithms.get(packetType); }
        public void setPreferredAlgorithm(String packetType, CompressionAlgorithm algorithm) { preferredAlgorithms.put(packetType, algorithm); }
        public Map<String, CompressionStats> getCompressionStats() { return compressionStats; }
    }
    
    private static class CompressionAlgorithm {
        private final String name;
        private final int strategy;
        private final int level;
        
        public CompressionAlgorithm(String name, int strategy, int level) {
            this.name = name;
            this.strategy = strategy;
            this.level = level;
        }
        
        public String getName() { return name; }
        public int getStrategy() { return strategy; }
        public int getLevel() { return level; }
    }
    
    private static class CompressionStats {
        private long totalSamples = 0;
        private long totalOriginalBytes = 0;
        private long totalCompressedBytes = 0;
        
        public void addSample(int originalSize, int compressedSize) {
            totalSamples++;
            totalOriginalBytes += originalSize;
            totalCompressedBytes += compressedSize;
        }
        
        public double getAverageCompressionRatio() {
            return totalOriginalBytes > 0 ? (double) totalCompressedBytes / totalOriginalBytes : 1.0;
        }
        
        public int getAveragePacketSize() {
            return totalSamples > 0 ? (int) (totalOriginalBytes / totalSamples) : 0;
        }
    }
    
    private static class PacketPattern {
        private final String playerId;
        private final String packetType;
        private final List<Integer> originalSizes = new ArrayList<>();
        private final List<Double> compressionRatios = new ArrayList<>();
        private boolean shouldCompress = true;
        private Priority priority = Priority.NORMAL;
        
        public enum Priority { LOW, NORMAL, HIGH }
        
        public PacketPattern(String playerId, String packetType) {
            this.playerId = playerId;
            this.packetType = packetType;
        }
        
        public void addSample(int originalSize, double compressionRatio) {
            originalSizes.add(originalSize);
            compressionRatios.add(compressionRatio);
            
            // Keep only recent samples
            if (originalSizes.size() > 100) {
                originalSizes.remove(0);
                compressionRatios.remove(0);
            }
        }
        
        public boolean hasEnoughSamples() {
            return originalSizes.size() >= 10;
        }
        
        public double getAverageCompressionRatio() {
            return compressionRatios.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        }
        
        public int getAveragePacketSize() {
            return (int) originalSizes.stream().mapToInt(Integer::intValue).average().orElse(0);
        }
        
        public long getTotalOriginalBytes() {
            return originalSizes.stream().mapToLong(Integer::longValue).sum();
        }
        
        public long getTotalCompressedBytes() {
            long totalOriginal = getTotalOriginalBytes();
            double avgRatio = getAverageCompressionRatio();
            return (long) (totalOriginal * avgRatio);
        }
        
        // Getters and setters
        public String getPlayerId() { return playerId; }
        public String getPacketType() { return packetType; }
        public boolean shouldCompress() { return shouldCompress; }
        public void setShouldCompress(boolean shouldCompress) { this.shouldCompress = shouldCompress; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
    }
}
