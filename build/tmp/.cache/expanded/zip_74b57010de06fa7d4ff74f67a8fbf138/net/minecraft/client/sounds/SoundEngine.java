package net.minecraft.client.sounds;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@OnlyIn(Dist.CLIENT)
public class SoundEngine {
    private static final Marker MARKER = MarkerFactory.getMarker("SOUNDS");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float PITCH_MIN = 0.5F;
    private static final float PITCH_MAX = 2.0F;
    private static final float VOLUME_MIN = 0.0F;
    private static final float VOLUME_MAX = 1.0F;
    private static final int MIN_SOURCE_LIFETIME = 20;
    private static final Set<ResourceLocation> ONLY_WARN_ONCE = Sets.newHashSet();
    private static final long DEFAULT_DEVICE_CHECK_INTERVAL_MS = 1000L;
    public static final String MISSING_SOUND = "FOR THE DEBUG!";
    public static final String OPEN_AL_SOFT_PREFIX = "OpenAL Soft on ";
    public static final int OPEN_AL_SOFT_PREFIX_LENGTH = "OpenAL Soft on ".length();
    private final MusicManager musicManager;
    public final SoundManager soundManager;
    private final Options options;
    private boolean loaded;
    private final Library library = new Library();
    private final Listener listener = this.library.getListener();
    private final SoundBufferLibrary soundBuffers;
    private final SoundEngineExecutor executor = new SoundEngineExecutor();
    private final ChannelAccess channelAccess = new ChannelAccess(this.library, this.executor);
    private int tickCount;
    private long lastDeviceCheckTime;
    private final AtomicReference<SoundEngine.DeviceCheckState> devicePoolState = new AtomicReference<>(SoundEngine.DeviceCheckState.NO_CHANGE);
    private final Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel = Maps.newHashMap();
    private final Multimap<SoundSource, SoundInstance> instanceBySource = HashMultimap.create();
    private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
    private final Map<SoundInstance, Integer> queuedSounds = Maps.newHashMap();
    private final Map<SoundInstance, Integer> soundDeleteTime = Maps.newHashMap();
    private final List<SoundEventListener> listeners = Lists.newArrayList();
    private final List<TickableSoundInstance> queuedTickableSounds = Lists.newArrayList();
    private final List<Sound> preloadQueue = Lists.newArrayList();

    public SoundEngine(MusicManager p_406379_, SoundManager p_120236_, Options p_120237_, ResourceProvider p_249332_) {
        this.musicManager = p_406379_;
        this.soundManager = p_120236_;
        this.options = p_120237_;
        this.soundBuffers = new SoundBufferLibrary(p_249332_);
        net.minecraftforge.fml.ModLoader.get().postEvent(new net.minecraftforge.client.event.sound.SoundEngineLoadEvent(this));
    }

    public void reload() {
        ONLY_WARN_ONCE.clear();

        for (SoundEvent soundevent : BuiltInRegistries.SOUND_EVENT) {
            if (soundevent != SoundEvents.EMPTY) {
                ResourceLocation resourcelocation = soundevent.location();
                if (this.soundManager.getSoundEvent(resourcelocation) == null) {
                    LOGGER.warn("Missing sound for event: {}", BuiltInRegistries.SOUND_EVENT.getKey(soundevent));
                    ONLY_WARN_ONCE.add(resourcelocation);
                }
            }
        }

        this.destroy();
        this.loadLibrary();
        net.minecraftforge.fml.ModLoader.get().postEvent(new net.minecraftforge.client.event.sound.SoundEngineLoadEvent(this));
    }

    private synchronized void loadLibrary() {
        if (!this.loaded) {
            try {
                String s = this.options.soundDevice().get();
                this.library.init("".equals(s) ? null : s, this.options.directionalAudio().get());
                this.listener.reset();
                this.listener.setGain(this.options.getSoundSourceVolume(SoundSource.MASTER));
                this.soundBuffers.preload(this.preloadQueue).thenRun(this.preloadQueue::clear);
                this.loaded = true;
                LOGGER.info(MARKER, "Sound engine started");
            } catch (RuntimeException runtimeexception) {
                LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", (Throwable)runtimeexception);
            }
        }
    }

