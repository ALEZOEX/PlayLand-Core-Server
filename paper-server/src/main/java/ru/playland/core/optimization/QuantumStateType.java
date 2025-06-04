package ru.playland.core.optimization;

/**
 * Types of quantum states for optimization
 */
public enum QuantumStateType {
    SERVER("Server Core", true),
    WORLD("World Processing", true),
    ENTITIES("Entity Management", true),
    CHUNKS("Chunk Loading", true),
    PLAYERS("Player Handling", true),
    REDSTONE("Redstone Simulation", false),
    PHYSICS("Physics Calculation", false),
    NETWORKING("Network Processing", true);
    
    private final String description;
    private final boolean canEntangle;
    
    QuantumStateType(String description, boolean canEntangle) {
        this.description = description;
        this.canEntangle = canEntangle;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canEntangle() {
        return canEntangle;
    }
    
    public boolean isCompatibleWith(QuantumStateType other) {
        // Define compatibility rules for quantum entanglement
        if (!this.canEntangle || !other.canEntangle) return false;
        
        // Server can entangle with everything
        if (this == SERVER || other == SERVER) return true;
        
        // World-related states can entangle
        return (this == WORLD && (other == ENTITIES || other == CHUNKS)) ||
               (other == WORLD && (this == ENTITIES || this == CHUNKS)) ||
               (this == ENTITIES && other == CHUNKS) ||
               (other == ENTITIES && this == CHUNKS);
    }
}
