package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
    private static final ResourceLocation BANNER_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/banner");
    private static final ResourceLocation DYE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/dye");
    private static final ResourceLocation PATTERN_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot/banner_pattern");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/scroller_disabled");
    private static final ResourceLocation PATTERN_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_selected");
    private static final ResourceLocation PATTERN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern_highlighted");
    private static final ResourceLocation PATTERN_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/pattern");
    private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/loom/error");
    private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/loom.png");
    private static final int PATTERN_COLUMNS = 4;
    private static final int PATTERN_ROWS = 4;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int PATTERN_IMAGE_SIZE = 14;
    private static final int SCROLLER_FULL_HEIGHT = 56;
    private static final int PATTERNS_X = 60;
    private static final int PATTERNS_Y = 13;
    private static final float BANNER_PATTERN_TEXTURE_SIZE = 64.0F;
    private static final float BANNER_PATTERN_WIDTH = 21.0F;
    private static final float BANNER_PATTERN_HEIGHT = 40.0F;
    private ModelPart flag;
    @Nullable
    private BannerPatternLayers resultBannerPatterns;
    private ItemStack bannerStack = ItemStack.EMPTY;
    private ItemStack dyeStack = ItemStack.EMPTY;
    private ItemStack patternStack = ItemStack.EMPTY;
    private boolean displayPatterns;
    private boolean hasMaxPatterns;
    private float scrollOffs;
    private boolean scrolling;
    private int startRow;

    public LoomScreen(LoomMenu p_99075_, Inventory p_99076_, Component p_99077_) {
        super(p_99075_, p_99076_, p_99077_);
        p_99075_.registerUpdateListener(this::containerChanged);
        this.titleLabelY -= 2;
    }

    @Override
    protected void init() {
        super.init();
        this.flag = this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG).getChild("flag");
    }

    @Override
    public void render(GuiGraphics p_283513_, int p_282700_, int p_282637_, float p_281433_) {
        super.render(p_283513_, p_282700_, p_282637_, p_281433_);
        this.renderTooltip(p_283513_, p_282700_, p_282637_);
    }

    private int totalRowCount() {
        return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
    }

    @Override
    protected void renderBg(GuiGraphics p_282870_, float p_281777_, int p_283331_, int p_283087_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_282870_.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        Slot slot = this.menu.getBannerSlot();
        Slot slot1 = this.menu.getDyeSlot();
        Slot slot2 = this.menu.getPatternSlot();
        Slot slot3 = this.menu.getResultSlot();
        if (!slot.hasItem()) {
            p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_SPRITE, i + slot.x, j + slot.y, 16, 16);
        }

        if (!slot1.hasItem()) {
            p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, DYE_SLOT_SPRITE, i + slot1.x, j + slot1.y, 16, 16);
        }

        if (!slot2.hasItem()) {
            p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_SPRITE, i + slot2.x, j + slot2.y, 16, 16);
        }

        int k = (int)(41.0F * this.scrollOffs);
        ResourceLocation resourcelocation = this.displayPatterns ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, i + 119, j + 13 + k, 12, 15);
        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            DyeColor dyecolor = ((BannerItem)slot3.getItem().getItem()).getColor();
            int l = i + 141;
            int i1 = j + 8;
            p_282870_.submitBannerPatternRenderState(this.flag, dyecolor, this.resultBannerPatterns, l, i1, l + 20, i1 + 40);
        } else if (this.hasMaxPatterns) {
            p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + slot3.x - 5, j + slot3.y - 5, 26, 26);
        }

        if (this.displayPatterns) {
            int l2 = i + 60;
            int i3 = j + 13;
            List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();

            label64:
            for (int j1 = 0; j1 < 4; j1++) {
                for (int k1 = 0; k1 < 4; k1++) {
                    int l1 = j1 + this.startRow;
                    int i2 = l1 * 4 + k1;
                    if (i2 >= list.size()) {
                        break label64;
                    }

                    int j2 = l2 + k1 * 14;
                    int k2 = i3 + j1 * 14;
                    boolean flag = p_283331_ >= j2 && p_283087_ >= k2 && p_283331_ < j2 + 14 && p_283087_ < k2 + 14;
                    ResourceLocation resourcelocation1;
                    if (i2 == this.menu.getSelectedBannerPatternIndex()) {
                        resourcelocation1 = PATTERN_SELECTED_SPRITE;
                    } else if (flag) {
                        resourcelocation1 = PATTERN_HIGHLIGHTED_SPRITE;
                    } else {
                        resourcelocation1 = PATTERN_SPRITE;
                    }

                    p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation1, j2, k2, 14, 14);
                    TextureAtlasSprite textureatlassprite = Sheets.getBannerMaterial(list.get(i2)).sprite();
                    this.renderBannerOnButton(p_282870_, j2, k2, textureatlassprite);
                }
            }
        }

        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
    }

    private void renderBannerOnButton(GuiGraphics p_410574_, int p_408841_, int p_407567_, TextureAtlasSprite p_409613_) {
        p_410574_.pose().pushMatrix();
        p_410574_.pose().translate(p_408841_ + 4, p_407567_ + 2);
        float f = p_409613_.getU0();
        float f1 = f + (p_409613_.getU1() - p_409613_.getU0()) * 21.0F / 64.0F;
        float f2 = p_409613_.getV1() - p_409613_.getV0();
        float f3 = p_409613_.getV0() + f2 / 64.0F;
        float f4 = f3 + f2 * 40.0F / 64.0F;
        int i = 5;
        int j = 10;
        p_410574_.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
        p_410574_.blit(p_409613_.atlasLocation(), 0, 0, 5, 10, f, f1, f3, f4);
        p_410574_.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(double p_99083_, double p_99084_, int p_99085_) {
        this.scrolling = false;
        if (this.displayPatterns) {
            int i = this.leftPos + 60;
            int j = this.topPos + 13;

            for (int k = 0; k < 4; k++) {
                for (int l = 0; l < 4; l++) {
                    double d0 = p_99083_ - (i + l * 14);
                    double d1 = p_99084_ - (j + k * 14);
                    int i1 = k + this.startRow;
                    int j1 = i1 * 4 + l;
                    if (d0 >= 0.0 && d1 >= 0.0 && d0 < 14.0 && d1 < 14.0 && this.menu.clickMenuButton(this.minecraft.player, j1)) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, j1);
                        return true;
                    }
                }
            }

            i = this.leftPos + 119;
            j = this.topPos + 9;
            if (p_99083_ >= i && p_99083_ < i + 12 && p_99084_ >= j && p_99084_ < j + 56) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(p_99083_, p_99084_, p_99085_);
    }

    @Override
    public boolean mouseDragged(double p_99087_, double p_99088_, int p_99089_, double p_99090_, double p_99091_) {
        int i = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && i > 0) {
            int j = this.topPos + 13;
            int k = j + 56;
            this.scrollOffs = ((float)p_99088_ - j - 7.5F) / (k - j - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startRow = Math.max((int)(this.scrollOffs * i + 0.5), 0);
            return true;
        } else {
            return super.mouseDragged(p_99087_, p_99088_, p_99089_, p_99090_, p_99091_);
        }
    }

    @Override
    public boolean mouseScrolled(double p_99079_, double p_99080_, double p_99081_, double p_298992_) {
        if (super.mouseScrolled(p_99079_, p_99080_, p_99081_, p_298992_)) {
            return true;
        } else {
            int i = this.totalRowCount() - 4;
            if (this.displayPatterns && i > 0) {
                float f = (float)p_298992_ / i;
                this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
                this.startRow = Math.max((int)(this.scrollOffs * i + 0.5F), 0);
            }

            return true;
        }
    }

    @Override
    protected boolean hasClickedOutside(double p_99093_, double p_99094_, int p_99095_, int p_99096_, int p_99097_) {
        return p_99093_ < p_99095_ || p_99094_ < p_99096_ || p_99093_ >= p_99095_ + this.imageWidth || p_99094_ >= p_99096_ + this.imageHeight;
    }

    private void containerChanged() {
        ItemStack itemstack = this.menu.getResultSlot().getItem();
        if (itemstack.isEmpty()) {
            this.resultBannerPatterns = null;
        } else {
            this.resultBannerPatterns = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        }

        ItemStack itemstack1 = this.menu.getBannerSlot().getItem();
        ItemStack itemstack2 = this.menu.getDyeSlot().getItem();
        ItemStack itemstack3 = this.menu.getPatternSlot().getItem();
        BannerPatternLayers bannerpatternlayers = itemstack1.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.hasMaxPatterns = bannerpatternlayers.layers().size() >= 6;
        if (this.hasMaxPatterns) {
            this.resultBannerPatterns = null;
        }

        if (!ItemStack.matches(itemstack1, this.bannerStack) || !ItemStack.matches(itemstack2, this.dyeStack) || !ItemStack.matches(itemstack3, this.patternStack)) {
            this.displayPatterns = !itemstack1.isEmpty() && !itemstack2.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
        }

        if (this.startRow >= this.totalRowCount()) {
            this.startRow = 0;
            this.scrollOffs = 0.0F;
        }

        this.bannerStack = itemstack1.copy();
        this.dyeStack = itemstack2.copy();
        this.patternStack = itemstack3.copy();
    }
}