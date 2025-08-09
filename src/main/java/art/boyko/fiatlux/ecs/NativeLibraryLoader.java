package art.boyko.fiatlux.ecs;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class to load native libraries from resources
 */
public class NativeLibraryLoader {
    
    private static boolean libraryLoaded = false;
    private static boolean loadAttempted = false;
    private static String loadError = null;
    
    /**
     * Load the fiatlux_ecs native library
     * @return true if successfully loaded, false otherwise
     */
    public static synchronized boolean loadNativeLibrary() {
        if (loadAttempted) {
            return libraryLoaded;
        }
        
        loadAttempted = true;
        
        try {
            String libraryName = getNativeLibraryName();
            String resourcePath = "/natives/" + libraryName;
            
            // Check if resource exists
            InputStream libraryStream = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
            if (libraryStream == null) {
                loadError = "Native library resource not found: " + resourcePath;
                System.err.println("⚠️ " + loadError);
                return false;
            }
            
            // Create temporary file
            Path tempDir = Files.createTempDirectory("fiatlux-natives");
            Path tempLibrary = tempDir.resolve(libraryName);
            
            // Copy library from resources to temporary file
            Files.copy(libraryStream, tempLibrary, StandardCopyOption.REPLACE_EXISTING);
            libraryStream.close();
            
            // Load the library
            System.load(tempLibrary.toAbsolutePath().toString());
            
            libraryLoaded = true;
            System.out.println("🦀 Successfully loaded Rust native library: " + libraryName);
            
            // Schedule cleanup on JVM shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempLibrary);
                    Files.deleteIfExists(tempDir);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }));
            
            return true;
            
        } catch (Exception e) {
            loadError = "Failed to load native library: " + e.getMessage();
            System.err.println("⚠️ " + loadError);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get the expected native library file name for the current platform
     */
    private static String getNativeLibraryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return "fiatlux_ecs.dll";
        } else if (os.contains("mac")) {
            return "libfiatlux_ecs.dylib";
        } else {
            return "libfiatlux_ecs.so";
        }
    }
    
    /**
     * Check if the native library is loaded
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }
    
    /**
     * Get the error message if library loading failed
     */
    public static String getLoadError() {
        return loadError;
    }
    
    /**
     * Reset the loader state (for testing purposes)
     */
    public static synchronized void reset() {
        libraryLoaded = false;
        loadAttempted = false;
        loadError = null;
    }
}