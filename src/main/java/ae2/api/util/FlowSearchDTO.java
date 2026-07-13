package ae2.api.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.List;

public class FlowSearchDTO {

    public DimensionalBlockPos location;
    public long netTotal;

    public FlowSearchDTO(DimensionalBlockPos location, long netTotal) {
        this.location = location;
        this.netTotal = netTotal;
    }

    public FlowSearchDTO(final NBTTagCompound data) {
        this.readFromNBT(data);
    }

    public void writeToNBT(final NBTTagCompound data) {
        data.setInteger("dim", location.getLevel().provider.getDimension());
        data.setInteger("x", location.getPos().getX());
        data.setInteger("y", location.getPos().getY());
        data.setInteger("z", location.getPos().getZ());
        data.setLong("netTotal", netTotal);
    }

    public static void writeListToNBT(final NBTTagCompound tag, List<FlowSearchDTO> list) {
        int i = 0;
        for(FlowSearchDTO d : list) {
            NBTTagCompound data = new NBTTagCompound();
            d.writeToNBT(data);
            tag.setTag("pos#" + i, data);
            i++;
        }
    }

    public static List<FlowSearchDTO> readAsListFromNBT(final NBTTagCompound tag) {
        List<FlowSearchDTO> list = new ArrayList<>();
        int i = 0;
        while(tag.hasKey("pos#" + i)) {
            NBTTagCompound data = tag.getCompoundTag("pos#" + i);
            list.add(new FlowSearchDTO(data));
            i++;
        }
        return list;
    }

    private void readFromNBT(final NBTTagCompound data) {
        int dim = data.getInteger("dim");
        int x = data.getInteger("x");
        int y = data.getInteger("y");
        int z = data.getInteger("z");
        this.location = new DimensionalBlockPos(DimensionManager.getWorld(dim), x, y, z);
        this.netTotal = data.getLong("netTotal");
    }
}
