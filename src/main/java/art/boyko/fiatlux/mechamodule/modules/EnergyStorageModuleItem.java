package art.boyko.fiatlux.mechamodule.modules;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Item for the Energy Storage Module
 */
public class EnergyStorageModuleItem extends MechaModuleItem {
    
    public EnergyStorageModuleItem() {
        super(EnergyStorageModule.MODULE_ID, 
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON)
        );
    }
}