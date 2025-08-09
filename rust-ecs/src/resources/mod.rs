use bevy_ecs::prelude::*;
use crate::events::{EnergyTransfer, EnergyChangeEvent};

/// Resource for tracking tick count and batch processing
#[derive(Resource)]
pub struct TickCounter {
    pub count: u64,
}

impl Default for TickCounter {
    fn default() -> Self {
        Self { count: 0 }
    }
}

/// Resource for batch processing control
#[derive(Resource)]
pub struct BatchProcessor {
    pub should_process: bool,
    pub pending_changes: u32,
}

impl Default for BatchProcessor {
    fn default() -> Self {
        Self {
            should_process: false,
            pending_changes: 0,
        }
    }
}

/// Resource for batching energy transfers
#[derive(Resource, Default)]
pub struct EnergyTransferBatch {
    pub pending_transfers: Vec<EnergyTransfer>,
    pub completed_transfers: Vec<EnergyTransfer>,
}

/// Resource for batching energy change events
#[derive(Resource, Default)]
pub struct EnergyEventBatch {
    pub events: Vec<EnergyChangeEvent>,
}

/// Resource for batching processing changes 
#[derive(Resource, Default)]
pub struct ProcessingChangeBatch {
    pub changes: Vec<(Entity, f32)>, // (entity, progress_delta)
}