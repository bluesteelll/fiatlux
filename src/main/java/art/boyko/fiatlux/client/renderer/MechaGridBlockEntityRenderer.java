package art.boyko.fiatlux.client.renderer;

import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class MechaGridBlockEntityRenderer implements BlockEntityRenderer<art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public MechaGridBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity blockEntity, float partialTick, PoseStack poseStack, 
                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        if (blockEntity.isEmpty()) {
            return; // Nothing to render
        }

        poseStack.pushPose();
        
        // Scale factor to fit 4x4x4 grid into 1x1x1 block space
        float scale = 1.0f / art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE;
        
        IMechaModule[][][] moduleGrid = blockEntity.getModuleGrid();
        long occupiedMask = blockEntity.getOccupiedMask();
        
        // Iterate through occupied positions only (performance optimization)
        for (int x = 0; x < art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE; x++) {
            for (int y = 0; y < art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE; y++) {
                for (int z = 0; z < art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE; z++) {
                    // Check if position is occupied using bit mask (faster than checking null)
                    int bitIndex = x + y * art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE + z * art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE * art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity.GRID_SIZE;
                    if ((occupiedMask & (1L << bitIndex)) == 0) {
                        continue; // Position is empty, skip
                    }
                    
                    IMechaModule module = moduleGrid[x][y][z];
                    if (module == null) {
                        continue; // Safety check
                    }
                    
                    // Get the render state from the module
                    BlockState blockState = module.getRenderState();
                    
                    poseStack.pushPose();
                    
                    // Position the module within the grid
                    float offsetX = x * scale;
                    float offsetY = y * scale;
                    float offsetZ = z * scale;
                    
                    poseStack.translate(offsetX, offsetY, offsetZ);
                    poseStack.scale(scale, scale, scale);
                    
                    // Render the module using its render state
                    renderModule(module, blockState, poseStack, bufferSource, packedLight, packedOverlay);
                    
                    // TODO: Render connections between modules
                    // renderModuleConnections(module, x, y, z, poseStack, bufferSource, packedLight, packedOverlay);
                    
                    poseStack.popPose();
                }
            }
        }
        
        poseStack.popPose();
    }

    private void renderModule(IMechaModule module, BlockState blockState, PoseStack poseStack, 
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        try {
            // Render module using its render state
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
            
            // TODO: Add special effects for active modules
            // if (module.needsTicking() && module is active) {
            //     renderModuleEffects(module, poseStack, bufferSource, packedLight, packedOverlay);
            // }
            
        } catch (Exception e) {
            // Fallback - skip rendering problematic modules
            // This prevents crashes from mod modules with rendering issues
        }
    }
    
    /**
     * Render connections between modules (future enhancement)
     */
    private void renderModuleConnections(IMechaModule module, int x, int y, int z, PoseStack poseStack, 
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // TODO: Implement connection rendering
        // This would show pipes, wires, etc. between connected modules
        // For now, this is just a placeholder for future development
    }
    
    /**
     * Render special effects for active modules (future enhancement)
     */
    private void renderModuleEffects(IMechaModule module, PoseStack poseStack, 
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // TODO: Add particle effects, glowing, etc. for active modules
    }

    @Override
    public boolean shouldRenderOffScreen(art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity blockEntity) {
        return false; // Only render when on screen for performance
    }

    @Override
    public int getViewDistance() {
        return 64; // Render distance in blocks
    }

    @Override
    public boolean shouldRender(art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity blockEntity, net.minecraft.world.phys.Vec3 cameraPos) {
        // Always render when in range and has modules
        return !blockEntity.isEmpty();
    }
}