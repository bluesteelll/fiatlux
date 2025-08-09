use bevy_ecs::prelude::*;

/// Component representing position within a 4x4x4 grid
#[derive(Component, Clone, Copy, Debug)]
pub struct ModulePosition {
    pub x: i32,
    pub y: i32, 
    pub z: i32,
}

impl ModulePosition {
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self { x, y, z }
    }
    
    /// Check if two positions are adjacent (Manhattan distance = 1)
    pub fn is_adjacent(&self, other: &ModulePosition) -> bool {
        let dx = (self.x - other.x).abs();
        let dy = (self.y - other.y).abs();
        let dz = (self.z - other.z).abs();
        dx + dy + dz == 1
    }
    
    /// Get Manhattan distance to another position
    pub fn manhattan_distance(&self, other: &ModulePosition) -> i32 {
        (self.x - other.x).abs() + (self.y - other.y).abs() + (self.z - other.z).abs()
    }
}

/// Component for identifying module type
#[derive(Component)]
pub struct ModuleType {
    pub type_id: String,
}

impl ModuleType {
    pub fn new(type_id: String) -> Self {
        Self { type_id }
    }
    
    pub fn is_energy_module(&self) -> bool {
        matches!(
            self.type_id.as_str(),
            "energy_generator" | "energy_storage" | "energy_consumer"
        )
    }
    
    pub fn is_processing_module(&self) -> bool {
        self.type_id == "processor"
    }
    
    pub fn is_display_module(&self) -> bool {
        self.type_id == "display"
    }
}