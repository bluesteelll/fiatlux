package art.boyko.fiatlux.client.renderer;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.init.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = FiatLux.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModBlockEntityRenderers {
    
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MECHA_GRID_BE.get(), MechaGridBlockEntityRenderer::new);
        
        FiatLux.LOGGER.info("Registered block entity renderers for " + FiatLux.MODID);
    }
}