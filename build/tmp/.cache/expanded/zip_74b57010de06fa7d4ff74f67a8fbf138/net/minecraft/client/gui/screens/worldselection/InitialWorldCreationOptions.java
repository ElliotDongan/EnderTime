package net.minecraft.client.gui.screens.worldselection;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record InitialWorldCreationOptions(
    WorldCreationUiState.SelectedGameMode selectedGameMode,
    Set<GameRules.Key<GameRules.BooleanValue>> disabledGameRules,
    @Nullable ResourceKey<FlatLevelGeneratorPreset> flatLevelPreset
) {
}