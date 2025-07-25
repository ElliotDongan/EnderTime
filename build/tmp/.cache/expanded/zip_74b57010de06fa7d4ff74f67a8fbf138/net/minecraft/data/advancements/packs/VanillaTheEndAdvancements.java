package net.minecraft.data.advancements.packs;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LevitationTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

public class VanillaTheEndAdvancements implements AdvancementSubProvider {
    @Override
    public void generate(HolderLookup.Provider p_256214_, Consumer<AdvancementHolder> p_250851_) {
        HolderGetter<EntityType<?>> holdergetter = p_256214_.lookupOrThrow(Registries.ENTITY_TYPE);
        AdvancementHolder advancementholder = Advancement.Builder.advancement()
            .display(
                Blocks.END_STONE,
                Component.translatable("advancements.end.root.title"),
                Component.translatable("advancements.end.root.description"),
                ResourceLocation.withDefaultNamespace("gui/advancements/backgrounds/end"),
                AdvancementType.TASK,
                false,
                false,
                false
            )
            .addCriterion("entered_end", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(Level.END))
            .save(p_250851_, "end/root");
        AdvancementHolder advancementholder1 = Advancement.Builder.advancement()
            .parent(advancementholder)
            .display(
                Blocks.DRAGON_HEAD,
                Component.translatable("advancements.end.kill_dragon.title"),
                Component.translatable("advancements.end.kill_dragon.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("killed_dragon", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(holdergetter, EntityType.ENDER_DRAGON)))
            .save(p_250851_, "end/kill_dragon");
        AdvancementHolder advancementholder2 = Advancement.Builder.advancement()
            .parent(advancementholder1)
            .display(
                Items.ENDER_PEARL,
                Component.translatable("advancements.end.enter_end_gateway.title"),
                Component.translatable("advancements.end.enter_end_gateway.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("entered_end_gateway", EnterBlockTrigger.TriggerInstance.entersBlock(Blocks.END_GATEWAY))
            .save(p_250851_, "end/enter_end_gateway");
        Advancement.Builder.advancement()
            .parent(advancementholder1)
            .display(
                Items.END_CRYSTAL,
                Component.translatable("advancements.end.respawn_dragon.title"),
                Component.translatable("advancements.end.respawn_dragon.description"),
                null,
                AdvancementType.GOAL,
                true,
                true,
                false
            )
            .addCriterion(
                "summoned_dragon",
                SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(holdergetter, EntityType.ENDER_DRAGON))
            )
            .save(p_250851_, "end/respawn_dragon");
        AdvancementHolder advancementholder3 = Advancement.Builder.advancement()
            .parent(advancementholder2)
            .display(
                Blocks.PURPUR_BLOCK,
                Component.translatable("advancements.end.find_end_city.title"),
                Component.translatable("advancements.end.find_end_city.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion(
                "in_city",
                PlayerTrigger.TriggerInstance.located(
                    LocationPredicate.Builder.inStructure(p_256214_.lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.END_CITY))
                )
            )
            .save(p_250851_, "end/find_end_city");
        Advancement.Builder.advancement()
            .parent(advancementholder1)
            .display(
                Items.DRAGON_BREATH,
                Component.translatable("advancements.end.dragon_breath.title"),
                Component.translatable("advancements.end.dragon_breath.description"),
                null,
                AdvancementType.GOAL,
                true,
                true,
                false
            )
            .addCriterion("dragon_breath", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DRAGON_BREATH))
            .save(p_250851_, "end/dragon_breath");
        Advancement.Builder.advancement()
            .parent(advancementholder3)
            .display(
                Items.SHULKER_SHELL,
                Component.translatable("advancements.end.levitate.title"),
                Component.translatable("advancements.end.levitate.description"),
                null,
                AdvancementType.CHALLENGE,
                true,
                true,
                false
            )
            .rewards(AdvancementRewards.Builder.experience(50))
            .addCriterion("levitated", LevitationTrigger.TriggerInstance.levitated(DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(50.0))))
            .save(p_250851_, "end/levitate");
        Advancement.Builder.advancement()
            .parent(advancementholder3)
            .display(
                Items.ELYTRA,
                Component.translatable("advancements.end.elytra.title"),
                Component.translatable("advancements.end.elytra.description"),
                null,
                AdvancementType.GOAL,
                true,
                true,
                false
            )
            .addCriterion("elytra", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ELYTRA))
            .save(p_250851_, "end/elytra");
        Advancement.Builder.advancement()
            .parent(advancementholder1)
            .display(
                Blocks.DRAGON_EGG,
                Component.translatable("advancements.end.dragon_egg.title"),
                Component.translatable("advancements.end.dragon_egg.description"),
                null,
                AdvancementType.GOAL,
                true,
                true,
                false
            )
            .addCriterion("dragon_egg", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.DRAGON_EGG))
            .save(p_250851_, "end/dragon_egg");
    }
}