package uzat1k.frozen;

import net.minecraft.core.BlockPos;
import java.util.Set;

public interface IHeatChunk {
    Set<BlockPos> frostmod$getHeatSources();
    void frostmod$addHeatSource(BlockPos pos);
    void frostmod$removeHeatSource(BlockPos pos);
}