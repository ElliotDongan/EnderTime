package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
    private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

    public ObjectSelectionList(Minecraft p_94442_, int p_94443_, int p_94444_, int p_94445_, int p_94446_) {
        super(p_94442_, p_94443_, p_94444_, p_94445_, p_94446_);
    }

    public ObjectSelectionList(Minecraft p_377120_, int p_378306_, int p_378593_, int p_375730_, int p_377551_, int p_376301_) {
        super(p_377120_, p_378306_, p_378593_, p_375730_, p_377551_, p_376301_);
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent p_265150_) {
        if (this.getItemCount() == 0) {
            return null;
        } else if (this.isFocused() && p_265150_ instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation) {
            E e1 = this.nextEntry(focusnavigationevent$arrownavigation.direction());
            if (e1 != null) {
                return ComponentPath.path(this, ComponentPath.leaf(e1));
            } else {
                this.setSelected(null);
                return null;
            }
        } else if (!this.isFocused()) {
            E e = this.getSelected();
            if (e == null) {
                e = this.nextEntry(p_265150_.getVerticalDirectionForInitialFocus());
            }

            return e == null ? null : ComponentPath.path(this, ComponentPath.leaf(e));
        } else {
            return null;
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169042_) {
        E e = this.getHovered();
        if (e != null) {
            this.narrateListElementPosition(p_169042_.nest(), e);
            e.updateNarration(p_169042_);
        } else {
            E e1 = this.getSelected();
            if (e1 != null) {
                this.narrateListElementPosition(p_169042_.nest(), e1);
                e1.updateNarration(p_169042_);
            }
        }

        if (this.isFocused()) {
            p_169042_.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements NarrationSupplier {
        public abstract Component getNarration();

        @Override
        public boolean mouseClicked(double p_333902_, double p_331922_, int p_328634_) {
            return true;
        }

        @Override
        public void updateNarration(NarrationElementOutput p_169044_) {
            p_169044_.add(NarratedElementType.TITLE, this.getNarration());
        }
    }
}