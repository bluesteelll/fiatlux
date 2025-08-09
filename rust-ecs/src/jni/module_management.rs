use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jboolean};
use crate::world_manager::WORLD_MANAGER;
use crate::systems::module_logic::*;
use crate::components::{EnergyComponent, ModulePosition, ModuleType, WorldPosition};
use crate::resources::TickCounter;
use bevy_ecs::prelude::*;
use bevy_ecs::system::IntoSystem;

/// Create a module entity in Rust ECS with full logic
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_createModuleNative(
    mut env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
    module_x: jint,
    module_y: jint,
    module_z: jint,
    module_type: JString,
) -> jlong {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    let module_pos = (module_x as i32, module_y as i32, module_z as i32);
    
    if let Ok(module_type_str) = env.get_string(&module_type) {
        let module_type_string: String = module_type_str.into();
        
        if let Some(mut world_manager) = WORLD_MANAGER.lock().ok() {
            let world_id = world_manager.get_or_create_world(world_pos);
            
            // Access the world and create entity
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    // Create entity with all necessary components
                    let entity = world.spawn((
                        ModulePosition::new(module_pos.0, module_pos.1, module_pos.2),
                        WorldPosition::new(world_pos.0, world_pos.1, world_pos.2),
                        ModuleType::new(module_type_string.clone()),
                        // EnergyComponent and other components will be added by lifecycle system
                    )).id();
                    
                    println!("🏗️ Rust: Created module '{}' entity {:?} at {:?} in world {}", 
                             module_type_string, entity, module_pos, world_id);
                    
                    return entity.index() as jlong;
                }
            }
        }
    }
    
    -1 // Error
}

/// Remove a module entity from Rust ECS
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_removeModule(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
    entity_id: jlong,
) -> jboolean {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    let entity = Entity::from_raw(entity_id as u32);
                    
                    if world.despawn(entity) {
                        println!("🗑️ Rust: Removed module entity {:?} from world {}", entity, world_id);
                        return 1; // Success
                    }
                }
            }
        }
    }
    
    0 // Failure
}

/// Tick all systems for a world - this is where ALL logic happens now
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_tickWorldNative(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
) {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    
                    // Initialize tick counter if not present
                    if !world.contains_resource::<TickCounter>() {
                        world.insert_resource(TickCounter { count: 0 });
                    }
                    
                    // Increment tick counter
                    {
                        let mut tick_counter = world.resource_mut::<TickCounter>();
                        tick_counter.count += 1;
                    }
                    
                    // ALL MODULE LOGIC HAPPENS HERE IN RUST!
                    let tick_count = world.resource::<TickCounter>().count;
                    
                    // Ensure sync manager resource exists
                    if !world.contains_resource::<crate::systems::sync::EnergySyncManager>() {
                        world.insert_resource(crate::systems::sync::EnergySyncManager::new());
                    }
                    
                    // Run module lifecycle system first (for newly added modules)
                    {
                        let mut lifecycle_system = IntoSystem::into_system(crate::systems::module_logic::rust_module_lifecycle_system);
                        lifecycle_system.initialize(world);
                        lifecycle_system.run((), world);
                    }
                    
                    // Run energy systems every tick
                    {
                        let mut gen_system = IntoSystem::into_system(crate::systems::module_logic::rust_energy_generation_system);
                        gen_system.initialize(world);
                        gen_system.run((), world);
                    }
                    {
                        let mut transfer_system = IntoSystem::into_system(crate::systems::module_logic::rust_energy_transfer_system);
                        transfer_system.initialize(world);
                        transfer_system.run((), world);
                    }
                    {
                        let mut consumption_system = IntoSystem::into_system(crate::systems::module_logic::rust_energy_consumption_system);
                        consumption_system.initialize(world);
                        consumption_system.run((), world);
                    }
                    
                    // Run sync detection system to mark changed modules
                    {
                        let mut sync_detection_system = IntoSystem::into_system(crate::systems::sync::energy_sync_detection_system);
                        sync_detection_system.initialize(world);
                        sync_detection_system.run((), world);
                    }
                    
                    // Run Java sync collection system
                    {
                        let mut java_sync_system = IntoSystem::into_system(crate::systems::module_logic::rust_java_sync_collection_system);
                        java_sync_system.initialize(world);
                        java_sync_system.run((), world);
                    }
                    
                    // Run batch processing systems every few ticks
                    if tick_count % 4 == 0 {
                        let mut timing_system = IntoSystem::into_system(crate::systems::batch::batch_timing_system);
                        timing_system.initialize(world);
                        timing_system.run((), world);
                        
                        let mut stats_system = IntoSystem::into_system(crate::systems::batch::batch_stats_system);
                        stats_system.initialize(world);
                        stats_system.run((), world);
                        
                        // Run sync batch system for preparing data to send to Java
                        let mut sync_batch_system = IntoSystem::into_system(crate::systems::sync::energy_sync_batch_system);
                        sync_batch_system.initialize(world);
                        sync_batch_system.run((), world);
                    }
                    
                    if tick_count % 20 == 0 {
                        println!("🚀 Rust: Processed world {} tick {} with energy systems", world_id, tick_count);
                    }
                    
                    // Apply all commands
                    world.flush();
                    
                    let tick_counter = world.resource::<TickCounter>();
                    if tick_counter.count % 20 == 0 { // Every second
                        println!("🚀 Rust: World {} ticked - all logic processed in Rust ECS", world_id);
                    }
                }
            }
        }
    }
}

