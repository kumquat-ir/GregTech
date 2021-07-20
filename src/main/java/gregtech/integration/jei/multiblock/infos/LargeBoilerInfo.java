package gregtech.integration.jei.multiblock.infos;

import com.google.common.collect.Lists;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoilerNew;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class LargeBoilerInfo extends MultiblockInfoPage {

    public final MetaTileEntityLargeBoilerNew boiler;

    public LargeBoilerInfo(MetaTileEntityLargeBoilerNew boiler) {
        this.boiler = boiler;
    }

    @Override
    public MultiblockControllerBase getController() {
        return boiler;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
            .aisle("FXX", "CCC", "CCC", "CCC")
            .aisle("XXX", "SPC", "CPC", "CCC")
            .aisle("IXX", "COC", "CCC", "CCC")
            .where('S', boiler, EnumFacing.WEST)
            .where('P', boiler.boilerType.getPipeCasingState())
            .where('X', boiler.boilerType.getFireBoxState())
            .where('C', boiler.boilerType.getCasingState())
            .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.LV], EnumFacing.SOUTH)
            .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.LV], EnumFacing.WEST)
            .where('F', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.LV], EnumFacing.WEST)
            .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("gregtech.multiblock.large_boiler.description")};
    }
}
