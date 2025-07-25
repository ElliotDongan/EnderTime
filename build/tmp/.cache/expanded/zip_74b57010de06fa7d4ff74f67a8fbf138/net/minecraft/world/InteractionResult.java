package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

public sealed interface InteractionResult
    permits InteractionResult.Success,
    InteractionResult.Fail,
    InteractionResult.Pass,
    InteractionResult.TryEmptyHandInteraction {
    InteractionResult.Success SUCCESS = new InteractionResult.Success(InteractionResult.SwingSource.CLIENT, InteractionResult.ItemContext.DEFAULT);
    InteractionResult.Success SUCCESS_SERVER = new InteractionResult.Success(InteractionResult.SwingSource.SERVER, InteractionResult.ItemContext.DEFAULT);
    InteractionResult.Success CONSUME = new InteractionResult.Success(InteractionResult.SwingSource.NONE, InteractionResult.ItemContext.DEFAULT);
    InteractionResult.Fail FAIL = new InteractionResult.Fail();
    InteractionResult.Pass PASS = new InteractionResult.Pass();
    InteractionResult.TryEmptyHandInteraction TRY_WITH_EMPTY_HAND = new InteractionResult.TryEmptyHandInteraction();

    default boolean consumesAction() {
        return false;
    }

    public record Fail() implements InteractionResult {
    }

    public record ItemContext(boolean wasItemInteraction, @Nullable ItemStack heldItemTransformedTo) {
        static InteractionResult.ItemContext NONE = new InteractionResult.ItemContext(false, null);
        static InteractionResult.ItemContext DEFAULT = new InteractionResult.ItemContext(true, null);
    }

    public record Pass() implements InteractionResult {
    }

    public record Success(InteractionResult.SwingSource swingSource, InteractionResult.ItemContext itemContext) implements InteractionResult {
        @Override
        public boolean consumesAction() {
            return true;
        }

        public InteractionResult.Success heldItemTransformedTo(ItemStack p_362659_) {
            return new InteractionResult.Success(this.swingSource, new InteractionResult.ItemContext(true, p_362659_));
        }

        public InteractionResult.Success withoutItem() {
            return new InteractionResult.Success(this.swingSource, InteractionResult.ItemContext.NONE);
        }

        public boolean wasItemInteraction() {
            return this.itemContext.wasItemInteraction;
        }

        @Nullable
        public ItemStack heldItemTransformedTo() {
            return this.itemContext.heldItemTransformedTo;
        }
    }

    public static enum SwingSource {
        NONE,
        CLIENT,
        SERVER;
    }

    public record TryEmptyHandInteraction() implements InteractionResult {
    }
}