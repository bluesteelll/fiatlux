use bevy_ecs::prelude::*;

/// Component for item storage
#[derive(Component, Debug)]
pub struct StorageComponent {
    pub item_slots: Vec<ItemSlot>,
    pub max_slots: i32,
}

/// Simplified item representation for ECS
#[derive(Debug, Clone)]
pub struct ItemSlot {
    pub item_id: String,
    pub count: i32,
    pub max_stack_size: i32,
}

impl StorageComponent {
    pub fn new(max_slots: i32) -> Self {
        Self {
            item_slots: Vec::new(),
            max_slots,
        }
    }
    
    /// Check if storage has space for more items
    pub fn has_space(&self) -> bool {
        (self.item_slots.len() as i32) < self.max_slots
    }
    
    /// Get number of used slots
    pub fn used_slots(&self) -> i32 {
        self.item_slots.len() as i32
    }
    
    /// Get number of free slots
    pub fn free_slots(&self) -> i32 {
        self.max_slots - self.used_slots()
    }
    
    /// Find slot with specific item
    pub fn find_item_slot(&self, item_id: &str) -> Option<usize> {
        self.item_slots
            .iter()
            .position(|slot| slot.item_id == item_id && slot.count < slot.max_stack_size)
    }
    
    /// Try to add item to storage
    pub fn try_add_item(&mut self, item_id: String, count: i32, max_stack_size: i32) -> i32 {
        let mut remaining = count;
        
        // Try to add to existing stacks first
        for slot in &mut self.item_slots {
            if slot.item_id == item_id && slot.count < slot.max_stack_size {
                let can_add = (slot.max_stack_size - slot.count).min(remaining);
                slot.count += can_add;
                remaining -= can_add;
                
                if remaining == 0 {
                    break;
                }
            }
        }
        
        // Create new slots if needed and space available
        while remaining > 0 && self.has_space() {
            let can_add = remaining.min(max_stack_size);
            self.item_slots.push(ItemSlot {
                item_id: item_id.clone(),
                count: can_add,
                max_stack_size,
            });
            remaining -= can_add;
        }
        
        count - remaining // Return amount actually added
    }
    
    /// Try to remove item from storage
    pub fn try_remove_item(&mut self, item_id: &str, count: i32) -> i32 {
        let mut remaining = count;
        let mut slots_to_remove = Vec::new();
        
        for (index, slot) in self.item_slots.iter_mut().enumerate() {
            if slot.item_id == item_id {
                let can_remove = slot.count.min(remaining);
                slot.count -= can_remove;
                remaining -= can_remove;
                
                if slot.count == 0 {
                    slots_to_remove.push(index);
                }
                
                if remaining == 0 {
                    break;
                }
            }
        }
        
        // Remove empty slots (in reverse order to maintain indices)
        for index in slots_to_remove.into_iter().rev() {
            self.item_slots.remove(index);
        }
        
        count - remaining // Return amount actually removed
    }
    
    /// Get total count of specific item
    pub fn get_item_count(&self, item_id: &str) -> i32 {
        self.item_slots
            .iter()
            .filter(|slot| slot.item_id == item_id)
            .map(|slot| slot.count)
            .sum()
    }
    
    /// Check if storage contains at least the specified amount of item
    pub fn contains_item(&self, item_id: &str, count: i32) -> bool {
        self.get_item_count(item_id) >= count
    }
    
    /// Clear all items from storage
    pub fn clear(&mut self) {
        self.item_slots.clear();
    }
    
    /// Get storage fill percentage
    pub fn fill_percentage(&self) -> f32 {
        if self.max_slots > 0 {
            self.used_slots() as f32 / self.max_slots as f32
        } else {
            0.0
        }
    }
}

impl ItemSlot {
    pub fn new(item_id: String, count: i32, max_stack_size: i32) -> Self {
        Self {
            item_id,
            count: count.min(max_stack_size),
            max_stack_size,
        }
    }
    
    pub fn is_full(&self) -> bool {
        self.count >= self.max_stack_size
    }
    
    pub fn remaining_space(&self) -> i32 {
        self.max_stack_size - self.count
    }
}

impl Default for StorageComponent {
    fn default() -> Self {
        Self::new(9) // Default 9 slots like a chest
    }
}