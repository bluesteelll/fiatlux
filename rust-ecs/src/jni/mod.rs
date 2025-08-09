use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jint, jboolean};

pub mod world_management;
pub mod entity_operations;
pub mod energy_operations;
pub mod batch_operations;

pub use world_management::*;
pub use entity_operations::*;
pub use energy_operations::*;
pub use batch_operations::*;

/// Helper function to convert Java string to Rust string
pub fn jstring_to_string(env: &mut JNIEnv, jstring: JString) -> Result<String, Box<dyn std::error::Error>> {
    Ok(env.get_string(&jstring)?.into())
}

/// Helper function to handle JNI errors
pub fn handle_jni_error<T>(result: Result<T, Box<dyn std::error::Error>>, default: T) -> T {
    match result {
        Ok(value) => value,
        Err(e) => {
            eprintln!("JNI Error: {}", e);
            default
        }
    }
}