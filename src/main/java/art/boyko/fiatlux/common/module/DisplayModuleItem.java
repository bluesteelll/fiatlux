package art.boyko.fiatlux.common.module;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
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