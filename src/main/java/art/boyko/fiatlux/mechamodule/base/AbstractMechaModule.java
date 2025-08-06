package art.boyko.fiatlux.mechamodule.base;

import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base implementation of IMechaModule providing common functionality
 */
public abstract class AbstractMechaModule implements IMechaModule {
    
    protected final ResourceLocation moduleId;
    protected final ModuleProperties properties;
    protected final Map<Class<? extends IModuleCapability>, IModuleCapability> capabilities;
    
    // Runtime data
    protected IModuleContext context;
    protected boolean isActive = false;
    
    protected AbstractMechaModule(ResourceLocation moduleId, ModuleProperties properties) {
        this.moduleId = moduleId;
        this.properties = properties;
        this.capabilities = new HashMap<>();
        initializeCapabilities();
    }
    
    /**
     * Initialize capabilities for this module.
     * Override in subclasses to add specific capabilities.
     */
    protected abstract void initializeCapabilities();
    
    @Override
    public ResourceLocation getModuleId() {
        return moduleId;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("module." + moduleId.getNamespace() + "." + moduleId.getPath());
    }
    
    @Override
    public ModuleProperties getProperties() {
        return properties;
    }
    
    @Override
    public BlockState getRenderState() {
        // Default implementation returns stone - override in subclasses
        return Blocks.STONE.defaultBlockState();
    }
    
    @Override
    public void onPlacedInGrid(IModuleContext context) {
        this.context = context;
        this.isActive = true;
        onActivated();
    }
    
    @Override
    public void onRemovedFromGrid(IModuleContext context) {
        onDeactivated();
        this.isActive = false;
        this.context = null;
    }
    
    @Override
    public void onNeighborChanged(IModuleContext context, Direction direction, @Nullable IMechaModule neighbor) {
        // Default implementation does nothing - override in subclasses if needed
    }
    
    @Override
    public void tick(IModuleContext context) {
        if (!isActive || !properties.needsTicking()) {
            return;
        }
        onTick(context);
    }
    
    @Override
    public boolean needsTicking() {
        return properties.needsTicking() && isActive;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IModuleCapability> Optional<T> getCapability(Direction direction, Class<T> capabilityType) {
        IModuleCapability capability = capabilities.get(capabilityType);
        if (capability != null && capabilityType.isInstance(capability)) {
            return Optional.of((T) capability);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Class<? extends IModuleCapability>> getProvidedCapabilities() {
        return new ArrayList<>(capabilities.keySet());
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("ModuleId", moduleId.toString());
        nbt.putBoolean("IsActive", isActive);
        
        // Save custom data
        CompoundTag customData = new CompoundTag();
        saveCustomData(customData);
        if (!customData.isEmpty()) {
            nbt.put("CustomData", customData);
        }
        
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        isActive = nbt.getBoolean("IsActive");
        
        // Load custom data
        if (nbt.contains("CustomData")) {
            loadCustomData(nbt.getCompound("CustomData"));
        }
    }
    
    @Override
    public List<Component> getTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(getDisplayName());
        
        // Add energy info if relevant
        if (properties.getEnergyConsumption() > 0) {
            tooltip.add(Component.translatable("tooltip.fiatlux.energy_consumption", properties.getEnergyConsumption()));
        }
        if (properties.getEnergyProduction() > 0) {
            tooltip.add(Component.translatable("tooltip.fiatlux.energy_production", properties.getEnergyProduction()));
        }
        
        return tooltip;
    }
    
    @Override
    public boolean canConnectTo(Direction direction, IMechaModule neighbor) {
        // Default implementation checks if both modules have compatible capabilities
        for (Class<? extends IModuleCapability> capType : getProvidedCapabilities()) {
            if (neighbor.getCapability(direction.getOpposite(), capType).isPresent()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public IMechaModule copy() {
        try {
            // Create new instance using no-arg constructor
            AbstractMechaModule copy = this.getClass().getDeclaredConstructor().newInstance();
            
            // Copy NBT data
            CompoundTag nbt = saveToNBT();
            copy.loadFromNBT(nbt);
            
            return copy;
        } catch (Exception e) {
            // Fallback: try to create via ModuleRegistry
            IMechaModule registryModule = art.boyko.fiatlux.mechamodule.registry.ModuleRegistry.createModule(moduleId);
            if (registryModule != null) {
                CompoundTag nbt = saveToNBT();
                registryModule.loadFromNBT(nbt);
                return registryModule;
            }
            throw new RuntimeException("Failed to copy module: " + moduleId, e);
        }
    }
    
    // Protected methods for subclasses
    
    /**
     * Add a capability to this module
     */
    protected <T extends IModuleCapability> void addCapability(Class<T> capabilityType, T capability) {
        capabilities.put(capabilityType, capability);
    }
    
    /**
     * Called when module is activated (placed in grid)
     */
    protected void onActivated() {
        // Override in subclasses if needed
    }
    
    /**
     * Called when module is deactivated (removed from grid)
     */
    protected void onDeactivated() {
        // Override in subclasses if needed
    }
    
    /**
     * Called every tick if needsTicking() returns true
     */
    protected void onTick(IModuleContext context) {
        // Override in subclasses if needed
    }
    
    /**
     * Save custom module data to NBT
     */
    protected void saveCustomData(CompoundTag nbt) {
        // Override in subclasses if needed
    }
    
    /**
     * Load custom module data from NBT
     */
    protected void loadCustomData(CompoundTag nbt) {
        // Override in subclasses if needed
    }
    
    /**
     * Get current context (may be null if not placed)
     */
    protected IModuleContext getContext() {
        return context;
    }
    
    /**
     * Check if module is currently active (placed in grid)
     */
    protected boolean isActive() {
        return isActive;
    }
    
    @Override
    public boolean hasGui() {
        return true; // Default implementation: most modules have basic GUI
    }
    
    @Override
    public void openGui(IModuleContext context, Player player) {
        if (context == null || !isActive) {
            return;
        }
        
        // Open basic module GUI
        openModuleGui(context, player);
    }
    
    protected void openModuleGui(IModuleContext context, Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // Get grid position from context
        net.minecraft.core.BlockPos worldPos = context.getWorldPosition();
        IModuleContext.GridPosition gridPos = context.getGridPosition();
        
        // Calculate module coordinates within the grid
        int moduleX = gridPos.x();
        int moduleY = gridPos.y();
        int moduleZ = gridPos.z();
        
        // Open GUI through network
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            art.boyko.fiatlux.network.NetworkHandler.openModuleGui(serverPlayer, worldPos, moduleX, moduleY, moduleZ);
        }
    }
}