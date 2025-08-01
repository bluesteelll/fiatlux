package art.boyko.fiatlux;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import art.boyko.fiatlux.custom.block.MechaGridBlock;
import art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.init.ModBlockEntities;
import art.boyko.fiatlux.init.ModBlocks;
import art.boyko.fiatlux.init.ModCreativeTabs;
import art.boyko.fiatlux.init.ModDataComponents;
import art.boyko.fiatlux.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(FiatLux.MODID)
public class FiatLux {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fiatlux";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Per-player cooldown to prevent multiple block removals from a single sustained click
    private final Map<UUID, Long> playerClickCooldowns = new ConcurrentHashMap<>(); 
    private static final long COOLDOWN_MILLIS = 200; // 200 milliseconds cooldown

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FiatLux(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register all mod content
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus); // Register block entities
        ModCreativeTabs.register(modEventBus);
        ModDataComponents.register(modEventBus); // Register data components

        // Note that this is necessary if and only if we want *this* class (FiatLux) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);

        // Add items to existing vanilla creative tabs
        modEventBus.addListener(this::addCreative);

        // Register mod configuration
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the mod block items to existing vanilla creative tabs
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.EXAMPLE_BLOCK_ITEM);
            event.accept(ModItems.LIGHT_BLOCK_ITEM);
            event.accept(ModItems.DECORATIVE_BLOCK_ITEM);
            event.accept(ModItems.REINFORCED_BLOCK_ITEM);
            // Add new blocks with BlockEntity
            event.accept(ModItems.SIMPLE_STORAGE_BLOCK_ITEM);
            event.accept(ModItems.ENERGY_STORAGE_BLOCK_ITEM);
            // Add MechaGrid block
            event.accept(ModItems.MECHA_GRID_BLOCK_ITEM);
        }
        
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.LIGHT_SWORD);
            event.accept(ModItems.TORCH_ITEM);
        }
        
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.MAGIC_GEM);
            event.accept(ModItems.LIGHT_CRYSTAL);
            event.accept(ModItems.COMPRESSED_COAL);
        }
        
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.EXAMPLE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof MechaGridBlock)) {
            return; // Not a MechaGridBlock, do nothing
        }

        // Always cancel the event to prevent default block breaking behavior on both sides
        event.setCanceled(true);

        // Only process the actual block modification logic on the server side
        if (level.isClientSide()) {
            return; // Client just cancels and waits for server update
        }

        // Ensure it's our MechaGridBlockEntity
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechaGridBlockEntity mechaGrid)) {
            return;
        }

        // Cast the block to MechaGridBlock to access its methods
        MechaGridBlock mechaGridBlock = (MechaGridBlock) blockState.getBlock();

        // Apply cooldown for single block removal (not for breaking the entire block)
        if (!player.isShiftKeyDown()) {
            long currentTime = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            Long lastClickTime = playerClickCooldowns.getOrDefault(playerId, 0L);

            if (currentTime - lastClickTime < COOLDOWN_MILLIS) {
                return; // On cooldown, do nothing
            }
            playerClickCooldowns.put(playerId, currentTime);
        }

        if (player.isShiftKeyDown()) {
            // Shift + Left Click: Break the entire MechaGridBlock
            mechaGrid.dropAllBlocks(level, pos);
            level.destroyBlock(pos, false); // Destroy the block without dropping itself
            player.sendSystemMessage(Component.literal("MechaGridBlock broken and contents dropped!"));
        } else {
            // Left Click: Remove a specific block using ray tracing
            BlockHitResult hitResult = mechaGridBlock.getPlayerPOVHitResult(level, player, pos);
            if (hitResult != null) {
                MechaGridBlock.GridPos targetPos = mechaGridBlock.findTargetGridPositionForRemoval(player, pos, hitResult, mechaGrid);
                if (targetPos != null) {
                    BlockState removedBlock = mechaGrid.removeBlock(targetPos.x(), targetPos.y(), targetPos.z());
                    if (removedBlock != null && !removedBlock.isAir()) {
                        // Replaced Block.popResource with manual item drop to prevent breaking animation and sound
                        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(removedBlock.getBlock()));
                        itemEntity.setDefaultPickUpDelay();
                        level.addFreshEntity(itemEntity);
                        player.sendSystemMessage(Component.literal("Removed " + removedBlock.getBlock().getName().getString() + " from [" + targetPos.x() + "," + targetPos.y() + "," + targetPos.z() + "]"));
                    } else {
                        player.sendSystemMessage(Component.literal("No block to remove at target position."));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("No block found to remove."));
                }
            } else {
                player.sendSystemMessage(Component.literal("Could not determine hit result for removal."));
            }
        }
    }
}