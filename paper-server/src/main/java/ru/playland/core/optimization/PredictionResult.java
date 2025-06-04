package ru.playland.core.optimization;

/**
 * Result of neural network lag prediction
 */
public class PredictionResult {
    
    private double lagProbability;
    private double severityLevel;
    private double timeToLag;
    private double[] currentState;
    private double[] prediction;
    
    public PredictionResult() {
        this.lagProbability = 0.0;
        this.severityLevel = 0.0;
        this.timeToLag = 0.0;
    }
    
    public boolean requiresAction() {
        return lagProbability > 0.3 || severityLevel > 0.5;
    }
    
    // Getters and setters
    public double getLagProbability() { return lagProbability; }
    public void setLagProbability(double lagProbability) { this.lagProbability = lagProbability; }
    
    public double getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(double severityLevel) { this.severityLevel = severityLevel; }
    
    public double getTimeToLag() { return timeToLag; }
    public void setTimeToLag(double timeToLag) { this.timeToLag = timeToLag; }
    
    public double[] getCurrentState() { return currentState; }
    public void setCurrentState(double[] currentState) { this.currentState = currentState; }
    
    public double[] getPrediction() { return prediction; }
    public void setPrediction(double[] prediction) { this.prediction = prediction; }
}
