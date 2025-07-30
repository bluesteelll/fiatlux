package art.boyko.fiatlux.custom.blockentity;

import art.boyko.fiatlux.init.ModBlockEntities;
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

public class EnergyStorageBlockEntity extends BlockEntity {
    private int energyStored = 0;
    private int tickCounter = 0;
    private static final int MAX_ENERGY = 100000; // 100k FE
    private static final int MAX_RECEIVE = 1000;  // 1k FE/tick
    private static final int MAX_EXTRACT = 1000;  // 1k FE/tick
    
    public EnergyStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ENERGY_STORAGE_BE.get(), pos, blockState);
    }

    // Server-side tick method
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        tickCounter++;
        
        // Example: slowly generate energy when empty
        if (tickCounter % 20 == 0 && energyStored < MAX_ENERGY / 4) { // Every second when less than 25% full
            receiveEnergy(10, false);
            setChanged();
        }
        
        // Example: slowly lose energy when full
        if (tickCounter % 40 == 0 && energyStored > MAX_ENERGY * 3 / 4) { // Every 2 seconds when more than 75% full
            extractEnergy(5, false);
            setChanged();
        }
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, maxReceive));
        
        if (!simulate) {
            energyStored += energyReceived;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        
        return energyReceived;
    }

    public int extractEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(energyStored, Math.min(MAX_EXTRACT, maxExtract));
        
        if (!simulate) {
            energyStored -= energyExtracted;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        
        return energyExtracted;
    }

    public boolean canExtract() {
        return energyStored > 0;
    }

    public boolean canReceive() {
        return energyStored < MAX_ENERGY;
    }

    public float getEnergyPercentage() {
        return (float) energyStored / MAX_ENERGY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("EnergyStored", energyStored);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStored = tag.getInt("EnergyStored");
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