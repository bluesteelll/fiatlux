package art.boyko.fiatlux.custom.block;

import art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.init.ModBlockEntities;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import javax.annotation.Nonnull;

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
    public @Nullable BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new MechaGridBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hitResult) {
        return handleInteraction(state, level, pos, player, hitResult, ItemStack.EMPTY);
    }

    @Override
    protected ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        InteractionResult result = handleInteraction(state, level, pos, player, hitResult, stack);
        return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult handleInteraction(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hitResult, ItemStack heldItem) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
            return InteractionResult.FAIL;
        }

        // Handle sneaking (remove block) vs normal (place block)
        if (player.isShiftKeyDown()) {
            // This part is now handled by the attack method for Left Click
            // We'll leave the right-click behavior for placing modules
            // For right-click, if shift is held, simply show grid status as before
            int totalModules = mechaGrid.getTotalModules();
            player.sendSystemMessage(Component.literal("MechaGrid: " + totalModules + "/64 modules placed"));
            return InteractionResult.SUCCESS;
        } else {
            // Place module or block using ray tracing
            if (!heldItem.isEmpty()) {
                GridPos targetPos = findPlacementPosition(player, pos, hitResult, mechaGrid);
                
                if (targetPos != null) {
                    boolean placed = false;
                    String placedName = "";
                    
                    // Check if it's a MechaModule item
                    if (MechaModuleItem.isMechaModuleItem(heldItem)) {
                        IMechaModule module = MechaModuleItem.createModuleFromStack(heldItem);
                        if (module != null && mechaGrid.placeModule(targetPos.x, targetPos.y, targetPos.z, module)) {
                            placed = true;
                            placedName = module.getDisplayName().getString();
                        }
                    }
                    // Fallback to legacy BlockItem support
                    else if (heldItem.getItem() instanceof BlockItem blockItem) {
                        BlockState blockToPlace = blockItem.getBlock().defaultBlockState();
                        if (mechaGrid.placeBlock(targetPos.x, targetPos.y, targetPos.z, blockToPlace)) {
                            placed = true;
                            placedName = blockToPlace.getBlock().getName().getString();
                        }
                    }
                    
                    if (placed) {
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }
                        player.sendSystemMessage(Component.literal("Placed " + placedName + " at [" + targetPos.x + "," + targetPos.y + "," + targetPos.z + "]"));
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    player.sendSystemMessage(Component.literal("No valid placement position found"));
                    return InteractionResult.SUCCESS;
                }
            } else {
                // Show grid status when empty hand
                int totalModules = mechaGrid.getTotalModules();
                player.sendSystemMessage(Component.literal("MechaGrid: " + totalModules + "/64 modules placed"));
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
        
        // Use 3D DDA algorithm to trace through grid, looking for an empty spot
        return traceRayThroughGrid(gridEntryX, gridEntryY, gridEntryZ, direction, mechaGrid, false);
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
     * @param findOccupied If true, searches for an occupied block. If false, searches for an empty block.
     */
    public GridPos traceRayThroughGrid(double startX, double startY, double startZ,
                                           @Nonnull net.minecraft.world.phys.Vec3 direction, @Nonnull MechaGridBlockEntity mechaGrid, boolean findOccupied) {
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

        GridPos resultPos = null; // Stores the position found
        int steps = 0;
        int maxSteps = 12; // Maximum possible steps through 4x4x4 grid

        while (steps < maxSteps && gridX >= 0 && gridX < 4 && gridY >= 0 && gridY < 4 && gridZ >= 0 && gridZ < 4) {
            // Check current position based on findOccupied flag
            if (findOccupied) {
                if (mechaGrid.isPositionOccupied(gridX, gridY, gridZ)) {
                    return new GridPos(gridX, gridY, gridZ); // Found occupied block
                }
            } else {
                if (!mechaGrid.isPositionOccupied(gridX, gridY, gridZ)) {
                    resultPos = new GridPos(gridX, gridY, gridZ); // Found empty block
                } else {
                    return resultPos; // Hit an occupied block, return last empty position
                }
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

        return resultPos; // Return the last empty position found (for placement) or null (if no occupied found for breaking)
    }

    /**
     * Handle module removal when player attacks the block
     */
    @Override
    protected void attack(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player) {
        if (level.isClientSide()) {
            return;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
            return;
        }
        
        // Find target position using ray tracing
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, pos);
        if (hitResult == null) {
            return;
        }
        
        GridPos targetPos = findTargetGridPositionForRemoval(player, pos, hitResult, mechaGrid);
        if (targetPos != null) {
            IMechaModule removedModule = mechaGrid.removeModule(targetPos.x, targetPos.y, targetPos.z);
            if (removedModule != null) {
                // Drop the module as an item
                ItemStack moduleStack = removedModule.toItemStack();
                Block.popResource(level, pos, moduleStack);
                
                player.sendSystemMessage(Component.literal("Removed " + removedModule.getDisplayName().getString() + " from [" + targetPos.x + "," + targetPos.y + "," + targetPos.z + "]"));
            }
        }
    }
    
    /**
     * Find module to break position using ray tracing through the grid
     */
    public GridPos findTargetGridPositionForRemoval(Player player, BlockPos blockPos, BlockHitResult hitResult, MechaGridBlockEntity mechaGrid) {
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

        // Use 3D DDA algorithm to trace through grid, looking for an occupied spot
        return traceRayThroughGrid(gridEntryX, gridEntryY, gridEntryZ, direction, mechaGrid, true);
    }

    /**
     * Helper to get player's POV hit result for accurate ray tracing
     */
    public BlockHitResult getPlayerPOVHitResult(@Nonnull Level level, @Nonnull Player player, @Nonnull BlockPos blockPos) {
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 viewVector = player.getViewVector(1.0F); // Current view direction
        double reach = player.blockInteractionRange(); // Player's block interaction range

        // Calculate ray end point
        net.minecraft.world.phys.Vec3 rayEnd = eyePos.add(viewVector.x * reach, viewVector.y * reach, viewVector.z * reach);

        // Perform ray trace against the block's collision shape
        // This will find the specific face and hit location on the MechaGridBlock
        // For accurate sub-block ray tracing, we want to trace through the internal grid.
        // So, we'll return a BlockHitResult that represents the *entry point* into our block,
        // which the findBlockToBreakPosition will then use for its internal grid raycast.
        BlockHitResult result = level.clip(new net.minecraft.world.level.ClipContext(eyePos, rayEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        
        if (result != null && result.getBlockPos().equals(blockPos)) {
            return result;
        }
        return null;
    }

    /**
     * Simple record for grid positions
     */
    public static record GridPos(int x, int y, int z) {}

    @Override
    protected RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // Custom rendering
    }

    @Override
    protected VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE; // Full block shape for interaction
    }
    
    @Override
    protected VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        // Get the block entity to check what blocks are inside
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
            return Shapes.empty(); // No collision if no block entity
        }
        
        // Return the cached collision shape
        return mechaGrid.getCachedCollisionShape();
    }
    
    @Override
    protected VoxelShape getInteractionShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        // This shape is used for raytracing when the player looks at the block
        // Keep full block shape so players can interact with any part of the block
        return SHAPE;
    }
    
    @Override
    protected float getShadeBrightness(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return 1.0F; // Full brightness, no shadow
    }
    
    @Override
    protected boolean isOcclusionShapeFullBlock(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return false; // Not a full block for occlusion
    }
    
    @Override
    protected VoxelShape getVisualShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return Shapes.empty(); // No visual occlusion
    }

    @Override
    protected boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return true; // Allow light to pass through
    }

    @Override
    protected void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MechaGridBlockEntity mechaGrid) {
                // Drop all modules stored in the grid
                mechaGrid.dropAllModules(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.MECHA_GRID_BE.get(),
                (level1, pos1, state1, blockEntity) -> blockEntity.tick(level1, pos1, state1));
    }
}
