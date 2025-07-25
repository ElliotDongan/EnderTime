/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Fired when a screenshot is taken, but before it is written to disk.
 *
 * <p>This event is {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.
 * If this event is cancelled, then the screenshot is not written to disk, and the message in the event will be posted
 * to the player's chat.</p>
 *
 * <p>This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus},
 * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 *
 * @see Screenshot
 */
public final class ScreenshotEvent extends MutableEvent implements Cancellable {
    public static final CancellableEventBus<ScreenshotEvent> BUS = CancellableEventBus.create(ScreenshotEvent.class);
    public static final Component DEFAULT_CANCEL_REASON = Component.literal("Screenshot canceled");

    private final NativeImage image;
    private File screenshotFile;

    private @Nullable Component resultMessage = null;

    @ApiStatus.Internal
    public ScreenshotEvent(NativeImage image, File screenshotFile) {
        this.image = image;
        this.screenshotFile = screenshotFile;
        try {
            this.screenshotFile = screenshotFile.getCanonicalFile(); // FORGE: Fix errors on Windows with paths that include \.\
        } catch (IOException ignored) { }
    }

    /** @return the in-memory image of the screenshot */
    public NativeImage getImage() {
        return this.image;
    }

    /** @return the file where the screenshot will be saved to */
    public File getScreenshotFile() {
        return this.screenshotFile;
    }

    /**
     * Sets the new file where the screenshot will be saved to.
     *
     * @param screenshotFile the new filepath
     */
    public void setScreenshotFile(File screenshotFile) {
        this.screenshotFile = screenshotFile;
    }

    /** @return the custom cancellation message, or {@code null} if no custom message is set */
    public @Nullable Component getResultMessage() {
        return this.resultMessage;
    }

    /**
     * Sets the new custom cancellation message used to inform the player.
     * <p>
     * It may be {@code null}, in which case the {@linkplain #DEFAULT_CANCEL_REASON default cancel reason} will be used.
     *
     * @param resultMessage the new result message
     */
    public void setResultMessage(Component resultMessage) {
        this.resultMessage = resultMessage;
    }

    /**
     * Returns the cancellation message to be used in informing the player.
     *
     * <p>If there is no custom message given ({@link #getResultMessage()} == {@code null}), then
     * the message will be the {@linkplain #DEFAULT_CANCEL_REASON default cancel reason message}.</p>
     *
     * @return the cancel message for the player
     */
    public Component getCancelMessage() {
        var message = this.getResultMessage();
        return message != null ? message : DEFAULT_CANCEL_REASON;
    }
}
