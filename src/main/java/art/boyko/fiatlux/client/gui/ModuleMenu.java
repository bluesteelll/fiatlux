package art.boyko.fiatlux.client.gui;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.server.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModuleMenu extends AbstractContainerMenu {
    private final MechaGridBlockEntity mechaGrid;
    private final IMechaModule module;
    private final int moduleX, moduleY, moduleZ;
    private final Level level;
    private final BlockPos blockPos;

    public ModuleMenu(int containerId, Inventory playerInventory, MechaGridBlockEntity mechaGrid, 
                      IMechaModule module, int moduleX, int moduleY, int moduleZ) {
        super(ModMenuTypes.MODULE_MENU.get(), containerId);
        this.mechaGrid = mechaGrid;
        this.module = module;
        this.moduleX = moduleX;
        this.moduleY = moduleY;
        this.moduleZ = moduleZ;
        this.level = playerInventory.player.level();
        this.blockPos = mechaGrid.getBlockPos();

        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Player hotbar
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof MechaGridBlockEntity grid)) {
            return false;
        }

        IMechaModule currentModule = grid.getModule(moduleX, moduleY, moduleZ);
        return currentModule != null && currentModule.equals(module) && 
               player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }
    
    // Update method called periodically to ensure GUI shows fresh data
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Force update of module data for client synchronization
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof MechaGridBlockEntity grid) {
            // Trigger a block entity data sync
            grid.setChanged();
        }
    }

    public IMechaModule getModule() {
        return module;
    }

    public MechaGridBlockEntity getMechaGrid() {
        return mechaGrid;
    }

    public int getModuleX() {
        return moduleX;
    }

    public int getModuleY() {
        return moduleY;
    }

    public int getModuleZ() {
        return moduleZ;
    }
}