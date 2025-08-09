package art.boyko.fiatlux.server.items;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import art.boyko.fiatlux.server.modules.display.DisplayModule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Item for the Display Module
 */
public class DisplayModuleItem extends MechaModuleItem {
    
    public DisplayModuleItem() {
        super(DisplayModule.MODULE_ID, 
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}