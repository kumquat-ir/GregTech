package gregtech.api.multiblock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class MutableBlockWorldState implements IBlockWorldState {

    private final World world;
    private final IPatternMatchContext matchContext;
    private final IPatternMatchContext layerContext;

    private BlockPos pos;
    private IBlockState state;
    private TileEntity tileEntity;
    private boolean tileEntityFetched; // TODO is this needed?

    public MutableBlockWorldState(World world, IPatternMatchContext matchContext, IPatternMatchContext layerContext) {
        this.world = world;
        this.matchContext = matchContext;
        this.layerContext = layerContext;
    }

    public void update(BlockPos posIn) {
        this.pos = posIn;
        this.state = world.getBlockState(pos);
        this.tileEntity = null;
        this.tileEntityFetched = false;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BlockPos getPos() {
        return pos.toImmutable();
    }

    @Override
    public IBlockState getBlockState() {
        return state;
    }

    @Override
    public IBlockState getOffsetState(EnumFacing face) {
        if (pos instanceof MutableBlockPos) {
            ((MutableBlockPos) pos).move(face);
            IBlockState blockState = world.getBlockState(pos);
            ((MutableBlockPos) pos).move(face.getOpposite());
            return blockState;
        }
        return world.getBlockState(pos.offset(face));
    }

    @Override
    public TileEntity getTileEntity() {
        if (tileEntity == null && !tileEntityFetched) {
            this.tileEntity = world.getTileEntity(pos);
            this.tileEntityFetched = true;
        }
        return tileEntity;
    }

    @Override
    public IPatternMatchContext getMatchContext() {
        return matchContext;
    }

    @Override
    public IPatternMatchContext getLayerContext() {
        return layerContext;
    }
}
