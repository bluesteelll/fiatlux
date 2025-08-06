package art.boyko.fiatlux.mechamodule.base;

import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Main interface for all MechaModules.
 * MechaModules are special blocks that exist only inside MechaGrid.
 * They can interact with each other through capabilities and contexts.
 */
public interface IMechaModule {
    
    /**
     * Get the unique identifier for this module type
     */
    ResourceLocation getModuleId();
    
    /**
     * Get the display name of this module
     */
    Component getDisplayName();
    
    /**
     * Get the module properties
     */
    ModuleProperties getProperties();
    
    /**
     * Get the block state representation for rendering
     */
    BlockState getRenderState();
    
    /**
     * Called when module is placed in MechaGrid
     * @param context The module context providing position and neighbor info
     */
    void onPlacedInGrid(IModuleContext context);
    
    /**
     * Called when module is removed from MechaGrid
     * @param context The module context
     */
    void onRemovedFromGrid(IModuleContext context);
    
    /**
     * Called when a neighbor module changes (placed/removed/updated)
     * @param context The module context
     * @param direction Direction of the neighbor that changed
     * @param neighbor The new neighbor module (null if removed)
     */
    void onNeighborChanged(IModuleContext context, Direction direction, @Nullable IMechaModule neighbor);
    
    /**
     * Called every tick if the module is active
     * @param context The module context
     */
    void tick(IModuleContext context);
    
    /**
     * Check if this module needs to tick
     */
    boolean needsTicking();
    
    /**
     * Get capability for the specified direction
     * @param direction Direction to check (null for internal capabilities)
     * @param capabilityType Type of capability requested
     * @return Capability instance or empty if not supported
     */
    <T extends IModuleCapability> Optional<T> getCapability(Direction direction, Class<T> capabilityType);
    
    /**
     * Get all capabilities this module provides
     */
    List<Class<? extends IModuleCapability>> getProvidedCapabilities();
    
    /**
     * Save module data to NBT
     */
    CompoundTag saveToNBT();
    
    /**
     * Load module data from NBT
     */
    void loadFromNBT(CompoundTag nbt);
    
    /**
     * Convert module back to ItemStack when removed from grid
     */
    ItemStack toItemStack();
    
    /**
     * Create a copy of this module for placement
     */
    IMechaModule copy();
    
    /**
     * Get tooltip information for UI display
     */
    List<Component> getTooltip();
    
    /**
     * Check if this module can connect to another module in the specified direction
     * @param direction Direction to check
     * @param neighbor Neighboring module
     * @return true if connection is possible
     */
    boolean canConnectTo(Direction direction, IMechaModule neighbor);
    
    /**
     * Check if this module has a GUI that can be opened
     * @return true if module has GUI
     */
    default boolean hasGui() {
        return false;
    }
    
    /**
     * Called when player right-clicks on this module to open GUI
     * This method should open the appropriate GUI screen
     * @param context The module context
     * @param player The player opening the GUI
     */
    default void openGui(IModuleContext context, Player player) {
        // Default implementation does nothing
    }
}