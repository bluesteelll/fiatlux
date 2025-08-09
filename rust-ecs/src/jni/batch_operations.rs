use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong};
use bevy_ecs::prelude::*;
use crate::world_manager::WORLDS;
use crate::resources::{BatchProcessor, EnergyEventBatch, TickCounter};
use crate::components::{EnergyComponent, ModulePosition, ProcessingComponent};
use crate::events::EnergyChangeEvent;

/// Tick the ECS world with batch processing
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_tickWorld(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        // Run batch timing manually
        let tick_count = {
            if let Some(mut tick_counter) = world.get_resource_mut::<TickCounter>() {
                tick_counter.count += 1;
                tick_counter.count
            } else {
                0
            }
        };
        
        if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
            batch_processor.should_process = tick_count % crate::BATCH_INTERVAL == 0;
        }
        
        // Check if we should process systems this tick
        let should_process = world.get_resource::<BatchProcessor>()
            .map(|bp| bp.should_process)
            .unwrap_or(false);
        
        // Only run systems every BATCH_INTERVAL ticks for maximum performance
        if should_process {
            // Clear event batch from previous cycle
            if let Some(mut event_batch) = world.get_resource_mut::<EnergyEventBatch>() {
                event_batch.events.clear();
            }
            
            // Run energy systems using manual queries (systems don't work directly on worlds)
            // Energy generation
            let mut generation_events = Vec::new();
            {
                let mut energy_query = world.query::<(Entity, &mut EnergyComponent)>();
                for (entity, mut energy) in energy_query.iter_mut(world) {
                    let old_current = energy.current;
                    
                    // Generate and consume energy
                    energy.generate();
                    energy.consume();
                    
                    if old_current != energy.current {
                        generation_events.push(EnergyChangeEvent::new(
                            entity, 
                            old_current, 
                            energy.current, 
                            energy.max_capacity
                        ));
                    }
                }
            }
            
            // Add generation events to batch
            if let Some(mut event_batch) = world.get_resource_mut::<EnergyEventBatch>() {
                event_batch.events.extend(generation_events);
            }
            
            // Energy transfer (simplified version)
            let energy_entities: Vec<_> = {
                let mut energy_query = world.query::<(Entity, &EnergyComponent, &ModulePosition)>();
                energy_query.iter(world).map(|(e, en, pos)| (e, *en, *pos)).collect()
            };
            
            let mut transfer_events = Vec::new();
            let mut transfers_to_apply = Vec::new();
            
            // Find transfers
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
                    
                    if pos_a.is_adjacent(&pos_b) {
                        let max_extract = energy_a.max_extractable();
                        let max_receive = energy_b.max_receivable();
                        let transfer_amount = max_extract.min(max_receive).min(50);
                        
                        if transfer_amount > 0 {
                            transfers_to_apply.push((entity_a, entity_b, transfer_amount));
                        }
                    }
                }
            }
            
            // Apply transfers sequentially to avoid borrow conflicts
            for (from_entity, to_entity, amount) in transfers_to_apply {
                // Process from entity first
                let from_result = {
                    if let Some(mut from_energy) = world.get_mut::<EnergyComponent>(from_entity) {
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
                            if let Some(mut to_energy) = world.get_mut::<EnergyComponent>(to_entity) {
                                let old_energy = to_energy.current;
                                let actual_received = to_energy.receive_energy(actual_extracted);
                                Some((old_energy, to_energy.current, to_energy.max_capacity, actual_received))
                            } else {
                                None
                            }
                        };
                        
                        if let Some((old_to, new_to, to_capacity, _actual_received)) = to_result {
                            transfer_events.push(EnergyChangeEvent::new(
                                from_entity, old_from, new_from, from_capacity
                            ));
                            
                            transfer_events.push(EnergyChangeEvent::new(
                                to_entity, old_to, new_to, to_capacity
                            ));
                        }
                    }
                }
            }
            
            // Add transfer events
            if let Some(mut event_batch) = world.get_resource_mut::<EnergyEventBatch>() {
                event_batch.events.extend(transfer_events);
            }
            
            // Processing system
            let mut processing_query = world.query::<(Entity, &mut ProcessingComponent)>();
            for (_entity, mut processor) in processing_query.iter_mut(world) {
                processor.tick();
            }
            
            // Update batch statistics
            let event_count = world.get_resource::<EnergyEventBatch>()
                .map(|eb| eb.events.len() as u32)
                .unwrap_or(0);
                
            if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
                batch_processor.pending_changes = event_count;
            }
        } else {
            // On non-processing ticks, just update minimal stats
            if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
                batch_processor.pending_changes += 1;
            }
        }
    }
}