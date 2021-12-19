package gregtech.integration.jei.recipe.primitive;

import com.google.common.collect.ImmutableList;
import gregtech.api.recipes.Recipe.ChanceEntry;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.info.MaterialFlags;
import gregtech.api.unification.material.properties.OreProperty;
import gregtech.api.unification.material.properties.PropertyKey;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import gregtech.common.metatileentities.MetaTileEntities;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class OreByProduct implements IRecipeWrapper {

    private static final ImmutableList<OrePrefix> ORES;

    static {
        List<OrePrefix> prefixes = new ArrayList<>();
        prefixes.add(OrePrefix.ore);
        prefixes.add(OrePrefix.oreNetherrack);
        prefixes.add(OrePrefix.oreEndstone);
        if (ConfigHolder.worldgen.allUniqueStoneTypes) {
            prefixes.add(OrePrefix.oreGranite);
            prefixes.add(OrePrefix.oreDiorite);
            prefixes.add(OrePrefix.oreAndesite);
            prefixes.add(OrePrefix.oreBasalt);
            prefixes.add(OrePrefix.oreBlackgranite);
            prefixes.add(OrePrefix.oreMarble);
            prefixes.add(OrePrefix.oreRedgranite);
            prefixes.add(OrePrefix.oreSand);
            prefixes.add(OrePrefix.oreRedSand);
        }
        ORES = ImmutableList.copyOf(prefixes);
    }

    private static final ImmutableList<OrePrefix> IN_PROCESSING_STEPS = ImmutableList.of(
            OrePrefix.crushed,
            OrePrefix.crushedPurified,
            OrePrefix.dustImpure,
            OrePrefix.dustPure,
            OrePrefix.crushedCentrifuged
    );

    private static final ImmutableList<ItemStack> ALWAYS_MACHINES = ImmutableList.of(
            new ItemStack(Blocks.FURNACE),
            MetaTileEntities.MACERATOR[0].getStackForm(),
            MetaTileEntities.MACERATOR[0].getStackForm(),
            MetaTileEntities.CENTRIFUGE[0].getStackForm(),
            MetaTileEntities.ORE_WASHER[0].getStackForm(),
            MetaTileEntities.THERMAL_CENTRIFUGE[0].getStackForm(),
            MetaTileEntities.MACERATOR[0].getStackForm(),
            MetaTileEntities.MACERATOR[0].getStackForm(),
            MetaTileEntities.CENTRIFUGE[0].getStackForm()
    );

    private final Int2ObjectMap<ChanceEntry> chances = new Int2ObjectOpenHashMap<>();
    private final List<List<ItemStack>> inputs = new ArrayList<>();
    private final List<List<ItemStack>> outputs = new ArrayList<>();
    private final List<List<FluidStack>> fluidInputs = new ArrayList<>();
    private boolean hasChemBath = false;
    private boolean hasSeparator = false;
    private boolean hasSifter = false;
    private int currentSlot;

    public OreByProduct(Material material) {
        OreProperty property = material.getProperty(PropertyKey.ORE);
        int oreMultiplier = property.getOreMultiplier();
        int byproductMultiplier = property.getByProductMultiplier();
        currentSlot = 0;
        Material[] byproducts = new Material[]{
                GTUtility.selectItemInList(0, material, property.getOreByProducts(), Material.class),
                GTUtility.selectItemInList(1, material, property.getOreByProducts(), Material.class),
                GTUtility.selectItemInList(2, material, property.getOreByProducts(), Material.class),
                GTUtility.selectItemInList(3, material, property.getOreByProducts(), Material.class)
        };

        // "INPUTS"

        Pair<Material, Integer> washedIn = property.getWashedIn();
        List<Material> separatedInto = property.getSeparatedInto();

        List<ItemStack> oreStacks = new ArrayList<>();
        for (OrePrefix prefix : ORES) {
            // get all ores with the relevant oredicts instead of just the first unified ore
            oreStacks.addAll(OreDictionary.getOres(prefix.name() + material.toCamelCaseString()));
        }
        inputs.add(oreStacks);

        // set up machines as inputs
        List<ItemStack> simpleWashers = new ArrayList<>();
        simpleWashers.add(new ItemStack(Items.CAULDRON));
        simpleWashers.add(MetaTileEntities.SIMPLE_ORE_WASHER[0].getStackForm());

        for (ItemStack stack : ALWAYS_MACHINES) {
            addToInputs(stack);
        }
        // same amount of lines as a for loop :trol:
        inputs.add(simpleWashers);
        inputs.add(simpleWashers);
        inputs.add(simpleWashers);

        if (washedIn != null && washedIn.getKey() != null) {
            hasChemBath = true;
            addToInputs(MetaTileEntities.CHEMICAL_BATH[0].getStackForm());
        } else {
            addToInputs(ItemStack.EMPTY);
        }
        if (separatedInto != null && !separatedInto.isEmpty()) {
            hasSeparator = true;
            addToInputs(MetaTileEntities.ELECTROMAGNETIC_SEPARATOR[0].getStackForm());
        } else {
            addToInputs(ItemStack.EMPTY);
        }
        if (material.hasProperty(PropertyKey.GEM)) {
            hasSifter = true;
            addToInputs(MetaTileEntities.SIFTER[0].getStackForm());
        } else {
            addToInputs(ItemStack.EMPTY);
        }

        // add prefixes that should count as inputs to input lists (they will not be displayed in actual page)
        for (OrePrefix prefix : IN_PROCESSING_STEPS) {
            List<ItemStack> tempList = new ArrayList<>();
            tempList.add(OreDictUnifier.get(prefix, material));
            inputs.add(tempList);
        }

        // total number of inputs added
        currentSlot += 21;

        // BASIC PROCESSING - always present

        // begin lots of logic duplication from OreRecipeHandler
        // direct smelt
        ItemStack smeltingResult;
        Material smeltingMaterial = property.getDirectSmeltResult() == null ? material : property.getDirectSmeltResult();
        if (smeltingMaterial.hasProperty(PropertyKey.INGOT)) {
            smeltingResult = OreDictUnifier.get(OrePrefix.ingot, smeltingMaterial);
        } else if (smeltingMaterial.hasProperty(PropertyKey.GEM)) {
            smeltingResult = OreDictUnifier.get(OrePrefix.gem, smeltingMaterial);
        } else {
            smeltingResult = OreDictUnifier.get(OrePrefix.dust, smeltingMaterial);
        }
        smeltingResult.setCount(smeltingResult.getCount() * oreMultiplier);
        addToOutputs(smeltingResult);

        // macerate ore -> crushed
        addToOutputs(material, OrePrefix.crushed, 2 * oreMultiplier);
        addToOutputs(byproducts[0], OrePrefix.dust, 1);
        addChance(1400, 850);

        // macerate crushed -> impure
        addToOutputs(material, OrePrefix.dustImpure, 1);
        addToOutputs(byproducts[0], OrePrefix.dust, byproductMultiplier);
        addChance(1400, 850);

        // centrifuge impure -> dust
        addToOutputs(material, OrePrefix.dust, 1);
        addToOutputs(byproducts[0], OrePrefix.dustTiny, 1);

        // ore wash crushed -> crushed purified
        addToOutputs(material, OrePrefix.crushedPurified, 1);
        addToOutputs(byproducts[0], OrePrefix.dustTiny, 3);
        List<FluidStack> fluidStacks = new ArrayList<>();
        fluidStacks.add(Materials.Water.getFluid(1000));
        fluidStacks.add(Materials.DistilledWater.getFluid(100));
        fluidInputs.add(fluidStacks);

        // TC crushed/crushed purified -> centrifuged
        addToOutputs(material, OrePrefix.crushedCentrifuged, 1);
        addToOutputs(byproducts[1], OrePrefix.dustTiny, byproductMultiplier * 3);

        // macerate centrifuged -> dust
        addToOutputs(material, OrePrefix.dust, 1);
        addToOutputs(byproducts[2], OrePrefix.dust, 1);
        addChance(1400, 850);

        // macerate crushed purified -> purified
        addToOutputs(material, OrePrefix.dustPure, 1);
        addToOutputs(byproducts[1], OrePrefix.dust, 1);
        addChance(1400, 850);

        // centrifuge purified -> dust
        addToOutputs(material, OrePrefix.dust, 1);
        addToOutputs(byproducts[1], OrePrefix.dustTiny, 1);

        // cauldron/simple washer
        addToOutputs(material, OrePrefix.crushed, 1);
        addToOutputs(material, OrePrefix.crushedPurified, 1);
        addToOutputs(material, OrePrefix.dustImpure, 1);
        addToOutputs(material, OrePrefix.dust, 1);
        addToOutputs(material, OrePrefix.dustPure, 1);
        addToOutputs(material, OrePrefix.dust, 1);

        // ADVANCED PROCESSING - only on some materials

        // chem bath
        if (hasChemBath) {
            addToOutputs(material, OrePrefix.crushedPurified, 1);
            addToOutputs(byproducts[3], OrePrefix.dust, byproductMultiplier);
            List<FluidStack> washedFluid = new ArrayList<>();
            washedFluid.add(washedIn.getKey().getFluid(washedIn.getValue()));
            fluidInputs.add(washedFluid);
        } else {
            addEmptyOutputs(2);
            List<FluidStack> washedFluid = new ArrayList<>();
            fluidInputs.add(washedFluid);
        }

        // electromagnetic separator
        if (hasSeparator) {
            ItemStack separatedStack1 = OreDictUnifier.get(OrePrefix.dustSmall, separatedInto.get(0));
            OrePrefix prefix = (separatedInto.get(separatedInto.size() - 1).getBlastTemperature() == 0 && separatedInto.get(separatedInto.size() - 1).hasProperty(PropertyKey.INGOT))
                    ? OrePrefix.nugget : OrePrefix.dustSmall;
            ItemStack separatedStack2 = OreDictUnifier.get(prefix, separatedInto.get(separatedInto.size() - 1), prefix == OrePrefix.nugget ? 2 : 1);

            addToOutputs(material, OrePrefix.dust, 1);
            addToOutputs(separatedStack1);
            addChance(4000, 850);
            addToOutputs(separatedStack2);
            addChance(2000, 600);
        } else {
            addEmptyOutputs(3);
        }

        // sifter
        if (hasSifter) {
            boolean highOutput = material.hasFlag(MaterialFlags.HIGH_SIFTER_OUTPUT);
            ItemStack flawedStack = OreDictUnifier.get(OrePrefix.gemFlawed, material);
            ItemStack chippedStack = OreDictUnifier.get(OrePrefix.gemChipped, material);

            addToOutputs(material, OrePrefix.gemExquisite, 1); //300 100 / 500 150
            addGemChance(300, 100, 500, 150, highOutput);
            addToOutputs(material, OrePrefix.gemFlawless, 1); //1000 150 / 1500 200
            addGemChance(1000, 150, 1500, 200, highOutput);
            addToOutputs(material, OrePrefix.gem, 1); //3500 500 / 5000 1000
            addGemChance(3500, 500, 5000, 1000, highOutput);
            addToOutputs(material, OrePrefix.dustPure, 1); //5000 750 / 2500 500
            addGemChance(5000, 750, 2500, 500, highOutput);

            if (!flawedStack.isEmpty()) {
                addToOutputs(flawedStack); //2500 300 / 2000 500
                addGemChance(2500, 300, 2000, 500, highOutput);
            } else {
                addEmptyOutputs(1);
            }
            if (!chippedStack.isEmpty()) {
                addToOutputs(chippedStack); //3500 400 / 3000 350
                addGemChance(3500, 400, 3000, 350, highOutput);
            } else {
                addEmptyOutputs(1);
            }
        } else {
            addEmptyOutputs(6);
        }
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setInputLists(VanillaTypes.FLUID, fluidInputs);
        ingredients.setOutputLists(VanillaTypes.ITEM, outputs);
    }
    
    public void addTooltip(int slotIndex, boolean input, Object ingredient, List<String> tooltip) {
        if (chances.containsKey(slotIndex)) {
            ChanceEntry entry = chances.get(slotIndex);
            double chance = entry.getChance() / 100.0;
            double boost = entry.getBoostPerTier() / 100.0;
            tooltip.add(I18n.format("gregtech.recipe.chance", chance, boost));
        }
    }

    public ChanceEntry getChance(int slot) {
        return chances.get(slot);
    }

    public boolean hasSifter() {
        return hasSifter;
    }

    public boolean hasSeparator() {
        return hasSeparator;
    }

    public boolean hasChemBath() {
        return hasChemBath;
    }

    private void addToOutputs(Material material, OrePrefix prefix, int size) {
        addToOutputs(OreDictUnifier.get(prefix, material, size));
    }

    private void addToOutputs(ItemStack stack) {
        List<ItemStack> tempList = new ArrayList<>();
        tempList.add(stack);
        outputs.add(tempList);
        currentSlot++;
    }

    private void addEmptyOutputs(int amount) {
        for (int i = 0; i < amount; i++) {
            addToOutputs(ItemStack.EMPTY);
        }
    }

    private void addToInputs(ItemStack stack) {
        List<ItemStack> tempList = new ArrayList<>();
        tempList.add(stack);
        inputs.add(tempList);
    }

    private void addChance(int base, int tier) {
        // this is solely for the chance overlay and tooltip, neither of which care about the ItemStack
        chances.put(currentSlot - 1, new ChanceEntry(ItemStack.EMPTY, base, tier));
    }

    // make the code less :weary:
    private void addGemChance(int baseLow, int tierLow, int baseHigh, int tierHigh, boolean high) {
        if (high) {
            addChance(baseHigh, tierHigh);
        } else {
            addChance(baseLow, tierLow);
        }
    }
}
