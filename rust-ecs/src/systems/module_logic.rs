use bevy_ecs::prelude::*;
use crate::components::{EnergyComponent, ModulePosition, ModuleType, WorldPosition};
use crate::resources::TickCounter;

/// Marker component for energy generators (TestModule, EnergyGeneratorModule, etc.)
#[derive(Component, Debug)]
pub struct EnergyGenerator {
    pub generation_rate: i32,
    pub max_capacity: i32,
}

/// Marker component for energy storage units
#[derive(Component, Debug)]
pub struct EnergyStorage {
    pub max_capacity: i32,
    pub max_transfer_rate: i32,
}

/// Marker component for energy consumers
#[derive(Component, Debug)]
pub struct EnergyConsumer {
    pub consumption_rate: i32,
}

/// Component to track module state from Java
#[derive(Component, Debug)]
pub struct JavaModuleSync {
    pub module_id: String,
    pub needs_sync: bool,
    pub last_sync_tick: u64,
}

/// System that handles ALL energy generation logic (moved from Java)
pub fn rust_energy_generation_system(
    _tick_counter: Res<TickCounter>,
    mut generator_query: Query<(Entity, &mut EnergyComponent, &EnergyGenerator, &ModulePosition, &WorldPosition), Without<EnergyConsumer>>,
    mut sync_query: Query<&mut JavaModuleSync>,
    mut sync_manager: Option<ResMut<crate::systems::sync::EnergySyncManager>>,
) {
    for (entity, mut energy, generator, position, world_pos) in generator_query.iter_mut() {
        let old_energy = energy.current;
        
        // RUST ECS LOGIC: Generate energy (replacing Java TestModule.onTick)
        if energy.current < generator.max_capacity {
            energy.current = (energy.current + generator.generation_rate).min(generator.max_capacity);
            
            // CRITICAL FIX: Directly notify EnergySyncManager about energy changes
            if let Some(ref mut sync_mgr) = sync_manager {
                let world_coords = (world_pos.x, world_pos.y, world_pos.z);
                let module_coords = (position.x, position.y, position.z);
                sync_mgr.mark_energy_changed(world_coords, module_coords, energy.current, energy.max_capacity);
                
                println!("🔄 Generator: Marked energy change for sync {} -> {}", old_energy, energy.current);
            }
            
            // Mark for Java sync (legacy system - keep for fallback)
            if let Ok(mut sync) = sync_query.get_mut(entity) {
                sync.needs_sync = true;
            }
            
            println!("⚡ Rust Generator {:?} at {:?} generated {} RF -> total: {}", 
                     entity, (position.x, position.y, position.z), 
                     energy.current - old_energy, energy.current);
        }
    }
}

/// System that handles ALL energy transfer logic (moved from Java)
pub fn rust_energy_transfer_system(
    mut energy_query: Query<(Entity, &mut EnergyComponent, &ModulePosition)>,
    mut sync_query: Query<&mut JavaModuleSync>,
) {
    // Collect all energy entities to avoid borrow checker issues
    let energy_entities: Vec<_> = energy_query
        .iter()
        .map(|(e, energy, pos)| (e, energy.current, energy.max_capacity, 
                                energy.can_extract, energy.can_receive, 
                                energy.max_transfer_rate, *pos))
        .collect();
    
    // Process energy transfers between adjacent modules
    let mut transfers = Vec::new();
    
    for i in 0..energy_entities.len() {
        let (entity_a, current_a, max_a, can_extract_a, can_receive_a, transfer_a, pos_a) = energy_entities[i];
        
        if !can_extract_a || current_a <= 0 {
            continue;
        }
        
        for j in (i + 1)..energy_entities.len() {
            let (entity_b, current_b, max_b, can_extract_b, can_receive_b, transfer_b, pos_b) = energy_entities[j];
            
            if !can_receive_b || current_b >= max_b {
                continue;
            }
            
            // Check if modules are adjacent
            if pos_a.is_adjacent(&pos_b) {
                let max_extract = transfer_a.min(current_a);
                let max_receive = transfer_b.min(max_b - current_b);
                let transfer_amount = max_extract.min(max_receive).min(50); // Max 50 RF per tick
                
                if transfer_amount > 0 {
                    transfers.push((entity_a, entity_b, transfer_amount));
                }
            }
        }
    }
    
    // Apply transfers
    for (from_entity, to_entity, amount) in transfers {
        // Extract from source
        if let Ok((_, mut from_energy, _)) = energy_query.get_mut(from_entity) {
            let actual_extracted = amount.min(from_energy.current);
            from_energy.current -= actual_extracted;
            
            // Mark source for sync
            if let Ok(mut sync) = sync_query.get_mut(from_entity) {
                sync.needs_sync = true;
            }
            
            // Receive at destination
            if let Ok((_, mut to_energy, _)) = energy_query.get_mut(to_entity) {
                let actual_received = actual_extracted.min(to_energy.max_capacity - to_energy.current);
                to_energy.current += actual_received;
                
                // Mark destination for sync
                if let Ok(mut sync) = sync_query.get_mut(to_entity) {
                    sync.needs_sync = true;
                }
                
                println!("🔄 Rust Transfer: {} RF from {:?} to {:?}", 
                         actual_received, from_entity, to_entity);
            }
        }
    }
}

