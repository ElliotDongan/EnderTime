package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.BlockEntityIdFix;
import net.minecraft.util.datafix.fixes.References;

public class V704 extends Schema {
    protected static final Map<String, String> ITEM_TO_BLOCKENTITY = DataFixUtils.make(() -> {
        Map<String, String> map = Maps.newHashMap();
        map.put("minecraft:furnace", "minecraft:furnace");
        map.put("minecraft:lit_furnace", "minecraft:furnace");
        map.put("minecraft:chest", "minecraft:chest");
        map.put("minecraft:trapped_chest", "minecraft:chest");
        map.put("minecraft:ender_chest", "minecraft:ender_chest");
        map.put("minecraft:jukebox", "minecraft:jukebox");
        map.put("minecraft:dispenser", "minecraft:dispenser");
        map.put("minecraft:dropper", "minecraft:dropper");
        map.put("minecraft:sign", "minecraft:sign");
        map.put("minecraft:mob_spawner", "minecraft:mob_spawner");
        map.put("minecraft:spawner", "minecraft:mob_spawner");
        map.put("minecraft:noteblock", "minecraft:noteblock");
        map.put("minecraft:brewing_stand", "minecraft:brewing_stand");
        map.put("minecraft:enhanting_table", "minecraft:enchanting_table");
        map.put("minecraft:command_block", "minecraft:command_block");
        map.put("minecraft:beacon", "minecraft:beacon");
        map.put("minecraft:skull", "minecraft:skull");
        map.put("minecraft:daylight_detector", "minecraft:daylight_detector");
        map.put("minecraft:hopper", "minecraft:hopper");
        map.put("minecraft:banner", "minecraft:banner");
        map.put("minecraft:flower_pot", "minecraft:flower_pot");
        map.put("minecraft:repeating_command_block", "minecraft:command_block");
        map.put("minecraft:chain_command_block", "minecraft:command_block");
        map.put("minecraft:shulker_box", "minecraft:shulker_box");
        map.put("minecraft:white_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:orange_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:magenta_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:light_blue_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:yellow_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:lime_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:pink_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:gray_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:silver_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:cyan_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:purple_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:blue_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:brown_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:green_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:red_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:black_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:bed", "minecraft:bed");
        map.put("minecraft:light_gray_shulker_box", "minecraft:shulker_box");
        map.put("minecraft:banner", "minecraft:banner");
        map.put("minecraft:white_banner", "minecraft:banner");
        map.put("minecraft:orange_banner", "minecraft:banner");
        map.put("minecraft:magenta_banner", "minecraft:banner");
        map.put("minecraft:light_blue_banner", "minecraft:banner");
        map.put("minecraft:yellow_banner", "minecraft:banner");
        map.put("minecraft:lime_banner", "minecraft:banner");
        map.put("minecraft:pink_banner", "minecraft:banner");
        map.put("minecraft:gray_banner", "minecraft:banner");
        map.put("minecraft:silver_banner", "minecraft:banner");
        map.put("minecraft:light_gray_banner", "minecraft:banner");
        map.put("minecraft:cyan_banner", "minecraft:banner");
        map.put("minecraft:purple_banner", "minecraft:banner");
        map.put("minecraft:blue_banner", "minecraft:banner");
        map.put("minecraft:brown_banner", "minecraft:banner");
        map.put("minecraft:green_banner", "minecraft:banner");
        map.put("minecraft:red_banner", "minecraft:banner");
        map.put("minecraft:black_banner", "minecraft:banner");
        map.put("minecraft:standing_sign", "minecraft:sign");
        map.put("minecraft:wall_sign", "minecraft:sign");
        map.put("minecraft:piston_head", "minecraft:piston");
        map.put("minecraft:daylight_detector_inverted", "minecraft:daylight_detector");
        map.put("minecraft:unpowered_comparator", "minecraft:comparator");
        map.put("minecraft:powered_comparator", "minecraft:comparator");
        map.put("minecraft:wall_banner", "minecraft:banner");
        map.put("minecraft:standing_banner", "minecraft:banner");
        map.put("minecraft:structure_block", "minecraft:structure_block");
        map.put("minecraft:end_portal", "minecraft:end_portal");
        map.put("minecraft:end_gateway", "minecraft:end_gateway");
        map.put("minecraft:sign", "minecraft:sign");
        map.put("minecraft:shield", "minecraft:banner");
        map.put("minecraft:white_bed", "minecraft:bed");
        map.put("minecraft:orange_bed", "minecraft:bed");
        map.put("minecraft:magenta_bed", "minecraft:bed");
        map.put("minecraft:light_blue_bed", "minecraft:bed");
        map.put("minecraft:yellow_bed", "minecraft:bed");
        map.put("minecraft:lime_bed", "minecraft:bed");
        map.put("minecraft:pink_bed", "minecraft:bed");
        map.put("minecraft:gray_bed", "minecraft:bed");
        map.put("minecraft:silver_bed", "minecraft:bed");
        map.put("minecraft:light_gray_bed", "minecraft:bed");
        map.put("minecraft:cyan_bed", "minecraft:bed");
        map.put("minecraft:purple_bed", "minecraft:bed");
        map.put("minecraft:blue_bed", "minecraft:bed");
        map.put("minecraft:brown_bed", "minecraft:bed");
        map.put("minecraft:green_bed", "minecraft:bed");
        map.put("minecraft:red_bed", "minecraft:bed");
        map.put("minecraft:black_bed", "minecraft:bed");
        map.put("minecraft:oak_sign", "minecraft:sign");
        map.put("minecraft:spruce_sign", "minecraft:sign");
        map.put("minecraft:birch_sign", "minecraft:sign");
        map.put("minecraft:jungle_sign", "minecraft:sign");
        map.put("minecraft:acacia_sign", "minecraft:sign");
        map.put("minecraft:dark_oak_sign", "minecraft:sign");
        map.put("minecraft:crimson_sign", "minecraft:sign");
        map.put("minecraft:warped_sign", "minecraft:sign");
        map.put("minecraft:skeleton_skull", "minecraft:skull");
        map.put("minecraft:wither_skeleton_skull", "minecraft:skull");
        map.put("minecraft:zombie_head", "minecraft:skull");
        map.put("minecraft:player_head", "minecraft:skull");
        map.put("minecraft:creeper_head", "minecraft:skull");
        map.put("minecraft:dragon_head", "minecraft:skull");
        map.put("minecraft:barrel", "minecraft:barrel");
        map.put("minecraft:conduit", "minecraft:conduit");
        map.put("minecraft:smoker", "minecraft:smoker");
        map.put("minecraft:blast_furnace", "minecraft:blast_furnace");
        map.put("minecraft:lectern", "minecraft:lectern");
        map.put("minecraft:bell", "minecraft:bell");
        map.put("minecraft:jigsaw", "minecraft:jigsaw");
        map.put("minecraft:campfire", "minecraft:campfire");
        map.put("minecraft:bee_nest", "minecraft:beehive");
        map.put("minecraft:beehive", "minecraft:beehive");
        map.put("minecraft:sculk_sensor", "minecraft:sculk_sensor");
        map.put("minecraft:decorated_pot", "minecraft:decorated_pot");
        map.put("minecraft:crafter", "minecraft:crafter");
        return ImmutableMap.copyOf(map);
    });
    protected static final HookFunction ADD_NAMES = new HookFunction() {
        @Override
        public <T> T apply(DynamicOps<T> p_18070_, T p_18071_) {
            return V99.addNames(new Dynamic<>(p_18070_, p_18071_), V704.ITEM_TO_BLOCKENTITY, V99.ITEM_TO_ENTITY);
        }
    };

