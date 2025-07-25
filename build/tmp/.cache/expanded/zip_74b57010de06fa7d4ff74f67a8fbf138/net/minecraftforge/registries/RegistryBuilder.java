/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.registries;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry.AddCallback;
import net.minecraftforge.registries.IForgeRegistry.BakeCallback;
import net.minecraftforge.registries.IForgeRegistry.ClearCallback;
import net.minecraftforge.registries.IForgeRegistry.CreateCallback;
import net.minecraftforge.registries.IForgeRegistry.MissingFactory;
import net.minecraftforge.registries.IForgeRegistry.ValidateCallback;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class RegistryBuilder<T> {
    public static <T> RegistryBuilder<T> of() {
        return new RegistryBuilder<T>();
    }

    public static <T> RegistryBuilder<T> of(String name) {
        return of(ResourceLocation.parse(name));
    }

    public static <T> RegistryBuilder<T> of(ResourceLocation name) {
        return new RegistryBuilder<T>().setName(name);
    }

    private static final int MAX_ID = Integer.MAX_VALUE - 1;

    private ResourceLocation registryName;
    private ResourceLocation optionalDefaultKey;
    private int minId = 0;
    private int maxId = MAX_ID;
    private final List<AddCallback<T>> addCallback = new ArrayList<>();
    private final List<ClearCallback<T>> clearCallback = new ArrayList<>();
    private final List<CreateCallback<T>> createCallback = new ArrayList<>();
    private final List<ValidateCallback<T>> validateCallback = new ArrayList<>();
    private final List<BakeCallback<T>> bakeCallback = new ArrayList<>();
    private boolean saveToDisc = true;
    private boolean sync = true;
    private boolean allowOverrides = true;
    private boolean allowModifications = false;
    private boolean hasWrapper = false;
    private MissingFactory<T> missingFactory;
    private final Set<ResourceLocation> legacyNames = new HashSet<>();
    @Nullable
    private Function<T, Holder.Reference<T>> intrusiveHolderCallback = null;

    public RegistryBuilder<T> setName(ResourceLocation name) {
        this.registryName = name;
        return this;
    }

    public RegistryBuilder<T> setIDRange(int min, int max) {
        this.minId = Math.max(min, 0);
        this.maxId = Math.min(max, MAX_ID);
        return this;
    }

    public RegistryBuilder<T> setMaxID(int max) {
        return this.setIDRange(0, max);
    }

    public RegistryBuilder<T> setDefaultKey(ResourceLocation key) {
        this.optionalDefaultKey = key;
        return this;
    }

    @SuppressWarnings("unchecked")
    public RegistryBuilder<T> addCallback(Object inst) {
        if (inst instanceof AddCallback)
            this.add((AddCallback<T>)inst);
        if (inst instanceof ClearCallback)
            this.add((ClearCallback<T>)inst);
        if (inst instanceof CreateCallback)
            this.add((CreateCallback<T>)inst);
        if (inst instanceof ValidateCallback)
            this.add((ValidateCallback<T>)inst);
        if (inst instanceof BakeCallback)
            this.add((BakeCallback<T>)inst);
        if (inst instanceof MissingFactory)
            this.set((MissingFactory<T>)inst);
        return this;
    }

    public RegistryBuilder<T> add(AddCallback<T> add) {
        this.addCallback.add(add);
        return this;
    }

    public RegistryBuilder<T> onAdd(AddCallback<T> add) {
        return this.add(add);
    }

    public RegistryBuilder<T> add(ClearCallback<T> clear) {
        this.clearCallback.add(clear);
        return this;
    }

    public RegistryBuilder<T> onClear(ClearCallback<T> clear) {
        return this.add(clear);
    }

    public RegistryBuilder<T> add(CreateCallback<T> create) {
        this.createCallback.add(create);
        return this;
    }

    public RegistryBuilder<T> onCreate(CreateCallback<T> create) {
        return this.add(create);
    }

    public RegistryBuilder<T> add(ValidateCallback<T> validate) {
        this.validateCallback.add(validate);
        return this;
    }

    public RegistryBuilder<T> onValidate(ValidateCallback<T> validate) {
        return this.add(validate);
    }

    public RegistryBuilder<T> add(BakeCallback<T> bake) {
        this.bakeCallback.add(bake);
        return this;
    }

    public RegistryBuilder<T> onBake(BakeCallback<T> bake) {
        return this.add(bake);
    }

    public RegistryBuilder<T> set(MissingFactory<T> missing) {
        this.missingFactory = missing;
        return this;
    }

    public RegistryBuilder<T> missing(MissingFactory<T> missing) {
        return this.set(missing);
    }

    public RegistryBuilder<T> disableSaving() {
        this.saveToDisc = false;
        return this;
    }

    /**
     * Prevents the registry from being synced to clients.
     */
    public RegistryBuilder<T> disableSync() {
        this.sync = false;
        return this;
    }

    public RegistryBuilder<T> disableOverrides() {
        this.allowOverrides = false;
        return this;
    }

    public RegistryBuilder<T> allowModification() {
        this.allowModifications = true;
        return this;
    }

    RegistryBuilder<T> hasWrapper() {
        this.hasWrapper = true;
        return this;
    }

    public RegistryBuilder<T> legacyName(String name) {
        return legacyName(ResourceLocation.parse(name));
    }

    public RegistryBuilder<T> legacyName(ResourceLocation name) {
        this.legacyNames.add(name);
        return this;
    }

    RegistryBuilder<T> intrusiveHolderCallback(Function<T, Holder.Reference<T>> intrusiveHolderCallback) {
        this.intrusiveHolderCallback = intrusiveHolderCallback;
        return this;
    }

    /**
     * Enables tags for this registry if not already.
     * All forge registries with wrappers inherently support tags.
     *
     * @return this builder
     * @see RegistryBuilder#hasWrapper()
     */
    public RegistryBuilder<T> hasTags() {
        // Tag system heavily relies on Registry<?> objects, so we need a wrapper for this registry to take advantage
        this.hasWrapper();
        return this;
    }

    /**
     * Modders: Use {@link NewRegistryEvent#create(RegistryBuilder)} instead
     */
    IForgeRegistry<T> create() {
        if (hasWrapper) {
            GameData.WrapperFactory<T> wrapper = GameData.createWrapperFactory(getDefault() != null);
            this.addCallback.addFirst(wrapper);
            this.createCallback.addFirst(wrapper);
        }
        return RegistryManager.ACTIVE.createRegistry(registryName, this);
    }

    @Nullable
    public AddCallback<T> getAdd() {
        if (addCallback.isEmpty())
            return null;
        if (addCallback.size() == 1)
            return addCallback.getFirst();

        var tmp = this.addCallback;
        return (owner, stage, id, key, obj, old) -> {
            for (var cb : tmp)
                cb.onAdd(owner, stage, id, key, obj, old);
        };
    }

    @Nullable
    public ClearCallback<T> getClear() {
        if (clearCallback.isEmpty())
            return null;
        if (clearCallback.size() == 1)
            return clearCallback.getFirst();

        var tmp = this.clearCallback;
        return (owner, stage) -> {
            for (var cb : tmp)
                cb.onClear(owner, stage);
        };
    }

    @Nullable
    public CreateCallback<T> getCreate() {
        if (createCallback.isEmpty())
            return null;
        if (createCallback.size() == 1)
            return createCallback.getFirst();

        var tmp = this.createCallback;
        return (owner, stage) -> {
            for (var cb : tmp)
                cb.onCreate(owner, stage);
        };
    }

    @Nullable
    public ValidateCallback<T> getValidate() {
        if (validateCallback.isEmpty())
            return null;
        if (validateCallback.size() == 1)
            return validateCallback.getFirst();

        var tmp = this.validateCallback;
        return (owner, stage, id, key, obj) -> {
            for (var cb : tmp)
                cb.onValidate(owner, stage, id, key, obj);
        };
    }

    @Nullable
    public BakeCallback<T> getBake() {
        if (bakeCallback.isEmpty())
            return null;
        if (bakeCallback.size() == 1)
            return bakeCallback.getFirst();

        var tmp = this.bakeCallback;
        return (owner, stage) -> {
            for (var cb : tmp)
                cb.onBake(owner, stage);
        };
    }

    @Nullable
    public ResourceLocation getDefault() {
        return this.optionalDefaultKey;
    }

    public int getMinId() {
        return minId;
    }

    public int getMaxId() {
        return maxId;
    }

    public boolean getAllowOverrides() {
        return allowOverrides;
    }

    public boolean getAllowModifications() {
        return allowModifications;
    }

    @Nullable
    public MissingFactory<T> getMissingFactory() {
        return missingFactory;
    }

    public boolean getSaveToDisc() {
        return saveToDisc;
    }

    public boolean getSync() {
        return sync;
    }

    public Set<ResourceLocation> getLegacyNames() {
        return legacyNames;
    }

    Function<T, Holder.Reference<T>> getIntrusiveHolderCallback() {
        return this.intrusiveHolderCallback;
    }

    boolean getHasWrapper() {
        return this.hasWrapper;
    }
}
