use bevy_ecs::prelude::*;
use crate::components::{EnergyComponent, ModulePosition};
use crate::resources::{EnergyEventBatch, EnergyTransferBatch};
use crate::events::{EnergyChangeEvent, EnergyTransfer};

/// System for energy generation and consumption
pub fn energy_generation_system(
    mut query: Query<(Entity, &mut EnergyComponent)>,
    mut event_batch: ResMut<EnergyEventBatch>,
) {
    for (entity, mut energy) in &mut query {
        let old_current = energy.current;
        
        // Generate energy
        energy.generate();
        
        // Consume energy  
        energy.consume();
        
        // Record event if energy changed
        if old_current != energy.current {
            event_batch.events.push(EnergyChangeEvent {
                entity,
                old_energy: old_current,
                new_energy: energy.current,
                max_capacity: energy.max_capacity,
            });
        }
    }
}

/// System for energy transfer between adjacent modules
pub fn energy_transfer_system(
    mut energy_query: Query<(Entity, &mut EnergyComponent, &ModulePosition)>,
    mut transfer_batch: ResMut<EnergyTransferBatch>,
    mut event_batch: ResMut<EnergyEventBatch>,
) {
    // Clear previous transfer data
    transfer_batch.pending_transfers.clear();
    transfer_batch.completed_transfers.clear();
    
    // Collect energy entities to avoid borrow checker issues
    let energy_entities: Vec<_> = energy_query
        .iter()
        .map(|(e, en, pos)| (e, *en, *pos))
        .collect();
    
    // Process transfers sequentially to avoid borrow conflicts
    let mut transfers_to_apply = Vec::new();
    
    for i in 0..energy_entities.len() {
        let (entity_a, energy_a, pos_a) = energy_entities[i];
        
        if !energy_a.can_extract || energy_a.current <= 0 {
            continue;
        }
        
        for j in (i + 1)..energy_entities.len() {
            let (entity_b, energy_b, pos_b) = energy_entities[j];
            
            if !energy_b.can_receive || energy_b.current >= energy_b.max_capacity {
                continue;
            }
            
            // Check adjacency
            if pos_a.is_adjacent(&pos_b) {
                let max_extract = energy_a.max_extractable();
                let max_receive = energy_b.max_receivable();
                let transfer_amount = max_extract.min(max_receive).min(50);
                
                if transfer_amount > 0 {
                    transfers_to_apply.push((entity_a, entity_b, transfer_amount));
                    
                    // Record pending transfer for monitoring
                    transfer_batch.pending_transfers.push(EnergyTransfer {
                        from_entity: entity_a,
                        to_entity: entity_b,
                        amount: transfer_amount,
                        direction: (pos_b.x - pos_a.x, pos_b.y - pos_a.y, pos_b.z - pos_a.z),
                    });
                }
            }
        }
    }
    
    // Apply transfers sequentially to avoid borrow conflicts
    for (from_entity, to_entity, amount) in transfers_to_apply {
        // Process from entity first
        let from_result = {
            if let Ok((_, mut from_energy, _)) = energy_query.get_mut(from_entity) {
                let old_energy = from_energy.current;
                let actual_extracted = from_energy.extract_energy(amount);
                Some((old_energy, from_energy.current, from_energy.max_capacity, actual_extracted))
            } else {
                None
            }
        };
        
        if let Some((old_from, new_from, from_capacity, actual_extracted)) = from_result {
            if actual_extracted > 0 {
                // Process to entity second
                let to_result = {
                    if let Ok((_, mut to_energy, _)) = energy_query.get_mut(to_entity) {
                        let old_energy = to_energy.current;
                        let actual_received = to_energy.receive_energy(actual_extracted);
                        Some((old_energy, to_energy.current, to_energy.max_capacity, actual_received))
                    } else {
                        None
                    }
                };
                
                if let Some((old_to, new_to, to_capacity, actual_received)) = to_result {
                    // Record transfer events
                    event_batch.events.push(EnergyChangeEvent {
                        entity: from_entity,
                        old_energy: old_from,
                        new_energy: new_from,
                        max_capacity: from_capacity,
                    });
                    
                    event_batch.events.push(EnergyChangeEvent {
                        entity: to_entity,
                        old_energy: old_to,
                        new_energy: new_to,
                        max_capacity: to_capacity,
                    });
                    
                    // Record completed transfer
                    transfer_batch.completed_transfers.push(EnergyTransfer {
                        from_entity,
                        to_entity,
                        amount: actual_received,
                        direction: (0, 0, 0),
                    });
                }
            }
        }
    }
}

/// System for energy balancing (optional, for more advanced energy distribution)
pub fn energy_balancing_system(
    mut query: Query<(Entity, &mut EnergyComponent, &ModulePosition)>,
    mut event_batch: ResMut<EnergyEventBatch>,
) {
    // This system could implement more sophisticated energy distribution
    // For now, it's a placeholder for future enhancements
    
    // Example: Balance energy levels between storage units
    let mut storage_units: Vec<_> = query
        .iter_mut()
        .filter(|(_, energy, _)| energy.can_receive && energy.can_extract && energy.max_capacity > 0)
        .collect();
    
    if storage_units.len() < 2 {
        return; // Need at least 2 storage units to balance
    }
    
    // Calculate average energy level
    let total_energy: i32 = storage_units.iter().map(|(_, energy, _)| energy.current).sum();
    let total_capacity: i32 = storage_units.iter().map(|(_, energy, _)| energy.max_capacity).sum();
    
    if total_capacity == 0 {
        return;
    }
    
    let target_ratio = total_energy as f32 / total_capacity as f32;
    
    // Balance energy levels (simplified approach)
    for (entity, energy, pos) in storage_units.iter_mut() {
        let target_energy = (energy.max_capacity as f32 * target_ratio) as i32;
        let energy_diff = target_energy - energy.current;
        
        if energy_diff.abs() > energy.max_transfer_rate / 4 {
            // Only balance if difference is significant
            let old_energy = energy.current;
            
            if energy_diff > 0 {
                // Need more energy
                let can_receive = energy.max_receivable().min(energy_diff);
                energy.current += can_receive;
            } else {
                // Have excess energy
                let can_extract = energy.max_extractable().min(-energy_diff);
                energy.current -= can_extract;
            }
            
            if old_energy != energy.current {
                event_batch.events.push(EnergyChangeEvent {
                    entity: *entity,
                    old_energy,
                    new_energy: energy.current,
                    max_capacity: energy.max_capacity,
                });
            }
        }
    }
}