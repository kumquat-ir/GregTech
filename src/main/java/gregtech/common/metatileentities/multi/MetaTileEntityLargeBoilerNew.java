package gregtech.common.metatileentities.multi;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IFuelInfo;
import gregtech.api.capability.IFuelable;
import gregtech.api.capability.impl.FluidFuelInfo;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemFuelInfo;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.tool.ISoftHammerItem;
import gregtech.api.gui.Widget.ClickData;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import gregtech.common.blocks.BlockFireboxCasing;
import gregtech.common.tools.DamageValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nonnull;
import java.util.*;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withHoverTextTranslate;

public class MetaTileEntityLargeBoilerNew extends MultiblockWithDisplayBase implements IFuelable {

    private static final int BOILING_TEMPERATURE = 100;
    public final LargeBoilerType boilerType;
    private int currentTemperature;
    private int fuelBurnTicksLeft;
    private int throttlePercentage = 100;
    private boolean isActive;
    private boolean wasActiveAndNeedsUpdate;
    private boolean hasNoWater;
    private int lastTickSteamOutput;
    private FluidTankList fluidImportInventory;
    private ItemHandlerList itemImportInventory;
    private FluidTankList steamOutputTank;

    private int EUtGenerated = 1;

    private int temperatureIncrease = 0;
    private int superTemperatureIncrease = 0;
    private int excessWater = 0; //Eliminate rounding errors for water
    private int excessFuel = 0; //Eliminate rounding errors for fuels that burn half items

    public MetaTileEntityLargeBoilerNew(ResourceLocation metaTileEntityId, LargeBoilerType boilerType) {
        super(metaTileEntityId);
        this.boilerType = boilerType;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeBoilerNew(metaTileEntityId, boilerType);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.fluidImportInventory = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.itemImportInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.steamOutputTank = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.fluidImportInventory = new FluidTankList(true);
        this.itemImportInventory = new ItemHandlerList(Collections.emptyList());
        this.steamOutputTank = new FluidTankList(true);
        this.currentTemperature = 0; //reset temperature
        this.fuelBurnTicksLeft = 0;
        this.hasNoWater = false;
        this.isActive = false;
        this.throttlePercentage = 100;
        this.EUtGenerated = 0;
        this.temperatureIncrease = 0;
        this.superTemperatureIncrease = 0;
        this.excessWater = 0;
        this.excessFuel = 0;
        replaceFireboxAsActive(false);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (!getWorld().isRemote && isStructureFormed()) {
            replaceFireboxAsActive(false);
        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.temperature", currentTemperature, boilerType.getMaxTemperature()));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.steam_output", lastTickSteamOutput, this.EUtGenerated));

            ITextComponent throttleText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle", throttlePercentage, (int) (getThrottleEfficiency() * 100));
            withHoverTextTranslate(throttleText, "gregtech.multiblock.large_boiler.throttle.tooltip");
            textList.add(throttleText);

