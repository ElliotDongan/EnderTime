package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
    private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int RECIPES_COLUMNS = 4;
    private static final int RECIPES_ROWS = 3;
    private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
    private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
    private static final int SCROLLER_FULL_HEIGHT = 54;
    private static final int RECIPES_X = 52;
    private static final int RECIPES_Y = 14;
    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    public StonecutterScreen(StonecutterMenu p_99310_, Inventory p_99311_, Component p_99312_) {
        super(p_99310_, p_99311_, p_99312_);
        p_99310_.registerUpdateListener(this::containerChanged);
        this.titleLabelY--;
    }

    @Override
    public void render(GuiGraphics p_281735_, int p_282517_, int p_282840_, float p_282389_) {
        super.render(p_281735_, p_282517_, p_282840_, p_282389_);
        this.renderTooltip(p_281735_, p_282517_, p_282840_);
    }

    @Override
    protected void renderBg(GuiGraphics p_283115_, float p_282453_, int p_282940_, int p_282328_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_283115_.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        int k = (int)(41.0F * this.scrollOffs);
        ResourceLocation resourcelocation = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        p_283115_.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, i + 119, j + 15 + k, 12, 15);
        int l = this.leftPos + 52;
        int i1 = this.topPos + 14;
        int j1 = this.startIndex + 12;
        this.renderButtons(p_283115_, p_282940_, p_282328_, l, i1, j1);
        this.renderRecipes(p_283115_, l, i1, j1);
    }

    @Override
    protected void renderTooltip(GuiGraphics p_282396_, int p_283157_, int p_282258_) {
        super.renderTooltip(p_282396_, p_283157_, p_282258_);
        if (this.displayRecipes) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 12;
            SelectableRecipe.SingleInputSet<StonecutterRecipe> singleinputset = this.menu.getVisibleRecipes();

            for (int l = this.startIndex; l < k && l < singleinputset.size(); l++) {
                int i1 = l - this.startIndex;
                int j1 = i + i1 % 4 * 16;
                int k1 = j + i1 / 4 * 18 + 2;
                if (p_283157_ >= j1 && p_283157_ < j1 + 16 && p_282258_ >= k1 && p_282258_ < k1 + 18) {
                    ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);
                    SlotDisplay slotdisplay = singleinputset.entries().get(l).recipe().optionDisplay();
                    p_282396_.setTooltipForNextFrame(this.font, slotdisplay.resolveForFirstStack(contextmap), p_283157_, p_282258_);
                }
            }
        }
    }

    private void renderButtons(GuiGraphics p_282733_, int p_282136_, int p_282147_, int p_281987_, int p_281276_, int p_282688_) {
        for (int i = this.startIndex; i < p_282688_ && i < this.menu.getNumberOfVisibleRecipes(); i++) {
            int j = i - this.startIndex;
            int k = p_281987_ + j % 4 * 16;
            int l = j / 4;
            int i1 = p_281276_ + l * 18 + 2;
            ResourceLocation resourcelocation;
            if (i == this.menu.getSelectedRecipeIndex()) {
                resourcelocation = RECIPE_SELECTED_SPRITE;
            } else if (p_282136_ >= k && p_282147_ >= i1 && p_282136_ < k + 16 && p_282147_ < i1 + 18) {
                resourcelocation = RECIPE_HIGHLIGHTED_SPRITE;
            } else {
                resourcelocation = RECIPE_SPRITE;
            }

            p_282733_.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, k, i1 - 1, 16, 18);
        }
    }

    private void renderRecipes(GuiGraphics p_281999_, int p_282658_, int p_282563_, int p_283352_) {
        SelectableRecipe.SingleInputSet<StonecutterRecipe> singleinputset = this.menu.getVisibleRecipes();
        ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

        for (int i = this.startIndex; i < p_283352_ && i < singleinputset.size(); i++) {
            int j = i - this.startIndex;
            int k = p_282658_ + j % 4 * 16;
            int l = j / 4;
            int i1 = p_282563_ + l * 18 + 2;
            SlotDisplay slotdisplay = singleinputset.entries().get(i).recipe().optionDisplay();
            p_281999_.renderItem(slotdisplay.resolveForFirstStack(contextmap), k, i1);
        }
    }

    @Override
    public boolean mouseClicked(double p_99318_, double p_99319_, int p_99320_) {
        this.scrolling = false;
        if (this.displayRecipes) {
            int i = this.leftPos + 52;
            int j = this.topPos + 14;
            int k = this.startIndex + 12;

            for (int l = this.startIndex; l < k; l++) {
                int i1 = l - this.startIndex;
                double d0 = p_99318_ - (i + i1 % 4 * 16);
                double d1 = p_99319_ - (j + i1 / 4 * 18);
                if (d0 >= 0.0 && d1 >= 0.0 && d0 < 16.0 && d1 < 18.0 && this.menu.clickMenuButton(this.minecraft.player, l)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, l);
                    return true;
                }
            }

            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (p_99318_ >= i && p_99318_ < i + 12 && p_99319_ >= j && p_99319_ < j + 54) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(p_99318_, p_99319_, p_99320_);
    }

    @Override
    public boolean mouseDragged(double p_99322_, double p_99323_, int p_99324_, double p_99325_, double p_99326_) {
        if (this.scrolling && this.isScrollBarActive()) {
            int i = this.topPos + 14;
            int j = i + 54;
            this.scrollOffs = ((float)p_99323_ - i - 7.5F) / (j - i - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startIndex = (int)(this.scrollOffs * this.getOffscreenRows() + 0.5) * 4;
            return true;
        } else {
            return super.mouseDragged(p_99322_, p_99323_, p_99324_, p_99325_, p_99326_);
        }
    }

    @Override
    public boolean mouseScrolled(double p_99314_, double p_99315_, double p_99316_, double p_297300_) {
        if (super.mouseScrolled(p_99314_, p_99315_, p_99316_, p_297300_)) {
            return true;
        } else {
            if (this.isScrollBarActive()) {
                int i = this.getOffscreenRows();
                float f = (float)p_297300_ / i;
                this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
                this.startIndex = (int)(this.scrollOffs * i + 0.5) * 4;
            }

            return true;
        }
    }

    private boolean isScrollBarActive() {
        return this.displayRecipes && this.menu.getNumberOfVisibleRecipes() > 12;
    }

    protected int getOffscreenRows() {
        return (this.menu.getNumberOfVisibleRecipes() + 4 - 1) / 4 - 3;
    }

    private void containerChanged() {
        this.displayRecipes = this.menu.hasInputItem();
        if (!this.displayRecipes) {
            this.scrollOffs = 0.0F;
            this.startIndex = 0;
        }
    }
}