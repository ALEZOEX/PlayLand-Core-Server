package ru.playland.core.optimization;

import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * Revolutionary Genetic Algorithm Optimizer
 * Uses evolutionary algorithms for server optimization
 * Integrated directly into Paper server core
 */
public class GeneticAlgorithmOptimizer {
    
    private static final Logger LOGGER = Logger.getLogger("PlayLand-Genetic");
    
    // Genetic algorithm configuration
    private static final int POPULATION_SIZE = 50;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;
    private static final int GENOME_LENGTH = 20;
    
    // Statistics
    private final AtomicLong generations = new AtomicLong(0);
    private final AtomicLong evolutionCycles = new AtomicLong(0);
    private final AtomicLong mutations = new AtomicLong(0);
    private final AtomicLong crossovers = new AtomicLong(0);
    
    // Genetic algorithm components
    private List<ServerGenome> population;
    private ServerGenome bestGenome;
    private double bestFitness = 0.0;
    private boolean initialized = false;
    
    private final Random random = new Random();
    
    public void initialize() {
        LOGGER.info("🧬 Initializing Genetic Algorithm Optimizer...");
        LOGGER.info("🔬 Entering evolutionary realm...");
        
        // Initialize population
        initializePopulation();
        
        // Start evolution process
        startEvolutionProcess();
        
        initialized = true;
        LOGGER.info("✅ Genetic Algorithm Optimizer initialized!");
        LOGGER.info("👥 Population size: " + POPULATION_SIZE);
        LOGGER.info("🧬 Genome length: " + GENOME_LENGTH);
        LOGGER.info("⚡ Mutation rate: " + (MUTATION_RATE * 100) + "%");
        LOGGER.info("💕 Crossover rate: " + (CROSSOVER_RATE * 100) + "%");
    }
    
    private void initializePopulation() {
        population = new ArrayList<>();
        
        // Create initial random population
        for (int i = 0; i < POPULATION_SIZE; i++) {
            ServerGenome genome = new ServerGenome(GENOME_LENGTH);
            genome.randomize();
            population.add(genome);
        }
        
        // Set initial best genome
        bestGenome = population.get(0);
        bestFitness = evaluateFitness(bestGenome);
    }
    
    public void performEvolution() {
        if (!initialized) return;
        
        evolutionCycles.incrementAndGet();
        
        // Evaluate fitness of all genomes
        evaluatePopulation();
        
        // Select parents for reproduction
        List<ServerGenome> parents = selectParents();
        
        // Create new generation through crossover and mutation
        List<ServerGenome> newGeneration = createNewGeneration(parents);
        
        // Replace old population with new generation
        population = newGeneration;
        
        // Update best genome
        updateBestGenome();
        
        generations.incrementAndGet();
    }
    
    private void startEvolutionProcess() {
        // Start background evolution process
        Thread evolutionThread = new Thread(() -> {
            while (initialized) {
                try {
                    // Perform evolution every 30 seconds
                    performEvolution();
                    
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.warning("Evolution process error: " + e.getMessage());
                }
            }
        });
        evolutionThread.setDaemon(true);
        evolutionThread.setName("PlayLand-Evolution-Processor");
        evolutionThread.start();
    }
    
    private void evaluatePopulation() {
        for (ServerGenome genome : population) {
            double fitness = evaluateFitness(genome);
            genome.setFitness(fitness);
        }
    }
    
    private double evaluateFitness(ServerGenome genome) {
        // Evaluate genome fitness based on server performance
        double fitness = 0.0;
        
        // Factors that contribute to fitness:
        // 1. TPS improvement
        double tpsImprovement = calculateTpsImprovement(genome);
        fitness += tpsImprovement * 0.4;
        
        // 2. Memory efficiency
        double memoryEfficiency = calculateMemoryEfficiency(genome);
        fitness += memoryEfficiency * 0.3;
        
        // 3. Player capacity
        double playerCapacity = calculatePlayerCapacity(genome);
        fitness += playerCapacity * 0.2;
        
        // 4. Vanilla compatibility
        double vanillaCompatibility = calculateVanillaCompatibility(genome);
        fitness += vanillaCompatibility * 0.1;
        
        return Math.max(0.0, Math.min(1.0, fitness));
    }
    
