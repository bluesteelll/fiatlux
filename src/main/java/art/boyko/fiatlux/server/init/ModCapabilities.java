package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register energy capability for MechaGridBlockEntity
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.MECHA_GRID_BE.get(),
            (blockEntity, direction) -> {
                if (blockEntity instanceof MechaGridBlockEntity mechaGrid) {
                    return mechaGrid.getEnergyStorageCapability(direction);
                }
                return null;
            }
        );
    }
}