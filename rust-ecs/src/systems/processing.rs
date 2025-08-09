use bevy_ecs::prelude::*;
use crate::components::{ProcessingComponent, EnergyComponent};
use crate::resources::ProcessingChangeBatch;

/// System for processing operations
pub fn processing_system(
    mut query: Query<(Entity, &mut ProcessingComponent, Option<&EnergyComponent>)>,
    mut change_batch: ResMut<ProcessingChangeBatch>,
) {
    change_batch.changes.clear();
    
    for (entity, mut processor, energy_opt) in &mut query {
        let old_progress = processor.current_progress;
        
        // Check if we have enough energy (if energy component exists)
        let can_process = if let Some(energy) = energy_opt {
            energy.current > 0 // Need at least some energy
        } else {
            true // No energy requirement
        };
        
        if can_process && processor.is_active {
            let completed = processor.tick();
            
            if completed {
                // Processing cycle completed - could trigger item production, etc.
                // For now, just record the completion
                change_batch.changes.push((entity, 100.0)); // Mark as completed
            } else if old_progress != processor.current_progress {
                // Progress changed
                let progress_delta = processor.current_progress - old_progress;
                change_batch.changes.push((entity, progress_delta));
            }
        }
    }
}

/// System for energy-consuming processing
pub fn energy_processing_system(
    mut query: Query<(Entity, &mut ProcessingComponent, &mut EnergyComponent)>,
    mut change_batch: ResMut<ProcessingChangeBatch>,
) {
    for (entity, mut processor, mut energy) in &mut query {
        if !processor.is_active {
            continue;
        }
        
        // Calculate energy cost per tick
        let energy_cost = (processor.processing_speed * 2.0) as i32; // 2 energy per speed unit
        
        if energy.current >= energy_cost {
            let old_progress = processor.current_progress;
            
            // Consume energy and process
            energy.current -= energy_cost;
            let completed = processor.tick();
            
            if completed {
                change_batch.changes.push((entity, 100.0)); // Mark as completed
            } else if old_progress != processor.current_progress {
                let progress_delta = processor.current_progress - old_progress;
                change_batch.changes.push((entity, progress_delta));
            }
        } else {
            // Not enough energy - slow down or stop processing
            processor.set_active(false);
        }
    }
}

/// System for automatic processing activation when energy is available
pub fn processing_activation_system(
    mut query: Query<(&mut ProcessingComponent, &EnergyComponent)>,
) {
    for (mut processor, energy) in &mut query {
        if !processor.is_active && energy.current > 20 {
            // Reactivate processing when energy is sufficient
            processor.set_active(true);
        }
    }
}