    private float getVolume(@Nullable SoundSource p_120259_) {
        return p_120259_ != null && p_120259_ != SoundSource.MASTER ? this.options.getSoundSourceVolume(p_120259_) : 1.0F;
    }

    public void updateCategoryVolume(SoundSource p_120261_, float p_120262_) {
        if (this.loaded) {
            if (p_120261_ == SoundSource.MASTER) {
                this.listener.setGain(p_120262_);
            } else {
                if (p_120261_ == SoundSource.MUSIC && this.options.getSoundSourceVolume(SoundSource.MUSIC) > 0.0F) {
                    this.musicManager.showNowPlayingToastIfNeeded();
                }

                this.instanceToChannel.forEach((p_120280_, p_120281_) -> {
                    float f = this.calculateVolume(p_120280_);
                    p_120281_.execute(p_405035_ -> p_405035_.setVolume(f));
                });
            }
        }
    }

    public void destroy() {
        if (this.loaded) {
            this.stopAll();
            this.soundBuffers.clear();
            this.library.cleanup();
            this.loaded = false;
        }
    }

    public void emergencyShutdown() {
        if (this.loaded) {
            this.library.cleanup();
        }
    }

    public void stop(SoundInstance p_120275_) {
        if (this.loaded) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(p_120275_);
            if (channelaccess$channelhandle != null) {
                channelaccess$channelhandle.execute(Channel::stop);
            }
        }
    }

    public void setVolume(SoundInstance p_377242_, float p_376894_) {
        if (this.loaded) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(p_377242_);
            if (channelaccess$channelhandle != null) {
                channelaccess$channelhandle.execute(p_374745_ -> p_374745_.setVolume(p_376894_ * this.calculateVolume(p_377242_)));
            }
        }
    }

    public void stopAll() {
        if (this.loaded) {
            this.executor.flush();
            this.instanceToChannel.values().forEach(p_120288_ -> p_120288_.execute(Channel::stop));
            this.instanceToChannel.clear();
            this.channelAccess.clear();
            this.queuedSounds.clear();
            this.tickingSounds.clear();
            this.instanceBySource.clear();
            this.soundDeleteTime.clear();
            this.queuedTickableSounds.clear();
        }
    }

    public void addEventListener(SoundEventListener p_120296_) {
        this.listeners.add(p_120296_);
    }

    public void removeEventListener(SoundEventListener p_120308_) {
        this.listeners.remove(p_120308_);
    }

    private boolean shouldChangeDevice() {
        if (this.library.isCurrentDeviceDisconnected()) {
            LOGGER.info("Audio device was lost!");
            return true;
        } else {
            long i = Util.getMillis();
            boolean flag = i - this.lastDeviceCheckTime >= 1000L;
            if (flag) {
                this.lastDeviceCheckTime = i;
                if (this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.NO_CHANGE, SoundEngine.DeviceCheckState.ONGOING)) {
                    String s = this.options.soundDevice().get();
                    Util.ioPool().execute(() -> {
                        if ("".equals(s)) {
                            if (this.library.hasDefaultDeviceChanged()) {
                                LOGGER.info("System default audio device has changed!");
                                this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
                            }
                        } else if (!this.library.getCurrentDeviceName().equals(s) && this.library.getAvailableSoundDevices().contains(s)) {
                            LOGGER.info("Preferred audio device has become available!");
                            this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
                        }

                        this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.NO_CHANGE);
                    });
                }
            }

            return this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.CHANGE_DETECTED, SoundEngine.DeviceCheckState.NO_CHANGE);
        }
    }

    public void tick(boolean p_120303_) {
        if (this.shouldChangeDevice()) {
            this.reload();
        }

        if (!p_120303_) {
            this.tickInGameSound();
        } else {
            this.tickMusicWhenPaused();
        }

        this.channelAccess.scheduleTick();
    }

    private void tickInGameSound() {
        this.tickCount++;
        this.queuedTickableSounds.stream().filter(SoundInstance::canPlaySound).forEach(this::play);
        this.queuedTickableSounds.clear();

        for (TickableSoundInstance tickablesoundinstance : this.tickingSounds) {
            if (!tickablesoundinstance.canPlaySound()) {
                this.stop(tickablesoundinstance);
            }

            tickablesoundinstance.tick();
            if (tickablesoundinstance.isStopped()) {
                this.stop(tickablesoundinstance);
            } else {
                float f = this.calculateVolume(tickablesoundinstance);
                float f1 = this.calculatePitch(tickablesoundinstance);
                Vec3 vec3 = new Vec3(tickablesoundinstance.getX(), tickablesoundinstance.getY(), tickablesoundinstance.getZ());
                ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(tickablesoundinstance);
                if (channelaccess$channelhandle != null) {
                    channelaccess$channelhandle.execute(p_194478_ -> {
                        p_194478_.setVolume(f);
                        p_194478_.setPitch(f1);
                        p_194478_.setSelfPosition(vec3);
                    });
                }
            }
        }

        Iterator<Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            ChannelAccess.ChannelHandle channelaccess$channelhandle1 = entry.getValue();
            SoundInstance soundinstance = entry.getKey();
            if (channelaccess$channelhandle1.isStopped()) {
                int i = this.soundDeleteTime.get(soundinstance);
                if (i <= this.tickCount) {
                    if (shouldLoopManually(soundinstance)) {
                        this.queuedSounds.put(soundinstance, this.tickCount + soundinstance.getDelay());
                    }

                    iterator.remove();
                    LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", channelaccess$channelhandle1);
                    this.soundDeleteTime.remove(soundinstance);

                    try {
                        this.instanceBySource.remove(soundinstance.getSource(), soundinstance);
                    } catch (RuntimeException runtimeexception) {
                    }

                    if (soundinstance instanceof TickableSoundInstance) {
                        this.tickingSounds.remove(soundinstance);
                    }
                }
            }
        }

        Iterator<Entry<SoundInstance, Integer>> iterator1 = this.queuedSounds.entrySet().iterator();

        while (iterator1.hasNext()) {
            Entry<SoundInstance, Integer> entry1 = iterator1.next();
            if (this.tickCount >= entry1.getValue()) {
                SoundInstance soundinstance1 = entry1.getKey();
                if (soundinstance1 instanceof TickableSoundInstance) {
                    ((TickableSoundInstance)soundinstance1).tick();
                }

                this.play(soundinstance1);
                iterator1.remove();
            }
        }
    }

    private void tickMusicWhenPaused() {
        Iterator<Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            ChannelAccess.ChannelHandle channelaccess$channelhandle = entry.getValue();
            SoundInstance soundinstance = entry.getKey();
            if (soundinstance.getSource() == SoundSource.MUSIC && channelaccess$channelhandle.isStopped()) {
                iterator.remove();
                LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", channelaccess$channelhandle);
                this.soundDeleteTime.remove(soundinstance);
                this.instanceBySource.remove(soundinstance.getSource(), soundinstance);
            }
        }
    }

    private static boolean requiresManualLooping(SoundInstance p_120316_) {
        return p_120316_.getDelay() > 0;
    }

    private static boolean shouldLoopManually(SoundInstance p_120319_) {
        return p_120319_.isLooping() && requiresManualLooping(p_120319_);
    }

    private static boolean shouldLoopAutomatically(SoundInstance p_120322_) {
        return p_120322_.isLooping() && !requiresManualLooping(p_120322_);
    }

    public boolean isActive(SoundInstance p_120306_) {
        if (!this.loaded) {
            return false;
        } else {
            return this.soundDeleteTime.containsKey(p_120306_) && this.soundDeleteTime.get(p_120306_) <= this.tickCount ? true : this.instanceToChannel.containsKey(p_120306_);
        }
    }

    public SoundEngine.PlayResult play(SoundInstance p_120313_) {
        if (!this.loaded) {
            return SoundEngine.PlayResult.NOT_STARTED;
        } else if (!p_120313_.canPlaySound()) {
            return SoundEngine.PlayResult.NOT_STARTED;
        } else {
            p_120313_ = net.minecraftforge.client.ForgeHooksClient.playSound(this, p_120313_);
            if (p_120313_ == null || !p_120313_.canPlaySound())
                return SoundEngine.PlayResult.NOT_STARTED;
            WeighedSoundEvents weighedsoundevents = p_120313_.resolve(this.soundManager);
            ResourceLocation resourcelocation = p_120313_.getLocation();
            if (weighedsoundevents == null) {
                if (ONLY_WARN_ONCE.add(resourcelocation)) {
                    LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", resourcelocation);
                }

                return SoundEngine.PlayResult.NOT_STARTED;
            } else {
                Sound sound = p_120313_.getSound();
                if (sound == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
                    return SoundEngine.PlayResult.NOT_STARTED;
                } else if (sound == SoundManager.EMPTY_SOUND) {
                    if (ONLY_WARN_ONCE.add(resourcelocation)) {
                        LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", resourcelocation);
                    }

                    return SoundEngine.PlayResult.NOT_STARTED;
                } else {
                    float f = p_120313_.getVolume();
                    float f1 = Math.max(f, 1.0F) * sound.getAttenuationDistance();
                    SoundSource soundsource = p_120313_.getSource();
                    float f2 = this.calculateVolume(f, soundsource);
                    float f3 = this.calculatePitch(p_120313_);
                    SoundInstance.Attenuation soundinstance$attenuation = p_120313_.getAttenuation();
                    boolean flag = p_120313_.isRelative();
                    if (!this.listeners.isEmpty()) {
                        float f4 = !flag && soundinstance$attenuation != SoundInstance.Attenuation.NONE ? f1 : Float.POSITIVE_INFINITY;

                        for (SoundEventListener soundeventlistener : this.listeners) {
                            soundeventlistener.onPlaySound(p_120313_, weighedsoundevents, f4);
                        }
                    }

                    boolean flag2 = false;
                    if (f2 == 0.0F) {
                        if (!p_120313_.canStartSilent() && soundsource != SoundSource.MUSIC) {
                            LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", sound.getLocation());
                            return SoundEngine.PlayResult.NOT_STARTED;
                        }

                        flag2 = true;
                    }

                    Vec3 vec3 = new Vec3(p_120313_.getX(), p_120313_.getY(), p_120313_.getZ());
                    if (this.listener.getGain() <= 0.0F && soundsource != SoundSource.MUSIC) {
                        LOGGER.debug(MARKER, "Skipped playing soundEvent: {}, master volume was zero", resourcelocation);
                        return SoundEngine.PlayResult.NOT_STARTED;
                    } else {
                        boolean flag3 = shouldLoopAutomatically(p_120313_);
                        boolean flag1 = sound.shouldStream();
                        CompletableFuture<ChannelAccess.ChannelHandle> completablefuture = this.channelAccess
                            .createHandle(sound.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
                        ChannelAccess.ChannelHandle channelaccess$channelhandle = completablefuture.join();
                        if (channelaccess$channelhandle == null) {
                            if (SharedConstants.IS_RUNNING_IN_IDE) {
                                LOGGER.warn("Failed to create new sound handle");
                            }

                            return SoundEngine.PlayResult.NOT_STARTED;
                        } else {
                            LOGGER.debug(MARKER, "Playing sound {} for event {}", sound.getLocation(), resourcelocation);
                            this.soundDeleteTime.put(p_120313_, this.tickCount + 20);
                            this.instanceToChannel.put(p_120313_, channelaccess$channelhandle);
                            this.instanceBySource.put(soundsource, p_120313_);
                            channelaccess$channelhandle.execute(p_194488_ -> {
                                p_194488_.setPitch(f3);
                                p_194488_.setVolume(f2);
                                if (soundinstance$attenuation == SoundInstance.Attenuation.LINEAR) {
                                    p_194488_.linearAttenuation(f1);
                                } else {
                                    p_194488_.disableAttenuation();
                                }

                                p_194488_.setLooping(flag3 && !flag1);
                                p_194488_.setSelfPosition(vec3);
                                p_194488_.setRelative(flag);
                            });
                            SoundInstance soundinstance = p_120313_;
                            if (!flag1) {
                                this.soundBuffers.getCompleteBuffer(sound.getPath()).thenAccept(p_377326_ -> channelaccess$channelhandle.execute(p_194495_ -> {
                                    p_194495_.attachStaticBuffer(p_377326_);
                                    p_194495_.play();
                                    net.minecraftforge.client.event.ForgeEventFactoryClient.onPlaySoundSource(this, soundinstance, p_194495_);
                                }));
                            } else {
                                this.soundBuffers.getStream(sound.getPath(), flag3).thenAccept(p_376862_ -> channelaccess$channelhandle.execute(p_194498_ -> {
                                    p_194498_.attachBufferStream(p_376862_);
                                    p_194498_.play();
                                    net.minecraftforge.client.event.ForgeEventFactoryClient.onPlayStreamingSource(this, soundinstance, p_194498_);
                                }));
                            }

                            if (p_120313_ instanceof TickableSoundInstance) {
                                this.tickingSounds.add((TickableSoundInstance)p_120313_);
                            }

                            return flag2 ? SoundEngine.PlayResult.STARTED_SILENTLY : SoundEngine.PlayResult.STARTED;
                        }
                    }
                }
            }
        }
    }

    public void queueTickingSound(TickableSoundInstance p_120283_) {
        this.queuedTickableSounds.add(p_120283_);
    }

    public void requestPreload(Sound p_120273_) {
        this.preloadQueue.add(p_120273_);
    }

    private float calculatePitch(SoundInstance p_120325_) {
        return Mth.clamp(p_120325_.getPitch(), 0.5F, 2.0F);
    }

    private float calculateVolume(SoundInstance p_120328_) {
        return this.calculateVolume(p_120328_.getVolume(), p_120328_.getSource());
    }

    private float calculateVolume(float p_235258_, SoundSource p_235259_) {
        return Mth.clamp(p_235258_ * this.getVolume(p_235259_), 0.0F, 1.0F);
    }

    public void pauseAllExcept(SoundSource... p_406622_) {
        if (this.loaded) {
            for (Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : this.instanceToChannel.entrySet()) {
                if (!List.of(p_406622_).contains(entry.getKey().getSource())) {
                    entry.getValue().execute(Channel::pause);
                }
            }
        }
    }

    public void resume() {
        if (this.loaded) {
            this.channelAccess.executeOnChannels(p_194510_ -> p_194510_.forEach(Channel::unpause));
        }
    }

    public void playDelayed(SoundInstance p_120277_, int p_120278_) {
        this.queuedSounds.put(p_120277_, this.tickCount + p_120278_);
    }

    public void updateSource(Camera p_120271_) {
        if (this.loaded && p_120271_.isInitialized()) {
            ListenerTransform listenertransform = new ListenerTransform(p_120271_.getPosition(), new Vec3(p_120271_.getLookVector()), new Vec3(p_120271_.getUpVector()));
            this.executor.execute(() -> this.listener.setTransform(listenertransform));
        }
    }

    public void stop(@Nullable ResourceLocation p_120300_, @Nullable SoundSource p_120301_) {
        if (p_120301_ != null) {
            for (SoundInstance soundinstance : this.instanceBySource.get(p_120301_)) {
                if (p_120300_ == null || soundinstance.getLocation().equals(p_120300_)) {
                    this.stop(soundinstance);
                }
            }
        } else if (p_120300_ == null) {
            this.stopAll();
        } else {
            for (SoundInstance soundinstance1 : this.instanceToChannel.keySet()) {
                if (soundinstance1.getLocation().equals(p_120300_)) {
                    this.stop(soundinstance1);
                }
            }
        }
    }

    public String getDebugString() {
        return this.library.getDebugString();
    }

    public List<String> getAvailableSoundDevices() {
        return this.library.getAvailableSoundDevices();
    }

    public ListenerTransform getListenerTransform() {
        return this.listener.getTransform();
    }

    @OnlyIn(Dist.CLIENT)
    static enum DeviceCheckState {
        ONGOING,
        CHANGE_DETECTED,
        NO_CHANGE;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum PlayResult {
        STARTED,
        STARTED_SILENTLY,
        NOT_STARTED;
    }
}
