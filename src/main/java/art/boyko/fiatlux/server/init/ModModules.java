package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import art.boyko.fiatlux.server.mechamodule.energy.EnergyModuleRegistry;
// import art.boyko.fiatlux.common.module.test.TestModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration for MechaModules and their corresponding items
 */
public class ModModules {

    /**
     * Register all module types and their factories
     */
    public static void registerModuleTypes() {
        // Register TestModule (commented out - module doesn't exist yet)
        // ModuleRegistry.register(TestModule.MODULE_ID, TestModule.class, TestModule::new);
        
        // Register energy modules - all logic is handled by Rust ECS!
        EnergyModuleRegistry.registerEnergyModules();
        
        FiatLux.LOGGER.info("Registered {} MechaModule types", ModuleRegistry.getRegisteredCount());
        FiatLux.LOGGER.info("Energy modules registered: {}", EnergyModuleRegistry.getAllEnergyModules().size());
    }

    /**
     * Initialize the module system
     * This method should be called in the mod constructor after item registration
     */
    public static void initialize(IEventBus eventBus) {
        registerModuleTypes();
        
        // Validate registration consistency
        if (!ModuleRegistry.validateRegistrations()) {
            throw new RuntimeException("MechaModule registration validation failed!");
        }
        
        FiatLux.LOGGER.info("MechaModule system initialized successfully");
    }
}