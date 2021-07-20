package gregtech.common.metatileentities.multi.electric.generator;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.unification.material.Materials;
import gregtech.common.blocks.BlockMetalCasing.MetalCasingType;
import gregtech.common.blocks.BlockMultiblockCasing.MultiblockCasingType;
import gregtech.common.blocks.BlockTurbineCasing.TurbineCasingType;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;
import scala.Int;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

public class MetaTileEntityLargeCombustionEngine extends FueledMultiblockController {

    private final boolean isMk2;

    public MetaTileEntityLargeCombustionEngine(ResourceLocation metaTileEntityId) {
        this(metaTileEntityId, false);
    }

    public MetaTileEntityLargeCombustionEngine(ResourceLocation metaTileEntityId, boolean isMk2) {
        super(metaTileEntityId, RecipeMaps.COMBUSTION_GENERATOR_FUELS, GTValues.V[isMk2 ? GTValues.IV : GTValues.EV]);
        this.isMk2 = isMk2;
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        return new LargeCombustionEngineWorkableHandler(this, recipeMap, () -> energyContainer, () -> importFluidHandler, maxVoltage, isMk2);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeCombustionEngine(metaTileEntityId, isMk2);
    }

    @Override // todo
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            FluidStack lubricantStack = importFluidHandler.drain(Materials.Lubricant.getFluid(Integer.MAX_VALUE), false);
            FluidStack oxygenStack = importFluidHandler.drain(Materials.Oxygen.getFluid(Integer.MAX_VALUE), false);
            FluidStack loxStack = importFluidHandler.drain(Materials.LiquidOxygen.getFluid(Integer.MAX_VALUE), false);
            FluidStack fuelStack = ((LargeCombustionEngineWorkableHandler) workableHandler).getFuelStack();
            int lubricantAmount = lubricantStack == null ? 0 : lubricantStack.amount;
            int oxygenAmount = oxygenStack == null ? 0 : oxygenStack.amount;
            int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

            ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_combustion_engine.lubricant_amount", lubricantAmount));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_combustion_engine.fuel_amount", fuelAmount, fuelName));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_combustion_engine.oxygen_amount", oxygenAmount));
            textList.add(new TextComponentTranslation(oxygenAmount >= 2 ? "gregtech.multiblock.large_combustion_engine.oxygen_boosted" : "gregtech.multiblock.large_combustion_engine.supply_oxygen_to_boost"));
        }
        super.addDisplayText(textList);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
            .aisle("XXX", "XDX", "XXX")
            .aisle("XHX", "HGH", "XHX")
            .aisle("XHX", "HGH", "XHX")
            .aisle("AAA", "AYA", "AAA")
            .where('X', statePredicate(getCasingState()))
            .where('G', statePredicate(gearboxPredicate()))
            .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS)))
            .where('D', abilityPartPredicate(MultiblockAbility.OUTPUT_ENERGY))
            .where('A', intakeCasingPredicate())
            .where('Y', selfPredicate())
            .build();
    }

    public IBlockState getCasingState() {
        return isMk2 ?
                MetaBlocks.METAL_CASING.getState(MetalCasingType.TUNGSTENSTEEL_ROBUST) :
                MetaBlocks.METAL_CASING.getState(MetalCasingType.TITANIUM_STABLE);
    }

    protected Predicate<BlockWorldState> intakeCasingPredicate() {
        IBlockState blockState = isMk2 ?
                MetaBlocks.MULTIBLOCK_CASING.getState(MultiblockCasingType.ENGINE_INTAKE_CASING_MK2) :
                MetaBlocks.MULTIBLOCK_CASING.getState(MultiblockCasingType.ENGINE_INTAKE_CASING);
        return blockWorldState -> {
            if (blockWorldState.getBlockState() != blockState)
                return false;
            IBlockState offsetState = blockWorldState.getOffsetState(getFrontFacing());
            return offsetState.getBlock().isAir(offsetState, blockWorldState.getWorld(), blockWorldState.getPos());
        };
    }

    protected IBlockState gearboxPredicate() {
        return isMk2 ?
                MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TUNGSTENSTEEL_GEARBOX) :
                MetaBlocks.TURBINE_CASING.getState(TurbineCasingType.TITANIUM_GEARBOX);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return isMk2 ?
                Textures.ROBUST_TUNGSTENSTEEL_CASING :
                Textures.STABLE_TITANIUM_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.DIESEL_ENGINE_OVERLAY;
    }
}
