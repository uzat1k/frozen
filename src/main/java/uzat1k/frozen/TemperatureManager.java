package uzat1k.frozen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class TemperatureManager {

    // Глобальная температура (меняется изо дня в день)
    public static int getWorldTemperature(Level level) {
        long currentDay = level.getDayTime() / 24000L;
        return (int) (-70.0 + 50.0 * Math.sin(currentDay * (Math.PI / 4.0)));
    }

    // Тот самый запрос за O(1) для кубика 4x4x4
    public static int getLocalTemperature(Level level, BlockPos pos) {
        // Округляем координаты игрока до центра его секции 4x4x4
        BlockPos cubeCenter = new BlockPos(
            ((pos.getX() >> 2) << 2) + 2,
            ((pos.getY() >> 2) << 2) + 2,
            ((pos.getZ() >> 2) << 2) + 2
        );

        int worldTemp = getWorldTemperature(level);
        int addedHeat = 0;

        int chunkX = cubeCenter.getX() >> 4;
        int chunkZ = cubeCenter.getZ() >> 4;

        // Запрашиваем тепло вокруг кубика
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunkAccess = level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                if (chunkAccess instanceof IHeatChunk heatChunk) {
                    for (BlockPos heatPos : heatChunk.frostmod$getHeatSources()) {
                        BlockState state = level.getBlockState(heatPos);
                        double dist = Math.sqrt(cubeCenter.distSqr(heatPos));

                        // Градиентное тепло: чем ближе к центру кубика, тем жарче!
                        if (state.is(Blocks.LAVA) && dist <= 16) {
                            addedHeat += (int)(100 * (1.0 - dist / 16.0));
                        } else if (state.is(Blocks.FURNACE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) && dist <= 12) {
                            addedHeat += (int)(60 * (1.0 - dist / 12.0));
                        } else if (state.is(Blocks.CAMPFIRE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) && dist <= 8) {
                            addedHeat += (int)(50 * (1.0 - dist / 8.0));
                        } else if ((state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) && dist <= 4) {
                            addedHeat += (int)(30 * (1.0 - dist / 4.0));
                        }
                    }
                }
            }
        }
        return worldTemp + addedHeat;
    }

    // Сканер табличек вокруг игрока
    public static void updateThermometersNearPlayer(Level level, BlockPos playerPos) {
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunkAccess = level.getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                // Если чанк полностью загружен, ищем в нем таблички
                if (chunkAccess instanceof LevelChunk chunk) {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be instanceof SignBlockEntity sign) {
                            processSign(sign, level);	
                        }
                    }
                }
            }
        }
    }

    private static void processSign(SignBlockEntity sign, Level level) {
        // Читаем первую строчку таблички
        String line0 = sign.getFrontText().getMessage(0, false).getString().toLowerCase();
        
        // Если игрок написал слово-триггер
        if (line0.contains("[термометр]") || line0.contains("[temp]")) {
            int local = getLocalTemperature(level, sign.getBlockPos());
            int world = getWorldTemperature(level);

            // Красиво переписываем табличку
            sign.updateText(text -> text
                .setMessage(0, Component.literal("§9[Термометр]"))
                .setMessage(1, Component.literal("§7В комнате: §c" + local + "°C"))
                .setMessage(2, Component.literal("§7На улице: §b" + world + "°C"))
                .setMessage(3, Component.literal("")), true);
                
            // Отправляем пакет обновления клиенту, чтобы текст изменился на глазах
            level.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
        }
    }
 // Универсальный метод проверки блока на тепло
    public static boolean isHeatSource(BlockState state) {
        if (state.is(Blocks.LAVA)) return true;
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) return true;
        if (state.is(Blocks.CAMPFIRE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) return true;
        if (state.is(Blocks.FURNACE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) return true;
        return false;
    }
}
