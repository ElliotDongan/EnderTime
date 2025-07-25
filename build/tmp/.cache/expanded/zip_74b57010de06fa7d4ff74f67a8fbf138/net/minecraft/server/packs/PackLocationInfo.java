package net.minecraft.server.packs;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;

public record PackLocationInfo(String id, Component title, PackSource source, Optional<KnownPack> knownPackInfo) {
    public Component createChatLink(boolean p_333920_, Component p_329432_) {
        return ComponentUtils.wrapInSquareBrackets(this.source.decorate(Component.literal(this.id)))
            .withStyle(
                p_390158_ -> p_390158_.withColor(p_333920_ ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .withInsertion(StringArgumentType.escapeIfRequired(this.id))
                    .withHoverEvent(new HoverEvent.ShowText(Component.empty().append(this.title).append("\n").append(p_329432_)))
            );
    }
}