package gregtech.api.multiblock;

import com.google.common.collect.ImmutableList;
import gregtech.api.util.IntRange;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class BlockPattern {

    private final Predicate<IBlockWorldState>[][][] blockMatches; //[z][y][x]
    private final Int2ObjectMap<Predicate<IPatternMatchContext>> layerMatchers;
    private final List<Predicate<IPatternMatchContext>> validators;
    private final int fingerLength; //z size
    private final int thumbLength; //y size
    private final int palmLength; //x size
    private final RelativeDirection[] structureDir;
    private final int[][] aisleRepetitions;
    private final List<Pair<Predicate<IBlockWorldState>, IntRange>> countMatches;

    // x, y, z, minZ, maxZ
    private int[] centerOffset = null;

    public BlockPattern(Predicate<IBlockWorldState>[][][] predicatesIn,
                        List<Pair<Predicate<IBlockWorldState>, IntRange>> countMatches,
                        Int2ObjectMap<Predicate<IPatternMatchContext>> layerMatchers,
                        List<Predicate<IPatternMatchContext>> validators,
                        RelativeDirection[] structureDir,
                        int[][] aisleRepetitions) {
        this.blockMatches = predicatesIn;
        this.countMatches = ImmutableList.copyOf(countMatches);
        this.layerMatchers = new Int2ObjectOpenHashMap<>(layerMatchers);
        this.validators = ImmutableList.copyOf(validators);
        this.fingerLength = predicatesIn.length;

        if (this.fingerLength > 0) {
            this.thumbLength = predicatesIn[0].length;

            if (this.thumbLength > 0) {
                this.palmLength = predicatesIn[0][0].length;
            } else {
                this.palmLength = 0;
            }
        } else {
            this.thumbLength = 0;
            this.palmLength = 0;
        }
        this.structureDir = structureDir;
        this.aisleRepetitions = aisleRepetitions;

        initializeCenterOffsets();
    }

    private void initializeCenterOffsets() {
        loop:
        for (int x = 0; x < this.palmLength; x++) {
            for (int y = 0; y < this.thumbLength; y++) {
                for (int z = 0, minZ = 0, maxZ = 0; z < this.fingerLength; minZ += aisleRepetitions[z][0], maxZ += aisleRepetitions[z][1], z++) {
                    Predicate<IBlockWorldState> predicate = this.blockMatches[z][y][x];
                    if (predicate instanceof PatternCenterPredicate) {
                        centerOffset = new int[]{x, y, z, minZ, maxZ};
                        break loop;
                    }
                }
            }
        }
        if (centerOffset == null) {
            throw new IllegalArgumentException("Didn't found center predicate");
        }
    }

    public int getFingerLength() {
        return this.fingerLength;
    }

    public int getThumbLength() {
        return this.thumbLength;
    }

    public int getPalmLength() {
        return this.palmLength;
    }

    public IPatternMatchContext checkPatternAt(World world, BlockPos centerPos, EnumFacing facing) {
        MutablePatternMatchContext matchContext = new MutablePatternMatchContext();
        MutablePatternMatchContext layerContext = new MutablePatternMatchContext();

        MutableBlockWorldState worldState = new MutableBlockWorldState(world, matchContext, layerContext);
        MutableBlockPos blockPos = new MutableBlockPos();

        int[] countMatchesCache = new int[countMatches.size()];
        boolean findFirstAisle = false;
        int minZ = -centerOffset[4];

        //Checking aisles
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            //Checking repeatable slices
            loop:
            for (r = 0; (findFirstAisle ? r < aisleRepetitions[c][1] : z <= -centerOffset[3]); r++) {
                //Checking single slice
                layerContext.resetContext();

                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        Predicate<IBlockWorldState> predicate = this.blockMatches[c][b][a];
                        setActualRelativeOffset(blockPos, x, y, z, facing);
                        blockPos.setPos(blockPos.getX() + centerPos.getX(), blockPos.getY() + centerPos.getY(), blockPos.getZ() + centerPos.getZ());
                        worldState.update(blockPos);

                        if (!predicate.test(worldState)) {
                            if (findFirstAisle) {
                                if (r < aisleRepetitions[c][0]) {//retreat to see if the first aisle can start later
                                    r = c = 0;
                                    z = minZ++;
                                    matchContext.resetContext();
                                    findFirstAisle = false;
                                }
                            } else {
                                //continue searching for the first aisle
                                z++;
                            }
                            continue loop;
                        }
                        for (int i = 0; i < countMatchesCache.length; i++) {
                            if (countMatches.get(i).getLeft().test(worldState)) {
                                countMatchesCache[i]++;
                            }
                        }
                    }
                }
                findFirstAisle = true;
                z++;

                //Check layer-local matcher predicate
                Predicate<IPatternMatchContext> layerPredicate = layerMatchers.get(c);
                if (layerPredicate != null && !layerPredicate.test(layerContext)) {
                    return null;
                }
            }
            //Repetitions out of range
            if (r < aisleRepetitions[c][0]) {
                return null;
            }
        }

        //Check count matches amount
        for (int i = 0; i < countMatchesCache.length; i++) {
            IntRange intRange = countMatches.get(i).getRight();
            if (!intRange.isInsideOf(countMatchesCache[i])) {
                return null; //count matches didn't match
            }
        }

        //Check general match predicates
        for (Predicate<IPatternMatchContext> validator : validators) {
            if (!validator.test(matchContext)) {
                return null;
            }
        }

        return matchContext;
    }

    private void setActualRelativeOffset(MutableBlockPos pos, int x, int y, int z, EnumFacing facing) {
        int[] c0 = new int[]{x, y, z}, c1 = new int[3];
        for (int i = 0; i < 3; i++) {
            switch (structureDir[i].getActualFacing(facing)) {
                case UP: c1[1] = c0[i]; break;
                case DOWN: c1[1] = -c0[i]; break;
                case WEST: c1[0] = -c0[i]; break;
                case EAST: c1[0] = c0[i]; break;
                case NORTH: c1[2] = -c0[i]; break;
                case SOUTH: c1[2] = c0[i]; break;
            }
        }
        pos.setPos(c1[0], c1[1], c1[2]);
    }

    /**
     * Relative direction when facing horizontally
     */
    public enum RelativeDirection {
        UP(f -> EnumFacing.UP),
        DOWN(f -> EnumFacing.DOWN),
        LEFT(EnumFacing::rotateYCCW),
        RIGHT(EnumFacing::rotateY),
        FRONT(Function.identity()),
        BACK(EnumFacing::getOpposite);

        Function<EnumFacing, EnumFacing> actualFacing;

        RelativeDirection(Function<EnumFacing, EnumFacing> actualFacing) {
            this.actualFacing = actualFacing;
        }

        public EnumFacing getActualFacing(EnumFacing facing) {
            return actualFacing.apply(facing);
        }
    }
}
