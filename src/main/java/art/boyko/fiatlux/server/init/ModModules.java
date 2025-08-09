package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import art.boyko.fiatlux.server.modules.test.TestModule;
import art.boyko.fiatlux.server.modules.energy.EnergyGeneratorModule;
import art.boyko.fiatlux.server.modules.energy.EnergyStorageModule;
import art.boyko.fiatlux.server.modules.processing.ProcessorModule;
import art.boyko.fiatlux.server.modules.display.DisplayModule;
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
        // Register all module types
        ModuleRegistry.register(TestModule.MODULE_ID, TestModule.class, TestModule::new);
        ModuleRegistry.register(EnergyGeneratorModule.MODULE_ID, EnergyGeneratorModule.class, EnergyGeneratorModule::new);
        ModuleRegistry.register(EnergyStorageModule.MODULE_ID, EnergyStorageModule.class, EnergyStorageModule::new);
        ModuleRegistry.register(ProcessorModule.MODULE_ID, ProcessorModule.class, ProcessorModule::new);
        ModuleRegistry.register(DisplayModule.MODULE_ID, DisplayModule.class, DisplayModule::new);
        
        FiatLux.LOGGER.info("Registered {} MechaModule types", ModuleRegistry.getRegisteredCount());
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