package ru.playland.core.compatibility;

/**
 * Plugin Compatibility Information
 * Stores compatibility data for individual plugins
 */
public class PluginCompatibilityInfo {
    
    private final String pluginName;
    private final String apiVersion;
    private final boolean isLegacy;
    private boolean isSupported;
    private boolean isAdapted;
    private long loadTime;
    private String adaptationMethod;
    
    public PluginCompatibilityInfo(String pluginName, String apiVersion, boolean isLegacy, boolean isSupported) {
        this.pluginName = pluginName;
        this.apiVersion = apiVersion;
        this.isLegacy = isLegacy;
        this.isSupported = isSupported;
        this.isAdapted = false;
        this.loadTime = System.currentTimeMillis();
        this.adaptationMethod = null;
    }
    
    // Getters
    public String getPluginName() {
        return pluginName;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public boolean isLegacy() {
        return isLegacy;
    }
    
    public boolean isSupported() {
        return isSupported;
    }
    
    public boolean isAdapted() {
        return isAdapted;
    }
    
    public long getLoadTime() {
        return loadTime;
    }
    
    public String getAdaptationMethod() {
        return adaptationMethod;
    }
    
    // Setters
    public void setSupported(boolean supported) {
        this.isSupported = supported;
    }
    
    public void setAdapted(boolean adapted) {
        this.isAdapted = adapted;
    }
    
    public void setAdaptationMethod(String adaptationMethod) {
        this.adaptationMethod = adaptationMethod;
    }
    
    @Override
    public String toString() {
        return "PluginCompatibilityInfo{" +
                "pluginName='" + pluginName + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", isLegacy=" + isLegacy +
                ", isSupported=" + isSupported +
                ", isAdapted=" + isAdapted +
                ", adaptationMethod='" + adaptationMethod + '\'' +
                '}';
    }
}
