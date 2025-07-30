package art.boyko.fiatlux.custom.block;

import art.boyko.fiatlux.custom.blockentity.EnergyStorageBlockEntity;
import art.boyko.fiatlux.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnergyStorageBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyStorageBlock> CODEC = simpleCodec(EnergyStorageBlock::new);

    public EnergyStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyStorageBlockEntity energyEntity) {
                int energy = energyEntity.getEnergyStored();
                int maxEnergy = energyEntity.getMaxEnergyStored();
                
                player.sendSystemMessage(Component.literal("Energy: " + energy + "/" + maxEnergy + " FE"));
                
                // Simple interaction: right-click to charge/discharge
                if (player.isShiftKeyDown()) {
                    energyEntity.extractEnergy(1000, false);
                    player.sendSystemMessage(Component.literal("Extracted 1000 FE"));
                } else {
                    energyEntity.receiveEnergy(1000, false);
                    player.sendSystemMessage(Component.literal("Added 1000 FE"));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(blockEntityType, ModBlockEntities.ENERGY_STORAGE_BE.get(),
                (level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1));
    }
}