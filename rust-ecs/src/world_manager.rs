use bevy_ecs::prelude::*;
use std::collections::HashMap;
use std::sync::{LazyLock, Mutex};
use crate::systems::sync::EnergySyncManager;

/// Global storage for ECS worlds indexed by world ID
pub static WORLDS: LazyLock<Mutex<HashMap<u64, World>>> = 
    LazyLock::new(|| Mutex::new(HashMap::new()));

/// Global counter for generating unique world IDs
pub static NEXT_WORLD_ID: LazyLock<Mutex<u64>> = 
    LazyLock::new(|| Mutex::new(1));

/// World manager that handles world creation and synchronization setup
pub struct WorldManager {
    worlds: HashMap<(i32, i32, i32), u64>,
    world_counter: u64,
}

impl Default for WorldManager {
    fn default() -> Self {
        Self {
            worlds: HashMap::new(),
            world_counter: 1,
        }
    }
}

impl WorldManager {
    pub fn new() -> Self {
        Self::default()
    }
    
    pub fn get_or_create_world(&mut self, world_pos: (i32, i32, i32)) -> u64 {
        if let Some(&world_id) = self.worlds.get(&world_pos) {
            return world_id;
        }
        
        // Create new world
        let world_id = self.world_counter;
        self.world_counter += 1;
        
        let mut world = World::new();
        
        // Initialize synchronization systems
        world.insert_resource(EnergySyncManager::new());
        
        // Initialize world with comprehensive systems for full logic separation
        println!("🌍 Setting up full Rust ECS logic for world {} at {:?}", world_id, world_pos);
        
        // Store in global worlds map
        {
            let mut worlds = WORLDS.lock().unwrap();
            worlds.insert(world_id, world);
        }
        
        self.worlds.insert(world_pos, world_id);
        
        println!("🌍 Created ECS world {} for position {:?} with energy sync", world_id, world_pos);
        world_id
    }
    
    pub fn get_world_id(&self, world_pos: &(i32, i32, i32)) -> Option<u64> {
        self.worlds.get(world_pos).copied()
    }
    
    pub fn get_world_id_for_pos(&self, world_pos: &(i32, i32, i32)) -> Option<u64> {
        self.worlds.get(world_pos).copied()
    }
    
    pub fn remove_world(&mut self, world_pos: (i32, i32, i32)) -> bool {
        if let Some(world_id) = self.worlds.remove(&world_pos) {
            let mut worlds = WORLDS.lock().unwrap();
            worlds.remove(&world_id).is_some()
        } else {
            false
        }
    }
}

/// Global world manager instance
pub static WORLD_MANAGER: LazyLock<Mutex<WorldManager>> = 
    LazyLock::new(|| Mutex::new(WorldManager::new()));

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