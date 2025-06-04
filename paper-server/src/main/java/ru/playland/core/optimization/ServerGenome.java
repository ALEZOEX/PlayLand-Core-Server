package ru.playland.core.optimization;

import java.util.Random;

/**
 * Represents a server optimization genome for genetic algorithms
 * Contains genes that control various server optimization parameters
 */
public class ServerGenome {
    
    private final double[] genes;
    private double fitness;
    private final Random random = new Random();
    
    public ServerGenome(int length) {
        this.genes = new double[length];
        this.fitness = 0.0;
    }
    
    public void randomize() {
        for (int i = 0; i < genes.length; i++) {
            genes[i] = random.nextDouble();
        }
    }
    
    public ServerGenome copy() {
        ServerGenome copy = new ServerGenome(genes.length);
        System.arraycopy(genes, 0, copy.genes, 0, genes.length);
        copy.fitness = this.fitness;
        return copy;
    }
    
    public void copyFrom(ServerGenome other) {
        if (other.genes.length != this.genes.length) {
            throw new IllegalArgumentException("Genome lengths must match");
        }
        System.arraycopy(other.genes, 0, this.genes, 0, genes.length);
        this.fitness = other.fitness;
    }
    
    // Getters and setters
    public double getGene(int index) {
        return genes[index];
    }
    
    public void setGene(int index, double value) {
        genes[index] = Math.max(0.0, Math.min(1.0, value));
    }
    
    public int getLength() {
        return genes.length;
    }
    
    public double getFitness() {
        return fitness;
    }
    
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
    
    public double[] getGenes() {
        return genes.clone();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServerGenome{fitness=").append(String.format("%.4f", fitness));
        sb.append(", genes=[");
        for (int i = 0; i < genes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.3f", genes[i]));
        }
        sb.append("]}");
        return sb.toString();
    }
}