/// Get energy data for a specific module (Java calls this for display)
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_getModuleEnergy(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
    entity_id: jlong,
) -> jlong {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get(&world_id) {
                    let entity = Entity::from_raw(entity_id as u32);
                    
                    if let Some(energy) = world.get::<EnergyComponent>(entity) {
                        // Pack energy data: current (16 bits) + max (16 bits) + flags (16 bits) + generation_rate (16 bits)
                        let current = (energy.current as u16) as u64;
                        let max_cap = (energy.max_capacity as u16) as u64;
                        let flags = ((if energy.can_receive { 1 } else { 0 }) | 
                                   (if energy.can_extract { 2 } else { 0 })) as u64;
                        let gen_rate = (energy.generation_rate as u16) as u64;
                        
                        return ((gen_rate << 48) | (flags << 32) | (max_cap << 16) | current) as jlong;
                    }
                }
            }
        }
    }
    
    -1 // No energy data
}

/// Get all modules that need synchronization with Java
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_getModulesToSync(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
) -> jlong {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    
                    // Check if we have pending sync data in EnergySyncManager
                    if let Some(sync_manager) = world.get_resource::<crate::systems::sync::EnergySyncManager>() {
                        if let Some(world_data) = sync_manager.get_world_sync_data(world_pos) {
                            return world_data.energy_updates.len() as jlong;
                        }
                    }
                    
                    // Fallback: count modules that need sync using JavaModuleSync
                    let sync_count = world
                        .query::<&JavaModuleSync>()
                        .iter(world)
                        .filter(|sync| sync.needs_sync)
                        .count();
                    
                    return sync_count as jlong;
                }
            }
        }
    }
    
    0
}

/// Get energy sync data for a specific world (returns packed data for all modules)
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_getSyncData(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
) -> jlong {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    
                    // Clear sync data after retrieving it
                    if let Some(mut sync_manager) = world.get_resource_mut::<crate::systems::sync::EnergySyncManager>() {
                        if let Some(world_data) = sync_manager.get_world_sync_data(world_pos) {
                            let update_count = world_data.energy_updates.len();
                            if update_count > 0 {
                                println!("📤 Rust: Sending {} energy updates to Java", update_count);
                                // For now, just return the count - later this could return actual data
                                sync_manager.clear_world_updates(world_pos);
                                return update_count as jlong;
                            }
                        }
                    }
                }
            }
        }
    }
    
    0 // No sync data
}

/// Force sync all modules (called when Java needs fresh data)
#[no_mangle]
pub extern "C" fn Java_art_boyko_fiatlux_ecs_RustModuleManager_forceSyncAll(
    _env: JNIEnv,
    _class: JClass,
    world_x: jint,
    world_y: jint,
    world_z: jint,
) {
    let world_pos = (world_x as i32, world_y as i32, world_z as i32);
    
    if let Some(world_manager) = WORLD_MANAGER.lock().ok() {
        if let Some(world_id) = world_manager.get_world_id(&world_pos) {
            if let Some(mut worlds) = crate::world_manager::WORLDS.lock().ok() {
                if let Some(world) = worlds.get_mut(&world_id) {
                    
                    // Register world in sync manager if not already registered
                    if !world.contains_resource::<crate::systems::sync::EnergySyncManager>() {
                        world.insert_resource(crate::systems::sync::EnergySyncManager::new());
                    }
                    
                    // Collect module data first to avoid borrow conflicts
                    let module_data: Vec<_> = world.query::<(&EnergyComponent, &ModulePosition)>().iter(world)
                        .map(|(energy, position)| (energy.current, energy.max_capacity, position.x, position.y, position.z))
                        .collect();
                    
                    if let Some(mut sync_manager) = world.get_resource_mut::<crate::systems::sync::EnergySyncManager>() {
                        sync_manager.register_world(world_pos);
                        
                        // Force mark all modules for sync
                        let sync_count = module_data.len();
                        for (current, max_capacity, x, y, z) in module_data {
                            let module_coords = (x, y, z);
                            sync_manager.mark_energy_changed(world_pos, module_coords, current, max_capacity);
                        }
                        
                        println!("🔄 Rust: Force sync {} modules in world {:?}", sync_count, world_pos);
                    }
                }
            }
        }
    }
}