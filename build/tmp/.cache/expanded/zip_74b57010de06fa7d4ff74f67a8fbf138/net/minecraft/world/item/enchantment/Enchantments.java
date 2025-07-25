package net.minecraft.world.item.enchantment;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.MovementPredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.item.enchantment.effects.ChangeItemDamage;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.ExplodeEffect;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;
import net.minecraft.world.item.enchantment.effects.PlaySoundEffect;
import net.minecraft.world.item.enchantment.effects.RemoveBinomial;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.item.enchantment.effects.SetValue;
import net.minecraft.world.item.enchantment.effects.SpawnParticlesEffect;
import net.minecraft.world.item.enchantment.effects.SummonEntityEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.EnchantmentActiveCheck;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import net.minecraft.world.level.storage.loot.providers.number.EnchantmentLevelProvider;
import net.minecraft.world.phys.Vec3;

public class Enchantments {
    public static final ResourceKey<Enchantment> PROTECTION = key("protection");
    public static final ResourceKey<Enchantment> FIRE_PROTECTION = key("fire_protection");
    public static final ResourceKey<Enchantment> FEATHER_FALLING = key("feather_falling");
    public static final ResourceKey<Enchantment> BLAST_PROTECTION = key("blast_protection");
    public static final ResourceKey<Enchantment> PROJECTILE_PROTECTION = key("projectile_protection");
    public static final ResourceKey<Enchantment> RESPIRATION = key("respiration");
    public static final ResourceKey<Enchantment> AQUA_AFFINITY = key("aqua_affinity");
    public static final ResourceKey<Enchantment> THORNS = key("thorns");
    public static final ResourceKey<Enchantment> DEPTH_STRIDER = key("depth_strider");
    public static final ResourceKey<Enchantment> FROST_WALKER = key("frost_walker");
    public static final ResourceKey<Enchantment> BINDING_CURSE = key("binding_curse");
    public static final ResourceKey<Enchantment> SOUL_SPEED = key("soul_speed");
    public static final ResourceKey<Enchantment> SWIFT_SNEAK = key("swift_sneak");
    public static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    public static final ResourceKey<Enchantment> SMITE = key("smite");
    public static final ResourceKey<Enchantment> BANE_OF_ARTHROPODS = key("bane_of_arthropods");
    public static final ResourceKey<Enchantment> KNOCKBACK = key("knockback");
    public static final ResourceKey<Enchantment> FIRE_ASPECT = key("fire_aspect");
    public static final ResourceKey<Enchantment> LOOTING = key("looting");
    public static final ResourceKey<Enchantment> SWEEPING_EDGE = key("sweeping_edge");
    public static final ResourceKey<Enchantment> EFFICIENCY = key("efficiency");
    public static final ResourceKey<Enchantment> SILK_TOUCH = key("silk_touch");
    public static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
    public static final ResourceKey<Enchantment> FORTUNE = key("fortune");
    public static final ResourceKey<Enchantment> POWER = key("power");
    public static final ResourceKey<Enchantment> PUNCH = key("punch");
    public static final ResourceKey<Enchantment> FLAME = key("flame");
    public static final ResourceKey<Enchantment> INFINITY = key("infinity");
    public static final ResourceKey<Enchantment> LUCK_OF_THE_SEA = key("luck_of_the_sea");
    public static final ResourceKey<Enchantment> LURE = key("lure");
    public static final ResourceKey<Enchantment> LOYALTY = key("loyalty");
    public static final ResourceKey<Enchantment> IMPALING = key("impaling");
    public static final ResourceKey<Enchantment> RIPTIDE = key("riptide");
    public static final ResourceKey<Enchantment> CHANNELING = key("channeling");
    public static final ResourceKey<Enchantment> MULTISHOT = key("multishot");
    public static final ResourceKey<Enchantment> QUICK_CHARGE = key("quick_charge");
    public static final ResourceKey<Enchantment> PIERCING = key("piercing");
    public static final ResourceKey<Enchantment> DENSITY = key("density");
    public static final ResourceKey<Enchantment> BREACH = key("breach");
    public static final ResourceKey<Enchantment> WIND_BURST = key("wind_burst");
    public static final ResourceKey<Enchantment> MENDING = key("mending");
    public static final ResourceKey<Enchantment> VANISHING_CURSE = key("vanishing_curse");

