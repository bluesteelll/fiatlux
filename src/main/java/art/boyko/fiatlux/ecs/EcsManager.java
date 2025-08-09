package art.boyko.fiatlux.ecs;

import art.boyko.fiatlux.server.FiatLux;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for ECS worlds and entities.
 * Coordinates between Minecraft blocks and Bevy ECS entities.
 */
public class EcsManager {
    
    // Map from world position to ECS world
    private static final Map<BlockPos, BevyEcsWorld> WORLDS = new ConcurrentHashMap<>();
    
    // Map from (world_pos, grid_pos) to entity
    private static final Map<String, EcsEntity> ENTITIES = new ConcurrentHashMap<>();
    
    /**
     * Get or create an ECS world for the given block position
     */
    public static BevyEcsWorld getOrCreateWorld(BlockPos worldPos) {
        return WORLDS.computeIfAbsent(worldPos, pos -> {
            FiatLux.LOGGER.info("Creating new ECS world at {}", pos);
            return new BevyEcsWorld();
        });
    }
    
    /**
     * Get ECS world for block position (without creating)
     */
    @Nullable
    public static BevyEcsWorld getWorld(BlockPos worldPos) {
        return WORLDS.get(worldPos);
    }
    
    /**
     * Get ECS world ID for block position
     * @return world ID or -1 if not found
     */
    public static long getWorldId(BlockPos worldPos) {
        BevyEcsWorld world = getWorld(worldPos);
        return world != null ? world.getWorldId() : -1;
    }
    
    /**
     * Remove ECS world for block position
     */
    public static void removeWorld(BlockPos worldPos) {
        BevyEcsWorld world = WORLDS.remove(worldPos);
        if (world != null) {
            FiatLux.LOGGER.info("Destroying ECS world at {}", worldPos);
            
            // Remove all entities from this world
            ENTITIES.entrySet().removeIf(entry -> {
                EcsEntity entity = entry.getValue();
                if (entity.world == world) {
                    entity.despawn();
                    return true;
                }
                return false;
            });
            
            world.destroy();
        }
    }
    
    /**
     * Spawn entity in the ECS world
     */
    public static EcsEntity spawnEntity(BlockPos worldPos, String moduleType, int gridX, int gridY, int gridZ) {
        BevyEcsWorld world = getOrCreateWorld(worldPos);
        long entityId = world.spawnEntity(moduleType, gridX, gridY, gridZ);
        
        if (entityId >= 0) {
            EcsEntity entity = new EcsEntity(entityId, world, moduleType, gridX, gridY, gridZ);
            String key = makeKey(worldPos, gridX, gridY, gridZ);
            ENTITIES.put(key, entity);
            
            FiatLux.LOGGER.debug("Spawned ECS entity {} at {}:({},{},{})", 
                               entityId, worldPos, gridX, gridY, gridZ);
            return entity;
        }
        
        FiatLux.LOGGER.warn("Failed to spawn ECS entity of type {} at {}:({},{},{})", 
                          moduleType, worldPos, gridX, gridY, gridZ);
        return null;
    }
    
    /**
     * Get entity at specific position
     */
    @Nullable
    public static EcsEntity getEntity(BlockPos worldPos, int gridX, int gridY, int gridZ) {
        String key = makeKey(worldPos, gridX, gridY, gridZ);
        return ENTITIES.get(key);
    }
    
    /**
     * Remove entity at specific position
     */
    public static boolean removeEntity(BlockPos worldPos, int gridX, int gridY, int gridZ) {
        String key = makeKey(worldPos, gridX, gridY, gridZ);
        EcsEntity entity = ENTITIES.remove(key);
        
        if (entity != null) {
            boolean success = entity.despawn();
            FiatLux.LOGGER.debug("Removed ECS entity {} at {}:({},{},{})", 
                               entity.getId(), worldPos, gridX, gridY, gridZ);
            return success;
        }
        
        return false;
    }
    
    /**
     * Tick all ECS worlds with batch processing optimization
     */
    public static void tickAllWorlds() {
        int processedWorlds = 0;
        int totalWorlds = 0;
        
        for (BevyEcsWorld world : WORLDS.values()) {
            if (world.isValid()) {
                totalWorlds++;
                world.tick();
                
                // Check if this world processed systems this tick (every 4th tick)
                if (world.willProcessThisTick()) {
                    processedWorlds++;
                }
            }
        }
        
        // Log performance stats every 80 ticks (every 4 seconds at 20 TPS)
        if (totalWorlds > 0 && processedWorlds > 0) {
            // Only log when we actually process systems for reduced spam
            BevyEcsWorld firstWorld = WORLDS.values().iterator().next();
            long tickCount = firstWorld.getTickCount();
            
            if (tickCount % 80 == 0) {
                FiatLux.LOGGER.info("🚀 ECS Batch Processing: {} worlds processed systems at tick {} ({}% efficiency)", 
                                  processedWorlds, tickCount, (processedWorlds * 100 / totalWorlds));
            }
        }
    }
    
    /**
     * Tick specific world with batch processing
     */
    public static void tickWorld(BlockPos worldPos) {
        BevyEcsWorld world = getWorld(worldPos);
        if (world != null && world.isValid()) {
            world.tick();
            
            // Optional: Log detailed stats for this specific world every 40 ticks (2 seconds)
            if (world.getTickCount() % 40 == 0 && world.willProcessThisTick()) {
                int pendingChanges = world.getPendingChanges();
                if (pendingChanges > 10) { // Only log if significant activity
                    FiatLux.LOGGER.debug("⚡ ECS World at {} processed {} pending changes at tick {}", 
                                       worldPos, pendingChanges, world.getTickCount());
                }
            }
        }
    }
    
    /**
     * Get count of active worlds
     */
    public static int getWorldCount() {
        return WORLDS.size();
    }
    
    /**
     * Get count of active entities
     */
    public static int getEntityCount() {
        return ENTITIES.size();
    }
    
    /**
     * Clean up all resources
     */
    public static void shutdown() {
        FiatLux.LOGGER.info("Shutting down ECS manager - {} worlds, {} entities", 
                          WORLDS.size(), ENTITIES.size());
        
        // Despawn all entities
        for (EcsEntity entity : ENTITIES.values()) {
            entity.despawn();
        }
        ENTITIES.clear();
        
        // Destroy all worlds
        for (BevyEcsWorld world : WORLDS.values()) {
            world.destroy();
        }
        WORLDS.clear();
    }
    
    /**
     * Create unique key for entity mapping
     */
    private static String makeKey(BlockPos worldPos, int gridX, int gridY, int gridZ) {
        return worldPos.getX() + "," + worldPos.getY() + "," + worldPos.getZ() + 
               ":" + gridX + "," + gridY + "," + gridZ;
    }
}