    private double calculateTpsImprovement(ServerGenome genome) {
        try {
            // Calculate TPS improvement based on genome and real server metrics
            double currentTPS = getCurrentTPS();
            double targetTPS = 20.0;

            double improvement = 0.0;

            // Each gene represents a different optimization parameter
            for (int i = 0; i < genome.getLength(); i++) {
                double gene = genome.getGene(i);

                switch (i % 5) { // Cycle through different optimization types
                    case 0: // Chunk loading optimization
                        improvement += gene * calculateChunkOptimizationImpact();
                        break;
                    case 1: // Entity processing optimization
                        improvement += gene * calculateEntityOptimizationImpact();
                        break;
                    case 2: // Memory management optimization
                        improvement += gene * calculateMemoryOptimizationImpact();
                        break;
                    case 3: // Network optimization
                        improvement += gene * calculateNetworkOptimizationImpact();
                        break;
                    case 4: // Tick scheduling optimization
                        improvement += gene * calculateTickOptimizationImpact();
                        break;
                }
            }

            // Normalize improvement based on current TPS deficit
            double tpsDeficit = Math.max(0, targetTPS - currentTPS);
            double normalizedImprovement = improvement / genome.getLength();

            // Scale improvement based on how much TPS can actually be improved
            return Math.min(1.0, normalizedImprovement * (tpsDeficit / targetTPS + 0.1));

        } catch (Exception e) {
            LOGGER.fine("TPS improvement calculation error: " + e.getMessage());
            return 0.5; // Default moderate improvement
        }
    }

    private double getCurrentTPS() {
        try {
            return org.bukkit.Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }

    private double calculateChunkOptimizationImpact() {
        try {
            // Calculate impact based on loaded chunks
            int totalChunks = 0;
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                totalChunks += world.getLoadedChunks().length;
            }

            // More chunks = higher potential impact from chunk optimization
            return Math.min(0.3, totalChunks / 1000.0 * 0.1);
        } catch (Exception e) {
            return 0.1; // Default impact
        }
    }

