use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong};
use bevy_ecs::prelude::*;
use crate::world_manager::{WORLDS, NEXT_WORLD_ID};
use crate::resources::*;

/// Create a new ECS world
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_createWorld(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let mut world = World::new();
    
    // Initialize resources
    world.insert_resource(TickCounter::default());
    world.insert_resource(BatchProcessor::default());
    world.insert_resource(EnergyTransferBatch::default());
    world.insert_resource(EnergyEventBatch::default());
    world.insert_resource(ProcessingChangeBatch::default());
    
    // Store world and return ID
    let mut worlds = WORLDS.lock().unwrap();
    let mut next_id = NEXT_WORLD_ID.lock().unwrap();
    let world_id = *next_id;
    *next_id += 1;
    
    worlds.insert(world_id, world);
    world_id as jlong
}

/// Destroy an ECS world
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_destroyWorld(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) {
    let mut worlds = WORLDS.lock().unwrap();
    worlds.remove(&(world_id as u64));
}

/// Get batch processing statistics
#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_getBatchStats(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) -> jlong {
    let worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get(&(world_id as u64)) {
        if let (Some(tick_counter), Some(batch_processor)) = (
            world.get_resource::<TickCounter>(),
            world.get_resource::<BatchProcessor>()
        ) {
            // Pack tick count and pending changes into jlong
            // High 32 bits: tick count, Low 32 bits: pending changes
            let packed = (tick_counter.count << 32) | (batch_processor.pending_changes as u64);
            return packed as jlong;
        }
    }
    -1
}