    public V704(int p_18036_, Schema p_18037_) {
        super(p_18036_, p_18037_);
    }

    @Override
    public Type<?> getChoiceType(TypeReference p_18060_, String p_18061_) {
        return Objects.equals(p_18060_.typeName(), References.BLOCK_ENTITY.typeName())
            ? super.getChoiceType(p_18060_, NamespacedSchema.ensureNamespaced(p_18061_))
            : super.getChoiceType(p_18060_, p_18061_);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_18063_) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_18063_);
        BlockEntityIdFix.ID_MAP
            .forEach(
                (p_390413_, p_390414_) -> map.put(p_390414_, Objects.requireNonNull(map.remove(p_390413_), () -> "Didn't find " + p_390413_ + " in schema"))
            );
        return map;
    }

    @Override
    public void registerTypes(Schema p_18065_, Map<String, Supplier<TypeTemplate>> p_18066_, Map<String, Supplier<TypeTemplate>> p_18067_) {
        super.registerTypes(p_18065_, p_18066_, p_18067_);
        p_18065_.registerType(
            true,
            References.BLOCK_ENTITY,
            () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(p_18065_), DSL.taggedChoiceLazy("id", NamespacedSchema.namespacedString(), p_18067_))
        );
        p_18065_.registerType(
            true,
            References.ITEM_STACK,
            () -> DSL.hook(DSL.optionalFields("id", References.ITEM_NAME.in(p_18065_), "tag", V99.itemStackTag(p_18065_)), ADD_NAMES, HookFunction.IDENTITY)
        );
    }
}