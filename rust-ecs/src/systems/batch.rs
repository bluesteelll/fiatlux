use bevy_ecs::prelude::*;
use crate::resources::{TickCounter, BatchProcessor, EnergyEventBatch};

/// Batch processing configuration
pub const BATCH_INTERVAL: u64 = 4;

/// System for managing batch processing timing
pub fn batch_timing_system(
    mut tick_counter: ResMut<TickCounter>,
    mut batch_processor: ResMut<BatchProcessor>,
) {
    tick_counter.count += 1;
    batch_processor.should_process = tick_counter.count % BATCH_INTERVAL == 0;
}

/// System for managing batch processing statistics
pub fn batch_stats_system(
    tick_counter: Res<TickCounter>,
    event_batch: Res<EnergyEventBatch>,
    mut batch_processor: ResMut<BatchProcessor>,
) {
    if batch_processor.should_process {
        // Update pending changes count based on events generated
        batch_processor.pending_changes = event_batch.events.len() as u32;
    } else {
        // On non-processing ticks, increment pending changes counter
        batch_processor.pending_changes += 1;
    }
}

/// Conditional system runner for batch processing
pub struct BatchSystemRunner {
    pub should_run: bool,
}

impl BatchSystemRunner {
    pub fn new() -> Self {
        Self { should_run: false }
    }
    
    pub fn update(&mut self, batch_processor: &BatchProcessor) {
        self.should_run = batch_processor.should_process;
    }
    
    pub fn run_if_needed<F>(&self, system: F) where F: FnOnce() {
        if self.should_run {
            system();
        }
    }
}

/// System for clearing old batch data
pub fn batch_cleanup_system(
    mut event_batch: ResMut<EnergyEventBatch>,
    batch_processor: Res<BatchProcessor>,
) {
    if batch_processor.should_process {
        // Clear events after processing
        event_batch.events.clear();
    }
}