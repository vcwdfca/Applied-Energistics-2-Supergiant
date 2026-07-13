package ae2.me;

import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.GenericStack;
import ae2.api.util.DimensionalBlockPos;
import ae2.api.util.FlowSearchDTO;
import ae2.core.AEConfig;
import ae2.me.helpers.MachineSource;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IngredientsFlowGridCache {

    private static final Logger LOGGER = LogManager.getLogger(IngredientsFlowGridCache.class);

    public record FlowRate(long in, long out) {

        public long net() {
            return this.in - this.out;
        }
    }

    private static final class Bucket {

        public final long start;
        public final Map<GenericStack, ItemFlowAccumulator> byIngredient = new Object2ObjectOpenHashMap<>();

        public Bucket(final long start) {
            this.start = start;
        }
    }

    private static final class ItemFlowAccumulator {

        public long in;
        public long out;
        public final Object2LongOpenHashMap<DimensionalBlockPos> netByLocation = new Object2LongOpenHashMap<>();

    }

    private static final long BUCKET_SIZE_MS = 200L;

    private static final String TRACKING_ENABLED_KEY = "ItemFlowTrackingEnabled";

    private final ArrayDeque<Bucket> buckets = new ArrayDeque<>();

    private boolean trackingEnabled = false;

    public IngredientsFlowGridCache(final IGrid grid) {}

    public boolean isTrackingEnabled() {
        return this.trackingEnabled;
    }

    public void setTrackingEnabled(final boolean enabled) {
        this.trackingEnabled = enabled;
        if(!enabled) {
            this.buckets.clear();
        }
    }

    public void recordFlow(final GenericStack diff, final IActionSource src) {
        if(!AEConfig.instance().isEnableIngredientsFlowTracking() || !this.trackingEnabled) {
            return;
        }

        if(!(src instanceof MachineSource machineSource) || machineSource.machine().isEmpty()) {
            return;
        }

        final IGridNode node = machineSource.machine().get().getActionableNode();
        if(node == null) {
            return;
        }

        if(!(node instanceof InWorldGridNode inWorldNode)) {
            LOGGER.error("Skipping ingredient flow tracking for non-world grid node {} because it has no physical block position.", node);
            return;
        }
        final DimensionalBlockPos location = new DimensionalBlockPos(node.getLevel(), inWorldNode.getLocation());

        final long now = System.currentTimeMillis();
        Bucket bucket = this.buckets.peekLast();

        if(bucket == null || now - bucket.start >= BUCKET_SIZE_MS) {
            bucket = new Bucket(now);
            this.buckets.addLast(bucket);
        }

        final long size = diff.amount();
        final ItemFlowAccumulator accumulator = bucket.byIngredient
            .computeIfAbsent(new GenericStack(diff.what(), 0), _ -> new ItemFlowAccumulator());

        if(size < 0) {
            accumulator.out -= size;
        } else {
            accumulator.in += size;
        }

        accumulator.netByLocation.addTo(location, size);
    }

    public List<FlowSearchDTO> getRecentFlow(final GenericStack queryStack) {
        final Object2LongLinkedOpenHashMap<DimensionalBlockPos> sources = new Object2LongLinkedOpenHashMap<>();
        final List<FlowSearchDTO> result = new ArrayList<>();

        for(final Bucket bucket : this.buckets) {
            for(final Map.Entry<GenericStack, ItemFlowAccumulator> entry : bucket.byIngredient.entrySet()) {

                if(!entry.getKey().what().equals(queryStack.what())) {
                    continue;
                }

                for(final Object2LongMap.Entry<DimensionalBlockPos> locationEntry : entry.getValue().netByLocation.object2LongEntrySet()) {
                    sources.addTo(locationEntry.getKey(), locationEntry.getLongValue());
                }
            }
        }

        for(final Object2LongMap.Entry<DimensionalBlockPos> entry : sources.object2LongEntrySet()) {
            if(entry.getLongValue() != 0) {
                result.add(new FlowSearchDTO(entry.getKey(), entry.getLongValue()));
            }
        }

        return result;
    }

    public Map<GenericStack, FlowRate> getAllRecentFlow() {
        final Object2LongLinkedOpenHashMap<GenericStack> totalsIn = new Object2LongLinkedOpenHashMap<>();
        final Object2LongLinkedOpenHashMap<GenericStack> totalsOut = new Object2LongLinkedOpenHashMap<>();
        final Map<GenericStack, FlowRate> result = new Object2ObjectOpenHashMap<>();

        for(final Bucket bucket : this.buckets) {
            for(final Map.Entry<GenericStack, ItemFlowAccumulator> entry : bucket.byIngredient.entrySet()) {
                totalsIn.addTo(entry.getKey(), entry.getValue().in);
                totalsOut.addTo(entry.getKey(), entry.getValue().out);
            }
        }

        for(final Object2LongMap.Entry<GenericStack> entry : totalsIn.object2LongEntrySet()) {
            result.put(entry.getKey(), new FlowRate(entry.getLongValue(), totalsOut.getLong(entry.getKey())));
        }

        return result;
    }

}
