package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;

public class RegistriesDatapackGenerator implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final java.util.function.Predicate<String> namespacePredicate;

    public RegistriesDatapackGenerator(PackOutput p_256643_, CompletableFuture<HolderLookup.Provider> p_255780_) {
        this(p_256643_, p_255780_, null);
    }

    public RegistriesDatapackGenerator(PackOutput p_256643_, CompletableFuture<HolderLookup.Provider> p_255780_, @org.jetbrains.annotations.Nullable java.util.Set<String> modIds) {
        this.registries = p_255780_;
        this.output = p_256643_;
        this.namespacePredicate = modIds == null ? namespace -> true : modIds::contains;
    }

    /** Forge: Call this to get the registry holder lookup provider that includes elements added via {@link net.minecraftforge.common.data.DatapackBuiltinEntriesProvider} */
    public CompletableFuture<HolderLookup.Provider> getRegistryProvider() {
       return registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_255785_) {
        return this.registries
            .thenCompose(
                p_325918_ -> {
                    DynamicOps<JsonElement> dynamicops = p_325918_.createSerializationContext(JsonOps.INSTANCE);
                    return CompletableFuture.allOf(
                        RegistryDataLoader.getWorldGenAndDimensionStream()
                            .flatMap(p_256552_ -> this.dumpRegistryCap(p_255785_, p_325918_, dynamicops, (RegistryDataLoader.RegistryData<?>)p_256552_).stream())
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
        CachedOutput p_256502_, HolderLookup.Provider p_256492_, DynamicOps<JsonElement> p_256000_, RegistryDataLoader.RegistryData<T> p_256449_
    ) {
        ResourceKey<? extends Registry<T>> resourcekey = p_256449_.key();
        return p_256492_.lookup(resourcekey)
            .map(
                p_358460_ -> {
                    PackOutput.PathProvider packoutput$pathprovider = this.output.createRegistryElementsPathProvider(resourcekey);
                    return CompletableFuture.allOf(
                        p_358460_.listElements()
                            .filter(holder -> shouldDump(holder.key()))
                            .map(
                                p_256105_ -> dumpValue(
                                    packoutput$pathprovider.json(p_256105_.key().location()),
                                    p_256502_,
                                    p_256000_,
                                    p_256449_.elementCodec(),
                                    p_256105_.value()
                                )
                            )
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    protected boolean shouldDump(ResourceKey<?> key) {
        return this.namespacePredicate.test(key.location().getNamespace());
    }

    private static <E> CompletableFuture<?> dumpValue(
        Path p_255678_, CachedOutput p_256438_, DynamicOps<JsonElement> p_256127_, Encoder<E> p_255938_, E p_256590_
    ) {
        return p_255938_.encodeStart(p_256127_, p_256590_)
            .mapOrElse(
                p_341074_ -> DataProvider.saveStable(p_256438_, p_341074_, p_255678_),
                p_341071_ -> CompletableFuture.failedFuture(new IllegalStateException("Couldn't generate file '" + p_255678_ + "': " + p_341071_.message()))
            );
    }

    @Override
    public String getName() {
        return "Registries";
    }
}
