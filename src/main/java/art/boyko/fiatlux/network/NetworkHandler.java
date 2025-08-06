package art.boyko.fiatlux.network;

import art.boyko.fiatlux.FiatLux;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {
    public static void openModuleGui(ServerPlayer player, BlockPos gridPos, int moduleX, int moduleY, int moduleZ) {
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, playerEntity) -> {
                    net.minecraft.world.level.block.entity.BlockEntity blockEntity = 
                        player.level().getBlockEntity(gridPos);
                    if (!(blockEntity instanceof art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity mechaGrid)) {
                        return null;
                    }
                    
                    art.boyko.fiatlux.mechamodule.base.IMechaModule module = 
                        mechaGrid.getModule(moduleX, moduleY, moduleZ);
                    if (module == null) {
                        return null;
                    }
                    
                    return new art.boyko.fiatlux.gui.ModuleMenu(containerId, inventory, 
                        mechaGrid, module, moduleX, moduleY, moduleZ);
                },
                net.minecraft.network.chat.Component.literal("Module Interface")
            ),
            (data) -> {
                data.writeBlockPos(gridPos);
                data.writeInt(moduleX);
                data.writeInt(moduleY);
                data.writeInt(moduleZ);
            }
        );
    }
}