package net.minecraft.data.loot;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput.PathProvider pathProvider;
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<LootTableProvider.SubProviderEntry> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public LootTableProvider(
        PackOutput p_254123_,
        Set<ResourceKey<LootTable>> p_254481_,
        List<LootTableProvider.SubProviderEntry> p_253798_,
        CompletableFuture<HolderLookup.Provider> p_330862_
    ) {
        this.pathProvider = p_254123_.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
        this.subProviders = p_253798_;
        this.requiredTables = p_254481_;
        this.registries = p_330862_;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_254060_) {
        return this.registries.thenCompose(p_325860_ -> this.run(p_254060_, p_325860_));
    }

    private CompletableFuture<?> run(CachedOutput p_327970_, HolderLookup.Provider p_331092_) {
        WritableRegistry<LootTable> writableregistry = new MappedRegistry<>(Registries.LOOT_TABLE, Lifecycle.experimental());
        Map<RandomSupport.Seed128bit, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        this.getTables().forEach(p_341016_ -> p_341016_.provider().apply(p_331092_).generate((p_358218_, p_358219_) -> {
            ResourceLocation resourcelocation = sequenceIdForLootTable(p_358218_);
            ResourceLocation resourcelocation1 = map.put(RandomSequence.seedForKey(resourcelocation), resourcelocation);
            if (resourcelocation1 != null) {
                Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + resourcelocation1 + " and " + p_358218_.location());
            }

            p_358219_.setRandomSequence(resourcelocation);
            LootTable loottable = p_358219_.setParamSet(p_341016_.paramSet).build();
            writableregistry.register(p_358218_, loottable, RegistrationInfo.BUILT_IN);
        }));
        writableregistry.freeze();
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        HolderGetter.Provider holdergetter$provider = new RegistryAccess.ImmutableRegistryAccess(List.of(writableregistry)).freeze();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, holdergetter$provider);

        validate(writableregistry, validationcontext, problemreporter$collector);

        if (!problemreporter$collector.isEmpty()) {
            problemreporter$collector.forEach(
                (p_405059_, p_405060_) -> LOGGER.warn("Found validation problem in {}: {}", p_405059_, p_405060_.description())
            );
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            return CompletableFuture.allOf(writableregistry.entrySet().stream().map(p_325852_ -> {
                ResourceKey<LootTable> resourcekey1 = p_325852_.getKey();
                LootTable loottable = p_325852_.getValue();
                Path path = this.pathProvider.json(resourcekey1.location());
                return DataProvider.saveStable(p_327970_, p_331092_, LootTable.DIRECT_CODEC, loottable, path);
            }).toArray(CompletableFuture[]::new));
        }
    }

    private static ResourceLocation sequenceIdForLootTable(ResourceKey<LootTable> p_331928_) {
        return p_331928_.location();
    }

    @Override
    public final String getName() {
        return "Loot Tables";
    }

    public List<LootTableProvider.SubProviderEntry> getTables() {
        return this.subProviders;
    }

    protected void validate(net.minecraft.core.Registry<LootTable> map, ValidationContext validationcontext, ProblemReporter report) {
        for (ResourceKey<LootTable> resourcekey : Sets.difference(this.requiredTables, map.registryKeySet())) {
            report.report(new LootTableProvider.MissingTableProblem(resourcekey));
        }

        map.listElements().forEach(table -> table.value().validate(
            validationcontext.setContextKeySet(table.value().getParamSet())
                .enterElement(new ProblemReporter.RootElementPathElement(table.key()), table.key())
        ));
    }

    public record MissingTableProblem(ResourceKey<LootTable> id) implements ProblemReporter.Problem {
        @Override
        public String description() {
            return "Missing built-in table: " + this.id.location();
        }
    }

    public record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet) {
    }
}
