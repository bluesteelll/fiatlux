use bevy_ecs::prelude::*;

/// Component for energy storage and transfer capabilities
#[derive(Component, Clone, Copy, Debug)]
pub struct EnergyComponent {
    pub current: i32,
    pub max_capacity: i32,
    pub generation_rate: i32,
    pub consumption_rate: i32,
    pub max_transfer_rate: i32,
    pub can_receive: bool,
    pub can_extract: bool,
}

impl EnergyComponent {
    /// Create a new energy generator
    pub fn generator(max_capacity: i32, generation_rate: i32, transfer_rate: i32) -> Self {
        Self {
            current: 0,
            max_capacity,
            generation_rate,
            consumption_rate: 0,
            max_transfer_rate: transfer_rate,
            can_receive: false,
            can_extract: true,
        }
    }
    
    /// Create a new energy storage
    pub fn storage(max_capacity: i32, transfer_rate: i32) -> Self {
        Self {
            current: 0,
            max_capacity,
            generation_rate: 0,
            consumption_rate: 0,
            max_transfer_rate: transfer_rate,
            can_receive: true,
            can_extract: true,
        }
    }
    
    /// Create a new energy consumer
    pub fn consumer(consumption_rate: i32, transfer_rate: i32) -> Self {
        Self {
            current: 0,
            max_capacity: 0,
            generation_rate: 0,
            consumption_rate,
            max_transfer_rate: transfer_rate,
            can_receive: true,
            can_extract: false,
        }
    }
    
    /// Check if this component can provide energy
    pub fn can_provide_energy(&self, amount: i32) -> bool {
        self.can_extract && self.current >= amount
    }
    
    /// Check if this component can accept energy
    pub fn can_accept_energy(&self, amount: i32) -> bool {
        self.can_receive && (self.current + amount) <= self.max_capacity
    }
    
    /// Get maximum amount that can be extracted
    pub fn max_extractable(&self) -> i32 {
        if self.can_extract {
            self.max_transfer_rate.min(self.current)
        } else {
            0
        }
    }
    
    /// Get maximum amount that can be received
    pub fn max_receivable(&self) -> i32 {
        if self.can_receive {
            self.max_transfer_rate.min(self.max_capacity - self.current)
        } else {
            0
        }
    }
    
    /// Extract energy (returns actual amount extracted)
    pub fn extract_energy(&mut self, max_amount: i32) -> i32 {
        let extractable = self.max_extractable().min(max_amount);
        self.current -= extractable;
        extractable
    }
    
    /// Receive energy (returns actual amount received)
    pub fn receive_energy(&mut self, amount: i32) -> i32 {
        let receivable = self.max_receivable().min(amount);
        self.current += receivable;
        receivable
    }
    
    /// Generate energy for this tick
    pub fn generate(&mut self) -> i32 {
        let old_current = self.current;
        if self.generation_rate > 0 {
            self.current = (self.current + self.generation_rate).min(self.max_capacity);
        }
        self.current - old_current
    }
    
    /// Consume energy for this tick
    pub fn consume(&mut self) -> i32 {
        let old_current = self.current;
        if self.consumption_rate > 0 {
            self.current = (self.current - self.consumption_rate).max(0);
        }
        old_current - self.current
    }
    
    /// Get energy fill percentage
    pub fn fill_percentage(&self) -> f32 {
        if self.max_capacity > 0 {
            self.current as f32 / self.max_capacity as f32
        } else {
            0.0
        }
    }
}

impl Default for EnergyComponent {
    fn default() -> Self {
        Self::storage(1000, 100)
    }
}