package ru.playland.core.optimization;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Quantum State for revolutionary optimization
 * Represents a quantum superposition of server states
 */
public class QuantumState {
    
    private final String id;
    private final QuantumStateType type;
    private final Random random = new Random();
    
    // Quantum properties
    private final AtomicBoolean inSuperposition = new AtomicBoolean(false);
    private final List<QuantumState> entangledStates = new ArrayList<>();
    private final AtomicLong lastMeasurement = new AtomicLong(System.nanoTime());
    
    // Performance properties
    private volatile double load = 0.5;
    private volatile double efficiency = 1.0;
    private volatile double resources = 1.0;
    private volatile boolean hasBarrier = false;
    
    public QuantumState(String id, QuantumStateType type) {
        this.id = id;
        this.type = type;
    }
    
    public void enterSuperposition() {
        inSuperposition.set(true);
    }
    
    public void exitSuperposition() {
        inSuperposition.set(false);
    }
    
    public boolean isInSuperposition() {
        return inSuperposition.get();
    }
    
    public double measure() {
        lastMeasurement.set(System.nanoTime());
        
        // Quantum measurement collapses superposition
        if (inSuperposition.get()) {
            exitSuperposition();
        }
        
        // Return measurement value
        return random.nextDouble();
    }
    
    public double measureLoad() {
        measure(); // Collapse superposition
        return load + (random.nextGaussian() * 0.1); // Add quantum uncertainty
    }
    
    public void applyQuantumOptimization(double factor) {
        efficiency = Math.max(0.1, Math.min(2.0, efficiency + factor));
        
        // Quantum optimization affects entangled states
        for (QuantumState entangled : entangledStates) {
            entangled.receiveEntangledOptimization(factor * 0.5);
        }
    }
    
    public void receiveEntangledOptimization(double factor) {
        efficiency = Math.max(0.1, Math.min(2.0, efficiency + factor));
    }
    
    public void entangleWith(QuantumState other) {
        if (!entangledStates.contains(other)) {
            entangledStates.add(other);
            other.entangledStates.add(this);
        }
    }
    
    public void transferLoad(QuantumState target, double amount) {
        if (amount > 0 && load > amount) {
            load -= amount;
            target.load += amount;
        }
    }
    
    public boolean hasPerformanceBarrier() {
        return hasBarrier || load > 0.9;
    }
    
    public boolean attemptQuantumTunneling() {
        if (!hasPerformanceBarrier()) return false;
        
        // Quantum tunneling probability
        double tunnelingProbability = Math.exp(-load * 10); // Exponential decay
        
        if (random.nextDouble() < tunnelingProbability) {
            load *= 0.8; // Reduce load through tunneling
            hasBarrier = false;
            return true;
        }
        
        return false;
    }
    
    public void processEntanglements() {
        // Process quantum entanglements
        for (QuantumState entangled : entangledStates) {
            // Synchronize quantum states
            double avgLoad = (load + entangled.load) / 2.0;
            double loadDiff = Math.abs(load - entangled.load);
            
            if (loadDiff > 0.1) {
                // Entanglement effect
                load = load * 0.9 + avgLoad * 0.1;
                entangled.load = entangled.load * 0.9 + avgLoad * 0.1;
            }
        }
    }
    
    public void update() {
        // Update quantum state
        
        // Natural load fluctuation
        load += (random.nextGaussian() * 0.01);
        load = Math.max(0.0, Math.min(1.0, load));
        
        // Efficiency decay
        efficiency = Math.max(0.5, efficiency * 0.999);
        
        // Random superposition entry
        if (!inSuperposition.get() && random.nextDouble() < 0.01) {
            enterSuperposition();
        }
    }
    
    public boolean isOverloaded() { return load > 0.8; }
    public void redistributeLoad() { load *= 0.9; }
    public void allocateResources(double amount) { resources = amount; }
    public void enhancePerformance(double factor) { efficiency *= (1.0 + factor * 0.1); }
    
    // Getters
    public String getId() { return id; }
    public QuantumStateType getType() { return type; }
    public double getLoad() { return load; }
    public double getEfficiency() { return efficiency; }
    public double getCurrentResources() { return resources; }
    public List<QuantumState> getEntangledStates() { return new ArrayList<>(entangledStates); }
}
