package name.modid.mixin;

import name.modid.IHeatChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements IHeatChunk {
    @Unique
    private final Set<BlockPos> frostmod$heatSources = new HashSet<>();

    @Override
    public Set<BlockPos> frostmod$getHeatSources() {
        return this.frostmod$heatSources;
    }

    @Override
    public void frostmod$addHeatSource(BlockPos pos) {
        this.frostmod$heatSources.add(pos.immutable());
    }

    @Override
    public void frostmod$removeHeatSource(BlockPos pos) {
        this.frostmod$heatSources.remove(pos);
    }

    // Внедряемся прямо в метод изменения блоков внутри самого чанка!
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void onSetBlockState(BlockPos pos, BlockState state, int isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (state != null) {
            if (isHeatSource(state)) {
                this.frostmod$addHeatSource(pos);
            } else {
                this.frostmod$removeHeatSource(pos);
            }
        }
    }

    @Unique
    private boolean isHeatSource(BlockState state) {
        if (state.is(Blocks.LAVA)) return true;
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) return true;
        if (state.is(Blocks.CAMPFIRE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) return true;
        if (state.is(Blocks.FURNACE) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) return true;
        return false;
    }
}