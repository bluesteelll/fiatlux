use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jint, jboolean};
use bevy_ecs::prelude::*;
use crate::world_manager::WORLDS;
use crate::components::EnergyComponent;
use crate::resources::{EnergyEventBatch, EnergyTransferBatch};

/// Get energy level of an entity
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getEnergyLevel(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    entity_id: jlong,
) -> jint {
    let worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get(&(world_id as u64)) {
        let entity = Entity::from_raw(entity_id as u32);
        if let Some(energy) = world.get::<EnergyComponent>(entity) {
            return energy.current;
        }
    }
    -1 // Error or no energy component
}

/// Set energy level for an entity (external integration)
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_setEnergyLevel(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    entity_id: jlong,
    new_energy: jint,
) -> jboolean {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let entity = Entity::from_raw(entity_id as u32);
        if let Some(mut energy) = world.get_mut::<EnergyComponent>(entity) {
            energy.current = new_energy.max(0).min(energy.max_capacity);
            return 1;
        }
    }
    0 // Error
}

/// Get all pending energy change events for synchronization
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getEnergyEvents(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) -> jlong {
    let worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get(&(world_id as u64)) {
        if let Some(event_batch) = world.get_resource::<EnergyEventBatch>() {
            // Pack event count and first event data for batch transfer
            if event_batch.events.is_empty() {
                return 0; // No events
            }
            
            let event_count = event_batch.events.len() as u64;
            return (event_count << 32) as jlong; // Pack count in high bits
        }
    }
    -1 // Error
}

/// Get specific energy event data by index
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getEnergyEventData(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    event_index: jint,
) -> jlong {
    let worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get(&(world_id as u64)) {
        if let Some(event_batch) = world.get_resource::<EnergyEventBatch>() {
            if let Some(event) = event_batch.events.get(event_index as usize) {
                // Pack: entity_id (32 bits) | new_energy (16 bits) | max_capacity (16 bits)
                let entity_bits = event.entity.index() as u64;
                let energy_bits = (event.new_energy as u64) & 0xFFFF;
                let capacity_bits = (event.max_capacity as u64) & 0xFFFF;
                
                return ((entity_bits << 32) | (energy_bits << 16) | capacity_bits) as jlong;
            }
        }
    }
    -1 // Error or index out of bounds
}

/// Batch get all entities with energy data for synchronization
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getAllEnergyData(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) -> jlong {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let mut energy_query = world.query::<(Entity, &EnergyComponent)>();
        let entity_count = energy_query.iter(world).count();
        
        if entity_count == 0 {
            return 0;
        }
        
        // Return entity count for batch processing
        entity_count as jlong
    } else {
        -1 // Error: world not found
    }
}

/// Get energy data for specific entity by index (for batch transfer)
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getEnergyDataByIndex(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    index: jint,
) -> jlong {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let mut energy_query = world.query::<(Entity, &EnergyComponent)>();
        let energy_entities: Vec<_> = energy_query.iter(world).collect();
        
        if let Some((entity, energy)) = energy_entities.get(index as usize) {
            // Pack: entity_id (32 bits) | current_energy (16 bits) | max_capacity (16 bits)
            let entity_bits = entity.index() as u64;
            let current_bits = (energy.current as u64) & 0xFFFF;
            let capacity_bits = (energy.max_capacity as u64) & 0xFFFF;
            
            return ((entity_bits << 32) | (current_bits << 16) | capacity_bits) as jlong;
        }
    }
    -1 // Error or index out of bounds
}

/// Get transfer statistics for monitoring
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getTransferStats(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) -> jlong {
    let worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get(&(world_id as u64)) {
        if let Some(transfer_batch) = world.get_resource::<EnergyTransferBatch>() {
            let pending_count = transfer_batch.pending_transfers.len() as u32;
            let completed_count = transfer_batch.completed_transfers.len() as u32;
            
            // Pack: pending_count (32 bits) | completed_count (32 bits)
            return ((pending_count as u64) << 32 | completed_count as u64) as jlong;
        }
    }
    -1 // Error
}