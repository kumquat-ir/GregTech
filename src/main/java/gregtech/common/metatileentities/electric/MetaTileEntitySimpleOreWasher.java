package gregtech.common.metatileentities.electric;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.OrientedOverlayRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
public class MetaTileEntitySimpleOreWasher extends SimpleMachineMetaTileEntity {

    public MetaTileEntitySimpleOreWasher(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, OrientedOverlayRenderer renderer, int tier) {
        super(metaTileEntityId, recipeMap, renderer, tier, true);
        initializeInventory();
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new ItemStackHandler(1);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySimpleOreWasher(metaTileEntityId, workable.recipeMap, renderer, getTier());
    }
}
