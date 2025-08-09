package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.client.gui.ModuleMenu;
import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, FiatLux.MODID);

    public static final Supplier<MenuType<ModuleMenu>> MODULE_MENU = MENUS.register("module_menu",
        () -> IMenuTypeExtension.create((containerId, inventory, data) -> {
            BlockPos pos = data.readBlockPos();
            int moduleX = data.readInt();
            int moduleY = data.readInt(); 
            int moduleZ = data.readInt();
            
            BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
            if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
                return null;
            }
            
            IMechaModule module = mechaGrid.getModule(moduleX, moduleY, moduleZ);
            if (module == null) {
                return null;
            }
            
            return new ModuleMenu(containerId, inventory, mechaGrid, module, moduleX, moduleY, moduleZ);
        }));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}