    private double calculateEntityOptimizationImpact() {
        try {
            // Calculate impact based on entity count
            int totalEntities = 0;
            for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                totalEntities += world.getEntities().size();
            }

            // More entities = higher potential impact from entity optimization
            return Math.min(0.25, totalEntities / 5000.0 * 0.15);
        } catch (Exception e) {
            return 0.1; // Default impact
        }
    }

    private double calculateMemoryOptimizationImpact() {
        try {
            // Calculate impact based on memory usage
            Runtime runtime = Runtime.getRuntime();
            double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();

            // Higher memory usage = higher potential impact from memory optimization
            return Math.min(0.2, memoryUsage * 0.25);
        } catch (Exception e) {
            return 0.1; // Default impact
        }
    }

    private double calculateNetworkOptimizationImpact() {
        try {
            // Calculate impact based on player count
            int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();

            // More players = higher potential impact from network optimization
            return Math.min(0.2, playerCount / 50.0 * 0.15);
        } catch (Exception e) {
            return 0.05; // Default impact
        }
    }

    private double calculateTickOptimizationImpact() {
        try {
            // Calculate impact based on system load
            java.lang.management.OperatingSystemMXBean osBean =
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double systemLoad = osBean.getSystemLoadAverage();

            if (systemLoad > 0) {
                // Higher system load = higher potential impact from tick optimization
                return Math.min(0.15, systemLoad / 4.0 * 0.1);
            }

            return 0.05; // Default impact
        } catch (Exception e) {
            return 0.05; // Default impact
        }
    }
    
    private double calculateMemoryEfficiency(ServerGenome genome) {
        // Calculate memory efficiency based on genome
        double efficiency = 0.0;
        
        for (int i = 0; i < genome.getLength(); i++) {
            double gene = genome.getGene(i);
            efficiency += (1.0 - gene) * 0.05; // Lower values = better memory efficiency
        }
        
        return efficiency / genome.getLength();
    }
    
    private double calculatePlayerCapacity(ServerGenome genome) {
        // Calculate player capacity based on genome
        double capacity = 0.0;
        
        for (int i = 0; i < genome.getLength(); i++) {
            double gene = genome.getGene(i);
            capacity += gene * gene * 0.1; // Quadratic relationship
        }
        
        return capacity / genome.getLength();
    }
    
    private double calculateVanillaCompatibility(ServerGenome genome) {
        // Calculate vanilla compatibility based on genome
        // Higher values = better compatibility
        double compatibility = 1.0;
        
        for (int i = 0; i < genome.getLength(); i++) {
            double gene = genome.getGene(i);
            if (gene > 0.8) { // Extreme values might break vanilla mechanics
                compatibility -= 0.05;
            }
        }
        
        return Math.max(0.0, compatibility);
    }
    
    private List<ServerGenome> selectParents() {
        List<ServerGenome> parents = new ArrayList<>();
        
        // Tournament selection
        for (int i = 0; i < POPULATION_SIZE; i++) {
            ServerGenome parent = tournamentSelection();
            parents.add(parent);
        }
        
        return parents;
    }
    
    private ServerGenome tournamentSelection() {
        // Select best genome from random tournament
        int tournamentSize = 5;
        ServerGenome best = null;
        double bestFitness = -1.0;
        
        for (int i = 0; i < tournamentSize; i++) {
            ServerGenome candidate = population.get(random.nextInt(population.size()));
            if (candidate.getFitness() > bestFitness) {
                best = candidate;
                bestFitness = candidate.getFitness();
            }
        }
        
        return best;
    }
    
    private List<ServerGenome> createNewGeneration(List<ServerGenome> parents) {
        List<ServerGenome> newGeneration = new ArrayList<>();
        
        for (int i = 0; i < POPULATION_SIZE; i += 2) {
            ServerGenome parent1 = parents.get(i);
            ServerGenome parent2 = parents.get((i + 1) % parents.size());
            
            // Crossover
            ServerGenome[] offspring = crossover(parent1, parent2);
            
            // Mutation
            mutate(offspring[0]);
            mutate(offspring[1]);
            
            newGeneration.add(offspring[0]);
            if (newGeneration.size() < POPULATION_SIZE) {
                newGeneration.add(offspring[1]);
            }
        }
        
        return newGeneration;
    }
    
    private ServerGenome[] crossover(ServerGenome parent1, ServerGenome parent2) {
        ServerGenome offspring1 = new ServerGenome(GENOME_LENGTH);
        ServerGenome offspring2 = new ServerGenome(GENOME_LENGTH);
        
        if (random.nextDouble() < CROSSOVER_RATE) {
            crossovers.incrementAndGet();
            
            // Single-point crossover
            int crossoverPoint = random.nextInt(GENOME_LENGTH);
            
            for (int i = 0; i < GENOME_LENGTH; i++) {
                if (i < crossoverPoint) {
                    offspring1.setGene(i, parent1.getGene(i));
                    offspring2.setGene(i, parent2.getGene(i));
                } else {
                    offspring1.setGene(i, parent2.getGene(i));
                    offspring2.setGene(i, parent1.getGene(i));
                }
            }
        } else {
            // No crossover - copy parents
            offspring1.copyFrom(parent1);
            offspring2.copyFrom(parent2);
        }
        
        return new ServerGenome[]{offspring1, offspring2};
    }
    
    private void mutate(ServerGenome genome) {
        for (int i = 0; i < genome.getLength(); i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                mutations.incrementAndGet();
                
                // Gaussian mutation
                double currentValue = genome.getGene(i);
                double mutation = random.nextGaussian() * 0.1;
                double newValue = Math.max(0.0, Math.min(1.0, currentValue + mutation));
                
                genome.setGene(i, newValue);
            }
        }
    }
    
    private void updateBestGenome() {
        for (ServerGenome genome : population) {
            if (genome.getFitness() > bestFitness) {
                bestGenome = genome.copy();
                bestFitness = genome.getFitness();
                
                LOGGER.info("🏆 New best genome found! Fitness: " + String.format("%.4f", bestFitness));
            }
        }
    }
    
    // Getters for monitoring
    public long getGenerations() { return generations.get(); }
    public long getEvolutionCycles() { return evolutionCycles.get(); }
    public long getMutations() { return mutations.get(); }
    public long getCrossovers() { return crossovers.get(); }
    public double getBestFitness() { return bestFitness; }
    public ServerGenome getBestGenome() { return bestGenome != null ? bestGenome.copy() : null; }
    public boolean isInitialized() { return initialized; }
}
