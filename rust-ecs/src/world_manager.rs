use bevy_ecs::prelude::*;
use std::collections::HashMap;
use std::sync::{LazyLock, Mutex};

/// Global storage for ECS worlds indexed by world ID
pub static WORLDS: LazyLock<Mutex<HashMap<u64, World>>> = 
    LazyLock::new(|| Mutex::new(HashMap::new()));

/// Global counter for generating unique world IDs
pub static NEXT_WORLD_ID: LazyLock<Mutex<u64>> = 
    LazyLock::new(|| Mutex::new(1));

/// Helper function to get world by ID
pub fn with_world<T, F>(world_id: u64, f: F) -> Option<T>
where
    F: FnOnce(&World) -> T,
{
    let worlds = WORLDS.lock().unwrap();
    worlds.get(&world_id).map(f)
}

/// Helper function to get mutable world by ID
pub fn with_world_mut<T, F>(world_id: u64, f: F) -> Option<T>
where
    F: FnOnce(&mut World) -> T,
{
    let mut worlds = WORLDS.lock().unwrap();
    worlds.get_mut(&world_id).map(f)
}

/// Get the number of active worlds
pub fn get_world_count() -> usize {
    let worlds = WORLDS.lock().unwrap();
    worlds.len()
}

/// Clean up all worlds (called on shutdown)
pub fn cleanup_all_worlds() {
    let mut worlds = WORLDS.lock().unwrap();
    worlds.clear();
}