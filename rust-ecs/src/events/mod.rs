use bevy_ecs::prelude::*;

/// Energy transfer request
#[derive(Debug, Clone)]
pub struct EnergyTransfer {
    pub from_entity: Entity,
    pub to_entity: Entity,
    pub amount: i32,
    pub direction: (i32, i32, i32), // Direction vector for validation
}

impl EnergyTransfer {
    pub fn new(from_entity: Entity, to_entity: Entity, amount: i32) -> Self {
        Self {
            from_entity,
            to_entity,
            amount,
            direction: (0, 0, 0),
        }
    }
    
    pub fn with_direction(mut self, direction: (i32, i32, i32)) -> Self {
        self.direction = direction;
        self
    }
}

/// Energy change event for Java communication
#[derive(Debug, Clone)]
pub struct EnergyChangeEvent {
    pub entity: Entity,
    pub old_energy: i32,
    pub new_energy: i32,
    pub max_capacity: i32,
}

impl EnergyChangeEvent {
    pub fn new(entity: Entity, old_energy: i32, new_energy: i32, max_capacity: i32) -> Self {
        Self {
            entity,
            old_energy,
            new_energy,
            max_capacity,
        }
    }
    
    pub fn energy_delta(&self) -> i32 {
        self.new_energy - self.old_energy
    }
    
    pub fn is_increase(&self) -> bool {
        self.new_energy > self.old_energy
    }
    
    pub fn is_decrease(&self) -> bool {
        self.new_energy < self.old_energy
    }
    
    pub fn has_changed(&self) -> bool {
        self.old_energy != self.new_energy
    }
}

/// Processing completion event
#[derive(Debug, Clone)]
pub struct ProcessingCompleteEvent {
    pub entity: Entity,
    pub processing_type: String,
    pub duration: f32,
}

/// Module lifecycle events
#[derive(Debug, Clone)]
pub enum ModuleEvent {
    Spawned { entity: Entity, module_type: String },
    Despawned { entity: Entity },
    StateChanged { entity: Entity, old_state: String, new_state: String },
}