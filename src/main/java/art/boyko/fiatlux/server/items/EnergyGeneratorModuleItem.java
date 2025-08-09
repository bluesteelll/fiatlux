package art.boyko.fiatlux.server.items;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import art.boyko.fiatlux.server.modules.energy.EnergyGeneratorModule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Item for the Energy Generator Module
 */
public class EnergyGeneratorModuleItem extends MechaModuleItem {
    
    public EnergyGeneratorModuleItem() {
        super(EnergyGeneratorModule.MODULE_ID, 
            new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}