    public static void bootstrap(BootstrapContext<Enchantment> p_343249_) {
        HolderGetter<DamageType> holdergetter = p_343249_.lookup(Registries.DAMAGE_TYPE);
        HolderGetter<Enchantment> holdergetter1 = p_343249_.lookup(Registries.ENCHANTMENT);
        HolderGetter<Item> holdergetter2 = p_343249_.lookup(Registries.ITEM);
        HolderGetter<Block> holdergetter3 = p_343249_.lookup(Registries.BLOCK);
        HolderGetter<EntityType<?>> holdergetter4 = p_343249_.lookup(Registries.ENTITY_TYPE);
        register(
            p_343249_,
            PROTECTION,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        10,
                        4,
                        Enchantment.dynamicCost(1, 11),
                        Enchantment.dynamicCost(12, 11),
                        1,
                        EquipmentSlotGroup.ARMOR
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(1.0F)),
                    DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY)))
                )
        );
        register(
            p_343249_,
            FIRE_PROTECTION,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        5,
                        4,
                        Enchantment.dynamicCost(10, 8),
                        Enchantment.dynamicCost(18, 8),
                        2,
                        EquipmentSlotGroup.ARMOR
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(2.0F)),
                    AllOfCondition.allOf(
                        DamageSourceCondition.hasDamageSource(
                            DamageSourcePredicate.Builder.damageType()
                                .tag(TagPredicate.is(DamageTypeTags.IS_FIRE))
                                .tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY))
                        )
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.fire_protection"),
                        Attributes.BURNING_TIME,
                        LevelBasedValue.perLevel(-0.15F),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
                )
        );
        register(
            p_343249_,
            FEATHER_FALLING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                        5,
                        4,
                        Enchantment.dynamicCost(5, 6),
                        Enchantment.dynamicCost(11, 6),
                        2,
                        EquipmentSlotGroup.ARMOR
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(3.0F)),
                    DamageSourceCondition.hasDamageSource(
                        DamageSourcePredicate.Builder.damageType()
                            .tag(TagPredicate.is(DamageTypeTags.IS_FALL))
                            .tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY))
                    )
                )
        );
        register(
            p_343249_,
            BLAST_PROTECTION,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        2,
                        4,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(13, 8),
                        4,
                        EquipmentSlotGroup.ARMOR
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(2.0F)),
                    DamageSourceCondition.hasDamageSource(
                        DamageSourcePredicate.Builder.damageType()
                            .tag(TagPredicate.is(DamageTypeTags.IS_EXPLOSION))
                            .tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY))
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.blast_protection"),
                        Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                        LevelBasedValue.perLevel(0.15F),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            PROJECTILE_PROTECTION,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        5,
                        4,
                        Enchantment.dynamicCost(3, 6),
                        Enchantment.dynamicCost(9, 6),
                        2,
                        EquipmentSlotGroup.ARMOR
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(2.0F)),
                    DamageSourceCondition.hasDamageSource(
                        DamageSourcePredicate.Builder.damageType()
                            .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE))
                            .tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY))
                    )
                )
        );
        register(
            p_343249_,
            RESPIRATION,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(10, 10),
                        Enchantment.dynamicCost(40, 10),
                        4,
                        EquipmentSlotGroup.HEAD
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.respiration"),
                        Attributes.OXYGEN_BONUS,
                        LevelBasedValue.perLevel(1.0F),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            AQUA_AFFINITY,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE), 2, 1, Enchantment.constantCost(1), Enchantment.constantCost(41), 4, EquipmentSlotGroup.HEAD
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.aqua_affinity"),
                        Attributes.SUBMERGED_MINING_SPEED,
                        LevelBasedValue.perLevel(4.0F),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
                )
        );
        register(
            p_343249_,
            THORNS,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        holdergetter2.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                        1,
                        3,
                        Enchantment.dynamicCost(10, 20),
                        Enchantment.dynamicCost(60, 20),
                        8,
                        EquipmentSlotGroup.ANY
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.VICTIM,
                    EnchantmentTarget.ATTACKER,
                    AllOf.entityEffects(
                        new DamageEntity(LevelBasedValue.constant(1.0F), LevelBasedValue.constant(5.0F), holdergetter.getOrThrow(DamageTypes.THORNS)),
                        new ChangeItemDamage(LevelBasedValue.constant(2.0F))
                    ),
                    LootItemRandomChanceCondition.randomChance(EnchantmentLevelProvider.forEnchantmentLevel(LevelBasedValue.perLevel(0.15F)))
                )
        );
        register(
            p_343249_,
            DEPTH_STRIDER,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(10, 10),
                        Enchantment.dynamicCost(25, 10),
                        4,
                        EquipmentSlotGroup.FEET
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.BOOTS_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.depth_strider"),
                        Attributes.WATER_MOVEMENT_EFFICIENCY,
                        LevelBasedValue.perLevel(0.33333334F),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            FROST_WALKER,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                        2,
                        2,
                        Enchantment.dynamicCost(10, 10),
                        Enchantment.dynamicCost(25, 10),
                        4,
                        EquipmentSlotGroup.FEET
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.BOOTS_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_IMMUNITY,
                    DamageImmunity.INSTANCE,
                    DamageSourceCondition.hasDamageSource(
                        DamageSourcePredicate.Builder.damageType()
                            .tag(TagPredicate.is(DamageTypeTags.BURN_FROM_STEPPING))
                            .tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY))
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.LOCATION_CHANGED,
                    new ReplaceDisk(
                        new LevelBasedValue.Clamped(LevelBasedValue.perLevel(3.0F, 1.0F), 0.0F, 16.0F),
                        LevelBasedValue.constant(1.0F),
                        new Vec3i(0, -1, 0),
                        Optional.of(
                            BlockPredicate.allOf(
                                BlockPredicate.matchesTag(new Vec3i(0, 1, 0), BlockTags.AIR),
                                BlockPredicate.matchesBlocks(Blocks.WATER),
                                BlockPredicate.matchesFluids(Fluids.WATER),
                                BlockPredicate.unobstructed()
                            )
                        ),
                        BlockStateProvider.simple(Blocks.FROSTED_ICE),
                        Optional.of(GameEvent.BLOCK_PLACE)
                    ),
                    AllOfCondition.allOf(
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnGround(true))
                        ),
                        InvertedLootItemCondition.invert(
                            LootItemEntityPropertyCondition.hasProperties(
                                LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity())
                            )
                        )
                    )
                )
        );
        register(
            p_343249_,
            BINDING_CURSE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.EQUIPPABLE_ENCHANTABLE), 1, 1, Enchantment.constantCost(25), Enchantment.constantCost(50), 8, EquipmentSlotGroup.ARMOR
                    )
                )
                .withEffect(EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
        );
        EntityPredicate.Builder entitypredicate$builder = EntityPredicate.Builder.entity()
            .periodicTick(5)
            .flags(EntityFlagsPredicate.Builder.flags().setIsFlying(false).setOnGround(true))
            .moving(MovementPredicate.horizontalSpeed(MinMaxBounds.Doubles.atLeast(1.0E-5F)))
            .movementAffectedBy(
                LocationPredicate.Builder.location()
                    .setBlock(net.minecraft.advancements.critereon.BlockPredicate.Builder.block().of(holdergetter3, BlockTags.SOUL_SPEED_BLOCKS))
            );
        AllOfCondition.Builder allofcondition$builder = AllOfCondition.allOf(
            InvertedLootItemCondition.invert(
                LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity())
                )
            ),
            AnyOfCondition.anyOf(
                AllOfCondition.allOf(
                    EnchantmentActiveCheck.enchantmentActiveCheck(),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setIsFlying(false))
                    ),
                    AnyOfCondition.anyOf(
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity()
                                .movementAffectedBy(
                                    LocationPredicate.Builder.location()
                                        .setBlock(
                                            net.minecraft.advancements.critereon.BlockPredicate.Builder.block().of(holdergetter3, BlockTags.SOUL_SPEED_BLOCKS)
                                        )
                                )
                        ),
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnGround(false)).build()
                        )
                    )
                ),
                AllOfCondition.allOf(
                    EnchantmentActiveCheck.enchantmentInactiveCheck(),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity()
                            .movementAffectedBy(
                                LocationPredicate.Builder.location()
                                    .setBlock(
                                        net.minecraft.advancements.critereon.BlockPredicate.Builder.block().of(holdergetter3, BlockTags.SOUL_SPEED_BLOCKS)
                                    )
                            )
                            .flags(EntityFlagsPredicate.Builder.flags().setIsFlying(false))
                    )
                )
            )
        );
        register(
            p_343249_,
            SOUL_SPEED,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                        1,
                        3,
                        Enchantment.dynamicCost(10, 10),
                        Enchantment.dynamicCost(25, 10),
                        8,
                        EquipmentSlotGroup.FEET
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.LOCATION_CHANGED,
                    AllOf.locationBasedEffects(
                        new EnchantmentAttributeEffect(
                            ResourceLocation.withDefaultNamespace("enchantment.soul_speed"),
                            Attributes.MOVEMENT_SPEED,
                            LevelBasedValue.perLevel(0.0405F, 0.0105F),
                            AttributeModifier.Operation.ADD_VALUE
                        ),
                        new EnchantmentAttributeEffect(
                            ResourceLocation.withDefaultNamespace("enchantment.soul_speed"),
                            Attributes.MOVEMENT_EFFICIENCY,
                            LevelBasedValue.constant(1.0F),
                            AttributeModifier.Operation.ADD_VALUE
                        )
                    ),
                    allofcondition$builder
                )
                .withEffect(
                    EnchantmentEffectComponents.LOCATION_CHANGED,
                    new ChangeItemDamage(LevelBasedValue.constant(1.0F)),
                    AllOfCondition.allOf(
                        LootItemRandomChanceCondition.randomChance(EnchantmentLevelProvider.forEnchantmentLevel(LevelBasedValue.constant(0.04F))),
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity()
                                .flags(EntityFlagsPredicate.Builder.flags().setOnGround(true))
                                .movementAffectedBy(
                                    LocationPredicate.Builder.location()
                                        .setBlock(
                                            net.minecraft.advancements.critereon.BlockPredicate.Builder.block().of(holdergetter3, BlockTags.SOUL_SPEED_BLOCKS)
                                        )
                                )
                        )
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.TICK,
                    new SpawnParticlesEffect(
                        ParticleTypes.SOUL,
                        SpawnParticlesEffect.inBoundingBox(),
                        SpawnParticlesEffect.offsetFromEntityPosition(0.1F),
                        SpawnParticlesEffect.movementScaled(-0.2F),
                        SpawnParticlesEffect.fixedVelocity(ConstantFloat.of(0.1F)),
                        ConstantFloat.of(1.0F)
                    ),
                    LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, entitypredicate$builder)
                )
                .withEffect(
                    EnchantmentEffectComponents.TICK,
                    new PlaySoundEffect(SoundEvents.SOUL_ESCAPE, ConstantFloat.of(0.6F), UniformFloat.of(0.6F, 1.0F)),
                    AllOfCondition.allOf(
                        LootItemRandomChanceCondition.randomChance(0.35F),
                        LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, entitypredicate$builder)
                    )
                )
        );
        register(
            p_343249_,
            SWIFT_SNEAK,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE),
                        1,
                        3,
                        Enchantment.dynamicCost(25, 25),
                        Enchantment.dynamicCost(75, 25),
                        8,
                        EquipmentSlotGroup.LEGS
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.swift_sneak"),
                        Attributes.SNEAKING_SPEED,
                        LevelBasedValue.perLevel(0.15F),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            SHARPNESS,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        10,
                        5,
                        Enchantment.dynamicCost(1, 11),
                        Enchantment.dynamicCost(21, 11),
                        1,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.0F, 0.5F)))
        );
        register(
            p_343249_,
            SMITE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5,
                        5,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE,
                    new AddValue(LevelBasedValue.perLevel(2.5F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(holdergetter4, EntityTypeTags.SENSITIVE_TO_SMITE))
                    )
                )
        );
        register(
            p_343249_,
            BANE_OF_ARTHROPODS,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5,
                        5,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE,
                    new AddValue(LevelBasedValue.perLevel(2.5F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(holdergetter4, EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS))
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.VICTIM,
                    new ApplyMobEffect(
                        HolderSet.direct(MobEffects.SLOWNESS),
                        LevelBasedValue.constant(1.5F),
                        LevelBasedValue.perLevel(1.5F, 0.5F),
                        LevelBasedValue.constant(3.0F),
                        LevelBasedValue.constant(3.0F)
                    ),
                    LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(holdergetter4, EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS))
                        )
                        .and(DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().isDirect(true)))
                )
        );
        register(
            p_343249_,
            KNOCKBACK,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5,
                        2,
                        Enchantment.dynamicCost(5, 20),
                        Enchantment.dynamicCost(55, 20),
                        2,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(EnchantmentEffectComponents.KNOCKBACK, new AddValue(LevelBasedValue.perLevel(1.0F)))
        );
        register(
            p_343249_,
            FIRE_ASPECT,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FIRE_ASPECT_ENCHANTABLE),
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        2,
                        2,
                        Enchantment.dynamicCost(10, 20),
                        Enchantment.dynamicCost(60, 20),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.VICTIM,
                    new Ignite(LevelBasedValue.perLevel(4.0F)),
                    DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().isDirect(true))
                )
        );
        register(
            p_343249_,
            LOOTING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.EQUIPMENT_DROPS,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.VICTIM,
                    new AddValue(LevelBasedValue.perLevel(0.01F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.ATTACKER,
                        EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(holdergetter4, EntityType.PLAYER))
                    )
                )
        );
        register(
            p_343249_,
            SWEEPING_EDGE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(5, 9),
                        Enchantment.dynamicCost(20, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.sweeping_edge"),
                        Attributes.SWEEPING_DAMAGE_RATIO,
                        new LevelBasedValue.Fraction(LevelBasedValue.perLevel(1.0F), LevelBasedValue.perLevel(2.0F, 1.0F)),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            EFFICIENCY,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MINING_ENCHANTABLE),
                        10,
                        5,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(51, 10),
                        1,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ATTRIBUTES,
                    new EnchantmentAttributeEffect(
                        ResourceLocation.withDefaultNamespace("enchantment.efficiency"),
                        Attributes.MINING_EFFICIENCY,
                        new LevelBasedValue.LevelsSquared(1.0F),
                        AttributeModifier.Operation.ADD_VALUE
                    )
                )
        );
        register(
            p_343249_,
            SILK_TOUCH,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MINING_LOOT_ENCHANTABLE), 1, 1, Enchantment.constantCost(15), Enchantment.constantCost(65), 8, EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.MINING_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.BLOCK_EXPERIENCE, new SetValue(LevelBasedValue.constant(0.0F)))
        );
        register(
            p_343249_,
            UNBREAKING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), 5, 3, Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(55, 8), 2, EquipmentSlotGroup.ANY
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.ITEM_DAMAGE,
                    new RemoveBinomial(new LevelBasedValue.Fraction(LevelBasedValue.perLevel(2.0F), LevelBasedValue.perLevel(10.0F, 5.0F))),
                    MatchTool.toolMatches(ItemPredicate.Builder.item().of(holdergetter2, ItemTags.ARMOR_ENCHANTABLE))
                )
                .withEffect(
                    EnchantmentEffectComponents.ITEM_DAMAGE,
                    new RemoveBinomial(new LevelBasedValue.Fraction(LevelBasedValue.perLevel(1.0F), LevelBasedValue.perLevel(2.0F, 1.0F))),
                    InvertedLootItemCondition.invert(MatchTool.toolMatches(ItemPredicate.Builder.item().of(holdergetter2, ItemTags.ARMOR_ENCHANTABLE)))
                )
        );
        register(
            p_343249_,
            FORTUNE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MINING_LOOT_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.MINING_EXCLUSIVE))
        );
        register(
            p_343249_,
            POWER,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.BOW_ENCHANTABLE),
                        10,
                        5,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(16, 10),
                        1,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE,
                    new AddValue(LevelBasedValue.perLevel(1.0F, 0.5F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity().of(holdergetter4, EntityTypeTags.ARROWS).build()
                    )
                )
        );
        register(
            p_343249_,
            PUNCH,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.BOW_ENCHANTABLE),
                        2,
                        2,
                        Enchantment.dynamicCost(12, 20),
                        Enchantment.dynamicCost(37, 20),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.KNOCKBACK,
                    new AddValue(LevelBasedValue.perLevel(1.0F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity().of(holdergetter4, EntityTypeTags.ARROWS).build()
                    )
                )
        );
        register(
            p_343249_,
            FLAME,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.BOW_ENCHANTABLE), 2, 1, Enchantment.constantCost(20), Enchantment.constantCost(50), 4, EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(EnchantmentEffectComponents.PROJECTILE_SPAWNED, new Ignite(LevelBasedValue.constant(100.0F)))
        );
        register(
            p_343249_,
            INFINITY,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.BOW_ENCHANTABLE), 1, 1, Enchantment.constantCost(20), Enchantment.constantCost(50), 8, EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.BOW_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.AMMO_USE,
                    new SetValue(LevelBasedValue.constant(0.0F)),
                    MatchTool.toolMatches(ItemPredicate.Builder.item().of(holdergetter2, Items.ARROW))
                )
        );
        register(
            p_343249_,
            LUCK_OF_THE_SEA,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FISHING_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(EnchantmentEffectComponents.FISHING_LUCK_BONUS, new AddValue(LevelBasedValue.perLevel(1.0F)))
        );
        register(
            p_343249_,
            LURE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.FISHING_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(EnchantmentEffectComponents.FISHING_TIME_REDUCTION, new AddValue(LevelBasedValue.perLevel(5.0F)))
        );
        register(
            p_343249_,
            LOYALTY,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE),
                        5,
                        3,
                        Enchantment.dynamicCost(12, 7),
                        Enchantment.constantCost(50),
                        2,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(EnchantmentEffectComponents.TRIDENT_RETURN_ACCELERATION, new AddValue(LevelBasedValue.perLevel(1.0F)))
        );
        register(
            p_343249_,
            IMPALING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE),
                        2,
                        5,
                        Enchantment.dynamicCost(1, 8),
                        Enchantment.dynamicCost(21, 8),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE,
                    new AddValue(LevelBasedValue.perLevel(2.5F)),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(holdergetter4, EntityTypeTags.SENSITIVE_TO_IMPALING)).build()
                    )
                )
        );
        register(
            p_343249_,
            RIPTIDE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE), 2, 3, Enchantment.dynamicCost(17, 7), Enchantment.constantCost(50), 4, EquipmentSlotGroup.HAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.RIPTIDE_EXCLUSIVE))
                .withSpecialEffect(EnchantmentEffectComponents.TRIDENT_SPIN_ATTACK_STRENGTH, new AddValue(LevelBasedValue.perLevel(1.5F, 0.75F)))
                .withSpecialEffect(EnchantmentEffectComponents.TRIDENT_SOUND, List.of(SoundEvents.TRIDENT_RIPTIDE_1, SoundEvents.TRIDENT_RIPTIDE_2, SoundEvents.TRIDENT_RIPTIDE_3))
        );
        register(
            p_343249_,
            CHANNELING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.TRIDENT_ENCHANTABLE), 1, 1, Enchantment.constantCost(25), Enchantment.constantCost(50), 8, EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.VICTIM,
                    AllOf.entityEffects(
                        new SummonEntityEffect(HolderSet.direct(EntityType.LIGHTNING_BOLT.builtInRegistryHolder()), false),
                        new PlaySoundEffect(SoundEvents.TRIDENT_THUNDER, ConstantFloat.of(5.0F), ConstantFloat.of(1.0F))
                    ),
                    AllOfCondition.allOf(
                        WeatherCheck.weather().setThundering(true),
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().located(LocationPredicate.Builder.location().setCanSeeSky(true))
                        ),
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.DIRECT_ATTACKER, EntityPredicate.Builder.entity().of(holdergetter4, EntityType.TRIDENT)
                        )
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.HIT_BLOCK,
                    AllOf.entityEffects(
                        new SummonEntityEffect(HolderSet.direct(EntityType.LIGHTNING_BOLT.builtInRegistryHolder()), false),
                        new PlaySoundEffect(SoundEvents.TRIDENT_THUNDER, ConstantFloat.of(5.0F), ConstantFloat.of(1.0F))
                    ),
                    AllOfCondition.allOf(
                        WeatherCheck.weather().setThundering(true),
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().of(holdergetter4, EntityType.TRIDENT)
                        ),
                        LocationCheck.checkLocation(LocationPredicate.Builder.location().setCanSeeSky(true)),
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.LIGHTNING_ROD)
                    )
                )
        );
        register(
            p_343249_,
            MULTISHOT,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE), 2, 1, Enchantment.constantCost(20), Enchantment.constantCost(50), 4, EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.CROSSBOW_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.PROJECTILE_COUNT, new AddValue(LevelBasedValue.perLevel(2.0F)))
                .withEffect(EnchantmentEffectComponents.PROJECTILE_SPREAD, new AddValue(LevelBasedValue.perLevel(10.0F)))
        );
        register(
            p_343249_,
            QUICK_CHARGE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                        5,
                        3,
                        Enchantment.dynamicCost(12, 20),
                        Enchantment.constantCost(50),
                        2,
                        EquipmentSlotGroup.MAINHAND,
                        EquipmentSlotGroup.OFFHAND
                    )
                )
                .withSpecialEffect(EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME, new AddValue(LevelBasedValue.perLevel(-0.25F)))
                .withSpecialEffect(
                    EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS,
                    List.of(
                        new CrossbowItem.ChargingSounds(Optional.of(SoundEvents.CROSSBOW_QUICK_CHARGE_1), Optional.empty(), Optional.of(SoundEvents.CROSSBOW_LOADING_END)),
                        new CrossbowItem.ChargingSounds(Optional.of(SoundEvents.CROSSBOW_QUICK_CHARGE_2), Optional.empty(), Optional.of(SoundEvents.CROSSBOW_LOADING_END)),
                        new CrossbowItem.ChargingSounds(Optional.of(SoundEvents.CROSSBOW_QUICK_CHARGE_3), Optional.empty(), Optional.of(SoundEvents.CROSSBOW_LOADING_END))
                    )
                )
        );
        register(
            p_343249_,
            PIERCING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                        10,
                        4,
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.constantCost(50),
                        1,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.CROSSBOW_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.PROJECTILE_PIERCING, new AddValue(LevelBasedValue.perLevel(1.0F)))
        );
        register(
            p_343249_,
            DENSITY,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MACE_ENCHANTABLE),
                        5,
                        5,
                        Enchantment.dynamicCost(5, 8),
                        Enchantment.dynamicCost(25, 8),
                        2,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.SMASH_DAMAGE_PER_FALLEN_BLOCK, new AddValue(LevelBasedValue.perLevel(0.5F)))
        );
        register(
            p_343249_,
            BREACH,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MACE_ENCHANTABLE),
                        2,
                        4,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .exclusiveWith(holdergetter1.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, new AddValue(LevelBasedValue.perLevel(-0.15F)))
        );
        register(
            p_343249_,
            WIND_BURST,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.MACE_ENCHANTABLE),
                        2,
                        3,
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.ATTACKER,
                    new ExplodeEffect(
                        false,
                        Optional.empty(),
                        Optional.of(LevelBasedValue.lookup(List.of(1.2F, 1.75F, 2.2F), LevelBasedValue.perLevel(1.5F, 0.35F))),
                        holdergetter3.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()),
                        Vec3.ZERO,
                        LevelBasedValue.constant(3.5F),
                        false,
                        Level.ExplosionInteraction.TRIGGER,
                        ParticleTypes.GUST_EMITTER_SMALL,
                        ParticleTypes.GUST_EMITTER_LARGE,
                        SoundEvents.WIND_CHARGE_BURST
                    ),
                    LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .flags(EntityFlagsPredicate.Builder.flags().setIsFlying(false))
                            .moving(MovementPredicate.fallDistance(MinMaxBounds.Doubles.atLeast(1.5)))
                    )
                )
        );
        register(
            p_343249_,
            MENDING,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                        2,
                        1,
                        Enchantment.dynamicCost(25, 25),
                        Enchantment.dynamicCost(75, 25),
                        4,
                        EquipmentSlotGroup.ANY
                    )
                )
                .withEffect(EnchantmentEffectComponents.REPAIR_WITH_XP, new MultiplyValue(LevelBasedValue.constant(2.0F)))
        );
        register(
            p_343249_,
            VANISHING_CURSE,
            Enchantment.enchantment(
                    Enchantment.definition(
                        holdergetter2.getOrThrow(ItemTags.VANISHING_ENCHANTABLE), 1, 1, Enchantment.constantCost(25), Enchantment.constantCost(50), 8, EquipmentSlotGroup.ANY
                    )
                )
                .withEffect(EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
        );
    }

    private static void register(BootstrapContext<Enchantment> p_345097_, ResourceKey<Enchantment> p_342560_, Enchantment.Builder p_344763_) {
        p_345097_.register(p_342560_, p_344763_.build(p_342560_.location()));
    }

    private static ResourceKey<Enchantment> key(String p_344280_) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace(p_344280_));
    }
}