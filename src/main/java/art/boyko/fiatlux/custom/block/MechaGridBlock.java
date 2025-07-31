package art.boyko.fiatlux.custom.block;

import art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MechaGridBlock extends BaseEntityBlock {
    public static final MapCodec<MechaGridBlock> CODEC = simpleCodec(MechaGridBlock::new);
    
    // Full block shape but transparent
    private static final VoxelShape SHAPE = Shapes.block();

    public MechaGridBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechaGridBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return handleInteraction(state, level, pos, player, hitResult, ItemStack.EMPTY);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = handleInteraction(state, level, pos, player, hitResult, stack);
        return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult handleInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, ItemStack heldItem) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
            return InteractionResult.FAIL;
        }

        // Calculate grid position based on hit location
        Direction face = hitResult.getDirection();
        double relativeX = hitResult.getLocation().x - pos.getX();
        double relativeY = hitResult.getLocation().y - pos.getY();
        double relativeZ = hitResult.getLocation().z - pos.getZ();

        // Clamp values to prevent out of bounds
        relativeX = Math.max(0.0, Math.min(1.0, relativeX));
        relativeY = Math.max(0.0, Math.min(1.0, relativeY));
        relativeZ = Math.max(0.0, Math.min(1.0, relativeZ));

        // Convert to grid coordinates (0-3 for each axis)
        int gridX = Math.max(0, Math.min(3, (int) (relativeX * 4)));
        int gridY = Math.max(0, Math.min(3, (int) (relativeY * 4)));
        int gridZ = Math.max(0, Math.min(3, (int) (relativeZ * 4)));

        // Handle sneaking (remove block) vs normal (place block)
        if (player.isShiftKeyDown()) {
            // Remove block from grid
            BlockState removedBlock = mechaGrid.removeBlock(gridX, gridY, gridZ);
            if (removedBlock != null && !removedBlock.isAir()) {
                // Drop the removed block
                Block.popResource(level, pos, new ItemStack(removedBlock.getBlock()));
                player.sendSystemMessage(Component.literal("Removed block at [" + gridX + "," + gridY + "," + gridZ + "]"));
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("No block at position [" + gridX + "," + gridY + "," + gridZ + "]"));
                return InteractionResult.SUCCESS;
            }
        } else {
            // Place block in grid or show status
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem blockItem) {
                BlockState blockToPlace = blockItem.getBlock().defaultBlockState();
                
                if (mechaGrid.placeBlock(gridX, gridY, gridZ, blockToPlace)) {
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("Placed " + blockToPlace.getBlock().getName().getString() + " at [" + gridX + "," + gridY + "," + gridZ + "]"));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(Component.literal("Position [" + gridX + "," + gridY + "," + gridZ + "] is already occupied"));
                    return InteractionResult.SUCCESS;
                }
            } else {
                // Show grid status when empty hand or non-block item
                int totalBlocks = mechaGrid.getTotalBlocks();
                player.sendSystemMessage(Component.literal("MechaGrid: " + totalBlocks + "/64 blocks placed"));
                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // Custom rendering
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f; // Full brightness for transparency
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // Allow light to pass through
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MechaGridBlockEntity mechaGrid) {
                // Drop all blocks stored in the grid
                mechaGrid.dropAllBlocks(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.MECHA_GRID_BE.get(),
                (level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1));
    }
}