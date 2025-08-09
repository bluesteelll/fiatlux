use bevy_ecs::prelude::*;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jint, jboolean};
use std::collections::HashMap;
use std::sync::{LazyLock, Mutex};

// Global storage for ECS worlds indexed by world ID
static WORLDS: LazyLock<Mutex<HashMap<u64, World>>> = LazyLock::new(|| Mutex::new(HashMap::new()));
static NEXT_WORLD_ID: LazyLock<Mutex<u64>> = LazyLock::new(|| Mutex::new(1));

// Component definitions that mirror MechaModule capabilities
#[derive(Component)]
pub struct ModulePosition {
    pub x: i32,
    pub y: i32, 
    pub z: i32,
}

#[derive(Component)]
pub struct ModuleType {
    pub type_id: String,
}

#[derive(Component)]
pub struct EnergyComponent {
    pub current: i32,
    pub max_capacity: i32,
    pub generation_rate: i32,
    pub consumption_rate: i32,
}

#[derive(Component)]
pub struct ProcessingComponent {
    pub processing_speed: f32,
    pub current_progress: f32,
}

#[derive(Component)]
pub struct StorageComponent {
    pub item_slots: Vec<String>, // Simplified item storage
    pub max_slots: i32,
}

#[derive(Component)]
pub struct DisplayComponent {
    pub display_text: String,
}

// Batch processing configuration
const BATCH_INTERVAL: u64 = 4; // Process systems every 4 ticks

// Resource for tracking tick count and batch processing
#[derive(Resource)]
pub struct TickCounter {
    pub count: u64,
}

// Resource for batch processing control
#[derive(Resource)]
pub struct BatchProcessor {
    pub should_process: bool,
    pub pending_changes: u32, // Track number of pending changes for optimization
}

// Component for tracking changed entities (optimization)
#[derive(Component)]
pub struct Changed;

// Resource for batching energy changes
#[derive(Resource, Default)]
pub struct EnergyChangeBatch {
    pub changes: Vec<(Entity, i32, i32)>, // (entity, generation, consumption)
}

// Resource for batching processing changes 
#[derive(Resource, Default)]
pub struct ProcessingChangeBatch {
    pub changes: Vec<(Entity, f32)>, // (entity, progress_delta)
}

// System for energy generation/consumption - batch optimized
pub fn energy_system(mut query: Query<(Entity, &mut EnergyComponent), Without<Changed>>) {
    for (_entity, mut energy) in &mut query {
        let old_current = energy.current;
        
        // Generate energy
        energy.current = (energy.current + energy.generation_rate).min(energy.max_capacity);
        
        // Consume energy
        energy.current = (energy.current - energy.consumption_rate).max(0);
        
        // Mark as changed if value actually changed
        if old_current != energy.current {
            // Will be marked as Changed in the batch system
        }
    }
}

// System for processing operations - batch optimized
pub fn processing_system(mut query: Query<(Entity, &mut ProcessingComponent), Without<Changed>>) {
    for (_entity, mut processor) in &mut query {
        if processor.current_progress < 100.0 {
            let old_progress = processor.current_progress;
            processor.current_progress += processor.processing_speed;
            
            if processor.current_progress >= 100.0 {
                processor.current_progress = 0.0;
                // Processing complete - would trigger callbacks to Java
            }
            
            // Mark as changed for next batch cycle
            if old_progress != processor.current_progress {
                // Will be marked as Changed in the batch system
            }
        }
    }
}

