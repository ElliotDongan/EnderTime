/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

/** Forge 1.20.5 - Removed, Mojang created a layered rendering system that should make this all obsolete finally.. - Lex 042724
package net.minecraftforge.client.event;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when an overlay is rendered to the screen.
 * See the two subclasses for listening to the two possible phases.
 *
 * <p>An overlay that is not normally active cannot be forced to render. In such cases, this event will not fire.</p>
 *
 * @see Pre
 * @see Post
 * /
public abstract class RenderGuiOverlayEvent extends Event
{
    private final Window window;
    private final GuiGraphics guiGraphics;
    private final float partialTick;
    private final NamedGuiOverlay overlay;

    @ApiStatus.Internal
    protected RenderGuiOverlayEvent(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay)
    {
        this.window = window;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
        this.overlay = overlay;
    }

    public Window getWindow()
    {
        return window;
    }

    public GuiGraphics getGuiGraphics()
    {
        return guiGraphics;
    }

    public float getPartialTick()
    {
        return partialTick;
    }

    public NamedGuiOverlay getOverlay()
    {
        return overlay;
    }

    /**
     * Fired <b>before</b> a GUI overlay is rendered to the screen.
     *
     * <p>This event is {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.
     * If this event is cancelled, then the overlay will not be rendered, and the corresponding {@link Post} event will
     * not be fired.</p>
     *
     * <p>This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     *
     * @see Post
     * /
    @Cancelable
    public static class Pre extends RenderGuiOverlayEvent
    {
        @ApiStatus.Internal
        public Pre(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay)
        {
            super(window, guiGraphics, partialTick, overlay);
        }
    }

    /**
     * Fired <b>after</b> an GUI overlay is rendered to the screen, if the corresponding {@link Pre} is not cancelled.
     *
     * <p>This event is not {@linkplain Cancelable cancellable}, and does not {@linkplain HasResult have a result}.</p>
     *
     * <p>This event is fired on the {@linkplain MinecraftForge#EVENT_BUS main Forge event bus},
     * only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     * /
    public static class Post extends RenderGuiOverlayEvent
    {
        @ApiStatus.Internal
        public Post(Window window, GuiGraphics guiGraphics, float partialTick, NamedGuiOverlay overlay)
        {
            super(window, guiGraphics, partialTick, overlay);
        }
    }
}
*/