/// System that handles ALL energy consumption logic (moved from Java)
pub fn rust_energy_consumption_system(
    tick_counter: Res<TickCounter>,
    mut consumer_query: Query<(Entity, &mut EnergyComponent, &EnergyConsumer, &ModulePosition), Without<EnergyGenerator>>,
    mut sync_query: Query<&mut JavaModuleSync>,
) {
    for (entity, mut energy, consumer, position) in consumer_query.iter_mut() {
        let old_energy = energy.current;
        
        // RUST ECS LOGIC: Consume energy (replacing Java consumption logic)
        if energy.current > 0 {
            let consumed = consumer.consumption_rate.min(energy.current);
            energy.current -= consumed;
            
            if consumed > 0 {
                // Mark for Java sync
                if let Ok(mut sync) = sync_query.get_mut(entity) {
                    sync.needs_sync = true;
                }
                
                println!("🔋 Rust Consumer {:?} at {:?} consumed {} RF -> remaining: {}", 
                         entity, (position.x, position.y, position.z), consumed, energy.current);
            }
        }
    }
}

/// System that manages module lifecycle and state
pub fn rust_module_lifecycle_system(
    tick_counter: Res<TickCounter>,
    mut commands: Commands,
    module_query: Query<(Entity, &ModuleType, &ModulePosition), Added<ModuleType>>,
) {
    for (entity, module_type, position) in module_query.iter() {
        println!("🏗️ Rust: Initializing module {:?} at {:?}", module_type.type_id, 
                 (position.x, position.y, position.z));
        
        // Add appropriate components based on module type
        match module_type.type_id.as_str() {
            "test_module" => {
                commands.entity(entity)
                    .insert(EnergyGenerator {
                        generation_rate: 10,
                        max_capacity: 1000,
                    })
                    .insert(EnergyComponent::generator(1000, 10, 100))
                    .insert(JavaModuleSync {
                        module_id: "test_module".to_string(),
                        needs_sync: false,
                        last_sync_tick: tick_counter.count,
                    });
            },
            "energy_storage" => {
                commands.entity(entity)
                    .insert(EnergyStorage {
                        max_capacity: 50000,
                        max_transfer_rate: 100,
                    })
                    .insert(EnergyComponent::storage(50000, 100))
                    .insert(JavaModuleSync {
                        module_id: "energy_storage".to_string(),
                        needs_sync: false,
                        last_sync_tick: tick_counter.count,
                    });
            },
            "energy_consumer" => {
                commands.entity(entity)
                    .insert(EnergyConsumer {
                        consumption_rate: 20,
                    })
                    .insert(EnergyComponent::consumer(20, 50))
                    .insert(JavaModuleSync {
                        module_id: "energy_consumer".to_string(),
                        needs_sync: false,
                        last_sync_tick: tick_counter.count,
                    });
            },
            _ => {
                // Default module setup
                commands.entity(entity)
                    .insert(EnergyComponent::default())
                    .insert(JavaModuleSync {
                        module_id: module_type.type_id.clone(),
                        needs_sync: false,
                        last_sync_tick: tick_counter.count,
                    });
            }
        }
    }
}

/// System that collects all modules that need synchronization with Java
pub fn rust_java_sync_collection_system(
    tick_counter: Res<TickCounter>,
    mut sync_query: Query<(Entity, &mut JavaModuleSync, &EnergyComponent, &ModulePosition)>,
) {
    let mut sync_count = 0;
    
    for (entity, mut sync, energy, position) in sync_query.iter_mut() {
        if sync.needs_sync && (tick_counter.count - sync.last_sync_tick) >= 5 {
            // Collect sync data (this would be sent to Java)
            println!("📤 Rust: Module {:?} at {:?} needs sync - Energy: {}/{}", 
                     entity, (position.x, position.y, position.z), energy.current, energy.max_capacity);
            
            sync.needs_sync = false;
            sync.last_sync_tick = tick_counter.count;
            sync_count += 1;
        }
    }
    
    if sync_count > 0 {
        println!("🚀 Rust: Collected {} modules for Java sync at tick {}", sync_count, tick_counter.count);
    }
}