// JNI exports for Java integration

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_createWorld(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let mut world = World::new();
    world.insert_resource(TickCounter { count: 0 });
    world.insert_resource(BatchProcessor { should_process: false, pending_changes: 0 });
    world.insert_resource(EnergyChangeBatch::default());
    world.insert_resource(ProcessingChangeBatch::default());
    
    let mut worlds = WORLDS.lock().unwrap();
    let mut next_id = NEXT_WORLD_ID.lock().unwrap();
    let world_id = *next_id;
    *next_id += 1;
    
    worlds.insert(world_id, world);
    world_id as jlong
}

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_destroyWorld(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) {
    let mut worlds = WORLDS.lock().unwrap();
    worlds.remove(&(world_id as u64));
}

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_spawnEntity(
    mut env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    module_type: JString,
    x: jint,
    y: jint,
    z: jint,
) -> jlong {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let type_id: String = env.get_string(&module_type).unwrap().into();
        
        let entity = world.spawn((
            ModulePosition { x, y, z },
            ModuleType { type_id: type_id.clone() },
        )).id();
        
        // Add specific components based on module type
        match type_id.as_str() {
            "energy_generator" => {
                world.entity_mut(entity).insert(EnergyComponent {
                    current: 0,
                    max_capacity: 1000,
                    generation_rate: 10,
                    consumption_rate: 0,
                });
            }
            "energy_storage" => {
                world.entity_mut(entity).insert(EnergyComponent {
                    current: 0,
                    max_capacity: 5000,
                    generation_rate: 0,
                    consumption_rate: 0,
                });
            }
            "processor" => {
                world.entity_mut(entity).insert(ProcessingComponent {
                    processing_speed: 1.0,
                    current_progress: 0.0,
                });
            }
            "display" => {
                world.entity_mut(entity).insert(DisplayComponent {
                    display_text: "".to_string(),
                });
            }
            _ => {} // Default entity with just position and type
        }
        
        entity.index() as jlong
    } else {
        -1 // Error: world not found
    }
}

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_despawnEntity(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    entity_id: jlong,
) -> jboolean {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let entity = Entity::from_raw(entity_id as u32);
        if world.despawn(entity) { 1 } else { 0 }
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_tickWorld(
    _env: JNIEnv,
    _class: JClass,
    world_id: jlong,
) {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        // Update tick counter and determine if we should process this tick
        let should_process = {
            if let Some(mut tick_counter) = world.get_resource_mut::<TickCounter>() {
                tick_counter.count += 1;
                tick_counter.count % BATCH_INTERVAL == 0
            } else {
                false
            }
        };
        
        // Update batch processor
        if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
            batch_processor.should_process = should_process;
        }
        
        // Only run systems every BATCH_INTERVAL ticks for maximum performance
        if should_process {
            // Clear Changed markers from previous batch
            let mut changed_query = world.query_filtered::<Entity, With<Changed>>();
            let changed_entities: Vec<Entity> = changed_query.iter(world).collect();
            
            for entity in changed_entities {
                world.entity_mut(entity).remove::<Changed>();
            }
            
            // Run energy system - batch optimized
            let mut energy_query = world.query::<(Entity, &mut EnergyComponent)>();
            let mut entities_to_mark: Vec<Entity> = Vec::new();
            
            for (entity, mut energy) in energy_query.iter_mut(world) {
                let old_current = energy.current;
                
                // Generate energy
                energy.current = (energy.current + energy.generation_rate).min(energy.max_capacity);
                
                // Consume energy  
                energy.current = (energy.current - energy.consumption_rate).max(0);
                
                // Track changed entities for next batch optimization
                if old_current != energy.current {
                    entities_to_mark.push(entity);
                }
            }
            
            // Run processing system - batch optimized
            let mut processing_query = world.query::<(Entity, &mut ProcessingComponent)>();
            for (entity, mut processor) in processing_query.iter_mut(world) {
                if processor.current_progress < 100.0 {
                    let old_progress = processor.current_progress;
                    processor.current_progress += processor.processing_speed;
                    
                    if processor.current_progress >= 100.0 {
                        processor.current_progress = 0.0;
                        // Processing complete - would trigger callbacks to Java
                    }
                    
                    // Track changed entities
                    if old_progress != processor.current_progress {
                        entities_to_mark.push(entity);
                    }
                }
            }
            
            // Mark changed entities for next batch optimization
            for entity in entities_to_mark {
                if let Some(mut entity_mut) = world.get_entity_mut(entity) {
                    entity_mut.insert(Changed);
                }
            }
            
            // Update pending changes counter for monitoring
            if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
                batch_processor.pending_changes = 0; // Reset after processing
            }
        } else {
            // On non-processing ticks, just increment pending changes counter
            if let Some(mut batch_processor) = world.get_resource_mut::<BatchProcessor>() {
                batch_processor.pending_changes += 1;
            }
        }
    }
}

// Add function to get batch processing statistics
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
            let packed = ((tick_counter.count as u64) << 32) | (batch_processor.pending_changes as u64);
            return packed as jlong;
        }
    }
    -1
}

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

#[no_mangle]
pub extern "system" fn Java_art_boyko_fiatlux_ecs_BevyEcsWorld_setDisplayText(
    mut env: JNIEnv,
    _class: JClass,
    world_id: jlong,
    entity_id: jlong,
    text: JString,
) -> jboolean {
    let mut worlds = WORLDS.lock().unwrap();
    if let Some(world) = worlds.get_mut(&(world_id as u64)) {
        let entity = Entity::from_raw(entity_id as u32);
        if let Some(mut display) = world.get_mut::<DisplayComponent>(entity) {
            let new_text: String = env.get_string(&text).unwrap().into();
            display.display_text = new_text;
            return 1;
        }
    }
    0
}