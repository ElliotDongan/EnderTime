package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class InfestedBlock extends Block {
    public static final MapCodec<InfestedBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360435_ -> p_360435_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("host").forGetter(InfestedBlock::getHostBlock), propertiesCodec())
            .apply(p_360435_, InfestedBlock::new)
    );
    private final Block hostBlock;
    private static final Map<Block, Block> BLOCK_BY_HOST_BLOCK = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> HOST_TO_INFESTED_STATES = Maps.newIdentityHashMap();
    private static final Map<BlockState, BlockState> INFESTED_TO_HOST_STATES = Maps.newIdentityHashMap();

    @Override
    public MapCodec<? extends InfestedBlock> codec() {
        return CODEC;
    }

    public InfestedBlock(Block p_54178_, BlockBehaviour.Properties p_54179_) {
        super(p_54179_.destroyTime(p_54178_.defaultDestroyTime() / 2.0F).explosionResistance(0.75F));
        this.hostBlock = p_54178_;
        BLOCK_BY_HOST_BLOCK.put(p_54178_, this);
    }

    public Block getHostBlock() {
        return this.hostBlock;
    }

    public static boolean isCompatibleHostBlock(BlockState p_54196_) {
        return BLOCK_BY_HOST_BLOCK.containsKey(p_54196_.getBlock());
    }

    private void spawnInfestation(ServerLevel p_54181_, BlockPos p_54182_) {
        Silverfish silverfish = EntityType.SILVERFISH.create(p_54181_, EntitySpawnReason.TRIGGERED);
        if (silverfish != null) {
            silverfish.snapTo(p_54182_.getX() + 0.5, p_54182_.getY(), p_54182_.getZ() + 0.5, 0.0F, 0.0F);
            p_54181_.addFreshEntity(silverfish);
            silverfish.spawnAnim();
        }
    }

    @Override
    protected void spawnAfterBreak(BlockState p_221360_, ServerLevel p_221361_, BlockPos p_221362_, ItemStack p_221363_, boolean p_221364_) {
        super.spawnAfterBreak(p_221360_, p_221361_, p_221362_, p_221363_, p_221364_);
        if (p_221361_.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !EnchantmentHelper.hasTag(p_221363_, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            this.spawnInfestation(p_221361_, p_221362_);
        }
    }

    public static BlockState infestedStateByHost(BlockState p_153431_) {
        return getNewStateWithProperties(HOST_TO_INFESTED_STATES, p_153431_, () -> BLOCK_BY_HOST_BLOCK.get(p_153431_.getBlock()).defaultBlockState());
    }

    public BlockState hostStateByInfested(BlockState p_153433_) {
        return getNewStateWithProperties(INFESTED_TO_HOST_STATES, p_153433_, () -> this.getHostBlock().defaultBlockState());
    }

    private static BlockState getNewStateWithProperties(Map<BlockState, BlockState> p_153424_, BlockState p_153425_, Supplier<BlockState> p_153426_) {
        return p_153424_.computeIfAbsent(p_153425_, p_153429_ -> {
            BlockState blockstate = p_153426_.get();

            for (Property property : p_153429_.getProperties()) {
                blockstate = blockstate.hasProperty(property) ? blockstate.setValue(property, p_153429_.getValue(property)) : blockstate;
            }

            return blockstate;
        });
    }
}