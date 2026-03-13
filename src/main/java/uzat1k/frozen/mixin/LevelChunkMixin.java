package uzat1k.frozen.mixin;

import uzat1k.frozen.IHeatChunk;

import uzat1k.frozen.TemperatureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void onSetBlockState(BlockPos pos, BlockState state, int isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (state != null) {
            // Теперь Миксин просто спрашивает у Демона, теплый ли это блок!
            if (TemperatureManager.isHeatSource(state)) {
                this.frostmod$addHeatSource(pos);
            } else {
                this.frostmod$removeHeatSource(pos);
            }
        }
    }
}