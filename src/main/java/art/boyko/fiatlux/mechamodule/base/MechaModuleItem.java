package art.boyko.fiatlux.mechamodule.base;

import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for items that can be placed as MechaModules in MechaGrid
 */
public class MechaModuleItem extends Item {
    
    private final ResourceLocation moduleId;
    
    public MechaModuleItem(ResourceLocation moduleId, Properties properties) {
        super(properties);
        this.moduleId = moduleId;
    }
    
    /**
     * Get the module ID that this item creates
     */
    public ResourceLocation getModuleId() {
        return moduleId;
    }
    
    /**
     * Create a MechaModule instance from this item
     */
    @Nullable
    public IMechaModule createModule() {
        return ModuleRegistry.createModule(moduleId);
    }
    
    /**
     * Check if this item can be placed as a module
     */
    public boolean canPlaceAsModule() {
        return ModuleRegistry.isRegistered(moduleId);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Add module-specific tooltip
        IMechaModule module = createModule();
        if (module != null) {
            List<Component> moduleTooltip = module.getTooltip();
            tooltipComponents.addAll(moduleTooltip);
        }
        
        tooltipComponents.add(Component.translatable("tooltip.fiatlux.mecha_module"));
    }
    
    /**
     * Check if this item is a MechaModule item
     */
    public static boolean isMechaModuleItem(ItemStack stack) {
        boolean result = stack.getItem() instanceof MechaModuleItem;
        System.out.println("🔍 isMechaModuleItem() check: " + stack.getItem().getDescriptionId() + 
                          " -> " + result + " (class: " + stack.getItem().getClass().getSimpleName() + ")");
        return result;
    }
    
    /**
     * Create a MechaModule from an ItemStack
     */
    @Nullable
    public static IMechaModule createModuleFromStack(ItemStack stack) {
        if (!isMechaModuleItem(stack)) {
            System.out.println("❌ createModuleFromStack() FAILED: not a MechaModuleItem");
            return null;
        }
        
        MechaModuleItem moduleItem = (MechaModuleItem) stack.getItem();
        IMechaModule module = moduleItem.createModule();
        System.out.println("✅ createModuleFromStack() SUCCESS: created " + (module != null ? module.getModuleId() : "null"));
        return module;
    }
}