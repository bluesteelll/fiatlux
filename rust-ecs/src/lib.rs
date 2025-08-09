use bevy_ecs::prelude::*;

// Module declarations
pub mod components;
pub mod systems;
pub mod resources;
pub mod events;
pub mod world_manager;
pub mod jni;

// Re-export commonly used items  
pub use components::*;
pub use resources::*;
pub use events::*;
// Note: systems::* not re-exported to avoid naming conflicts

// Batch processing configuration
pub const BATCH_INTERVAL: u64 = 4;

