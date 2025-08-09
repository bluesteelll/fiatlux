use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jint, jboolean};
use bevy_ecs::prelude::*;
use crate::world_manager::WORLDS;
use crate::components::*;
use crate::jni::jstring_to_string;

/// Spawn a new entity in the ECS world
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
        let type_id = match jstring_to_string(&mut env, module_type) {
            Ok(s) => s,
            Err(_) => return -1,
        };
        
        let position = ModulePosition::new(x, y, z);
        let module_type_comp = ModuleType::new(type_id.clone());
        
        let mut entity_mut = world.spawn((position, module_type_comp));
        
        // Add specific components based on module type
        match type_id.as_str() {
            // Basic energy modules
            "energy_generator" | "basic_energy_generator" => {
                entity_mut.insert(EnergyComponent::generator(1000, 10, 100));
            }
            "energy_storage" | "basic_energy_storage" => {
                entity_mut.insert(EnergyComponent::storage(5000, 100));
            }
            "energy_consumer" | "basic_energy_consumer" => {
                entity_mut.insert(EnergyComponent::consumer(10, 50));
            }
            
            // Advanced energy modules (examples for inheritance)
            "advanced_energy_generator" => {
                entity_mut.insert(EnergyComponent::generator(2000, 25, 200));
            }
            "advanced_energy_storage" => {
                entity_mut.insert(EnergyComponent::storage(10000, 200));
            }
            "high_power_consumer" => {
                entity_mut.insert(EnergyComponent::consumer(50, 100));
            }
            
            // Processing modules
            "processor" => {
                entity_mut.insert(ProcessingComponent::new(1.0));
                // Processors often need energy too
                entity_mut.insert(EnergyComponent::consumer(20, 100));
            }
            "advanced_processor" => {
                entity_mut.insert(ProcessingComponent::new(2.0));
                entity_mut.insert(EnergyComponent::consumer(40, 150));
            }
            
            // Display modules
            "display" => {
                entity_mut.insert(DisplayComponent::new(String::new()));
            }
            
            // Storage modules  
            "storage" => {
                entity_mut.insert(StorageComponent::new(9));
            }
            "advanced_storage" => {
                entity_mut.insert(StorageComponent::new(27));
            }
            
            _ => {} // Default entity with just position and type
        }
        
        entity_mut.id().index() as jlong
    } else {
        -1 // Error: world not found
    }
}

/// Despawn an entity from the ECS world
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

/// Set display text for a display module
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
            match jstring_to_string(&mut env, text) {
                Ok(new_text) => {
                    display.set_text(new_text);
                    1
                }
                Err(_) => 0,
            }
        } else {
            0
        }
    } else {
        0
    }
}