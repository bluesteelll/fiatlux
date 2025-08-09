package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for all energy-related modules
 * Реестр всех энергетических модулей
 */
public class EnergyModuleRegistry {
    
    private static final Map<ResourceLocation, EnergyModuleInfo> ENERGY_MODULES = new HashMap<>();
    
    public static void registerEnergyModules() {
        // Register basic energy modules
        register(BasicEnergyGeneratorModule.MODULE_ID, 
                BasicEnergyGeneratorModule::new,
                EnergyModuleType.GENERATOR,
                "Basic Energy Generator",
                "Generates energy over time");
        
        register(BasicEnergyStorageModule.MODULE_ID,
                BasicEnergyStorageModule::new,
                EnergyModuleType.STORAGE,
                "Basic Energy Storage", 
                "Stores and transfers energy");
        
        register(BasicEnergyConsumerModule.MODULE_ID,
                BasicEnergyConsumerModule::new,
                EnergyModuleType.CONSUMER,
                "Basic Energy Consumer",
                "Consumes energy to perform work");
        
        // Register advanced energy modules
        register(AdvancedEnergyGeneratorModule.MODULE_ID,
                AdvancedEnergyGeneratorModule::new,
                EnergyModuleType.GENERATOR,
                "Advanced Energy Generator",
                "High-capacity generator with overclocking");
        
        register(EnergyProcessorModule.MODULE_ID,
                EnergyProcessorModule::new,
                EnergyModuleType.PROCESSOR,
                "Energy Processor",
                "Processes items using energy");
        
        // Register with main module registry
        ENERGY_MODULES.forEach((id, info) -> {
            @SuppressWarnings("unchecked")
            Class<AbstractEnergyModule> moduleClass = (Class<AbstractEnergyModule>) info.moduleClass;
            ModuleRegistry.register(id, moduleClass, info.factory);
        });
    }
    
    private static void register(ResourceLocation id, Supplier<AbstractEnergyModule> factory, 
                               EnergyModuleType type, String displayName, String description) {
        ENERGY_MODULES.put(id, new EnergyModuleInfo(
                id, factory, type, displayName, description, factory.get().getClass()
        ));
    }
    
    /**
     * Get energy module info by ID
     */
    public static EnergyModuleInfo getEnergyModuleInfo(ResourceLocation moduleId) {
        return ENERGY_MODULES.get(moduleId);
    }
    
    /**
     * Get all registered energy modules
     */
    public static Map<ResourceLocation, EnergyModuleInfo> getAllEnergyModules() {
        return new HashMap<>(ENERGY_MODULES);
    }
    
    /**
     * Get modules by energy type
     */
    public static Map<ResourceLocation, EnergyModuleInfo> getModulesByType(EnergyModuleType type) {
        Map<ResourceLocation, EnergyModuleInfo> result = new HashMap<>();
        ENERGY_MODULES.forEach((id, info) -> {
            if (info.type == type) {
                result.put(id, info);
            }
        });
        return result;
    }
    
    /**
     * Check if module is an energy module
     */
    public static boolean isEnergyModule(ResourceLocation moduleId) {
        return ENERGY_MODULES.containsKey(moduleId);
    }
    
    public enum EnergyModuleType {
        GENERATOR("Generator", "Produces energy"),
        STORAGE("Storage", "Stores and transfers energy"),
        CONSUMER("Consumer", "Consumes energy"),
        PROCESSOR("Processor", "Processes items using energy");
        
        private final String displayName;
        private final String description;
        
        EnergyModuleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Component getDisplayComponent() {
            return Component.literal(displayName);
        }
    }
    
    public static class EnergyModuleInfo {
        public final ResourceLocation id;
        public final Supplier<AbstractEnergyModule> factory;
        public final EnergyModuleType type;
        public final String displayName;
        public final String description;
        public final Class<?> moduleClass;
        
        public EnergyModuleInfo(ResourceLocation id, Supplier<AbstractEnergyModule> factory, 
                              EnergyModuleType type, String displayName, String description,
                              Class<?> moduleClass) {
            this.id = id;
            this.factory = factory;
            this.type = type;
            this.displayName = displayName;
            this.description = description;
            this.moduleClass = moduleClass;
        }
        
        public AbstractEnergyModule create() {
            return factory.get();
        }
        
        public Component getDisplayNameComponent() {
            return Component.literal(displayName);
        }
        
        public Component getDescriptionComponent() {
            return Component.literal(description);
        }
    }
}