package ae2.client.gui.cellterminal.widget;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * Tooltip content produced by a cell terminal widget. A non-empty {@link #stack} tells the caller to render the
 * tooltip through the item tooltip pipeline instead of as plain lines.
 */
public record WidgetTooltip(List<String> lines, ItemStack stack) {
    public static final WidgetTooltip EMPTY = new WidgetTooltip(List.of(), ItemStack.EMPTY);

    public WidgetTooltip {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(stack, "stack");
    }

    public static WidgetTooltip text(List<String> lines) {
        return lines.isEmpty() ? EMPTY : new WidgetTooltip(lines, ItemStack.EMPTY);
    }

    public static WidgetTooltip item(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new WidgetTooltip(List.of(), stack);
    }

    public static WidgetTooltip item(ItemStack stack, List<String> lines) {
        return stack.isEmpty() ? text(lines) : new WidgetTooltip(lines, stack);
    }

    public boolean isEmpty() {
        return this.lines.isEmpty() && this.stack.isEmpty();
    }
}
