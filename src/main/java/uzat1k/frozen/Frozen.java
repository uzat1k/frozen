package uzat1k.frozen;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;

public class Frozen implements ModInitializer {
    public static final String MOD_ID = "frozen";
    public static final Logger LOGGER = LoggerFactory.getLogger("winter-mod");

    // Память температур игроков
    private final Map<ServerPlayer, Integer> playerTemperatures = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Зимний мод 2.0: Демон Температур запущен!");

        // ==============================
        // СКАНЕР ЧАНКОВ ПРИ ЗАГРУЗКЕ
        // ==============================
        ServerChunkEvents.CHUNK_LOAD.register((serverLevel, chunk) -> {
            if (chunk instanceof IHeatChunk heatChunk) {
                heatChunk.frostmod$getHeatSources().clear(); // Очищаем от старого мусора
                
                int startX = chunk.getPos().x << 4;
                int startZ = chunk.getPos().z << 4;
                
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    
                    if (section == null || section.hasOnlyAir()) continue;
                    
                    // Узнаем нижнюю координату Y для этой секции чанка
                    int sectionBottomY = serverLevel.getMinY() + (i * 16);
                    
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockPos pos = new BlockPos(startX + x, sectionBottomY + y, startZ + z);
                                BlockState state = chunk.getBlockState(pos);
                                
                                if (TemperatureManager.isHeatSource(state)) {
                                    heatChunk.frostmod$addHeatSource(pos);
                                }
                            }
                        }
                    }
                }
            }
        });

        // ==============================
        // ОСНОВНАЯ ЛОГИКА КАЖДЫЙ ТИК
        // ==============================
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() == Level.NETHER) return;

            boolean updateThermometers = (level.getGameTime() % 60 == 0); // Раз в 3 секунды
            boolean calculateTemp = (level.getGameTime() % 10 == 0);      // Раз в полсекунды

            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) continue;

                BlockPos playerPos = player.blockPosition();

                if (updateThermometers) {
                    TemperatureManager.updateThermometersNearPlayer(level, playerPos);
                }

                int currentTemp;

                if (calculateTemp) {
                    int worldTemp = TemperatureManager.getWorldTemperature(level);
                    int localTemp = TemperatureManager.getLocalTemperature(level, playerPos);

                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    
                    if (isWarmItem(mainHand, worldTemp) || isWarmItem(offHand, worldTemp)) {
                        localTemp += 60; 
                    }

                    currentTemp = localTemp;
                    playerTemperatures.put(player, currentTemp);
                } else {
                    currentTemp = playerTemperatures.getOrDefault(player, TemperatureManager.getWorldTemperature(level));
                }

                int currentFreeze = player.getTicksFrozen();
                int maxFreeze = player.getTicksRequiredToFreeze(); 

                // Замерзание начинается от -10 градусов
                if (currentTemp <= -10) {
                    int freezeToAdd = 2; 
                    double tempRatio = (currentTemp + 120) / 110.0;
                    int ticksBetweenFreeze = (int) Math.max(2, 2 + tempRatio * 15);

                    if (level.getGameTime() % ticksBetweenFreeze == 0) {
                        freezeToAdd += 1;
                    }
                    
                    player.setTicksFrozen(Math.min(currentFreeze + freezeToAdd, maxFreeze + 40));
                } else {
                    // Игрок в тепле
                    player.setTicksFrozen(Math.max(currentFreeze - 4, 0));
                }
            }
        });
    }

    private boolean isWarmItem(ItemStack stack, int worldTemperature) {
        if (stack.is(Items.LAVA_BUCKET)) {
            return true;
        }
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
            return worldTemperature > -50;
        }
        return false;
    }
}