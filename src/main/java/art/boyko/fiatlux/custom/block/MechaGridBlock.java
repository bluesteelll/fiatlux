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

        // Handle sneaking (remove block) vs normal (place block)
        if (player.isShiftKeyDown()) {
            // For removal, use the exact hit position like before
            GridPos targetPos = getHitGridPosition(hitResult, pos);
            
            BlockState removedBlock = mechaGrid.removeBlock(targetPos.x, targetPos.y, targetPos.z);
            if (removedBlock != null && !removedBlock.isAir()) {
                Block.popResource(level, pos, new ItemStack(removedBlock.getBlock()));
                player.sendSystemMessage(Component.literal("Removed block at [" + targetPos.x + "," + targetPos.y + "," + targetPos.z + "]"));
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("No block at position [" + targetPos.x + "," + targetPos.y + "," + targetPos.z + "]"));
                return InteractionResult.SUCCESS;
            }
        } else {
            // Place block using ray tracing
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem blockItem) {
                GridPos targetPos = findPlacementPosition(player, pos, hitResult, mechaGrid);
                
                if (targetPos != null) {
                    BlockState blockToPlace = blockItem.getBlock().defaultBlockState();
                    
                    if (mechaGrid.placeBlock(targetPos.x, targetPos.y, targetPos.z, blockToPlace)) {
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }
                        player.sendSystemMessage(Component.literal("Placed " + blockToPlace.getBlock().getName().getString() + " at [" + targetPos.x + "," + targetPos.y + "," + targetPos.z + "]"));
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    player.sendSystemMessage(Component.literal("No valid placement position found"));
                    return InteractionResult.SUCCESS;
                }
            } else {
                // Show grid status when empty hand or non-block item
                int totalBlocks = mechaGrid.getTotalBlocks();
                player.sendSystemMessage(Component.literal("MechaGrid: " + totalBlocks + "/64 blocks placed"));
                return InteractionResult.SUCCESS;
            }
        }
        
        // Default return (should never reach here, but compiler needs it)
        return InteractionResult.PASS;
    }

    /**
     * Find placement position using ray tracing through the grid
     */
    private GridPos findPlacementPosition(Player player, BlockPos blockPos, BlockHitResult hitResult, MechaGridBlockEntity mechaGrid) {
        // Get ray origin and direction
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 hitVec = hitResult.getLocation();
        net.minecraft.world.phys.Vec3 direction = hitVec.subtract(eyePos).normalize();
        
        // Calculate entry point into the block
        net.minecraft.world.phys.Vec3 blockMin = net.minecraft.world.phys.Vec3.atLowerCornerOf(blockPos);
        net.minecraft.world.phys.Vec3 blockMax = blockMin.add(1, 1, 1);
        
        // Find where ray enters the block
        net.minecraft.world.phys.Vec3 entryPoint = rayBoxIntersection(eyePos, direction, blockMin, blockMax);
        if (entryPoint == null) {
            return null;
        }
        
        // Convert to grid space (0-4 range for continuous coordinates)
        double gridEntryX = (entryPoint.x - blockPos.getX()) * 4;
        double gridEntryY = (entryPoint.y - blockPos.getY()) * 4;
        double gridEntryZ = (entryPoint.z - blockPos.getZ()) * 4;
        
        // Use 3D DDA algorithm to trace through grid
        return traceRayThroughGrid(gridEntryX, gridEntryY, gridEntryZ, direction, mechaGrid);
    }

    /**
     * Ray-box intersection test
     */
    private net.minecraft.world.phys.Vec3 rayBoxIntersection(net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 direction, 
                                                             net.minecraft.world.phys.Vec3 boxMin, net.minecraft.world.phys.Vec3 boxMax) {
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;
        
        // Check each axis
        for (int i = 0; i < 3; i++) {
            double originComp = getComponent(origin, i);
            double dirComp = getComponent(direction, i);
            double minComp = getComponent(boxMin, i);
            double maxComp = getComponent(boxMax, i);
            
            if (Math.abs(dirComp) < 1e-8) {
                if (originComp < minComp || originComp > maxComp) {
                    return null;
                }
            } else {
                double t1 = (minComp - originComp) / dirComp;
                double t2 = (maxComp - originComp) / dirComp;
                
                tMin = Math.max(tMin, Math.min(t1, t2));
                tMax = Math.min(tMax, Math.max(t1, t2));
                
                if (tMin > tMax) {
                    return null;
                }
            }
        }
        
        if (tMin < 0) {
            tMin = 0; // Ray starts inside box
        }
        
        return origin.add(direction.scale(tMin));
    }

    /**
     * Helper to get vector component by index
     */
    private double getComponent(net.minecraft.world.phys.Vec3 vec, int index) {
        return switch (index) {
            case 0 -> vec.x;
            case 1 -> vec.y;
            case 2 -> vec.z;
            default -> 0;
        };
    }

    /**
     * 3D DDA ray tracing through voxel grid
     */
    private GridPos traceRayThroughGrid(double startX, double startY, double startZ, 
                                       net.minecraft.world.phys.Vec3 direction, MechaGridBlockEntity mechaGrid) {
        // Clamp starting position to grid bounds
        startX = Math.max(0, Math.min(3.999, startX));
        startY = Math.max(0, Math.min(3.999, startY));
        startZ = Math.max(0, Math.min(3.999, startZ));
        
        // Current grid position
        int gridX = (int) startX;
        int gridY = (int) startY;
        int gridZ = (int) startZ;
        
        // Step direction for each axis
        int stepX = direction.x > 0 ? 1 : -1;
        int stepY = direction.y > 0 ? 1 : -1;
        int stepZ = direction.z > 0 ? 1 : -1;
        
        // Calculate t values for next grid boundary crossing
        double tMaxX = direction.x != 0 ? ((stepX > 0 ? gridX + 1 : gridX) - startX) / direction.x : Double.POSITIVE_INFINITY;
        double tMaxY = direction.y != 0 ? ((stepY > 0 ? gridY + 1 : gridY) - startY) / direction.y : Double.POSITIVE_INFINITY;
        double tMaxZ = direction.z != 0 ? ((stepZ > 0 ? gridZ + 1 : gridZ) - startZ) / direction.z : Double.POSITIVE_INFINITY;
        
        // Delta t for stepping to next grid boundary
        double tDeltaX = direction.x != 0 ? 1.0 / Math.abs(direction.x) : Double.POSITIVE_INFINITY;
        double tDeltaY = direction.y != 0 ? 1.0 / Math.abs(direction.y) : Double.POSITIVE_INFINITY;
        double tDeltaZ = direction.z != 0 ? 1.0 / Math.abs(direction.z) : Double.POSITIVE_INFINITY;
        
        GridPos lastEmpty = null;
        int steps = 0;
        int maxSteps = 12; // Maximum possible steps through 4x4x4 grid
        
        while (steps < maxSteps && gridX >= 0 && gridX < 4 && gridY >= 0 && gridY < 4 && gridZ >= 0 && gridZ < 4) {
            // Check current position
            if (!mechaGrid.isPositionOccupied(gridX, gridY, gridZ)) {
                lastEmpty = new GridPos(gridX, gridY, gridZ);
            } else {
                // Hit an occupied block, return last empty position
                return lastEmpty;
            }
            
            // Step to next grid cell
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                gridX += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                gridY += stepY;
                tMaxY += tDeltaY;
            } else {
                gridZ += stepZ;
                tMaxZ += tDeltaZ;
            }
            
            steps++;
        }
        
        return lastEmpty;
    }

    /**
     * Get grid position from hit location (for removal)
     */
    private GridPos getHitGridPosition(BlockHitResult hitResult, BlockPos pos) {
        double relativeX = hitResult.getLocation().x - pos.getX();
        double relativeY = hitResult.getLocation().y - pos.getY();
        double relativeZ = hitResult.getLocation().z - pos.getZ();
        
        // Adjust for face hit to get the block in front of the face
        Direction face = hitResult.getDirection();
        relativeX -= face.getStepX() * 0.01;
        relativeY -= face.getStepY() * 0.01;
        relativeZ -= face.getStepZ() * 0.01;
        
        // Clamp and convert to grid coordinates
        int gridX = Math.max(0, Math.min(3, (int) (relativeX * 4)));
        int gridY = Math.max(0, Math.min(3, (int) (relativeY * 4)));
        int gridZ = Math.max(0, Math.min(3, (int) (relativeZ * 4)));
        
        return new GridPos(gridX, gridY, gridZ);
    }

    /**
     * Simple record for grid positions
     */
    private record GridPos(int x, int y, int z) {}

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