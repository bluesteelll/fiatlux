package art.boyko.fiatlux.mechamodule.registry;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for all MechaModule types.
 * Handles registration, creation, and serialization of modules.
 */
public class ModuleRegistry {
    
    private static final Map<ResourceLocation, ModuleFactory> MODULE_FACTORIES = new HashMap<>();
    private static final Map<Class<? extends IMechaModule>, ResourceLocation> MODULE_TYPES = new HashMap<>();
    
    /**
     * Register a new module type
     * @param moduleId Unique identifier for the module type
     * @param moduleClass Class of the module
     * @param factory Factory function to create new instances
     */
    public static <T extends IMechaModule> void register(ResourceLocation moduleId, Class<T> moduleClass, Supplier<T> factory) {
        if (MODULE_FACTORIES.containsKey(moduleId)) {
            throw new IllegalArgumentException("Module type already registered: " + moduleId);
        }
        
        ModuleFactory moduleFactory = new ModuleFactory(moduleClass, factory);
        MODULE_FACTORIES.put(moduleId, moduleFactory);
        MODULE_TYPES.put(moduleClass, moduleId);
    }
    
    /**
     * Create a new module instance by ID
     * @param moduleId Module type ID
     * @return New module instance, or null if type is not registered
     */
    @Nullable
    public static IMechaModule createModule(ResourceLocation moduleId) {
        ModuleFactory factory = MODULE_FACTORIES.get(moduleId);
        if (factory == null) {
            return null;
        }
        
        return factory.create();
    }
    
    /**
     * Create a module from NBT data
     * @param nbt NBT compound containing module data
     * @return Loaded module instance, or null if unable to load
     */
    @Nullable
    public static IMechaModule createModuleFromNBT(CompoundTag nbt) {
        if (!nbt.contains("ModuleId")) {
            return null;
        }
        
        ResourceLocation moduleId = ResourceLocation.parse(nbt.getString("ModuleId"));
        IMechaModule module = createModule(moduleId);
        
        if (module != null) {
            module.loadFromNBT(nbt);
        }
        
        return module;
    }
    
    /**
     * Create a module from an ItemStack
     * @param itemStack ItemStack containing module data
     * @return Module instance, or null if ItemStack doesn't contain a valid module
     */
    @Nullable
    public static IMechaModule createModuleFromItemStack(ItemStack itemStack) {
        // Check if ItemStack has module data component
        // This would need to be integrated with the mod's data component system
        
        // For now, try to determine module type from item ID
        ResourceLocation itemId = itemStack.getItem().builtInRegistryHolder().key().location();
        
        // Convert item ID to module ID (assuming same namespace and path)
        ResourceLocation moduleId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), itemId.getPath());
        
        return createModule(moduleId);
    }
    
    /**
     * Get the module ID for a given module class
     * @param moduleClass Module class
     * @return Module ID, or null if class is not registered
     */
    @Nullable
    public static ResourceLocation getModuleId(Class<? extends IMechaModule> moduleClass) {
        return MODULE_TYPES.get(moduleClass);
    }
    
    /**
     * Get the module class for a given module ID
     * @param moduleId Module ID
     * @return Module class, or null if ID is not registered
     */
    @Nullable
    public static Class<? extends IMechaModule> getModuleClass(ResourceLocation moduleId) {
        ModuleFactory factory = MODULE_FACTORIES.get(moduleId);
        return factory != null ? factory.getModuleClass() : null;
    }
    
    /**
     * Check if a module type is registered
     * @param moduleId Module ID to check
     * @return true if registered
     */
    public static boolean isRegistered(ResourceLocation moduleId) {
        return MODULE_FACTORIES.containsKey(moduleId);
    }
    
    /**
     * Get all registered module IDs
     * @return Collection of all registered module IDs
     */
    public static Collection<ResourceLocation> getRegisteredModuleIds() {
        return MODULE_FACTORIES.keySet();
    }
    
    /**
     * Get all registered module classes
     * @return Collection of all registered module classes
     */
    public static Collection<Class<? extends IMechaModule>> getRegisteredModuleClasses() {
        return MODULE_TYPES.keySet();
    }
    
    /**
     * Clear all registrations (mainly for testing)
     */
    public static void clear() {
        MODULE_FACTORIES.clear();
        MODULE_TYPES.clear();
    }
    
    /**
     * Get the number of registered modules
     */
    public static int getRegisteredCount() {
        return MODULE_FACTORIES.size();
    }
    
    /**
     * Validate module registration consistency
     * @return true if all registrations are consistent
     */
    public static boolean validateRegistrations() {
        // Check that all factory classes match the reverse mapping
        for (Map.Entry<ResourceLocation, ModuleFactory> entry : MODULE_FACTORIES.entrySet()) {
            ResourceLocation moduleId = entry.getKey();
            Class<? extends IMechaModule> moduleClass = entry.getValue().getModuleClass();
            
            ResourceLocation mappedId = MODULE_TYPES.get(moduleClass);
            if (!moduleId.equals(mappedId)) {
                return false;
            }
        }
        
        // Check that all reverse mappings have corresponding factories
        for (Map.Entry<Class<? extends IMechaModule>, ResourceLocation> entry : MODULE_TYPES.entrySet()) {
            Class<? extends IMechaModule> moduleClass = entry.getKey();
            ResourceLocation moduleId = entry.getValue();
            
            ModuleFactory factory = MODULE_FACTORIES.get(moduleId);
            if (factory == null || !factory.getModuleClass().equals(moduleClass)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Factory wrapper for module creation
     */
    private static class ModuleFactory {
        private final Class<? extends IMechaModule> moduleClass;
        private final Supplier<? extends IMechaModule> factory;
        
        public ModuleFactory(Class<? extends IMechaModule> moduleClass, Supplier<? extends IMechaModule> factory) {
            this.moduleClass = moduleClass;
            this.factory = factory;
        }
        
        public IMechaModule create() {
            return factory.get();
        }
        
        public Class<? extends IMechaModule> getModuleClass() {
            return moduleClass;
        }
    }
}