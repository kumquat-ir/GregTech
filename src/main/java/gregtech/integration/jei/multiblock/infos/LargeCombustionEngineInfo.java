package gregtech.integration.jei.multiblock.infos;

import com.google.common.collect.Lists;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMetalCasing.MetalCasingType;
import gregtech.common.blocks.BlockMultiblockCasing.MultiblockCasingType;
import gregtech.common.blocks.BlockTurbineCasing.TurbineCasingType;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class LargeCombustionEngineInfo extends MultiblockInfoPage {

    private final boolean isMk2;

    public LargeCombustionEngineInfo(boolean isMk2) {
        this.isMk2 = isMk2;
    }

    @Override
    public MultiblockControllerBase getController() {
        return isMk2 ?
                MetaTileEntities.EXTREME_COMBUSTION_ENGINE :
                MetaTileEntities.LARGE_COMBUSTION_ENGINE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("AAA", "ACA", "AAA")
                .aisle("HHH", "HGH", "HHH")
                .aisle("HHH", "FGH", "HHH")
                .aisle("HHH", "HEH", "HHH")
                .where('H', getCasingState())
                .where('G', getGearboxState())
                .where('A', getEngineState())
                .where('C', isMk2 ? MetaTileEntities.EXTREME_COMBUSTION_ENGINE : MetaTileEntities.LARGE_COMBUSTION_ENGINE, EnumFacing.NORTH)
                .where('F', MetaTileEntities.FLUID_IMPORT_HATCH[isMk2 ? GTValues.IV : GTValues.EV], EnumFacing.WEST)
                .where('E', MetaTileEntities.ENERGY_OUTPUT_HATCH[isMk2 ? GTValues.IV : GTValues.EV], EnumFacing.SOUTH)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    private IBlockState getCasingState() {
        return isMk2 ?
                MetaBlocks.METAL_CASING.getState(MetalCasingType.TUNGSTENSTEEL_ROBUST) :
                MetaBlocks.METAL_CASING.getState(MetalCasingType.TITANIUM_STABLE);
    }

    private IBlockState getGearboxState() {
        return isMk2 ?
                MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TUNGSTENSTEEL_GEARBOX) :
                MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TITANIUM_GEARBOX);
    }

    private IBlockState getEngineState() {
        return isMk2 ?
                MetaBlocks.MULTIBLOCK_CASING.getState(MultiblockCasingType.ENGINE_INTAKE_CASING_MK2) :
                MetaBlocks.MULTIBLOCK_CASING.getState(MultiblockCasingType.ENGINE_INTAKE_CASING);
    }

    @Override
    public String[] getDescription() {
        return isMk2 ?
                new String[]{I18n.format("gregtech.multiblock.extreme_combustion_engine.description")} :
                new String[]{I18n.format("gregtech.multiblock.large_combustion_engine.description")};
    }
}
