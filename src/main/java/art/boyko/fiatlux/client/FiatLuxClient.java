package art.boyko.fiatlux.client;

import art.boyko.fiatlux.client.gui.ModuleScreen;
import art.boyko.fiatlux.server.init.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = art.boyko.fiatlux.server.FiatLux.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = art.boyko.fiatlux.server.FiatLux.MODID, value = Dist.CLIENT)
public class FiatLuxClient {
    public FiatLuxClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        art.boyko.fiatlux.server.FiatLux.LOGGER.info("HELLO FROM CLIENT SETUP");
        art.boyko.fiatlux.server.FiatLux.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MODULE_MENU.get(), ModuleScreen::new);
    }
}


