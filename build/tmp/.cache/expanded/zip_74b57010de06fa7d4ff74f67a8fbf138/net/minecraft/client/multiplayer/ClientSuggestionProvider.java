package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.PermissionSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSuggestionProvider implements PermissionSource, SharedSuggestionProvider {
    private final ClientPacketListener connection;
    private final Minecraft minecraft;
    private int pendingSuggestionsId = -1;
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestionsFuture;
    private final Set<String> customCompletionSuggestions = new HashSet<>();
    private final boolean allowsRestrictedCommands;

    public ClientSuggestionProvider(ClientPacketListener p_105165_, Minecraft p_105166_, boolean p_408965_) {
        this.connection = p_105165_;
        this.minecraft = p_105166_;
        this.allowsRestrictedCommands = p_408965_;
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        List<String> list = Lists.newArrayList();

        for (PlayerInfo playerinfo : this.connection.getOnlinePlayers()) {
            list.add(playerinfo.getProfile().getName());
        }

        return list;
    }

    @Override
    public Collection<String> getCustomTabSugggestions() {
        if (this.customCompletionSuggestions.isEmpty()) {
            return this.getOnlinePlayerNames();
        } else {
            Set<String> set = new HashSet<>(this.getOnlinePlayerNames());
            set.addAll(this.customCompletionSuggestions);
            return set;
        }
    }

    @Override
    public Collection<String> getSelectedEntities() {
        return (Collection<String>)(this.minecraft.hitResult != null && this.minecraft.hitResult.getType() == HitResult.Type.ENTITY
            ? Collections.singleton(((EntityHitResult)this.minecraft.hitResult).getEntity().getStringUUID())
            : Collections.emptyList());
    }

    @Override
    public Collection<String> getAllTeams() {
        return this.connection.scoreboard().getTeamNames();
    }

    @Override
    public Stream<ResourceLocation> getAvailableSounds() {
        return this.minecraft.getSoundManager().getAvailableSounds().stream();
    }

    @Override
    public boolean hasPermission(int p_105178_) {
        return this.allowsRestrictedCommands || p_105178_ == 0;
    }

    @Override
    public boolean allowsSelectors() {
        return this.allowsRestrictedCommands;
    }

    @Override
    public CompletableFuture<Suggestions> suggestRegistryElements(
        ResourceKey<? extends Registry<?>> p_212429_,
        SharedSuggestionProvider.ElementSuggestionType p_212430_,
        SuggestionsBuilder p_212431_,
        CommandContext<?> p_212432_
    ) {
        return this.registryAccess().lookup(p_212429_).map(p_404893_ -> {
            this.suggestRegistryElements(p_404893_, p_212430_, p_212431_);
            return p_212431_.buildFuture();
        }).orElseGet(() -> this.customSuggestion(p_212432_));
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> p_212423_) {
        if (this.pendingSuggestionsFuture != null) {
            this.pendingSuggestionsFuture.cancel(false);
        }

        this.pendingSuggestionsFuture = new CompletableFuture<>();
        int i = ++this.pendingSuggestionsId;
        this.connection.send(new ServerboundCommandSuggestionPacket(i, p_212423_.getInput()));
        return this.pendingSuggestionsFuture;
    }

    private static String prettyPrint(double p_105168_) {
        return String.format(Locale.ROOT, "%.2f", p_105168_);
    }

    private static String prettyPrint(int p_105170_) {
        return Integer.toString(p_105170_);
    }

    @Override
    public Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
        HitResult hitresult = this.minecraft.hitResult;
        if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
            return Collections.singleton(
                new SharedSuggestionProvider.TextCoordinates(prettyPrint(blockpos.getX()), prettyPrint(blockpos.getY()), prettyPrint(blockpos.getZ()))
            );
        } else {
            return SharedSuggestionProvider.super.getRelevantCoordinates();
        }
    }

    @Override
    public Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
        HitResult hitresult = this.minecraft.hitResult;
        if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            Vec3 vec3 = hitresult.getLocation();
            return Collections.singleton(
                new SharedSuggestionProvider.TextCoordinates(prettyPrint(vec3.x), prettyPrint(vec3.y), prettyPrint(vec3.z))
            );
        } else {
            return SharedSuggestionProvider.super.getAbsoluteCoordinates();
        }
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        return this.connection.levels();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.connection.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.connection.enabledFeatures();
    }

    public void completeCustomSuggestions(int p_105172_, Suggestions p_105173_) {
        if (p_105172_ == this.pendingSuggestionsId) {
            this.pendingSuggestionsFuture.complete(p_105173_);
            this.pendingSuggestionsFuture = null;
            this.pendingSuggestionsId = -1;
        }
    }

    public void modifyCustomCompletions(ClientboundCustomChatCompletionsPacket.Action p_240810_, List<String> p_240765_) {
        switch (p_240810_) {
            case ADD:
                this.customCompletionSuggestions.addAll(p_240765_);
                break;
            case REMOVE:
                p_240765_.forEach(this.customCompletionSuggestions::remove);
                break;
            case SET:
                this.customCompletionSuggestions.clear();
                this.customCompletionSuggestions.addAll(p_240765_);
        }
    }

    public boolean allowsRestrictedCommands() {
        return this.allowsRestrictedCommands;
    }
}