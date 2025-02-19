package gregtech.common.pipelike.laser.net;

import gregtech.api.pipenet.WorldPipeNet;
import gregtech.common.pipelike.laser.LaserPipeProperties;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class WorldLaserPipeNet extends WorldPipeNet<LaserPipeProperties, LaserPipeNet> {
    private static final String DATA_ID = "gregtech.laser_pipe_net";
    public WorldLaserPipeNet(String name) {
        super(name);
    }

    @Nonnull
    public static WorldLaserPipeNet getWorldPipeNet(@Nonnull World world) {
        WorldLaserPipeNet netWorldData = (WorldLaserPipeNet) world.loadData(WorldLaserPipeNet.class, DATA_ID);
        if (netWorldData == null) {
            netWorldData = new WorldLaserPipeNet(DATA_ID);
            world.setData(DATA_ID, netWorldData);
        }
        netWorldData.setWorldAndInit(world);
        return netWorldData;
    }

    @Override
    protected LaserPipeNet createNetInstance() {
        return new LaserPipeNet(this);
    }
}
