package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<ResourceKey<LootTable>> KEY_CODEC = ResourceKey.codec(Registries.LOOT_TABLE);
    public static final ContextKeySet DEFAULT_PARAM_SET = LootContextParamSets.ALL_PARAMS;
    public static final long RANDOMIZE_SEED = 0L;
    public static final Codec<LootTable> DIRECT_CODEC = Codec.lazyInitialized(
        () -> RecordCodecBuilder.create(
            p_327557_ -> p_327557_.group(
                    LootContextParamSets.CODEC.lenientOptionalFieldOf("type", DEFAULT_PARAM_SET).forGetter(p_297013_ -> p_297013_.paramSet),
                    ResourceLocation.CODEC.optionalFieldOf("random_sequence").forGetter(p_297014_ -> p_297014_.randomSequence),
                    LootPool.CONDITIONAL_CODEC.listOf().optionalFieldOf("pools", List.of()).forGetter(p_297012_ -> p_297012_.pools),
                    LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(p_297010_ -> p_297010_.functions)
                )
                .apply(p_327557_, LootTable::new)
        )
    );
    public static final Codec<Holder<LootTable>> CODEC = RegistryFileCodec.create(Registries.LOOT_TABLE, DIRECT_CODEC);
    public static final LootTable EMPTY = new LootTable(LootContextParamSets.EMPTY, Optional.empty(), List.of(), List.of());
    private final ContextKeySet paramSet;
    private final Optional<ResourceLocation> randomSequence;
    private final List<LootPool> pools;
    private final List<LootItemFunction> functions;
    private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

    LootTable(ContextKeySet p_365565_, Optional<ResourceLocation> p_298628_, List<LootPool> p_298771_, List<LootItemFunction> p_301234_) {
        this.paramSet = p_365565_;
        this.randomSequence = p_298628_;
        this.pools = Lists.newArrayList(p_298771_);
        this.functions = p_301234_;
        this.compositeFunction = LootItemFunctions.compose(p_301234_);
    }

    public static Consumer<ItemStack> createStackSplitter(ServerLevel p_287765_, Consumer<ItemStack> p_251308_) {
        return p_287570_ -> {
            if (p_287570_.isItemEnabled(p_287765_.enabledFeatures())) {
                if (p_287570_.getCount() < p_287570_.getMaxStackSize()) {
                    p_251308_.accept(p_287570_);
                } else {
                    int i = p_287570_.getCount();

                    while (i > 0) {
                        ItemStack itemstack = p_287570_.copyWithCount(Math.min(p_287570_.getMaxStackSize(), i));
                        i -= itemstack.getCount();
                        p_251308_.accept(itemstack);
                    }
                }
            }
        };
    }

    @Deprecated // Use a non-'Raw' version of 'getRandomItems', so that the Forge Global Loot Modifiers will be applied
    public void getRandomItemsRaw(LootParams p_287669_, Consumer<ItemStack> p_287781_) {
        this.getRandomItemsRaw(new LootContext.Builder(p_287669_).create(this.randomSequence), p_287781_);
    }

    @Deprecated // Use a non-'Raw' version of 'getRandomItems', so that the Forge Global Loot Modifiers will be applied
    public void getRandomItemsRaw(LootContext p_79132_, Consumer<ItemStack> p_79133_) {
        LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(this);
        if (p_79132_.pushVisitedElement(visitedentry)) {
            Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, p_79133_, p_79132_);

            for (LootPool lootpool : this.pools) {
                lootpool.addRandomItems(consumer, p_79132_);
            }

            p_79132_.popVisitedElement(visitedentry);
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
        }
    }

    public void getRandomItems(LootParams p_287748_, long p_287729_, Consumer<ItemStack> p_287583_) {
        this.getRandomItems(new LootContext.Builder(p_287748_).withOptionalRandomSeed(p_287729_).create(this.randomSequence)).forEach(p_287583_);
    }

    public void getRandomItems(LootParams p_287704_, Consumer<ItemStack> p_287617_) {
        this.getRandomItems(p_287704_).forEach(p_287617_);
    }

    public void getRandomItems(LootContext p_79149_, Consumer<ItemStack> p_79150_) {
        this.getRandomItems(p_79149_).forEach(p_79150_);
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams p_345012_, RandomSource p_344559_) {
        return this.getRandomItems(new LootContext.Builder(p_345012_).withOptionalRandomSource(p_344559_).create(this.randomSequence));
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams p_287574_, long p_287773_) {
        return this.getRandomItems(new LootContext.Builder(p_287574_).withOptionalRandomSeed(p_287773_).create(this.randomSequence));
    }

    public ObjectArrayList<ItemStack> getRandomItems(LootParams p_287616_) {
        return this.getRandomItems(new LootContext.Builder(p_287616_).create(this.randomSequence));
    }

    private ObjectArrayList<ItemStack> getRandomItems(LootContext p_230923_) {
        ObjectArrayList<ItemStack> objectarraylist = new ObjectArrayList<>();
        this.getRandomItemsRaw(p_230923_, createStackSplitter(p_230923_.getLevel(), objectarraylist::add));
        objectarraylist = net.minecraftforge.common.ForgeHooks.modifyLoot(this, objectarraylist, p_230923_);
        return objectarraylist;
    }

    public ContextKeySet getParamSet() {
        return this.paramSet;
    }

    public void validate(ValidationContext p_79137_) {
        for (int i = 0; i < this.pools.size(); i++) {
            this.pools.get(i).validate(p_79137_.forChild(new ProblemReporter.IndexedFieldPathElement("pools", i)));
        }

        for (int j = 0; j < this.functions.size(); j++) {
            this.functions.get(j).validate(p_79137_.forChild(new ProblemReporter.IndexedFieldPathElement("functions", j)));
        }
    }

    public void fill(Container p_287662_, LootParams p_287743_, long p_287585_) {
        LootContext lootcontext = new LootContext.Builder(p_287743_).withOptionalRandomSeed(p_287585_).create(this.randomSequence);
        ObjectArrayList<ItemStack> objectarraylist = this.getRandomItems(lootcontext);
        RandomSource randomsource = lootcontext.getRandom();
        List<Integer> list = this.getAvailableSlots(p_287662_, randomsource);
        this.shuffleAndSplitItems(objectarraylist, list.size(), randomsource);

        for (ItemStack itemstack : objectarraylist) {
            if (list.isEmpty()) {
                LOGGER.warn("Tried to over-fill a container");
                return;
            }

            if (itemstack.isEmpty()) {
                p_287662_.setItem(list.remove(list.size() - 1), ItemStack.EMPTY);
            } else {
                p_287662_.setItem(list.remove(list.size() - 1), itemstack);
            }
        }
    }

    private void shuffleAndSplitItems(ObjectArrayList<ItemStack> p_230925_, int p_230926_, RandomSource p_230927_) {
        List<ItemStack> list = Lists.newArrayList();
        Iterator<ItemStack> iterator = p_230925_.iterator();

        while (iterator.hasNext()) {
            ItemStack itemstack = iterator.next();
            if (itemstack.isEmpty()) {
                iterator.remove();
            } else if (itemstack.getCount() > 1) {
                list.add(itemstack);
                iterator.remove();
            }
        }

        while (p_230926_ - p_230925_.size() - list.size() > 0 && !list.isEmpty()) {
            ItemStack itemstack2 = list.remove(Mth.nextInt(p_230927_, 0, list.size() - 1));
            int i = Mth.nextInt(p_230927_, 1, itemstack2.getCount() / 2);
            ItemStack itemstack1 = itemstack2.split(i);
            if (itemstack2.getCount() > 1 && p_230927_.nextBoolean()) {
                list.add(itemstack2);
            } else {
                p_230925_.add(itemstack2);
            }

            if (itemstack1.getCount() > 1 && p_230927_.nextBoolean()) {
                list.add(itemstack1);
            } else {
                p_230925_.add(itemstack1);
            }
        }

        p_230925_.addAll(list);
        Util.shuffle(p_230925_, p_230927_);
    }

    private List<Integer> getAvailableSlots(Container p_230920_, RandomSource p_230921_) {
        ObjectArrayList<Integer> objectarraylist = new ObjectArrayList<>();

        for (int i = 0; i < p_230920_.getContainerSize(); i++) {
            if (p_230920_.getItem(i).isEmpty()) {
                objectarraylist.add(i);
            }
        }

        Util.shuffle(objectarraylist, p_230921_);
        return objectarraylist;
    }

    public static LootTable.Builder lootTable() {
        return new LootTable.Builder();
    }

    private ResourceLocation lootTableId;
    public void setLootTableId(final ResourceLocation id) {
        if (this.lootTableId != null) {
            throw new IllegalStateException("Attempted to rename loot table from '" + this.lootTableId + "' to '" + id + "': this is not supported");
        }
        this.lootTableId = java.util.Objects.requireNonNull(id);
    }

    public ResourceLocation getLootTableId() {
        return this.lootTableId;
    }

    @org.jetbrains.annotations.Nullable
    public LootPool removePool(String name) {
        checkFrozen();
        for (var pool : this.pools) {
            if (name.equals(pool.getName())) {
                this.pools.remove(pool);
                return pool;
            }
        }
        return null;
    }

    public int poolCount() {
        return this.pools.size();
    }

    public LootPool getPool(int entry) {
        return this.pools.get(entry);
    }

    @org.jetbrains.annotations.Nullable
    public LootPool removePool(int entry) {
        checkFrozen();
        return this.pools.remove(entry);
    }

    public void addPool(LootPool pool) {
        checkFrozen();
        if (pools.stream().anyMatch(e -> e == pool)) {
            throw new RuntimeException("Attempted to add a duplicate pool to loot table: " + pool);
        }
        this.pools.add(pool);
    }

    private boolean isFrozen = false;
    public void freeze() {
        this.isFrozen = true;
        this.pools.forEach(LootPool::freeze);
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    private void checkFrozen() {
        if (this.isFrozen()) {
            throw new RuntimeException("Attempted to modify LootTable after being finalized!");
        }
    }

    public static class Builder implements FunctionUserBuilder<LootTable.Builder> {
        private final ImmutableList.Builder<LootPool> pools = ImmutableList.builder();
        private final ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();
        private ContextKeySet paramSet = LootTable.DEFAULT_PARAM_SET;
        private Optional<ResourceLocation> randomSequence = Optional.empty();

        public LootTable.Builder withPool(LootPool.Builder p_79162_) {
            this.pools.add(p_79162_.build());
            return this;
        }

        public LootTable.Builder setParamSet(ContextKeySet p_366887_) {
            this.paramSet = p_366887_;
            return this;
        }

        public LootTable.Builder setRandomSequence(ResourceLocation p_287667_) {
            this.randomSequence = Optional.of(p_287667_);
            return this;
        }

        public LootTable.Builder apply(LootItemFunction.Builder p_79164_) {
            this.functions.add(p_79164_.build());
            return this;
        }

        public LootTable.Builder unwrap() {
            return this;
        }

        public LootTable build() {
            return new LootTable(this.paramSet, this.randomSequence, this.pools.build(), this.functions.build());
        }
    }
}
