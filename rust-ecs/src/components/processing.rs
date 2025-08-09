use bevy_ecs::prelude::*;

/// Component for processing operations
#[derive(Component, Debug)]
pub struct ProcessingComponent {
    pub processing_speed: f32,
    pub current_progress: f32,
    pub max_progress: f32,
    pub is_active: bool,
}

impl ProcessingComponent {
    pub fn new(processing_speed: f32) -> Self {
        Self {
            processing_speed,
            current_progress: 0.0,
            max_progress: 100.0,
            is_active: true,
        }
    }
    
    /// Process for one tick, returns true if processing completed
    pub fn tick(&mut self) -> bool {
        if !self.is_active {
            return false;
        }
        
        if self.current_progress < self.max_progress {
            self.current_progress += self.processing_speed;
            
            if self.current_progress >= self.max_progress {
                self.current_progress = 0.0; // Reset for next cycle
                return true; // Processing completed
            }
        }
        
        false
    }
    
    /// Get processing completion percentage
    pub fn completion_percentage(&self) -> f32 {
        if self.max_progress > 0.0 {
            (self.current_progress / self.max_progress).min(1.0)
        } else {
            0.0
        }
    }
    
    /// Check if processing is complete
    pub fn is_complete(&self) -> bool {
        self.current_progress >= self.max_progress
    }
    
    /// Reset processing progress
    pub fn reset(&mut self) {
        self.current_progress = 0.0;
    }
    
    /// Set processing active state
    pub fn set_active(&mut self, active: bool) {
        self.is_active = active;
    }
}

impl Default for ProcessingComponent {
    fn default() -> Self {
        Self::new(1.0)
    }
}