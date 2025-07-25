package net.minecraft.client;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Options {
    static final Logger LOGGER = LogUtils.getLogger();
    static final Gson GSON = new Gson();
    private static final TypeToken<List<String>> LIST_OF_STRINGS_TYPE = new TypeToken<List<String>>() {};
    public static final int RENDER_DISTANCE_TINY = 2;
    public static final int RENDER_DISTANCE_SHORT = 4;
    public static final int RENDER_DISTANCE_NORMAL = 8;
    public static final int RENDER_DISTANCE_FAR = 12;
    public static final int RENDER_DISTANCE_REALLY_FAR = 16;
    public static final int RENDER_DISTANCE_EXTREME = 32;
    private static final Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);
    public static final String DEFAULT_SOUND_DEVICE = "";
    private static final Component ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND = Component.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
    private final OptionInstance<Boolean> darkMojangStudiosBackground = OptionInstance.createBoolean(
        "options.darkMojangStudiosBackgroundColor", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND), false
    );
    private static final Component ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES = Component.translatable("options.hideLightningFlashes.tooltip");
    private final OptionInstance<Boolean> hideLightningFlash = OptionInstance.createBoolean("options.hideLightningFlashes", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES), false);
    private static final Component ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS = Component.translatable("options.hideSplashTexts.tooltip");
    private final OptionInstance<Boolean> hideSplashTexts = OptionInstance.createBoolean("options.hideSplashTexts", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS), false);
    private final OptionInstance<Double> sensitivity = new OptionInstance<>("options.sensitivity", OptionInstance.noTooltip(), (p_232096_, p_232097_) -> {
        if (p_232097_ == 0.0) {
            return genericValueLabel(p_232096_, Component.translatable("options.sensitivity.min"));
        } else {
            return p_232097_ == 1.0 ? genericValueLabel(p_232096_, Component.translatable("options.sensitivity.max")) : percentValueLabel(p_232096_, 2.0 * p_232097_);
        }
    }, OptionInstance.UnitDouble.INSTANCE, 0.5, p_232115_ -> {});
    private final OptionInstance<Integer> renderDistance;
    private final OptionInstance<Integer> simulationDistance;
    private int serverRenderDistance = 0;
    private final OptionInstance<Double> entityDistanceScaling = new OptionInstance<>(
        "options.entityDistanceScaling",
        OptionInstance.noTooltip(),
        Options::percentValueLabel,
        new OptionInstance.IntRange(2, 20).xmap(p_232020_ -> p_232020_ / 4.0, p_232112_ -> (int)(p_232112_ * 4.0)),
        Codec.doubleRange(0.5, 5.0),
        1.0,
        p_333143_ -> {}
    );
    public static final int UNLIMITED_FRAMERATE_CUTOFF = 260;
    private final OptionInstance<Integer> framerateLimit = new OptionInstance<>(
        "options.framerateLimit",
        OptionInstance.noTooltip(),
        (p_232048_, p_232049_) -> p_232049_ == 260
            ? genericValueLabel(p_232048_, Component.translatable("options.framerateLimit.max"))
            : genericValueLabel(p_232048_, Component.translatable("options.framerate", p_232049_)),
        new OptionInstance.IntRange(1, 26).xmap(p_232003_ -> p_232003_ * 10, p_232094_ -> p_232094_ / 10),
        Codec.intRange(10, 260),
        120,
        p_357644_ -> Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(p_357644_)
    );
    private static final Component INACTIVITY_FPS_LIMIT_TOOLTIP_MINIMIZED = Component.translatable("options.inactivityFpsLimit.minimized.tooltip");
    private static final Component INACTIVITY_FPS_LIMIT_TOOLTIP_AFK = Component.translatable("options.inactivityFpsLimit.afk.tooltip");
    private final OptionInstance<InactivityFpsLimit> inactivityFpsLimit = new OptionInstance<>(
        "options.inactivityFpsLimit",
        p_357645_ -> {
            return switch (p_357645_) {
                case MINIMIZED -> Tooltip.create(INACTIVITY_FPS_LIMIT_TOOLTIP_MINIMIZED);
                case AFK -> Tooltip.create(INACTIVITY_FPS_LIMIT_TOOLTIP_AFK);
            };
        },
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(Arrays.asList(InactivityFpsLimit.values()), InactivityFpsLimit.CODEC),
        InactivityFpsLimit.AFK,
        p_364366_ -> {}
    );
    private final OptionInstance<CloudStatus> cloudStatus = new OptionInstance<>(
        "options.renderClouds",
        OptionInstance.noTooltip(),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(
            Arrays.asList(CloudStatus.values()),
            Codec.withAlternative(CloudStatus.CODEC, Codec.BOOL, p_232082_ -> p_232082_ ? CloudStatus.FANCY : CloudStatus.OFF)
        ),
        CloudStatus.FANCY,
        p_407447_ -> {}
    );
    private final OptionInstance<Integer> cloudRange = new OptionInstance<>(
        "options.renderCloudsDistance",
        OptionInstance.noTooltip(),
        (p_231916_, p_270801_) -> genericValueLabel(p_231916_, Component.translatable("options.chunks", p_270801_)),
        new OptionInstance.IntRange(2, 128, true),
        128,
        p_404798_ -> Minecraft.getInstance().levelRenderer.getCloudRenderer().markForRebuild()
    );
    private static final Component GRAPHICS_TOOLTIP_FAST = Component.translatable("options.graphics.fast.tooltip");
    private static final Component GRAPHICS_TOOLTIP_FABULOUS = Component.translatable(
        "options.graphics.fabulous.tooltip", Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC)
    );
    private static final Component GRAPHICS_TOOLTIP_FANCY = Component.translatable("options.graphics.fancy.tooltip");
    private final OptionInstance<GraphicsStatus> graphicsMode = new OptionInstance<>(
        "options.graphics",
        p_325283_ -> {
            return switch (p_325283_) {
                case FANCY -> Tooltip.create(GRAPHICS_TOOLTIP_FANCY);
                case FAST -> Tooltip.create(GRAPHICS_TOOLTIP_FAST);
                case FABULOUS -> Tooltip.create(GRAPHICS_TOOLTIP_FABULOUS);
            };
        },
        (p_231904_, p_231905_) -> {
            MutableComponent mutablecomponent = Component.translatable(p_231905_.getKey());
            return p_231905_ == GraphicsStatus.FABULOUS ? mutablecomponent.withStyle(ChatFormatting.ITALIC) : mutablecomponent;
        },
        new OptionInstance.AltEnum<>(
            Arrays.asList(GraphicsStatus.values()),
            Stream.of(GraphicsStatus.values()).filter(p_231943_ -> p_231943_ != GraphicsStatus.FABULOUS).collect(Collectors.toList()),
            () -> Minecraft.getInstance().isRunning() && Minecraft.getInstance().getGpuWarnlistManager().isSkippingFabulous(),
            (p_231862_, p_231863_) -> {
                Minecraft minecraft = Minecraft.getInstance();
                GpuWarnlistManager gpuwarnlistmanager = minecraft.getGpuWarnlistManager();
                if (p_231863_ == GraphicsStatus.FABULOUS && gpuwarnlistmanager.willShowWarning()) {
                    gpuwarnlistmanager.showWarning();
                } else {
                    p_231862_.set(p_231863_);
                    minecraft.levelRenderer.allChanged();
                }
            },
            Codec.INT.xmap(GraphicsStatus::byId, GraphicsStatus::getId)
        ),
        GraphicsStatus.FANCY,
        p_406694_ -> {}
    );
    private final OptionInstance<Boolean> ambientOcclusion = OptionInstance.createBoolean("options.ao", true, p_263512_ -> Minecraft.getInstance().levelRenderer.allChanged());
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_NONE = Component.translatable("options.prioritizeChunkUpdates.none.tooltip");
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED = Component.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_NEARBY = Component.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
    private final OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates = new OptionInstance<>(
        "options.prioritizeChunkUpdates",
        p_325284_ -> {
            return switch (p_325284_) {
                case NONE -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NONE);
                case PLAYER_AFFECTED -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED);
                case NEARBY -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NEARBY);
            };
        },
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(
            Arrays.asList(PrioritizeChunkUpdates.values()), Codec.INT.xmap(PrioritizeChunkUpdates::byId, PrioritizeChunkUpdates::getId)
        ),
        PrioritizeChunkUpdates.NONE,
        p_406500_ -> {}
    );
    public List<String> resourcePacks = Lists.newArrayList();
    public List<String> incompatibleResourcePacks = Lists.newArrayList();
    private final OptionInstance<ChatVisiblity> chatVisibility = new OptionInstance<>(
        "options.chat.visibility",
        OptionInstance.noTooltip(),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(Arrays.asList(ChatVisiblity.values()), Codec.INT.xmap(ChatVisiblity::byId, ChatVisiblity::getId)),
        ChatVisiblity.FULL,
        p_409148_ -> {}
    );
    private final OptionInstance<Double> chatOpacity = new OptionInstance<>(
        "options.chat.opacity",
        OptionInstance.noTooltip(),
        (p_232088_, p_232089_) -> percentValueLabel(p_232088_, p_232089_ * 0.9 + 0.1),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232106_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatLineSpacing = new OptionInstance<>(
        "options.chat.line_spacing", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.0, p_407179_ -> {}
    );
    private static final Component MENU_BACKGROUND_BLURRINESS_TOOLTIP = Component.translatable("options.accessibility.menu_background_blurriness.tooltip");
    private static final int BLURRINESS_DEFAULT_VALUE = 5;
    private final OptionInstance<Integer> menuBackgroundBlurriness = new OptionInstance<>(
        "options.accessibility.menu_background_blurriness",
        OptionInstance.cachedConstantTooltip(MENU_BACKGROUND_BLURRINESS_TOOLTIP),
        Options::genericValueOrOffLabel,
        new OptionInstance.IntRange(0, 10),
        5,
        p_409301_ -> {}
    );
    private final OptionInstance<Double> textBackgroundOpacity = new OptionInstance<>(
        "options.accessibility.text_background_opacity",
        OptionInstance.noTooltip(),
        Options::percentValueLabel,
        OptionInstance.UnitDouble.INSTANCE,
        0.5,
        p_232100_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> panoramaSpeed = new OptionInstance<>(
        "options.accessibility.panorama_speed", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_408843_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_CONTRAST_MODE = Component.translatable("options.accessibility.high_contrast.tooltip");
    private final OptionInstance<Boolean> highContrast = OptionInstance.createBoolean(
        "options.accessibility.high_contrast", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_CONTRAST_MODE), false, p_275860_ -> {
            PackRepository packrepository = Minecraft.getInstance().getResourcePackRepository();
            boolean flag1 = packrepository.getSelectedIds().contains("high_contrast");
            if (!flag1 && p_275860_) {
                if (packrepository.addPack("high_contrast")) {
                    this.updateResourcePacks(packrepository);
                }
            } else if (flag1 && !p_275860_ && packrepository.removePack("high_contrast")) {
                this.updateResourcePacks(packrepository);
            }
        }
    );
    private static final Component HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP = Component.translatable("options.accessibility.high_contrast_block_outline.tooltip");
    private final OptionInstance<Boolean> highContrastBlockOutline = OptionInstance.createBoolean(
        "options.accessibility.high_contrast_block_outline", OptionInstance.cachedConstantTooltip(HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP), false
    );
    private final OptionInstance<Boolean> narratorHotkey = OptionInstance.createBoolean(
        "options.accessibility.narrator_hotkey",
        OptionInstance.cachedConstantTooltip(
            Minecraft.ON_OSX
                ? Component.translatable("options.accessibility.narrator_hotkey.mac.tooltip")
                : Component.translatable("options.accessibility.narrator_hotkey.tooltip")
        ),
        true
    );
    @Nullable
    public String fullscreenVideoModeString;
    public boolean hideServerAddress;
    public boolean advancedItemTooltips;
    public boolean pauseOnLostFocus = true;
    private final Set<PlayerModelPart> modelParts = EnumSet.allOf(PlayerModelPart.class);
    private final OptionInstance<HumanoidArm> mainHand = new OptionInstance<>(
        "options.mainHand",
        OptionInstance.noTooltip(),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(Arrays.asList(HumanoidArm.values()), HumanoidArm.CODEC),
        HumanoidArm.RIGHT,
        p_407732_ -> {}
    );
    public int overrideWidth;
    public int overrideHeight;
    private final OptionInstance<Double> chatScale = new OptionInstance<>(
        "options.chat.scale",
        OptionInstance.noTooltip(),
        (p_232078_, p_232079_) -> (Component)(p_232079_ == 0.0 ? CommonComponents.optionStatus(p_232078_, false) : percentValueLabel(p_232078_, p_232079_)),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232092_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatWidth = new OptionInstance<>(
        "options.chat.width",
        OptionInstance.noTooltip(),
        (p_232068_, p_232069_) -> pixelValueLabel(p_232068_, ChatComponent.getWidth(p_232069_)),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232084_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatHeightUnfocused = new OptionInstance<>(
        "options.chat.height.unfocused",
        OptionInstance.noTooltip(),
        (p_232058_, p_232059_) -> pixelValueLabel(p_232058_, ChatComponent.getHeight(p_232059_)),
        OptionInstance.UnitDouble.INSTANCE,
        ChatComponent.defaultUnfocusedPct(),
        p_232074_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatHeightFocused = new OptionInstance<>(
        "options.chat.height.focused",
        OptionInstance.noTooltip(),
        (p_232045_, p_232046_) -> pixelValueLabel(p_232045_, ChatComponent.getHeight(p_232046_)),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232064_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatDelay = new OptionInstance<>(
        "options.chat.delay_instant",
        OptionInstance.noTooltip(),
        (p_232030_, p_232031_) -> p_232031_ <= 0.0
            ? Component.translatable("options.chat.delay_none")
            : Component.translatable("options.chat.delay", String.format(Locale.ROOT, "%.1f", p_232031_)),
        new OptionInstance.IntRange(0, 60).xmap(p_231986_ -> p_231986_ / 10.0, p_232054_ -> (int)(p_232054_ * 10.0)),
        Codec.doubleRange(0.0, 6.0),
        0.0,
        p_232039_ -> Minecraft.getInstance().getChatListener().setMessageDelay(p_232039_)
    );
    private static final Component ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME = Component.translatable("options.notifications.display_time.tooltip");
    private final OptionInstance<Double> notificationDisplayTime = new OptionInstance<>(
        "options.notifications.display_time",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME),
        (p_264664_, p_407064_) -> genericValueLabel(p_264664_, Component.translatable("options.multiplier", p_407064_)),
        new OptionInstance.IntRange(5, 100).xmap(p_264666_ -> p_264666_ / 10.0, p_264667_ -> (int)(p_264667_ * 10.0)),
        Codec.doubleRange(0.5, 10.0),
        1.0,
        p_407875_ -> {}
    );
    private final OptionInstance<Integer> mipmapLevels = new OptionInstance<>(
        "options.mipmapLevels",
        OptionInstance.noTooltip(),
        (p_232033_, p_232034_) -> (Component)(p_232034_ == 0 ? CommonComponents.optionStatus(p_232033_, false) : genericValueLabel(p_232033_, p_232034_)),
        new OptionInstance.IntRange(0, 4),
        4,
        p_409839_ -> {}
    );
    public boolean useNativeTransport = true;
    private final OptionInstance<AttackIndicatorStatus> attackIndicator = new OptionInstance<>(
        "options.attackIndicator",
        OptionInstance.noTooltip(),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(
            Arrays.asList(AttackIndicatorStatus.values()), Codec.INT.xmap(AttackIndicatorStatus::byId, AttackIndicatorStatus::getId)
        ),
        AttackIndicatorStatus.CROSSHAIR,
        p_407714_ -> {}
    );
    public TutorialSteps tutorialStep = TutorialSteps.MOVEMENT;
    public boolean joinedFirstServer = false;
    private final OptionInstance<Integer> biomeBlendRadius = new OptionInstance<>("options.biomeBlendRadius", OptionInstance.noTooltip(), (p_232016_, p_232017_) -> {
        int i = p_232017_ * 2 + 1;
        return genericValueLabel(p_232016_, Component.translatable("options.biomeBlendRadius." + i));
    }, new OptionInstance.IntRange(0, 7, false), 2, p_232026_ -> Minecraft.getInstance().levelRenderer.allChanged());
    private final OptionInstance<Double> mouseWheelSensitivity = new OptionInstance<>(
        "options.mouseWheelSensitivity",
        OptionInstance.noTooltip(),
        (p_232013_, p_232014_) -> genericValueLabel(p_232013_, Component.literal(String.format(Locale.ROOT, "%.2f", p_232014_))),
        new OptionInstance.IntRange(-200, 100).xmap(Options::logMouse, Options::unlogMouse),
        Codec.doubleRange(logMouse(-200), logMouse(100)),
        logMouse(0),
        p_410500_ -> {}
    );
    private final OptionInstance<Boolean> rawMouseInput = OptionInstance.createBoolean("options.rawMouseInput", true, p_232062_ -> {
        Window window = Minecraft.getInstance().getWindow();
        if (window != null) {
            window.updateRawMouseInput(p_232062_);
        }
    });
    public int glDebugVerbosity = 1;
    private final OptionInstance<Boolean> autoJump = OptionInstance.createBoolean("options.autoJump", false);
    private static final Component ACCESSIBILITY_TOOLTIP_ROTATE_WITH_MINECART = Component.translatable("options.rotateWithMinecart.tooltip");
    private final OptionInstance<Boolean> rotateWithMinecart = OptionInstance.createBoolean("options.rotateWithMinecart", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_ROTATE_WITH_MINECART), false);
    private final OptionInstance<Boolean> operatorItemsTab = OptionInstance.createBoolean("options.operatorItemsTab", false);
    private final OptionInstance<Boolean> autoSuggestions = OptionInstance.createBoolean("options.autoSuggestCommands", true);
    private final OptionInstance<Boolean> chatColors = OptionInstance.createBoolean("options.chat.color", true);
    private final OptionInstance<Boolean> chatLinks = OptionInstance.createBoolean("options.chat.links", true);
    private final OptionInstance<Boolean> chatLinksPrompt = OptionInstance.createBoolean("options.chat.links.prompt", true);
    private final OptionInstance<Boolean> enableVsync = OptionInstance.createBoolean("options.vsync", true, p_232052_ -> {
        if (Minecraft.getInstance().getWindow() != null) {
            Minecraft.getInstance().getWindow().updateVsync(p_232052_);
        }
    });
    private final OptionInstance<Boolean> entityShadows = OptionInstance.createBoolean("options.entityShadows", true);
    private final OptionInstance<Boolean> forceUnicodeFont = OptionInstance.createBoolean("options.forceUnicodeFont", false, p_325286_ -> updateFontOptions());
    private final OptionInstance<Boolean> japaneseGlyphVariants = OptionInstance.createBoolean(
        "options.japaneseGlyphVariants",
        OptionInstance.cachedConstantTooltip(Component.translatable("options.japaneseGlyphVariants.tooltip")),
        japaneseGlyphVariantsDefault(),
        p_325287_ -> updateFontOptions()
    );
    private final OptionInstance<Boolean> invertYMouse = OptionInstance.createBoolean("options.invertMouse", false);
    private final OptionInstance<Boolean> discreteMouseScroll = OptionInstance.createBoolean("options.discrete_mouse_scroll", false);
    private static final Component REALMS_NOTIFICATIONS_TOOLTIP = Component.translatable("options.realmsNotifications.tooltip");
    private final OptionInstance<Boolean> realmsNotifications = OptionInstance.createBoolean("options.realmsNotifications", OptionInstance.cachedConstantTooltip(REALMS_NOTIFICATIONS_TOOLTIP), true);
    private static final Component ALLOW_SERVER_LISTING_TOOLTIP = Component.translatable("options.allowServerListing.tooltip");
    private final OptionInstance<Boolean> allowServerListing = OptionInstance.createBoolean(
        "options.allowServerListing", OptionInstance.cachedConstantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, p_362316_ -> {}
    );
    private final OptionInstance<Boolean> reducedDebugInfo = OptionInstance.createBoolean("options.reducedDebugInfo", false);
    private final Map<SoundSource, OptionInstance<Double>> soundSourceVolumes = Util.makeEnumMap(
        SoundSource.class, p_389134_ -> this.createSoundSliderOptionInstance("soundCategory." + p_389134_.getName(), p_389134_)
    );
    private final OptionInstance<Boolean> showSubtitles = OptionInstance.createBoolean("options.showSubtitles", false);
    private static final Component DIRECTIONAL_AUDIO_TOOLTIP_ON = Component.translatable("options.directionalAudio.on.tooltip");
    private static final Component DIRECTIONAL_AUDIO_TOOLTIP_OFF = Component.translatable("options.directionalAudio.off.tooltip");
    private final OptionInstance<Boolean> directionalAudio = OptionInstance.createBoolean(
        "options.directionalAudio", p_231858_ -> p_231858_ ? Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_ON) : Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_OFF), false, p_404796_ -> {
            SoundManager soundmanager = Minecraft.getInstance().getSoundManager();
            soundmanager.reload();
            soundmanager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    );
    private final OptionInstance<Boolean> backgroundForChatOnly = new OptionInstance<>(
        "options.accessibility.text_background",
        OptionInstance.noTooltip(),
        (p_231976_, p_231977_) -> p_231977_
            ? Component.translatable("options.accessibility.text_background.chat")
            : Component.translatable("options.accessibility.text_background.everywhere"),
        OptionInstance.BOOLEAN_VALUES,
        true,
        p_367178_ -> {}
    );
    private final OptionInstance<Boolean> touchscreen = OptionInstance.createBoolean("options.touchscreen", false);
    private final OptionInstance<Boolean> fullscreen = OptionInstance.createBoolean("options.fullscreen", false, p_231970_ -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null && minecraft.getWindow().isFullscreen() != p_231970_) {
            minecraft.getWindow().toggleFullScreen();
            this.fullscreen().set(minecraft.getWindow().isFullscreen());
        }
    });
    private final OptionInstance<Boolean> bobView = OptionInstance.createBoolean("options.viewBobbing", true);
    private static final Component MOVEMENT_TOGGLE = Component.translatable("options.key.toggle");
    private static final Component MOVEMENT_HOLD = Component.translatable("options.key.hold");
    private final OptionInstance<Boolean> toggleCrouch = new OptionInstance<>(
        "key.sneak", OptionInstance.noTooltip(), (p_231956_, p_231957_) -> p_231957_ ? MOVEMENT_TOGGLE : MOVEMENT_HOLD, OptionInstance.BOOLEAN_VALUES, false, p_367542_ -> {}
    );
    private final OptionInstance<Boolean> toggleSprint = new OptionInstance<>(
        "key.sprint", OptionInstance.noTooltip(), (p_231910_, p_231911_) -> p_231911_ ? MOVEMENT_TOGGLE : MOVEMENT_HOLD, OptionInstance.BOOLEAN_VALUES, false, p_366394_ -> {}
    );
    public boolean skipMultiplayerWarning;
    private static final Component CHAT_TOOLTIP_HIDE_MATCHED_NAMES = Component.translatable("options.hideMatchedNames.tooltip");
    private final OptionInstance<Boolean> hideMatchedNames = OptionInstance.createBoolean("options.hideMatchedNames", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_HIDE_MATCHED_NAMES), true);
    private final OptionInstance<Boolean> showAutosaveIndicator = OptionInstance.createBoolean("options.autosaveIndicator", true);
    private static final Component CHAT_TOOLTIP_ONLY_SHOW_SECURE = Component.translatable("options.onlyShowSecureChat.tooltip");
    private final OptionInstance<Boolean> onlyShowSecureChat = OptionInstance.createBoolean("options.onlyShowSecureChat", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_ONLY_SHOW_SECURE), false);
    public final KeyMapping keyUp = new KeyMapping("key.forward", 87, "key.categories.movement");
    public final KeyMapping keyLeft = new KeyMapping("key.left", 65, "key.categories.movement");
    public final KeyMapping keyDown = new KeyMapping("key.back", 83, "key.categories.movement");
    public final KeyMapping keyRight = new KeyMapping("key.right", 68, "key.categories.movement");
    public final KeyMapping keyJump = new KeyMapping("key.jump", 32, "key.categories.movement");
    public final KeyMapping keyShift = new ToggleKeyMapping("key.sneak", 340, "key.categories.movement", this.toggleCrouch::get);
    public final KeyMapping keySprint = new ToggleKeyMapping("key.sprint", 341, "key.categories.movement", this.toggleSprint::get);
    public final KeyMapping keyInventory = new KeyMapping("key.inventory", 69, "key.categories.inventory");
    public final KeyMapping keySwapOffhand = new KeyMapping("key.swapOffhand", 70, "key.categories.inventory");
    public final KeyMapping keyDrop = new KeyMapping("key.drop", 81, "key.categories.inventory");
    public final KeyMapping keyUse = new KeyMapping("key.use", InputConstants.Type.MOUSE, 1, "key.categories.gameplay");
    public final KeyMapping keyAttack = new KeyMapping("key.attack", InputConstants.Type.MOUSE, 0, "key.categories.gameplay");
    public final KeyMapping keyPickItem = new KeyMapping("key.pickItem", InputConstants.Type.MOUSE, 2, "key.categories.gameplay");
    public final KeyMapping keyChat = new KeyMapping("key.chat", 84, "key.categories.multiplayer");
    public final KeyMapping keyPlayerList = new KeyMapping("key.playerlist", 258, "key.categories.multiplayer");
    public final KeyMapping keyCommand = new KeyMapping("key.command", 47, "key.categories.multiplayer");
    public final KeyMapping keySocialInteractions = new KeyMapping("key.socialInteractions", 80, "key.categories.multiplayer");
    public final KeyMapping keyScreenshot = new KeyMapping("key.screenshot", 291, "key.categories.misc");
    public final KeyMapping keyTogglePerspective = new KeyMapping("key.togglePerspective", 294, "key.categories.misc");
    public final KeyMapping keySmoothCamera = new KeyMapping("key.smoothCamera", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
    public final KeyMapping keyFullscreen = new KeyMapping("key.fullscreen", 300, "key.categories.misc");
    public final KeyMapping keySpectatorOutlines = new KeyMapping("key.spectatorOutlines", InputConstants.UNKNOWN.getValue(), "key.categories.misc");
    public final KeyMapping keyAdvancements = new KeyMapping("key.advancements", 76, "key.categories.misc");
    public final KeyMapping keyQuickActions = new KeyMapping("key.quickActions", 71, "key.categories.misc");
    public final KeyMapping[] keyHotbarSlots = new KeyMapping[]{
        new KeyMapping("key.hotbar.1", 49, "key.categories.inventory"),
        new KeyMapping("key.hotbar.2", 50, "key.categories.inventory"),
        new KeyMapping("key.hotbar.3", 51, "key.categories.inventory"),
        new KeyMapping("key.hotbar.4", 52, "key.categories.inventory"),
        new KeyMapping("key.hotbar.5", 53, "key.categories.inventory"),
        new KeyMapping("key.hotbar.6", 54, "key.categories.inventory"),
        new KeyMapping("key.hotbar.7", 55, "key.categories.inventory"),
        new KeyMapping("key.hotbar.8", 56, "key.categories.inventory"),
        new KeyMapping("key.hotbar.9", 57, "key.categories.inventory")
    };
    public final KeyMapping keySaveHotbarActivator = new KeyMapping("key.saveToolbarActivator", 67, "key.categories.creative");
    public final KeyMapping keyLoadHotbarActivator = new KeyMapping("key.loadToolbarActivator", 88, "key.categories.creative");
    public KeyMapping[] keyMappings = ArrayUtils.addAll(
        (KeyMapping[])(new KeyMapping[]{
            this.keyAttack,
            this.keyUse,
            this.keyUp,
            this.keyLeft,
            this.keyDown,
            this.keyRight,
            this.keyJump,
            this.keyShift,
            this.keySprint,
            this.keyDrop,
            this.keyInventory,
            this.keyChat,
            this.keyPlayerList,
            this.keyPickItem,
            this.keyCommand,
            this.keySocialInteractions,
            this.keyScreenshot,
            this.keyTogglePerspective,
            this.keySmoothCamera,
            this.keyFullscreen,
            this.keySpectatorOutlines,
            this.keySwapOffhand,
            this.keySaveHotbarActivator,
            this.keyLoadHotbarActivator,
            this.keyAdvancements,
            this.keyQuickActions
        }),
        (KeyMapping[])this.keyHotbarSlots
    );
    protected Minecraft minecraft;
    private final File optionsFile;
    public boolean hideGui;
    private CameraType cameraType = CameraType.FIRST_PERSON;
    public String lastMpIp = "";
    public boolean smoothCamera;
    private final OptionInstance<Integer> fov = new OptionInstance<>(
        "options.fov",
        OptionInstance.noTooltip(),
        (p_231999_, p_232000_) -> {
            return switch (p_232000_) {
                case 70 -> genericValueLabel(p_231999_, Component.translatable("options.fov.min"));
                case 110 -> genericValueLabel(p_231999_, Component.translatable("options.fov.max"));
                default -> genericValueLabel(p_231999_, p_232000_);
            };
        },
        new OptionInstance.IntRange(30, 110),
        Codec.DOUBLE.xmap(p_232007_ -> (int)(p_232007_ * 40.0 + 70.0), p_232009_ -> (p_232009_.intValue() - 70.0) / 40.0),
        70,
        p_231992_ -> Minecraft.getInstance().levelRenderer.needsUpdate()
    );
    private static final Component TELEMETRY_TOOLTIP = Component.translatable(
        "options.telemetry.button.tooltip", Component.translatable("options.telemetry.state.minimal"), Component.translatable("options.telemetry.state.all")
    );
    private final OptionInstance<Boolean> telemetryOptInExtra = OptionInstance.createBoolean(
        "options.telemetry.button",
        OptionInstance.cachedConstantTooltip(TELEMETRY_TOOLTIP),
        (p_261356_, p_261357_) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.allowsTelemetry()) {
                return Component.translatable("options.telemetry.state.none");
            } else {
                return p_261357_ && minecraft.extraTelemetryAvailable()
                    ? Component.translatable("options.telemetry.state.all")
                    : Component.translatable("options.telemetry.state.minimal");
            }
        },
        false,
        p_406715_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT = Component.translatable("options.screenEffectScale.tooltip");
    private final OptionInstance<Double> screenEffectScale = new OptionInstance<>(
        "options.screenEffectScale", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_332854_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_FOV_EFFECT = Component.translatable("options.fovEffectScale.tooltip");
    private final OptionInstance<Double> fovEffectScale = new OptionInstance<>(
        "options.fovEffectScale",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_FOV_EFFECT),
        Options::percentValueOrOffLabel,
        OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
        Codec.doubleRange(0.0, 1.0),
        1.0,
        p_368983_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT = Component.translatable("options.darknessEffectScale.tooltip");
    private final OptionInstance<Double> darknessEffectScale = new OptionInstance<>(
        "options.darknessEffectScale",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT),
        Options::percentValueOrOffLabel,
        OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
        1.0,
        p_363697_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_GLINT_SPEED = Component.translatable("options.glintSpeed.tooltip");
    private final OptionInstance<Double> glintSpeed = new OptionInstance<>(
        "options.glintSpeed", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_SPEED), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.5, p_357646_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH = Component.translatable("options.glintStrength.tooltip");
    private final OptionInstance<Double> glintStrength = new OptionInstance<>(
        "options.glintStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.75, p_357643_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH = Component.translatable("options.damageTiltStrength.tooltip");
    private final OptionInstance<Double> damageTiltStrength = new OptionInstance<>(
        "options.damageTiltStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_406782_ -> {}
    );
    private final OptionInstance<Double> gamma = new OptionInstance<>("options.gamma", OptionInstance.noTooltip(), (p_231913_, p_231914_) -> {
        int i = (int)(p_231914_ * 100.0);
        if (i == 0) {
            return genericValueLabel(p_231913_, Component.translatable("options.gamma.min"));
        } else if (i == 50) {
            return genericValueLabel(p_231913_, Component.translatable("options.gamma.default"));
        } else {
            return i == 100 ? genericValueLabel(p_231913_, Component.translatable("options.gamma.max")) : genericValueLabel(p_231913_, i);
        }
    }, OptionInstance.UnitDouble.INSTANCE, 0.5, p_408641_ -> {});
    public static final int AUTO_GUI_SCALE = 0;
    private static final int MAX_GUI_SCALE_INCLUSIVE = 2147483646;
    private final OptionInstance<Integer> guiScale = new OptionInstance<>(
        "options.guiScale",
        OptionInstance.noTooltip(),
        (p_231982_, p_231983_) -> p_231983_ == 0 ? Component.translatable("options.guiScale.auto") : Component.literal(Integer.toString(p_231983_)),
        new OptionInstance.ClampingLazyMaxIntRange(0, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            return !minecraft.isRunning() ? 2147483646 : minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        }, 2147483646),
        0,
        p_325288_ -> this.minecraft.resizeDisplay()
    );
    private final OptionInstance<ParticleStatus> particles = new OptionInstance<>(
        "options.particles",
        OptionInstance.noTooltip(),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(Arrays.asList(ParticleStatus.values()), Codec.INT.xmap(ParticleStatus::byId, ParticleStatus::getId)),
        ParticleStatus.ALL,
        p_404800_ -> {}
    );
    private final OptionInstance<NarratorStatus> narrator = new OptionInstance<>(
        "options.narrator",
        OptionInstance.noTooltip(),
        (p_231907_, p_231908_) -> (Component)(this.minecraft.getNarrator().isActive()
            ? p_231908_.getName()
            : Component.translatable("options.narrator.notavailable")),
        new OptionInstance.Enum<>(Arrays.asList(NarratorStatus.values()), Codec.INT.xmap(NarratorStatus::byId, NarratorStatus::getId)),
        NarratorStatus.OFF,
        p_231860_ -> this.minecraft.getNarrator().updateNarratorStatus(p_231860_)
    );
    public String languageCode = "en_us";
    private final OptionInstance<String> soundDevice = new OptionInstance<>(
        "options.audioDevice",
        OptionInstance.noTooltip(),
        (p_231919_, p_231920_) -> {
            if ("".equals(p_231920_)) {
                return Component.translatable("options.audioDevice.default");
            } else {
                return p_231920_.startsWith("OpenAL Soft on ")
                    ? Component.literal(p_231920_.substring(SoundEngine.OPEN_AL_SOFT_PREFIX_LENGTH))
                    : Component.literal(p_231920_);
            }
        },
        new OptionInstance.LazyEnum<>(
            () -> Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).toList(),
            // FORGE: fix incorrect string comparison - PR #8767
            p_232011_ -> Minecraft.getInstance().isRunning() && (p_232011_ == null || !p_232011_.isEmpty()) && !Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().contains(p_232011_)
                ? Optional.empty()
                : Optional.of(p_232011_),
            Codec.STRING
        ),
        "",
        p_404797_ -> {
            SoundManager soundmanager = Minecraft.getInstance().getSoundManager();
            soundmanager.reload();
            soundmanager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    );
    public boolean onboardAccessibility = true;
    private static final Component MUSIC_FREQUENCY_TOOLTIP = Component.translatable("options.music_frequency.tooltip");
    private final OptionInstance<MusicManager.MusicFrequency> musicFrequency = new OptionInstance<>(
        "options.music_frequency",
        OptionInstance.cachedConstantTooltip(MUSIC_FREQUENCY_TOOLTIP),
        OptionInstance.forOptionEnum(),
        new OptionInstance.Enum<>(Arrays.asList(MusicManager.MusicFrequency.values()), MusicManager.MusicFrequency.CODEC),
        MusicManager.MusicFrequency.DEFAULT,
        p_404799_ -> Minecraft.getInstance().getMusicManager().setMinutesBetweenSongs(p_404799_)
    );
    private static final Component NOW_PLAYING_TOAST_TOOLTIP = Component.translatable("options.showNowPlayingToast.tooltip");
    private final OptionInstance<Boolean> showNowPlayingToast = OptionInstance.createBoolean(
        "options.showNowPlayingToast", OptionInstance.cachedConstantTooltip(NOW_PLAYING_TOAST_TOOLTIP), false, p_404793_ -> {
            if (p_404793_) {
                this.minecraft.getToastManager().createNowPlayingToast();
            } else {
                this.minecraft.getToastManager().removeNowPlayingToast();
            }
        }
    );
    public boolean syncWrites;
    public boolean startedCleanly = true;
    private final Map<String, String> unknownKeys = new java.util.HashMap<>();

    public OptionInstance<Boolean> darkMojangStudiosBackground() {
        return this.darkMojangStudiosBackground;
    }

    public OptionInstance<Boolean> hideLightningFlash() {
        return this.hideLightningFlash;
    }

    public OptionInstance<Boolean> hideSplashTexts() {
        return this.hideSplashTexts;
    }

    public OptionInstance<Double> sensitivity() {
        return this.sensitivity;
    }

    public OptionInstance<Integer> renderDistance() {
        return this.renderDistance;
    }

    public OptionInstance<Integer> simulationDistance() {
        return this.simulationDistance;
    }

    public OptionInstance<Double> entityDistanceScaling() {
        return this.entityDistanceScaling;
    }

    public OptionInstance<Integer> framerateLimit() {
        return this.framerateLimit;
    }

    public OptionInstance<InactivityFpsLimit> inactivityFpsLimit() {
        return this.inactivityFpsLimit;
    }

    public OptionInstance<CloudStatus> cloudStatus() {
        return this.cloudStatus;
    }

    public OptionInstance<Integer> cloudRange() {
        return this.cloudRange;
    }

    public OptionInstance<GraphicsStatus> graphicsMode() {
        return this.graphicsMode;
    }

    public OptionInstance<Boolean> ambientOcclusion() {
        return this.ambientOcclusion;
    }

    public OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates() {
        return this.prioritizeChunkUpdates;
    }

    public void updateResourcePacks(PackRepository p_275268_) {
        List<String> list = ImmutableList.copyOf(this.resourcePacks);
        this.resourcePacks.clear();
        this.incompatibleResourcePacks.clear();

        for (Pack pack : p_275268_.getSelectedPacks()) {
            if (!pack.isFixedPosition()) {
                this.resourcePacks.add(pack.getId());
                if (!pack.getCompatibility().isCompatible()) {
                    this.incompatibleResourcePacks.add(pack.getId());
                }
            }
        }

        this.save();
        List<String> list1 = ImmutableList.copyOf(this.resourcePacks);
        if (!list1.equals(list)) {
            this.minecraft.reloadResourcePacks();
        }
    }

    public OptionInstance<ChatVisiblity> chatVisibility() {
        return this.chatVisibility;
    }

    public OptionInstance<Double> chatOpacity() {
        return this.chatOpacity;
    }

    public OptionInstance<Double> chatLineSpacing() {
        return this.chatLineSpacing;
    }

    public OptionInstance<Integer> menuBackgroundBlurriness() {
        return this.menuBackgroundBlurriness;
    }

    public int getMenuBackgroundBlurriness() {
        return this.menuBackgroundBlurriness().get();
    }

    public OptionInstance<Double> textBackgroundOpacity() {
        return this.textBackgroundOpacity;
    }

    public OptionInstance<Double> panoramaSpeed() {
        return this.panoramaSpeed;
    }

    public OptionInstance<Boolean> highContrast() {
        return this.highContrast;
    }

    public OptionInstance<Boolean> highContrastBlockOutline() {
        return this.highContrastBlockOutline;
    }

    public OptionInstance<Boolean> narratorHotkey() {
        return this.narratorHotkey;
    }

    public OptionInstance<HumanoidArm> mainHand() {
        return this.mainHand;
    }

    public OptionInstance<Double> chatScale() {
        return this.chatScale;
    }

    public OptionInstance<Double> chatWidth() {
        return this.chatWidth;
    }

    public OptionInstance<Double> chatHeightUnfocused() {
        return this.chatHeightUnfocused;
    }

    public OptionInstance<Double> chatHeightFocused() {
        return this.chatHeightFocused;
    }

    public OptionInstance<Double> chatDelay() {
        return this.chatDelay;
    }

    public OptionInstance<Double> notificationDisplayTime() {
        return this.notificationDisplayTime;
    }

    public OptionInstance<Integer> mipmapLevels() {
        return this.mipmapLevels;
    }

    public OptionInstance<AttackIndicatorStatus> attackIndicator() {
        return this.attackIndicator;
    }

    public OptionInstance<Integer> biomeBlendRadius() {
        return this.biomeBlendRadius;
    }

    private static double logMouse(int p_231966_) {
        return Math.pow(10.0, p_231966_ / 100.0);
    }

    private static int unlogMouse(double p_231840_) {
        return Mth.floor(Math.log10(p_231840_) * 100.0);
    }

    public OptionInstance<Double> mouseWheelSensitivity() {
        return this.mouseWheelSensitivity;
    }

    public OptionInstance<Boolean> rawMouseInput() {
        return this.rawMouseInput;
    }

    public OptionInstance<Boolean> autoJump() {
        return this.autoJump;
    }

    public OptionInstance<Boolean> rotateWithMinecart() {
        return this.rotateWithMinecart;
    }

    public OptionInstance<Boolean> operatorItemsTab() {
        return this.operatorItemsTab;
    }

    public OptionInstance<Boolean> autoSuggestions() {
        return this.autoSuggestions;
    }

    public OptionInstance<Boolean> chatColors() {
        return this.chatColors;
    }

    public OptionInstance<Boolean> chatLinks() {
        return this.chatLinks;
    }

    public OptionInstance<Boolean> chatLinksPrompt() {
        return this.chatLinksPrompt;
    }

    public OptionInstance<Boolean> enableVsync() {
        return this.enableVsync;
    }

    public OptionInstance<Boolean> entityShadows() {
        return this.entityShadows;
    }

    private static void updateFontOptions() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null) {
            minecraft.updateFontOptions();
            minecraft.resizeDisplay();
        }
    }

    public OptionInstance<Boolean> forceUnicodeFont() {
        return this.forceUnicodeFont;
    }

    private static boolean japaneseGlyphVariantsDefault() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
    }

    public OptionInstance<Boolean> japaneseGlyphVariants() {
        return this.japaneseGlyphVariants;
    }

    public OptionInstance<Boolean> invertYMouse() {
        return this.invertYMouse;
    }

    public OptionInstance<Boolean> discreteMouseScroll() {
        return this.discreteMouseScroll;
    }

    public OptionInstance<Boolean> realmsNotifications() {
        return this.realmsNotifications;
    }

    public OptionInstance<Boolean> allowServerListing() {
        return this.allowServerListing;
    }

    public OptionInstance<Boolean> reducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public final float getFinalSoundSourceVolume(SoundSource p_408775_) {
        return p_408775_ == SoundSource.MASTER ? this.getSoundSourceVolume(p_408775_) : this.getSoundSourceVolume(p_408775_) * this.getSoundSourceVolume(SoundSource.MASTER);
    }

    public final float getSoundSourceVolume(SoundSource p_92148_) {
        return this.getSoundSourceOptionInstance(p_92148_).get().floatValue();
    }

    public final OptionInstance<Double> getSoundSourceOptionInstance(SoundSource p_251574_) {
        return Objects.requireNonNull(this.soundSourceVolumes.get(p_251574_));
    }

    private OptionInstance<Double> createSoundSliderOptionInstance(String p_250353_, SoundSource p_249262_) {
        return new OptionInstance<>(
            p_250353_,
            OptionInstance.noTooltip(),
            Options::percentValueOrOffLabel,
            OptionInstance.UnitDouble.INSTANCE,
            1.0,
            p_247768_ -> Minecraft.getInstance().getSoundManager().updateSourceVolume(p_249262_, p_247768_.floatValue())
        );
    }

    public OptionInstance<Boolean> showSubtitles() {
        return this.showSubtitles;
    }

    public OptionInstance<Boolean> directionalAudio() {
        return this.directionalAudio;
    }

    public OptionInstance<Boolean> backgroundForChatOnly() {
        return this.backgroundForChatOnly;
    }

    public OptionInstance<Boolean> touchscreen() {
        return this.touchscreen;
    }

    public OptionInstance<Boolean> fullscreen() {
        return this.fullscreen;
    }

    public OptionInstance<Boolean> bobView() {
        return this.bobView;
    }

    public OptionInstance<Boolean> toggleCrouch() {
        return this.toggleCrouch;
    }

    public OptionInstance<Boolean> toggleSprint() {
        return this.toggleSprint;
    }

    public OptionInstance<Boolean> hideMatchedNames() {
        return this.hideMatchedNames;
    }

    public OptionInstance<Boolean> showAutosaveIndicator() {
        return this.showAutosaveIndicator;
    }

    public OptionInstance<Boolean> onlyShowSecureChat() {
        return this.onlyShowSecureChat;
    }

    public OptionInstance<Integer> fov() {
        return this.fov;
    }

    public OptionInstance<Boolean> telemetryOptInExtra() {
        return this.telemetryOptInExtra;
    }

    public OptionInstance<Double> screenEffectScale() {
        return this.screenEffectScale;
    }

    public OptionInstance<Double> fovEffectScale() {
        return this.fovEffectScale;
    }

    public OptionInstance<Double> darknessEffectScale() {
        return this.darknessEffectScale;
    }

    public OptionInstance<Double> glintSpeed() {
        return this.glintSpeed;
    }

    public OptionInstance<Double> glintStrength() {
        return this.glintStrength;
    }

    public OptionInstance<Double> damageTiltStrength() {
        return this.damageTiltStrength;
    }

    public OptionInstance<Double> gamma() {
        return this.gamma;
    }

    public OptionInstance<Integer> guiScale() {
        return this.guiScale;
    }

    public OptionInstance<ParticleStatus> particles() {
        return this.particles;
    }

    public OptionInstance<NarratorStatus> narrator() {
        return this.narrator;
    }

    public OptionInstance<String> soundDevice() {
        return this.soundDevice;
    }

    public void onboardingAccessibilityFinished() {
        this.onboardAccessibility = false;
        this.save();
    }

    public OptionInstance<MusicManager.MusicFrequency> musicFrequency() {
        return this.musicFrequency;
    }

    public OptionInstance<Boolean> showNowPlayingToast() {
        return this.showNowPlayingToast;
    }

    public Options(Minecraft p_92138_, File p_92139_) {
        setForgeKeybindProperties();
        this.minecraft = p_92138_;
        this.optionsFile = new File(p_92139_, "options.txt");
        boolean flag = Runtime.getRuntime().maxMemory() >= 1000000000L;
        this.renderDistance = new OptionInstance<>(
            "options.renderDistance",
            OptionInstance.noTooltip(),
            (p_404794_, p_404795_) -> genericValueLabel(p_404794_, Component.translatable("options.chunks", p_404795_)),
            new OptionInstance.IntRange(2, flag ? 32 : 16, false),
            12,
            p_231951_ -> Minecraft.getInstance().levelRenderer.needsUpdate()
        );
        this.simulationDistance = new OptionInstance<>(
            "options.simulationDistance",
            OptionInstance.noTooltip(),
            (p_231962_, p_407323_) -> genericValueLabel(p_231962_, Component.translatable("options.chunks", p_407323_)),
            new OptionInstance.IntRange(5, flag ? 32 : 16, false),
            12,
            p_409118_ -> {}
        );
        this.syncWrites = Util.getPlatform() == Util.OS.WINDOWS;
        this.load();
    }

    public float getBackgroundOpacity(float p_92142_) {
        return this.backgroundForChatOnly.get() ? p_92142_ : this.textBackgroundOpacity().get().floatValue();
    }

    public int getBackgroundColor(float p_92171_) {
        return ARGB.colorFromFloat(this.getBackgroundOpacity(p_92171_), 0.0F, 0.0F, 0.0F);
    }

    public int getBackgroundColor(int p_92144_) {
        return this.backgroundForChatOnly.get() ? p_92144_ : ARGB.colorFromFloat(this.textBackgroundOpacity.get().floatValue(), 0.0F, 0.0F, 0.0F);
    }

    private void processDumpedOptions(Options.OptionAccess p_329807_) {
        p_329807_.process("ao", this.ambientOcclusion);
        p_329807_.process("biomeBlendRadius", this.biomeBlendRadius);
        p_329807_.process("enableVsync", this.enableVsync);
        p_329807_.process("entityDistanceScaling", this.entityDistanceScaling);
        p_329807_.process("entityShadows", this.entityShadows);
        p_329807_.process("forceUnicodeFont", this.forceUnicodeFont);
        p_329807_.process("japaneseGlyphVariants", this.japaneseGlyphVariants);
        p_329807_.process("fov", this.fov);
        p_329807_.process("fovEffectScale", this.fovEffectScale);
        p_329807_.process("darknessEffectScale", this.darknessEffectScale);
        p_329807_.process("glintSpeed", this.glintSpeed);
        p_329807_.process("glintStrength", this.glintStrength);
        p_329807_.process("prioritizeChunkUpdates", this.prioritizeChunkUpdates);
        p_329807_.process("fullscreen", this.fullscreen);
        p_329807_.process("gamma", this.gamma);
        p_329807_.process("graphicsMode", this.graphicsMode);
        p_329807_.process("guiScale", this.guiScale);
        p_329807_.process("maxFps", this.framerateLimit);
        p_329807_.process("inactivityFpsLimit", this.inactivityFpsLimit);
        p_329807_.process("mipmapLevels", this.mipmapLevels);
        p_329807_.process("narrator", this.narrator);
        p_329807_.process("particles", this.particles);
        p_329807_.process("reducedDebugInfo", this.reducedDebugInfo);
        p_329807_.process("renderClouds", this.cloudStatus);
        p_329807_.process("cloudRange", this.cloudRange);
        p_329807_.process("renderDistance", this.renderDistance);
        p_329807_.process("simulationDistance", this.simulationDistance);
        p_329807_.process("screenEffectScale", this.screenEffectScale);
        p_329807_.process("soundDevice", this.soundDevice);
    }

    private void processOptions(Options.FieldAccess p_168428_) {
        this.processDumpedOptions(p_168428_);
        p_168428_.process("autoJump", this.autoJump);
        p_168428_.process("rotateWithMinecart", this.rotateWithMinecart);
        p_168428_.process("operatorItemsTab", this.operatorItemsTab);
        p_168428_.process("autoSuggestions", this.autoSuggestions);
        p_168428_.process("chatColors", this.chatColors);
        p_168428_.process("chatLinks", this.chatLinks);
        p_168428_.process("chatLinksPrompt", this.chatLinksPrompt);
        p_168428_.process("discrete_mouse_scroll", this.discreteMouseScroll);
        p_168428_.process("invertYMouse", this.invertYMouse);
        p_168428_.process("realmsNotifications", this.realmsNotifications);
        p_168428_.process("showSubtitles", this.showSubtitles);
        p_168428_.process("directionalAudio", this.directionalAudio);
        p_168428_.process("touchscreen", this.touchscreen);
        p_168428_.process("bobView", this.bobView);
        p_168428_.process("toggleCrouch", this.toggleCrouch);
        p_168428_.process("toggleSprint", this.toggleSprint);
        p_168428_.process("darkMojangStudiosBackground", this.darkMojangStudiosBackground);
        p_168428_.process("hideLightningFlashes", this.hideLightningFlash);
        p_168428_.process("hideSplashTexts", this.hideSplashTexts);
        p_168428_.process("mouseSensitivity", this.sensitivity);
        p_168428_.process("damageTiltStrength", this.damageTiltStrength);
        p_168428_.process("highContrast", this.highContrast);
        p_168428_.process("highContrastBlockOutline", this.highContrastBlockOutline);
        p_168428_.process("narratorHotkey", this.narratorHotkey);
        this.resourcePacks = p_168428_.process("resourcePacks", this.resourcePacks, Options::readListOfStrings, GSON::toJson);
        this.incompatibleResourcePacks = p_168428_.process("incompatibleResourcePacks", this.incompatibleResourcePacks, Options::readListOfStrings, GSON::toJson);
        this.lastMpIp = p_168428_.process("lastServer", this.lastMpIp);
        this.languageCode = p_168428_.process("lang", this.languageCode);
        p_168428_.process("chatVisibility", this.chatVisibility);
        p_168428_.process("chatOpacity", this.chatOpacity);
        p_168428_.process("chatLineSpacing", this.chatLineSpacing);
        p_168428_.process("textBackgroundOpacity", this.textBackgroundOpacity);
        p_168428_.process("backgroundForChatOnly", this.backgroundForChatOnly);
        this.hideServerAddress = p_168428_.process("hideServerAddress", this.hideServerAddress);
        this.advancedItemTooltips = p_168428_.process("advancedItemTooltips", this.advancedItemTooltips);
        this.pauseOnLostFocus = p_168428_.process("pauseOnLostFocus", this.pauseOnLostFocus);
        this.overrideWidth = p_168428_.process("overrideWidth", this.overrideWidth);
        this.overrideHeight = p_168428_.process("overrideHeight", this.overrideHeight);
        p_168428_.process("chatHeightFocused", this.chatHeightFocused);
        p_168428_.process("chatDelay", this.chatDelay);
        p_168428_.process("chatHeightUnfocused", this.chatHeightUnfocused);
        p_168428_.process("chatScale", this.chatScale);
        p_168428_.process("chatWidth", this.chatWidth);
        p_168428_.process("notificationDisplayTime", this.notificationDisplayTime);
        this.useNativeTransport = p_168428_.process("useNativeTransport", this.useNativeTransport);
        p_168428_.process("mainHand", this.mainHand);
        p_168428_.process("attackIndicator", this.attackIndicator);
        this.tutorialStep = p_168428_.process("tutorialStep", this.tutorialStep, TutorialSteps::getByName, TutorialSteps::getName);
        p_168428_.process("mouseWheelSensitivity", this.mouseWheelSensitivity);
        p_168428_.process("rawMouseInput", this.rawMouseInput);
        this.glDebugVerbosity = p_168428_.process("glDebugVerbosity", this.glDebugVerbosity);
        this.skipMultiplayerWarning = p_168428_.process("skipMultiplayerWarning", this.skipMultiplayerWarning);
        p_168428_.process("hideMatchedNames", this.hideMatchedNames);
        this.joinedFirstServer = p_168428_.process("joinedFirstServer", this.joinedFirstServer);
        this.syncWrites = p_168428_.process("syncChunkWrites", this.syncWrites);
        p_168428_.process("showAutosaveIndicator", this.showAutosaveIndicator);
        p_168428_.process("allowServerListing", this.allowServerListing);
        p_168428_.process("onlyShowSecureChat", this.onlyShowSecureChat);
        p_168428_.process("panoramaScrollSpeed", this.panoramaSpeed);
        p_168428_.process("telemetryOptInExtra", this.telemetryOptInExtra);
        this.onboardAccessibility = p_168428_.process("onboardAccessibility", this.onboardAccessibility);
        p_168428_.process("menuBackgroundBlurriness", this.menuBackgroundBlurriness);
        this.startedCleanly = p_168428_.process("startedCleanly", this.startedCleanly);
        p_168428_.process("showNowPlayingToast", this.showNowPlayingToast);
        p_168428_.process("musicFrequency", this.musicFrequency);
        processOptionsKeysOnly(p_168428_);
        processOptionsEnd(p_168428_);
    }

    // FORGE: split off to allow reloading keys after mod loading is done
    private void processOptionsKeysOnly(Options.FieldAccess p_168428_) {
        for (KeyMapping keymapping : this.keyMappings) {
            String s = keymapping.saveString() + (keymapping.getKeyModifier() != net.minecraftforge.client.settings.KeyModifier.NONE ? ":" + keymapping.getKeyModifier() : "");
            String s1 = p_168428_.process("key_" + keymapping.getName(), s);
            if (!s.equals(s1)) {
                keymapping.setKey(InputConstants.getKey(s1));
                if (s1.indexOf(':') != -1) {
                    String[] pts = s1.split(":");
                    keymapping.setKeyModifierAndCode(net.minecraftforge.client.settings.KeyModifier.valueFromString(pts[1]), InputConstants.getKey(pts[0]));
                } else {
                    keymapping.setKeyModifierAndCode(net.minecraftforge.client.settings.KeyModifier.NONE, InputConstants.getKey(s1));
                }
            }
        }
    }

    private void processOptionsEnd(Options.FieldAccess p_168428_) {
        for (SoundSource soundsource : SoundSource.values()) {
            p_168428_.process("soundCategory_" + soundsource.getName(), this.soundSourceVolumes.get(soundsource));
        }

        for (PlayerModelPart playermodelpart : PlayerModelPart.values()) {
            boolean flag = this.modelParts.contains(playermodelpart);
            boolean flag1 = p_168428_.process("modelPart_" + playermodelpart.getId(), flag);
            if (flag1 != flag) {
                this.setModelPart(playermodelpart, flag1);
            }
        }
    }

    public void load() {
        load(false);
    }

    public void load(boolean limited) {
        try {
            if (!this.optionsFile.exists()) {
                return;
            }

            CompoundTag compoundtag = new CompoundTag();

            try (BufferedReader bufferedreader = Files.newReader(this.optionsFile, Charsets.UTF_8)) {
                bufferedreader.lines().forEach(p_231896_ -> {
                    try {
                        Iterator<String> iterator = OPTION_SPLITTER.split(p_231896_).iterator();
                        compoundtag.putString(iterator.next(), iterator.next());
                    } catch (Exception exception1) {
                        LOGGER.warn("Skipping bad option: {}", p_231896_);
                    }
                });
            }

            final CompoundTag compoundtag1 = this.dataFix(compoundtag);
            Optional<String> optional = compoundtag1.getString("fancyGraphics");
            if (optional.isPresent() && !compoundtag1.contains("graphicsMode")) {
                this.graphicsMode.set(isTrue(optional.get()) ? GraphicsStatus.FANCY : GraphicsStatus.FAST);
            }

            java.util.function.Consumer<FieldAccess> processor = limited ? this::processOptionsKeysOnly : this::processOptions;
            processor.accept(
                new Options.FieldAccess() {
                    @Nullable
                    private String getValue(String p_394210_) {
                        Tag tag = compoundtag1.get(p_394210_);
                        if (tag == null) {
                            return null;
                        } else if (tag instanceof StringTag(String s)) {
                            return s;
                        } else {
                            throw new IllegalStateException("Cannot read field of wrong type, expected string: " + tag);
                        }
                    }

                    @Override
                    public <T> void process(String p_232125_, OptionInstance<T> p_232126_) {
                        String s = this.getValue(p_232125_);
                        if (s != null) {
                            JsonElement jsonelement = LenientJsonParser.parse(s.isEmpty() ? "\"\"" : s);
                            p_232126_.codec()
                                .parse(JsonOps.INSTANCE, jsonelement)
                                .ifError(
                                    p_389137_ -> Options.LOGGER.error("Error parsing option value {} for option {}: {}", s, p_232126_, p_389137_.message())
                                )
                                .ifSuccess(p_232126_::set);
                        }
                    }

                    @Override
                    public int process(String p_168467_, int p_168468_) {
                        String s = this.getValue(p_168467_);
                        if (s != null) {
                            try {
                                return Integer.parseInt(s);
                            } catch (NumberFormatException numberformatexception) {
                                Options.LOGGER.warn("Invalid integer value for option {} = {}", p_168467_, s, numberformatexception);
                            }
                        }

                        return p_168468_;
                    }

                    @Override
                    public boolean process(String p_168483_, boolean p_168484_) {
                        String s = this.getValue(p_168483_);
                        return s != null ? Options.isTrue(s) : p_168484_;
                    }

                    @Override
                    public String process(String p_168480_, String p_168481_) {
                        return MoreObjects.firstNonNull(this.getValue(p_168480_), p_168481_);
                    }

                    @Override
                    public float process(String p_168464_, float p_168465_) {
                        String s = this.getValue(p_168464_);
                        if (s == null) {
                            return p_168465_;
                        } else if (Options.isTrue(s)) {
                            return 1.0F;
                        } else if (Options.isFalse(s)) {
                            return 0.0F;
                        } else {
                            try {
                                return Float.parseFloat(s);
                            } catch (NumberFormatException numberformatexception) {
                                Options.LOGGER.warn("Invalid floating point value for option {} = {}", p_168464_, s, numberformatexception);
                                return p_168465_;
                            }
                        }
                    }

                    @Override
                    public <T> T process(String p_168470_, T p_168471_, Function<String, T> p_168472_, Function<T, String> p_168473_) {
                        String s = this.getValue(p_168470_);
                        return s == null ? p_168471_ : p_168472_.apply(s);
                    }
                }
            );
            compoundtag1.getString("fullscreenResolution").ifPresent(p_389133_ -> this.fullscreenVideoModeString = p_389133_);
            KeyMapping.resetMapping();

            if (limited) {
                this.unknownKeys.clear();
            } else {
                var knownKeys = Arrays.stream(this.keyMappings).map(k -> "key_" + k.getName()).collect(Collectors.toSet());
                for (var entry : compoundtag1.entrySet()) {
                    if (entry.getKey().startsWith("key_") && entry.getValue().asString().isPresent() && !knownKeys.contains(entry.getKey()))
                        this.unknownKeys.put(entry.getKey(), entry.getValue().asString().get());
                }
            }

        } catch (Exception exception) {
            LOGGER.error("Failed to load options", (Throwable)exception);
        }
    }

    static boolean isTrue(String p_168436_) {
        return "true".equals(p_168436_);
    }

    static boolean isFalse(String p_168441_) {
        return "false".equals(p_168441_);
    }

    private CompoundTag dataFix(CompoundTag p_92165_) {
        int i = 0;

        try {
            i = p_92165_.getString("version").map(Integer::parseInt).orElse(0);
        } catch (RuntimeException runtimeexception) {
        }

        return DataFixTypes.OPTIONS.updateToCurrentVersion(this.minecraft.getFixerUpper(), p_92165_, i);
    }

    public void save() {
        try (final PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8))) {
            printwriter.println("version:" + SharedConstants.getCurrentVersion().dataVersion().version());
            var seen = new java.util.HashSet<String>();
            this.processOptions(
                new Options.FieldAccess() {
                    public void writePrefix(String p_168491_) {
                        seen.add(p_168491_);
                        printwriter.print(p_168491_);
                        printwriter.print(':');
                    }

                    @Override
                    public <T> void process(String p_232135_, OptionInstance<T> p_232136_) {
                        p_232136_.codec()
                            .encodeStart(JsonOps.INSTANCE, p_232136_.get())
                            .ifError(p_325293_ -> Options.LOGGER.error("Error saving option " + p_232136_ + ": " + p_325293_))
                            .ifSuccess(p_232140_ -> {
                                this.writePrefix(p_232135_);
                                printwriter.println(Options.GSON.toJson(p_232140_));
                            });
                    }

                    @Override
                    public int process(String p_168499_, int p_168500_) {
                        this.writePrefix(p_168499_);
                        printwriter.println(p_168500_);
                        return p_168500_;
                    }

                    @Override
                    public boolean process(String p_168515_, boolean p_168516_) {
                        this.writePrefix(p_168515_);
                        printwriter.println(p_168516_);
                        return p_168516_;
                    }

                    @Override
                    public String process(String p_168512_, String p_168513_) {
                        this.writePrefix(p_168512_);
                        printwriter.println(p_168513_);
                        return p_168513_;
                    }

                    @Override
                    public float process(String p_168496_, float p_168497_) {
                        this.writePrefix(p_168496_);
                        printwriter.println(p_168497_);
                        return p_168497_;
                    }

                    @Override
                    public <T> T process(String p_168502_, T p_168503_, Function<String, T> p_168504_, Function<T, String> p_168505_) {
                        this.writePrefix(p_168502_);
                        printwriter.println(p_168505_.apply(p_168503_));
                        return p_168503_;
                    }
                }
            );
            String s = this.getFullscreenVideoModeString();
            if (s != null) {
                printwriter.println("fullscreenResolution:" + s);
            }
            // Forge add any unknown keys so that Mods can add keybindings between vanilla loading/saving its config.
            for (var entry : this.unknownKeys.entrySet()) {
                if (!seen.contains(entry.getKey()))
                    printwriter.println(entry.getKey() + ":" + entry.getValue());
            }

        } catch (Exception exception) {
            LOGGER.error("Failed to save options", (Throwable)exception);
        }

        this.broadcastOptions();
    }

    @Nullable
    private String getFullscreenVideoModeString() {
        Window window = this.minecraft.getWindow();
        if (window == null) {
            return this.fullscreenVideoModeString;
        } else {
            return window.getPreferredFullscreenVideoMode().isPresent() ? window.getPreferredFullscreenVideoMode().get().write() : null;
        }
    }

    public ClientInformation buildPlayerInformation() {
        int i = 0;

        for (PlayerModelPart playermodelpart : this.modelParts) {
            i |= playermodelpart.getMask();
        }

        return new ClientInformation(
            this.languageCode,
            this.renderDistance.get(),
            this.chatVisibility.get(),
            this.chatColors.get(),
            i,
            this.mainHand.get(),
            this.minecraft.isTextFilteringEnabled(),
            this.allowServerListing.get(),
            this.particles.get()
        );
    }

    public void broadcastOptions() {
        if (net.minecraftforge.client.loading.ClientModLoader.isLoading()) return; //Don't save settings before mods add keybindigns and the like to prevent them from being deleted.
        if (this.minecraft.player != null) {
            this.minecraft.player.connection.broadcastClientInformation(this.buildPlayerInformation());
        }
    }

    public void setModelPart(PlayerModelPart p_92155_, boolean p_92156_) {
        if (p_92156_) {
            this.modelParts.add(p_92155_);
        } else {
            this.modelParts.remove(p_92155_);
        }
    }

    public boolean isModelPartEnabled(PlayerModelPart p_168417_) {
        return this.modelParts.contains(p_168417_);
    }

    public CloudStatus getCloudsType() {
        return this.cloudStatus.get();
    }

    public boolean useNativeTransport() {
        return this.useNativeTransport;
    }

    public void loadSelectedResourcePacks(PackRepository p_92146_) {
        Set<String> set = Sets.newLinkedHashSet();
        Iterator<String> iterator = this.resourcePacks.iterator();

        while (iterator.hasNext()) {
            String s = iterator.next();
            Pack pack = p_92146_.getPack(s);
            if (pack == null && !s.startsWith("file/")) {
                pack = p_92146_.getPack("file/" + s);
            }

            if (pack == null) {
                LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", s);
                iterator.remove();
            } else if (!pack.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(s)) {
                LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", s);
                iterator.remove();
            } else if (pack.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(s)) {
                LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", s);
                this.incompatibleResourcePacks.remove(s);
            } else {
                set.add(pack.getId());
            }
        }

        p_92146_.setSelected(set);
    }

    public CameraType getCameraType() {
        return this.cameraType;
    }

    public void setCameraType(CameraType p_92158_) {
        this.cameraType = p_92158_;
    }

    private static List<String> readListOfStrings(String p_298720_) {
        List<String> list = GsonHelper.fromNullableJson(GSON, p_298720_, LIST_OF_STRINGS_TYPE);
        return (List<String>)(list != null ? list : Lists.newArrayList());
    }

    public File getFile() {
        return this.optionsFile;
    }

    public String dumpOptionsForReport() {
        final List<Pair<String, Object>> list = new ArrayList<>();
        this.processDumpedOptions(new Options.OptionAccess() {
            @Override
            public <T> void process(String p_328704_, OptionInstance<T> p_330356_) {
                list.add(Pair.of(p_328704_, p_330356_.get()));
            }
        });
        list.add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenVideoModeString)));
        list.add(Pair.of("glDebugVerbosity", this.glDebugVerbosity));
        list.add(Pair.of("overrideHeight", this.overrideHeight));
        list.add(Pair.of("overrideWidth", this.overrideWidth));
        list.add(Pair.of("syncChunkWrites", this.syncWrites));
        list.add(Pair.of("useNativeTransport", this.useNativeTransport));
        list.add(Pair.of("resourcePacks", this.resourcePacks));
        return list.stream()
            .sorted(Comparator.comparing(Pair::getFirst))
            .map(p_325285_ -> p_325285_.getFirst() + ": " + p_325285_.getSecond())
            .collect(Collectors.joining(System.lineSeparator()));
    }

    public void setServerRenderDistance(int p_193771_) {
        this.serverRenderDistance = p_193771_;
    }

    public int getEffectiveRenderDistance() {
        return this.serverRenderDistance > 0 ? Math.min(this.renderDistance.get(), this.serverRenderDistance) : this.renderDistance.get();
    }

    private static Component pixelValueLabel(Component p_231953_, int p_231954_) {
        return Component.translatable("options.pixel_value", p_231953_, p_231954_);
    }

    private static Component percentValueLabel(Component p_231898_, double p_231899_) {
        return Component.translatable("options.percent_value", p_231898_, (int)(p_231899_ * 100.0));
    }

    public static Component genericValueLabel(Component p_231922_, Component p_231923_) {
        return Component.translatable("options.generic_value", p_231922_, p_231923_);
    }

    public static Component genericValueLabel(Component p_231901_, int p_231902_) {
        return genericValueLabel(p_231901_, Component.literal(Integer.toString(p_231902_)));
    }

    private void setForgeKeybindProperties() {
        var inGame = net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;
        keyUp.setKeyConflictContext(inGame);
        keyLeft.setKeyConflictContext(inGame);
        keyDown.setKeyConflictContext(inGame);
        keyRight.setKeyConflictContext(inGame);
        keyJump.setKeyConflictContext(inGame);
        keyShift.setKeyConflictContext(inGame);
        keySprint.setKeyConflictContext(inGame);
        keyAttack.setKeyConflictContext(inGame);
        keyChat.setKeyConflictContext(inGame);
        keyPlayerList.setKeyConflictContext(inGame);
        keyCommand.setKeyConflictContext(inGame);
        keyTogglePerspective.setKeyConflictContext(inGame);
        keySmoothCamera.setKeyConflictContext(inGame);
    }

    public static Component genericValueOrOffLabel(Component p_345288_, int p_344826_) {
        return p_344826_ == 0 ? genericValueLabel(p_345288_, CommonComponents.OPTION_OFF) : genericValueLabel(p_345288_, p_344826_);
    }

    private static Component percentValueOrOffLabel(Component p_335881_, double p_328979_) {
        return p_328979_ == 0.0 ? genericValueLabel(p_335881_, CommonComponents.OPTION_OFF) : percentValueLabel(p_335881_, p_328979_);
    }

    @OnlyIn(Dist.CLIENT)
    public interface FieldAccess extends Options.OptionAccess {
        int process(String p_168523_, int p_168524_);

        boolean process(String p_168535_, boolean p_168536_);

        String process(String p_168533_, String p_168534_);

        float process(String p_168521_, float p_168522_);

        <T> T process(String p_168525_, T p_168526_, Function<String, T> p_168527_, Function<T, String> p_168528_);
    }

    @OnlyIn(Dist.CLIENT)
    interface OptionAccess {
        <T> void process(String p_330128_, OptionInstance<T> p_329013_);
    }
}
