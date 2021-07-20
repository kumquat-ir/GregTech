package gregtech.common.metatileentities.multi;

import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.SimpleCubeRenderer;
import gregtech.api.unification.material.type.Material;
import net.minecraft.block.state.IBlockState;

public class LargeBoilerType {

    private final Material material;
    private final IBlockState casingState;
    private final IBlockState fireBoxState;
    private final IBlockState pipeCasingState;
    private final OrientedOverlayRenderer frontOverlay;
    private final ICubeRenderer solidCasingRenderer;
    private final SimpleCubeRenderer fireboxIdleRenderer;
    private final SimpleCubeRenderer firefoxActiveRenderer;

    private final int EUt;
    private final int boilerTemperatureIncrease;
    private final double durationMultiplier;
    private final int maxTemperature;

    public LargeBoilerType(Material material, IBlockState casingState, IBlockState fireBoxState, IBlockState pipeCasingState, OrientedOverlayRenderer frontOverlay,
                           ICubeRenderer solidCasingRenderer, SimpleCubeRenderer fireboxIdleRenderer, SimpleCubeRenderer firefoxActiveRenderer,
                           int EUt, int boilerTemperatureIncrease, double durationMultiplier, int maxTemperature) {
        this.material = material;
        this.casingState = casingState;
        this.fireBoxState = fireBoxState;
        this.pipeCasingState = pipeCasingState;
        this.frontOverlay = frontOverlay;
        this.solidCasingRenderer = solidCasingRenderer;
        this.fireboxIdleRenderer = fireboxIdleRenderer;
        this.firefoxActiveRenderer = firefoxActiveRenderer;
        this.EUt = EUt;
        this.boilerTemperatureIncrease = boilerTemperatureIncrease;
        this.durationMultiplier = durationMultiplier;
        this.maxTemperature = maxTemperature;
    }

    public Material getMaterial() {
        return this.material;
    }

    public IBlockState getCasingState() {
        return this.casingState;
    }

    public IBlockState getFireBoxState() {
        return this.fireBoxState;
    }

    public IBlockState getPipeCasingState() {
        return this.pipeCasingState;
    }

    public OrientedOverlayRenderer getFrontOverlay() {
        return this.frontOverlay;
    }

    public ICubeRenderer getSolidCasingRenderer() {
        return solidCasingRenderer;
    }

    public SimpleCubeRenderer getFireboxIdleRenderer() {
        return fireboxIdleRenderer;
    }

    public SimpleCubeRenderer getFirefoxActiveRenderer() {
        return firefoxActiveRenderer;
    }

    public int getEUt() {
        return this.EUt;
    }

    public int getBoilerTemperatureIncrease() {
        return this.boilerTemperatureIncrease;
    }

    public double getDurationMultiplier() {
        return this.durationMultiplier;
    }

    public int getMaxTemperature() {
        return this.maxTemperature;
    }
}
