use bevy_ecs::prelude::*;
use std::collections::HashMap;
use crate::components::{EnergyComponent, ModulePosition, WorldPosition};

/// Energy synchronization data that gets sent to Java
#[derive(Debug, Clone)]
pub struct EnergySync {
    pub position: (i32, i32, i32),
    pub energy: i32,
    pub max_energy: i32,
    pub changed: bool,
}

/// World synchronization data for a MechaGrid
#[derive(Debug, Clone)]
pub struct WorldSyncData {
    pub world_pos: (i32, i32, i32),
    pub energy_updates: Vec<EnergySync>,
    pub dirty: bool,
    pub last_sync_tick: u64,
}

impl WorldSyncData {
    pub fn new(world_pos: (i32, i32, i32)) -> Self {
        Self {
            world_pos,
            energy_updates: Vec::new(),
            dirty: false,
            last_sync_tick: 0,
        }
    }
    
    pub fn add_energy_update(&mut self, sync: EnergySync) {
        // Remove old update for same position if exists
        self.energy_updates.retain(|e| e.position != sync.position);
        self.energy_updates.push(sync);
        self.dirty = true;
    }
    
    pub fn clear_updates(&mut self, current_tick: u64) {
        self.energy_updates.clear();
        self.dirty = false;
        self.last_sync_tick = current_tick;
    }
    
    pub fn should_sync(&self, current_tick: u64) -> bool {
        self.dirty && (current_tick - self.last_sync_tick) >= 5 // Sync every 5 ticks (4 times/sec)
    }
}

/// Global resource for tracking energy synchronization
#[derive(Resource, Default)]
pub struct EnergySyncManager {
    worlds: HashMap<(i32, i32, i32), WorldSyncData>,
    current_tick: u64,
}

impl EnergySyncManager {
    pub fn new() -> Self {
        Self {
            worlds: HashMap::new(),
            current_tick: 0,
        }
    }
    
    pub fn register_world(&mut self, world_pos: (i32, i32, i32)) {
        self.worlds.entry(world_pos).or_insert_with(|| WorldSyncData::new(world_pos));
    }
    
    pub fn mark_energy_changed(&mut self, world_pos: (i32, i32, i32), module_pos: (i32, i32, i32), energy: i32, max_energy: i32) {
        if let Some(world_data) = self.worlds.get_mut(&world_pos) {
            let sync = EnergySync {
                position: module_pos,
                energy,
                max_energy,
                changed: true,
            };
            world_data.add_energy_update(sync);
        }
    }
    
    pub fn get_pending_syncs(&mut self) -> Vec<&mut WorldSyncData> {
        self.current_tick += 1;
        self.worlds.values_mut()
            .filter(|world| world.should_sync(self.current_tick))
            .collect()
    }
    
    pub fn get_world_sync_data(&self, world_pos: (i32, i32, i32)) -> Option<&WorldSyncData> {
        self.worlds.get(&world_pos)
    }
    
    pub fn clear_world_updates(&mut self, world_pos: (i32, i32, i32)) {
        if let Some(world_data) = self.worlds.get_mut(&world_pos) {
            world_data.clear_updates(self.current_tick);
        }
    }
}

/// Component to track if energy has changed and needs sync
#[derive(Component)]
pub struct EnergyDirty {
    pub world_pos: (i32, i32, i32),
    pub module_pos: (i32, i32, i32),
}

/// System to detect energy changes and mark for synchronization
pub fn energy_sync_detection_system(
    mut sync_manager: ResMut<EnergySyncManager>,
    query: Query<(Entity, &EnergyComponent, &ModulePosition, &WorldPosition), Changed<EnergyComponent>>,
) {
    for (entity, energy, position, world_pos) in query.iter() {
        let world_coords = (world_pos.x, world_pos.y, world_pos.z);
        let module_coords = (position.x, position.y, position.z);
        
        sync_manager.mark_energy_changed(world_coords, module_coords, energy.current, energy.max_capacity);
        
        println!("🔄 Energy sync marked for world {:?} module {:?}: {}/{}", 
                 world_coords, module_coords, energy.current, energy.max_capacity);
    }
}

/// System to batch and prepare synchronization data for Java
pub fn energy_sync_batch_system(
    mut sync_manager: ResMut<EnergySyncManager>,
) {
    let pending_syncs = sync_manager.get_pending_syncs();
    
    for world_data in pending_syncs {
        if !world_data.energy_updates.is_empty() {
            println!("📦 Preparing energy sync batch for world {:?}: {} updates", 
                     world_data.world_pos, world_data.energy_updates.len());
            
            // In real implementation, this would call JNI to send data to Java
            // For now, we'll just log the batch
            for sync in &world_data.energy_updates {
                println!("  └─ Module {:?}: {}/{} RF", 
                         sync.position, sync.energy, sync.max_energy);
            }
        }
    }
}

/// System to handle synchronization with Java side
pub fn java_sync_bridge_system(
    _sync_manager: ResMut<EnergySyncManager>,
) {
    // This system will be called by JNI from Java to get pending sync data
    // For now, it's a placeholder that can be expanded
}