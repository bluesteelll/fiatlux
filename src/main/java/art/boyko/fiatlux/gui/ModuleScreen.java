package art.boyko.fiatlux.gui;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;

public class ModuleScreen extends AbstractContainerScreen<ModuleMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("fiatlux", "textures/gui/module_gui.png");
    
    public ModuleScreen(ModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 166;
        this.imageWidth = 176;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        
        IMechaModule module = menu.getModule();
        if (module == null) return;

        // Display module information using getTooltip() for dynamic data
        List<Component> tooltipLines = module.getTooltip();
        int yOffset = this.titleLabelY;
        
        for (Component line : tooltipLines) {
            guiGraphics.drawString(this.font, line, 8, yOffset, 0x404040, false);
            yOffset += 10;
        }
        
        yOffset += 5; // Add some spacing
        
        // Display module position
        String posText = String.format("Position: [%d, %d, %d]", 
            menu.getModuleX(), menu.getModuleY(), menu.getModuleZ());
        guiGraphics.drawString(this.font, posText, 8, yOffset, 0x606060, false);
        yOffset += 15;

        // Display capabilities
        List<Class<? extends IModuleCapability>> capabilities = module.getProvidedCapabilities();
        if (!capabilities.isEmpty()) {
            guiGraphics.drawString(this.font, "Capabilities:", 8, yOffset, 0x404040, false);
            yOffset += 10;
            
            for (Class<? extends IModuleCapability> cap : capabilities) {
                String capName = cap.getSimpleName().replace("I", "").replace("Capability", "");
                guiGraphics.drawString(this.font, "- " + capName, 16, yOffset, 0x606060, false);
                yOffset += 8;
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        
        // Add custom tooltips for module information areas if needed
        int relativeX = x - leftPos;
        int relativeY = y - topPos;
        
        // Example: tooltip for capabilities area
        if (relativeX >= 8 && relativeX <= 168 && relativeY >= 35 && relativeY <= 75) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Module Information"));
            tooltip.add(Component.literal("Right-click module in grid to access this screen"));
            guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
        }
    }
}