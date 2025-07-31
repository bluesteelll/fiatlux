package art.boyko.fiatlux.client.renderer;

import art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class MechaGridBlockEntityRenderer implements BlockEntityRenderer<MechaGridBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public MechaGridBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(MechaGridBlockEntity blockEntity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        if (blockEntity.isEmpty()) {
            return; // Nothing to render
        }

        poseStack.pushPose();
        
        // Scale factor to fit 4x4x4 grid into 1x1x1 block space
        float scale = 1.0f / MechaGridBlockEntity.GRID_SIZE;
        
        BlockState[][][] grid = blockEntity.getGrid();
        long occupiedMask = blockEntity.getOccupiedMask();
        
        // Iterate through occupied positions only (performance optimization)
        for (int x = 0; x < MechaGridBlockEntity.GRID_SIZE; x++) {
            for (int y = 0; y < MechaGridBlockEntity.GRID_SIZE; y++) {
                for (int z = 0; z < MechaGridBlockEntity.GRID_SIZE; z++) {
                    // Check if position is occupied using bit mask (faster than checking if air)
                    int bitIndex = x + y * MechaGridBlockEntity.GRID_SIZE + z * MechaGridBlockEntity.GRID_SIZE * MechaGridBlockEntity.GRID_SIZE;
                    if ((occupiedMask & (1L << bitIndex)) == 0) {
                        continue; // Position is empty, skip
                    }
                    
                    BlockState blockState = grid[x][y][z];
                    if (blockState.isAir()) {
                        continue; // Safety check
                    }
                    
                    poseStack.pushPose();
                    
                    // Position the block within the grid
                    float offsetX = x * scale;
                    float offsetY = y * scale;
                    float offsetZ = z * scale;
                    
                    poseStack.translate(offsetX, offsetY, offsetZ);
                    poseStack.scale(scale, scale, scale);
                    
                    // Render the block with appropriate render type
                    renderBlock(blockState, poseStack, bufferSource, packedLight, packedOverlay);
                    
                    poseStack.popPose();
                }
            }
        }
        
        poseStack.popPose();
    }

    private void renderBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource bufferSource, 
                           int packedLight, int packedOverlay) {
        try {
            // Render solid blocks
            if (!blockState.canOcclude()) {
                blockRenderer.renderSingleBlock(
                    blockState,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    RenderType.cutout()
                );
            } else {
                blockRenderer.renderSingleBlock(
                    blockState,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    RenderType.solid()
                );
            }
        } catch (Exception e) {
            // Fallback - skip rendering problematic blocks
            // This prevents crashes from mod blocks with rendering issues
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MechaGridBlockEntity blockEntity) {
        return false; // Only render when on screen for performance
    }

    @Override
    public int getViewDistance() {
        return 64; // Render distance in blocks
    }

    @Override
    public boolean shouldRender(MechaGridBlockEntity blockEntity, net.minecraft.world.phys.Vec3 cameraPos) {
        // Always render when in range and not empty
        return !blockEntity.isEmpty();
    }
}