package name.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;

public class Frozen implements ModInitializer {
    public static final String MOD_ID = "frozen";
    public static final Logger LOGGER = LoggerFactory.getLogger("winter-mod");

    // Память: кому тепло, а кому холодно
    private final Map<ServerPlayer, Boolean> warmPlayers = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Зимний мод загружен! Берегите тепло...");

        // ==========================================
        // РЕГИСТРАЦИЯ КОМАНДЫ /temperature И /temp
        // ==========================================
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            
            // Основная команда /temperature
            dispatcher.register(Commands.literal("temperature")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    int currentTemp = getCurrentTemperature(source.getLevel());
                    
                    // Отправляем сообщение тому, кто вызвал команду (игроку или в консоль)
                    // Примечание: Для версий ниже 1.20 используйте source.sendSuccess(Component.literal(...), false);
                    source.sendSuccess(() -> Component.literal("❄ Текущая температура воздуха: " + currentTemp + "°C"), false);
                    return 1;
                })
            );

            // Короткий алиас /temp
            dispatcher.register(Commands.literal("temp")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    int currentTemp = getCurrentTemperature(source.getLevel());
                    
                    source.sendSuccess(() -> Component.literal("❄ Текущая температура воздуха: " + currentTemp + "°C"), false);
                    return 1;
                })
            );
        });

        // ==========================================
        // ЛОГИКА ОБРАБОТКИ ТИКОВ СЕРВЕРА
        // ==========================================
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() == Level.NETHER) {
                return;
            }

            // Вычисляем текущую температуру для этого дня
            int currentTemp = getCurrentTemperature(level);

            // Определяем, нужно ли в ЭТОТ тик делать тяжелую проверку блоков (раз в 10 тиков)
            boolean doHeavyCheck = (level.getGameTime() % 10 == 0);

            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }

                boolean isWarm = false;

                // Если пришло время тяжелой проверки:
                if (doHeavyCheck) {
                    BlockPos playerPos = player.blockPosition();

                    // 1. Проверка предметов в руках (теперь зависит от температуры)
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();
                    
                    if (isWarmItem(mainHand, currentTemp) || isWarmItem(offHand, currentTemp)) {
                        isWarm = true;
                    }

                    // 2. Проверка чанков (если в руках нет спасающего источника)
                    if (!isWarm) {
                        int chunkX = playerPos.getX() >> 4;
                        int chunkZ = playerPos.getZ() >> 4;

                        searchLoop:
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                ChunkAccess chunk = level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                                
                                if (chunk instanceof IHeatChunk heatChunk) {
                                    for (BlockPos heatPos : heatChunk.frostmod$getHeatSources()) {
                                        BlockState state = level.getBlockState(heatPos);
                                        double distanceSq = playerPos.distSqr(heatPos);

                                        if (state.is(Blocks.LAVA) && distanceSq <= 16 * 16) isWarm = true;
                                        else if (state.is(Blocks.FURNACE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) && distanceSq <= 12 * 12) isWarm = true;
                                        else if (state.is(Blocks.CAMPFIRE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) && distanceSq <= 8 * 8) isWarm = true;
                                        else if ((state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) && distanceSq <= 4 * 4) isWarm = true;
                                        
                                        if (isWarm) break searchLoop;
                                    }
                                }
                            }
                        }
                    }
                    // Сохраняем результат в память!
                    warmPlayers.put(player, isWarm);
                } else {
                    // Если сейчас обычный тик, берем готовый ответ из памяти
                    isWarm = warmPlayers.getOrDefault(player, false);
                }

                // ==========================================
                // ЛОГИКА ЗАМЕРЗАНИЯ КАЖДЫЙ ТИК
                // ==========================================
                int currentFreeze = player.getTicksFrozen();
                int maxFreeze = player.getTicksRequiredToFreeze(); 

                if (!isWarm) {
                    // Ванилла КАЖДЫЙ ТИК отнимает 2. Компенсируем это:
                    int freezeToAdd = 2;
                    
                    // Расчет интервала замерзания в зависимости от температуры:
                    // При -120°C: interval = 3 (быстро, добавляем +1 каждые 3 тика)
                    // При -20°C: interval = 30 (медленно, добавляем +1 каждые 30 тиков)
                    int interval = 3 + (int) (((currentTemp + 120.0) / 100.0) * 27);

                    // Избегаем деления на ноль или отрицательных значений на всякий случай
                    interval = Math.max(1, interval);

                    if (level.getGameTime() % interval == 0) {
                        freezeToAdd += 1;
                    }
                    
                    player.setTicksFrozen(Math.min(currentFreeze + freezeToAdd, maxFreeze + 40));
                } else {
                    // Игрок в тепле
                    player.setTicksFrozen(Math.max(currentFreeze - 3, 0));
                }
            }
        });
    }

    /**
     * Возвращает детерминированную температуру (от -120 до -20)
     * Сделан public static, чтобы команда могла легко к нему обращаться.
     */
    public static int getCurrentTemperature(Level level) {
        long day = level.getDayTime() / 24000L;
        
        // Синусоида с периодом в 10 игровых дней. Значение от -1.0 до 1.0
        double wave = Math.sin(day * (Math.PI * 2 / 10.0));
        
        // Средняя точка -70, амплитуда 50. Результат ровно от -120 до -20.
        return (int) (-70 + wave * 50);
    }

    /**
     * Проверяет, спасает ли предмет в руках от холода, учитывая температуру
     */
    private boolean isWarmItem(ItemStack stack, int currentTemp) {
        // Ведро лавы очень горячее, спасает всегда
        if (stack.is(Items.LAVA_BUCKET)) {
            return true;
        }

        // Факелы спасают ТОЛЬКО если температура ВЫШЕ -50 градусов (т.е. -49, -20 и т.д.)
        if (currentTemp > -50) {
            return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
        }

        return false;
    }
}