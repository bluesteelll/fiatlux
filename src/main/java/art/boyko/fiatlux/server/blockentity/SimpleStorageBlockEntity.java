package art.boyko.fiatlux.server.blockentity;

import art.boyko.fiatlux.server.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimpleStorageBlockEntity extends BlockEntity {
    private int storedItems = 0;
    private int tickCounter = 0;
    private static final int MAX_STORED_ITEMS = 64;
    
    public SimpleStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SIMPLE_STORAGE_BE.get(), pos, blockState);
    }

    // Server-side tick method
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        tickCounter++;
        
        // Example: do something every 100 ticks (5 seconds)
        if (tickCounter % 100 == 0) {
            // You can add periodic logic here
            setChanged(); // Mark this block entity as dirty so it gets saved
        }
    }

    public int getStoredItems() {
        return storedItems;
    }

    public void addItem() {
        if (storedItems < MAX_STORED_ITEMS) {
            storedItems++;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void removeItem() {
        if (storedItems > 0) {
            storedItems--;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public boolean isFull() {
        return storedItems >= MAX_STORED_ITEMS;
    }

    public boolean isEmpty() {
        return storedItems <= 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredItems", storedItems);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedItems = tag.getInt("StoredItems");
        tickCounter = tag.getInt("TickCounter");
    }

    // Sync data to client
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}