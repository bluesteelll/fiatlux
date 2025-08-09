package art.boyko.fiatlux.server.items;

import art.boyko.fiatlux.mechamodule.base.MechaModuleItem;
import art.boyko.fiatlux.server.modules.test.TestModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;

/**
 * Item version of TestModule for placing in MechaGrid
 */
public class TestModuleItem extends MechaModuleItem {
    
    public TestModuleItem() {
        super(TestModule.MODULE_ID, new Properties()
                .stacksTo(64)
                .rarity(Rarity.COMMON)
        );
    }
}