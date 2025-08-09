package art.boyko.fiatlux.server.datagen;

import java.util.concurrent.CompletableFuture;

import art.boyko.fiatlux.server.FiatLux;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = FiatLux.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        if (event.includeClient()) {
            generator.addProvider(true, new ModItemModelProvider(packOutput, existingFileHelper));
            generator.addProvider(true, new ModBlockStateProvider(packOutput, existingFileHelper));
            generator.addProvider(true, new ModLanguageProvider(packOutput, "en_us"));
        }

        if (event.includeServer()) {
            generator.addProvider(true, new ModRecipeProvider(packOutput, lookupProvider));
            generator.addProvider(true, new ModLootTableProvider(packOutput, lookupProvider));
        }

        FiatLux.LOGGER.info("Data generation setup complete for " + FiatLux.MODID);
    }
}