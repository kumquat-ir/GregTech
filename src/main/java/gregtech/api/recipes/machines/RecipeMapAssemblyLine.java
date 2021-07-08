package gregtech.api.recipes.machines;

import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.function.DoubleSupplier;

public class RecipeMapAssemblyLine<R extends RecipeBuilder<R>> extends RecipeMap<R> {
    private TextureArea progressBarTexture;
    private ProgressWidget.MoveType moveType;

    public RecipeMapAssemblyLine(String unlocalizedName, int minInputs, int maxInputs, int minOutputs, int maxOutputs, int minFluidInputs, int maxFluidInputs, int minFluidOutputs, int maxFluidOutputs, R defaultRecipe, boolean isHidden) {
        super(unlocalizedName, minInputs, maxInputs, minOutputs, maxOutputs, minFluidInputs, maxFluidInputs, minFluidOutputs, maxFluidOutputs, defaultRecipe, isHidden);
    }

    @Override
    public RecipeMapAssemblyLine<R> setProgressBar(TextureArea progressBar, ProgressWidget.MoveType moveType) {
        this.progressBarTexture = progressBar;
        this.moveType = moveType;
        super.setProgressBar(progressBar, moveType);
        return this;
    }

    @Override
    public ModularUI.Builder createUITemplate(DoubleSupplier progressSupplier, IItemHandlerModifiable importItems, IItemHandlerModifiable exportItems, FluidTankList importFluids, FluidTankList exportFluids) {
        ModularUI.Builder builder = ModularUI.defaultBuilder();
        builder.widget(new ImageWidget(62 + 18, 1, 72, 72)
                .setImage(TextureArea.fullImage("textures/gui/icon/assembly_line.png")));
        this.addInventorySlotGroup(builder, importItems, importFluids, false);
        this.addInventorySlotGroup(builder, exportItems, exportFluids, true);
        return builder;
    }

    @Override
    protected void addInventorySlotGroup(ModularUI.Builder builder, IItemHandlerModifiable itemHandler, FluidTankList fluidHandler, boolean isOutputs) {
        int itemInputsCount = itemHandler.getSlots();
        int fluidInputsCount = fluidHandler.getTanks();
        boolean invertFluids = false;
        if (itemInputsCount == 0) {
            int tmp = itemInputsCount;
            itemInputsCount = fluidInputsCount;
            fluidInputsCount = tmp;
            invertFluids = true;
        }
        int[] inputSlotGrid = determineSlotsGrid(itemInputsCount);
        int itemSlotsToLeft = inputSlotGrid[0];
        int itemSlotsToDown = inputSlotGrid[1];
        int startInputsX = 80 - itemSlotsToLeft * 18;
        int startInputsY = 37 - (int) (itemSlotsToDown / 2.0 * 18);

        if (!isOutputs) {
            // TODO make data item always appear in this slot
            addDataSlot(builder, startInputsX + 18 * 7, 1 + 18 * 2, 17, itemHandler); // Data Slot
            for (int i = 0; i < itemSlotsToDown; i++) {
                for (int j = 0; j < itemSlotsToLeft; j++) {
                    int slotIndex = i * itemSlotsToLeft + j;
                    addSlot(builder, startInputsX + 18 * j, startInputsY + 18 * i, slotIndex, itemHandler, fluidHandler, invertFluids, isOutputs);
                }
            }
            if (fluidInputsCount > 0 || invertFluids) {
                if (itemSlotsToDown >= fluidInputsCount) {
                    int startSpecX = startInputsX + 18 * 5;
                    for (int i = 0; i < fluidInputsCount; i++) {
                        addSlot(builder, startSpecX, startInputsY + 18 * i, i, itemHandler, fluidHandler, !invertFluids, isOutputs);
                    }
                }
            }
        } else {
            addSlot(builder, startInputsX + 18 * 4, 1, 18, itemHandler, fluidHandler, invertFluids, isOutputs); // Output Slot
        }
    }

    protected void addDataSlot(ModularUI.Builder builder, int x, int y, int slotIndex, IItemHandlerModifiable itemHandler) {
        builder.widget((new SlotWidget(itemHandler, slotIndex, x, y, true, true)).setBackgroundTexture(GuiTextures.SLOT, GuiTextures.DATA_ORB_OVERLAY));

    }

    protected static int[] determineSlotsGrid(int itemInputsCount) {
        int itemSlotsToLeft = 0;
        int itemSlotsToDown = 0;
        double sqrt = Math.sqrt(itemInputsCount);
        if (sqrt % 1 == 0) { //check if square root is integer
            //case for 1, 4, 9 slots - it's square inputs (the most common case)
            itemSlotsToLeft = itemSlotsToDown = (int) sqrt;
        } else if (itemInputsCount % 3 == 0) {
            //case for 3 and 6 slots - 3 by horizontal and i / 3 by vertical (common case too)
            itemSlotsToDown = itemInputsCount / 3;
            itemSlotsToLeft = 3;
        } else if (itemInputsCount % 2 == 0) {
            //case for 2 inputs - 2 by horizontal and i / 3 by vertical (for 2 slots)
            itemSlotsToDown = itemInputsCount / 2;
            itemSlotsToLeft = 2;
        } else {
            itemSlotsToDown = 4;
            itemSlotsToLeft = 4;
        }
        return new int[] { itemSlotsToLeft, itemSlotsToDown };
    }
}
