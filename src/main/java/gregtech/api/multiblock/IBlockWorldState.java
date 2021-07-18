package gregtech.api.multiblock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

public interface IBlockWorldState {

    World getWorld();

    BlockPos getPos();

    IBlockState getBlockState();

    IBlockState getOffsetState(EnumFacing face);

    TileEntity getTileEntity();

    IPatternMatchContext getMatchContext();

    IPatternMatchContext getLayerContext();

    // TODO move?
    static PatternCenterPredicate wrap(Predicate<IBlockWorldState> predicate) {
        return predicate::test;
    }
}
