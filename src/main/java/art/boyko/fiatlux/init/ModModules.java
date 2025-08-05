package art.boyko.fiatlux.init;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import art.boyko.fiatlux.mechamodule.test.TestModule;
import art.boyko.fiatlux.mechamodule.test.TestModuleItem;
import art.boyko.fiatlux.mechamodule.modules.EnergyGeneratorModule;
import art.boyko.fiatlux.mechamodule.modules.EnergyStorageModule;
import art.boyko.fiatlux.mechamodule.modules.ProcessorModule;
import art.boyko.fiatlux.mechamodule.modules.DisplayModule;
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
        // Register TestModule
        ModuleRegistry.register(TestModule.MODULE_ID, TestModule.class, TestModule::new);
        
        // Register new modules
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