            ITextComponent buttonText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle_modify");
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[-]"), "sub"));
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[+]"), "add"));
            textList.add(buttonText);
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, ClickData clickData) {
        super.handleDisplayClick(componentData, clickData);
        int modifier = componentData.equals("add") ? 1 : -1;
        int result = (clickData.isShiftClick ? 1 : 5) * modifier;
        this.throttlePercentage = MathHelper.clamp(throttlePercentage + result, 20, 100);
    }

    @Override
    protected void updateFormedValid() {
        if (this.EUtGenerated > 0) {
            if (this.superTemperatureIncrease > 0) {
                //todo 6 - 6 is supposed to be maximum maintenance problems fixed - current maintenance problems fixed
                this.currentTemperature = Math.max(currentTemperature + superTemperatureIncrease, boilerType.getMaxTemperature() - ((6 - 6) * 1000));
            }

            // Manage temperature and fuel burn time
            if (fuelBurnTicksLeft > 0 && currentTemperature < boilerType.getMaxTemperature()) {
                --this.fuelBurnTicksLeft;
                if (getOffsetTimer() % 20 == 0) {
                    this.currentTemperature = Math.min(this.currentTemperature + this.temperatureIncrease, boilerType.getMaxTemperature());
                }
                if (fuelBurnTicksLeft == 0) {
                    this.wasActiveAndNeedsUpdate = true;
                }
            } else if (currentTemperature > 0 && getOffsetTimer() % 20 == 0) {
                this.currentTemperature -= 20;
            }

            int generatedEU = this.EUtGenerated * 2 * this.currentTemperature / boilerType.getMaxTemperature();
            if (generatedEU > 0) {


                // water usage
                long amount = (generatedEU + GTValues.STEAM_PER_WATER) / GTValues.STEAM_PER_WATER;
                excessWater += amount * GTValues.STEAM_PER_WATER - generatedEU;
                amount -= excessWater / GTValues.STEAM_PER_WATER;
                excessWater %= GTValues.STEAM_PER_WATER;

                boolean doWaterDrain = getOffsetTimer() % 20 == 0;
                FluidStack drainedWater = null;
                for (FluidStack fluidStack : RecipeMaps.BOILER_WATER_FLUIDS) {
                    drainedWater = fluidImportInventory.drain(new FluidStack(fluidStack.getFluid(), (int) amount), doWaterDrain);
                }

                // check temperature, if no water and high temp -> explode
                if (drainedWater != null && drainedWater.amount > 0) {
                    if (currentTemperature > BOILING_TEMPERATURE && hasNoWater) {
                        getWorld().setBlockToAir(getPos());
                        if (ConfigHolder.doExplosions ){
                            float explosionPower = currentTemperature / (float) BOILING_TEMPERATURE * 2.0f;
                            getWorld().createExplosion(null, getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                                    explosionPower, true);
                        }
                        GTLog.logger.info("Boiler at: [" + getPos().getX() + ", " + getPos().getY() + ", " + getPos().getZ() + "] had no water!");
                    }
                    this.hasNoWater = false;

                    // output steam
                    if (currentTemperature >= BOILING_TEMPERATURE) {
                        FluidStack steamStack = ModHandler.getSteam(generatedEU);
                        steamOutputTank.fill(steamStack, true);
                        this.lastTickSteamOutput = generatedEU;
                    }
                } else {
                    this.hasNoWater = true;
                }
            }


        }

        // consume more fuel if necessary
        if (fuelBurnTicksLeft == 0) {
            int fuelMaxBurnTime = setupRecipeAndConsumeInputs();
            if (fuelMaxBurnTime > 0) {
                this.fuelBurnTicksLeft = fuelMaxBurnTime;
                System.out.println("fuelBurnTicksLeft " + this.fuelBurnTicksLeft);
                System.out.println("EUGenerated " + this.EUtGenerated);
                if (wasActiveAndNeedsUpdate) {
                    this.wasActiveAndNeedsUpdate = false;
                } else setActive(true);
                markDirty();
            }
        }

        if (wasActiveAndNeedsUpdate) {
            this.wasActiveAndNeedsUpdate = false;
            setActive(false);
        }
    }

    private int setupRecipeAndConsumeInputs() {
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);

            if (fuelStack == null || GTUtility.isValidWaterFluid(fuelStack))
                continue; // ignore empty tanks and non-valid water

            // Check Combustion Generator Fuels
            FuelRecipe dieselRecipe = RecipeMaps.COMBUSTION_GENERATOR_FUELS.findRecipe(GTValues.V[GTValues.MAX], fuelStack);
            int fuelAmountToConsume = 1000;

            if (dieselRecipe != null) {
                if (fuelStack.amount >= fuelAmountToConsume) {
                    fluidTank.drain(fuelAmountToConsume, true);

                    // total duration for fuelAmountToConsume worth of fuel
                    int dieselRecipeTotalDuration = dieselRecipe.getDuration() / dieselRecipe.getRecipeFluid().amount * fuelAmountToConsume;

                    // save EUt generated
                    this.EUtGenerated = boilerType.getEUt();

                    // Boiler Specific Duration Multiplier * Throttle Percentage * Diesel Fuel Duration for Fuel Amount To Consume / 2
                    int duration = (int) Math.max(1, this.boilerType.getDurationMultiplier() * getThrottleMultiplier() * dieselRecipeTotalDuration / 2);

                    // save efficiency
                    this.temperatureIncrease = duration / 20 * boilerType.getBoilerTemperatureIncrease() * 4 / 1000;
                    System.out.println("temperatureIncrease " + this.temperatureIncrease);

                    return duration;

                } else continue;
            }

            // Check Semi Fluid Generator Fuels
            FuelRecipe denseFuelRecipe = RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.findRecipe(GTValues.V[GTValues.MAX], fuelStack);
            if (denseFuelRecipe != null) {
                if (fuelStack.amount >= fuelAmountToConsume) {
                    fluidTank.drain(fuelAmountToConsume, true);

                    // total duration for fuelAmountToConsume worth of fuel
                    int semiFluidRecipeTotalDuration = denseFuelRecipe.getDuration() / denseFuelRecipe.getRecipeFluid().amount * fuelAmountToConsume;

                    // save EUt generated
                    this.EUtGenerated = boilerType.getEUt();

                    // Boiler Specific Duration Multiplier * Throttle Percentage * Semi Fluid Fuel Duration for Fuel Amount To Consume * 2
                    int duration = (int) Math.max(1, this.boilerType.getDurationMultiplier() * getThrottleMultiplier() * semiFluidRecipeTotalDuration * 2);

                    // save temperature increase
                    this.temperatureIncrease = duration * boilerType.getBoilerTemperatureIncrease() * 4 / 100;

                    return duration;

                }
            }
        }
        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            ItemStack itemStack = itemImportInventory.getStackInSlot(slotIndex);

            int solidFuelDuration = TileEntityFurnace.getItemBurnTime(itemStack);

            if (solidFuelDuration / 80 > 0) {

                if (itemStack.getCount() == 1) {
                    ItemStack containerItem = itemStack.getItem().getContainerItem(itemStack);
                    itemImportInventory.setStackInSlot(slotIndex, containerItem);
                } else {
                    itemStack.shrink(1);
                    itemImportInventory.setStackInSlot(slotIndex, itemStack);
                }

                // store excess fuel
                this.excessFuel += solidFuelDuration % 80;
                solidFuelDuration += this.excessFuel / 80;
                this.excessFuel %= 80;

                // Solid Fuel Burn Time / 80 * Boiler Specific Duration Multiplier * Throttle Percentage
                solidFuelDuration *= this.boilerType.getDurationMultiplier() * getThrottleMultiplier();

                // apply super increase
                // save efficiency
                this.temperatureIncrease = solidFuelDuration * boilerType.getBoilerTemperatureIncrease();
                if (this.temperatureIncrease > 5000) {
                    this.temperatureIncrease = 0;
                    this.superTemperatureIncrease = 20;
                }

                // save EUt generated
                this.EUtGenerated = boilerType.getEUt();

                return solidFuelDuration;
            }
        }
        this.EUtGenerated = 0;
        return 0;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("CurrentTemperature", currentTemperature);
        data.setInteger("FuelBurnTicksLeft", fuelBurnTicksLeft);
        data.setBoolean("HasNoWater", hasNoWater);
        data.setInteger("ThrottlePercentage", throttlePercentage);
        data.setInteger("EUtGenerated", EUtGenerated);
        data.setInteger("TemperatureIncrease", temperatureIncrease);
        data.setInteger("SuperTemperatureIncrease", superTemperatureIncrease);
        data.setInteger("ExcessWater", excessWater);
        data.setInteger("ExcessFuel", excessFuel);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.currentTemperature = data.getInteger("CurrentTemperature");
        this.fuelBurnTicksLeft = data.getInteger("FuelBurnTicksLeft");
        this.hasNoWater = data.getBoolean("HasNoWater");
        this.EUtGenerated = data.getInteger("EUtGenerated");
        this.temperatureIncrease = data.getInteger("TemperatureIncrease");
        this.superTemperatureIncrease = data.getInteger("SuperTemperatureIncrease");
        this.excessWater = data.getInteger("ExcessWater");
        this.excessFuel = data.getInteger("ExcessFuel");
        if (data.hasKey("ThrottlePercentage")) {
            this.throttlePercentage = data.getInteger("ThrottlePercentage");
        }
        this.isActive = fuelBurnTicksLeft > 0;
    }

    private void setActive(boolean active) {
        this.isActive = active;
        if (!getWorld().isRemote) {
            if (isStructureFormed()) {
                replaceFireboxAsActive(active);
            }
            writeCustomData(100, buf -> buf.writeBoolean(isActive));
            markDirty();
        }
    }

    private double getThrottleMultiplier() {
        return throttlePercentage / 100.0;
    }

    private double getThrottleEfficiency() {
        return MathHelper.clamp(1.0 + 0.3 * Math.log(getThrottleMultiplier()), 0.4, 1.0);
    }

    private void replaceFireboxAsActive(boolean isActive) {
        BlockPos centerPos = getPos().offset(getFrontFacing().getOpposite()).down();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos blockPos = centerPos.add(x, 0, z);
                IBlockState blockState = getWorld().getBlockState(blockPos);
                if (blockState.getBlock() instanceof BlockFireboxCasing) {
                    blockState = blockState.withProperty(BlockFireboxCasing.ACTIVE, isActive);
                    getWorld().setBlockState(blockPos, blockState);
                }
            }
        }
    }

    @Override
    public int getLightValueForPart(IMultiblockPart sourcePart) {
        return sourcePart == null ? 0 : (isActive ? 15 : 0);
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 100) {
            this.isActive = buf.readBoolean();
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return boilerType == null ? null : FactoryBlockPattern.start()
                .aisle("XXX", "CCC", "CCC", "CCC")
                .aisle("XXX", "CPC", "CPC", "CCC")
                .aisle("XXX", "CSC", "CCC", "CCC")
                .setAmountAtLeast('X', 4)
                .setAmountAtLeast('C', 20)
                .where('S', selfPredicate())
                .where('P', statePredicate(boilerType.getPipeCasingState()))
                .where('X', state -> statePredicate(GTUtility.getAllPropertyValues(boilerType.getFireBoxState(), BlockFireboxCasing.ACTIVE))
                        .or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS)).test(state))
                .where('C', statePredicate(boilerType.getCasingState()).or(abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS)))
                .build();
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return boilerType.getFrontOverlay();
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        //noinspection SuspiciousMethodCalls
        int importFluidsSize = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        //noinspection SuspiciousMethodCalls
        return importFluidsSize >= 1 && (importFluidsSize >= 2 ||
                abilities.containsKey(MultiblockAbility.IMPORT_ITEMS)) &&
                abilities.containsKey(MultiblockAbility.EXPORT_FLUIDS);
    }

    private boolean isFireboxPart(IMultiblockPart sourcePart) {
        return isStructureFormed() && (((MetaTileEntity) sourcePart).getPos().getY() < getPos().getY());
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        if (sourcePart != null && isFireboxPart(sourcePart)) {
            return isActive ? boilerType.getFirefoxActiveRenderer() : boilerType.getFireboxIdleRenderer();
        }
        return boilerType.getSolidCasingRenderer();
    }

    @Override
    public boolean shouldRenderOverlay(IMultiblockPart sourcePart) {
        return sourcePart == null || !isFireboxPart(sourcePart);
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ItemStack itemStack = playerIn.getHeldItem(hand);
        if (!itemStack.isEmpty() && itemStack.hasCapability(GregtechCapabilities.CAPABILITY_MALLET, null)) {
            ISoftHammerItem softHammerItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_MALLET, null);

            if (getWorld().isRemote) {
                return true;
            }
            if (!softHammerItem.damageItem(DamageValues.DAMAGE_FOR_SOFT_HAMMER, false)) {
                return false;
            }
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        T result = super.getCapability(capability, side);
        if (result != null)
            return result;
        if (capability == GregtechCapabilities.CAPABILITY_FUELABLE) {
            return GregtechCapabilities.CAPABILITY_FUELABLE.cast(this);
        }
        return null;
    }

    @Override
    public Collection<IFuelInfo> getFuels() {
        if (!isStructureFormed())
            return Collections.emptySet();
        final LinkedHashMap<Object, IFuelInfo> fuels = new LinkedHashMap<Object, IFuelInfo>();
        int fluidCapacity = 0; // fluid capacity is all non water tanks
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (!GTUtility.isValidWaterFluid(fuelStack))
                fluidCapacity += fluidTank.getCapacity();
        }
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (fuelStack == null || GTUtility.isValidWaterFluid(fuelStack))
                continue;

            int fuelAmountToConsume = 1000;
            int duration = 0;

            FuelRecipe dieselRecipe = RecipeMaps.COMBUSTION_GENERATOR_FUELS.findRecipe(GTValues.V[GTValues.MAX], fuelStack);
            if (dieselRecipe != null) {
                // total duration for fuelAmountToConsume worth of fuel
                int dieselRecipeTotalDuration = dieselRecipe.getDuration() / dieselRecipe.getRecipeFluid().amount * fuelAmountToConsume;

                // Boiler Specific Duration Multiplier * Throttle Percentage * Diesel Fuel Duration for Fuel Amount To Consume / 2
                duration = (int) Math.max(1, this.boilerType.getDurationMultiplier() * getThrottleMultiplier() * dieselRecipeTotalDuration / 2);
            }

            FuelRecipe denseFuelRecipe = RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.findRecipe(GTValues.V[GTValues.MAX], fuelStack);
            if (denseFuelRecipe != null) {
                // total duration for fuelAmountToConsume worth of fuel
                int semiFluidRecipeTotalDuration = denseFuelRecipe.getDuration() / denseFuelRecipe.getRecipeFluid().amount * fuelAmountToConsume;

                // Boiler Specific Duration Multiplier * Throttle Percentage * Semi Fluid Fuel Duration for Fuel Amount To Consume * 2
                duration = (int) Math.max(1, this.boilerType.getDurationMultiplier() * getThrottleMultiplier() * semiFluidRecipeTotalDuration * 2);
            }

            FluidFuelInfo fluidFuelInfo = (FluidFuelInfo) fuels.get(fuelStack.getUnlocalizedName());
            if (fluidFuelInfo == null) {
                fluidFuelInfo = new FluidFuelInfo(fuelStack, fuelStack.amount, fluidCapacity, fuelAmountToConsume, duration);
                fuels.put(fuelStack.getUnlocalizedName(), fluidFuelInfo);
            } else {
                fluidFuelInfo.addFuelRemaining(fuelStack.amount);
                fluidFuelInfo.addFuelBurnTime(duration);
            }
        }
        int itemCapacity = 0; // item capacity is all slots
        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            itemCapacity += itemImportInventory.getSlotLimit(slotIndex);
        }

        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            ItemStack itemStack = itemImportInventory.getStackInSlot(slotIndex);

            int solidFuelDuration = TileEntityFurnace.getItemBurnTime(itemStack);

            if (solidFuelDuration / 80 > 0) {

                // store excess fuel
                this.excessFuel += solidFuelDuration % 80;
                solidFuelDuration += this.excessFuel / 80;
                this.excessFuel %= 80;

                // Solid Fuel Burn Time / 80 * Boiler Specific Duration Multiplier * Throttle Percentage
                solidFuelDuration *= this.boilerType.getDurationMultiplier() * getThrottleMultiplier();
            }
            if (solidFuelDuration > 0) {
                ItemFuelInfo itemFuelInfo = (ItemFuelInfo) fuels.get(itemStack.getTranslationKey());
                if (itemFuelInfo == null) {
                    itemFuelInfo = new ItemFuelInfo(itemStack, itemStack.getCount(), itemCapacity, 1, (long) itemStack.getCount() * solidFuelDuration);
                    fuels.put(itemStack.getTranslationKey(), itemFuelInfo);
                } else {
                    itemFuelInfo.addFuelRemaining(itemStack.getCount());
                    itemFuelInfo.addFuelBurnTime((long) itemStack.getCount() * solidFuelDuration);
                }
            }
        }
        return fuels.values();
    }
}
