package art.boyko.fiatlux.server.items;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import art.boyko.fiatlux.server.modules.processing.ProcessorModule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Item for the Processor Module
 */
public class ProcessorModuleItem extends MechaModuleItem {
    
    public ProcessorModuleItem() {
        super(ProcessorModule.MODULE_ID, 
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON)
        );
    }
}