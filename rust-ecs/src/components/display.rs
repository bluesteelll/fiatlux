use bevy_ecs::prelude::*;

/// Component for display modules that show information
#[derive(Component, Debug)]
pub struct DisplayComponent {
    pub display_text: String,
    pub max_length: usize,
    pub is_visible: bool,
}

impl DisplayComponent {
    pub fn new(text: String) -> Self {
        Self {
            display_text: text,
            max_length: 256,
            is_visible: true,
        }
    }
    
    pub fn with_max_length(mut self, max_length: usize) -> Self {
        self.max_length = max_length;
        self.truncate_if_needed();
        self
    }
    
    /// Set display text, truncating if necessary
    pub fn set_text(&mut self, text: String) {
        self.display_text = text;
        self.truncate_if_needed();
    }
    
    /// Append text to current display
    pub fn append_text(&mut self, text: &str) {
        self.display_text.push_str(text);
        self.truncate_if_needed();
    }
    
    /// Clear display text
    pub fn clear(&mut self) {
        self.display_text.clear();
    }
    
    /// Set visibility
    pub fn set_visible(&mut self, visible: bool) {
        self.is_visible = visible;
    }
    
    /// Get display text (empty if not visible)
    pub fn get_display_text(&self) -> &str {
        if self.is_visible {
            &self.display_text
        } else {
            ""
        }
    }
    
    /// Check if display has content
    pub fn has_content(&self) -> bool {
        !self.display_text.is_empty()
    }
    
    /// Get text length
    pub fn text_length(&self) -> usize {
        self.display_text.len()
    }
    
    /// Truncate text if it exceeds max length
    fn truncate_if_needed(&mut self) {
        if self.display_text.len() > self.max_length {
            self.display_text.truncate(self.max_length - 3);
            self.display_text.push_str("...");
        }
    }
    
    /// Format text with line breaks for display
    pub fn format_for_display(&self, line_width: usize) -> Vec<String> {
        if !self.is_visible {
            return vec![];
        }
        
        let mut lines = Vec::new();
        let mut current_line = String::new();
        
        for word in self.display_text.split_whitespace() {
            if current_line.len() + word.len() + 1 > line_width && !current_line.is_empty() {
                lines.push(current_line);
                current_line = String::new();
            }
            
            if !current_line.is_empty() {
                current_line.push(' ');
            }
            current_line.push_str(word);
        }
        
        if !current_line.is_empty() {
            lines.push(current_line);
        }
        
        lines
    }
}

impl Default for DisplayComponent {
    fn default() -> Self {
        Self::new(String::new())
    }
}