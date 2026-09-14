package ae2.crafting;

import com.mojang.authlib.GameProfile;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Objects;
import java.util.UUID;

/**
 * Applies the player-crafting lifecycle ({@code Item.onCreated} plus {@code PlayerEvent.ItemCraftedEvent}) to
 * recipe results computed outside a real crafting context, so patterns bake in the same result mutations a
 * manual craft would apply.
 *
 * <p>The simulated player is anonymous on purpose: it has no inventory contents, its position is unset and its
 * world reference is nulled out. Recipes or event listeners that depend on a real player's dimension or position
 * therefore fail fast with a {@link RuntimeException} (typically an NPE); callers are expected to treat such
 * failures as "this recipe cannot be encoded/produced".</p>
 */
public final class CraftingEventSimulation {
    private static final GameProfile SIMULATED_PLAYER_PROFILE =
        new GameProfile(UUID.fromString("0ae2a7ce-9a52-4b7e-8b1c-6f6f51e2a0be"), "[AE2]");
    private static volatile FakePlayer simulatedPlayer;

    private CraftingEventSimulation() {
    }

    /**
     * Runs the crafting lifecycle hooks on a computed recipe result and returns the (possibly mutated) output.
     *
     * @throws RuntimeException when the recipe or an event listener depends on real player context
     */
    public static ItemStack processCraftingResult(ItemStack output, InventoryCrafting input, World world) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(input, "input");
        if (!(world instanceof WorldServer worldServer)) {
            return output;
        }
        var player = getSimulatedPlayer(worldServer);
        output.getItem().onCreated(output, world, player);
        FMLCommonHandler.instance().firePlayerCraftingEvent(player, output, input);
        return output;
    }

    private static FakePlayer getSimulatedPlayer(WorldServer worldServer) {
        var player = simulatedPlayer;
        if (player == null) {
            synchronized (CraftingEventSimulation.class) {
                player = simulatedPlayer;
                if (player == null) {
                    player = new FakePlayer(worldServer, SIMULATED_PLAYER_PROFILE);
                    player.world = null;
                    simulatedPlayer = player;
                }
            }
        }
        return player;
    }
}
