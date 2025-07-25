package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public class Ingredient implements StackedContents.IngredientInfo<Holder<Item>>, Predicate<ItemStack> {
    private static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> VANILLA_CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM)
        .map(Ingredient::new, p_359816_ -> p_359816_.values);
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = net.minecraftforge.common.ForgeHooks.ingredientStreamCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = CONTENTS_STREAM_CODEC
        .map(
            ingredient -> ingredient.items().count() == 0 ? Optional.empty() : Optional.of(ingredient),
            ingredient -> ingredient.orElseGet(() -> Ingredient.of())
        );
    public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
    private static final Codec<Ingredient> VANILLA_CODEC = ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, p_359811_ -> p_359811_.values);
    private static final com.mojang.serialization.MapCodec<Ingredient> VANILLA_MAP_CODEC = VANILLA_CODEC.fieldOf("value");
    public static final Codec<Ingredient> CODEC = net.minecraftforge.common.ForgeHooks.ingredientBaseCodec(VANILLA_CODEC);
    protected final HolderSet<Item> values;

    protected Ingredient(HolderSet<Item> p_368516_) {
        this(p_368516_, true);
    }

    protected Ingredient(HolderSet<Item> p_368516_, boolean validate) {
        if (validate)
        p_368516_.unwrap().ifRight(p_359817_ -> {
            if (p_359817_.isEmpty()) {
                throw new UnsupportedOperationException("Ingredients can't be empty");
            } else if (p_359817_.contains(Items.AIR.builtInRegistryHolder())) {
                throw new UnsupportedOperationException("Ingredient can't contain air");
            }
        });
        this.values = p_368516_;
    }

    public static boolean testOptionalIngredient(Optional<Ingredient> p_367191_, ItemStack p_364232_) {
        return p_367191_.<Boolean>map(p_359819_ -> p_359819_.test(p_364232_)).orElseGet(p_364232_::isEmpty);
    }

    @Deprecated
    public Stream<Holder<Item>> items() {
        return this.values.stream();
    }

    public boolean isEmpty() {
        return this.values.size() == 0;
    }

    public boolean test(ItemStack p_43914_) {
        return p_43914_.is(this.values);
    }

    public boolean acceptsItem(Holder<Item> p_378483_) {
        return this.values.contains(p_378483_);
    }

    @Override
    public boolean equals(Object p_300457_) {
        return p_300457_ instanceof Ingredient ingredient ? Objects.equals(this.values, ingredient.values) : false;
    }

    public static Ingredient of(ItemLike p_361218_) {
        return new Ingredient(HolderSet.direct(p_361218_.asItem().builtInRegistryHolder()));
    }

    public static Ingredient of(ItemLike... p_43930_) {
        return of(Arrays.stream(p_43930_));
    }

    public static Ingredient of(Stream<? extends ItemLike> p_43922_) {
        return new Ingredient(HolderSet.direct(p_43922_.map(p_359813_ -> p_359813_.asItem().builtInRegistryHolder()).toList()));
    }

    public static Ingredient of(HolderSet<Item> p_369402_) {
        return new Ingredient(p_369402_);
    }

    public SlotDisplay display() {
        return (SlotDisplay)this.values
            .unwrap()
            .map(SlotDisplay.TagSlotDisplay::new, p_359812_ -> new SlotDisplay.Composite(p_359812_.stream().map(Ingredient::displayForSingleItem).toList()));
    }

    public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> p_361451_) {
        return p_361451_.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
    }

    private static SlotDisplay displayForSingleItem(Holder<Item> p_363723_) {
        SlotDisplay slotdisplay = new SlotDisplay.ItemSlotDisplay(p_363723_);
        ItemStack itemstack = p_363723_.value().getCraftingRemainder();
        if (!itemstack.isEmpty()) {
            SlotDisplay slotdisplay1 = new SlotDisplay.ItemStackSlotDisplay(itemstack);
            return new SlotDisplay.WithRemainder(slotdisplay, slotdisplay1);
        } else {
            return slotdisplay;
        }
    }

    public boolean isSimple() {
        return true;
    }

    private final boolean isVanilla = this.getClass() == Ingredient.class;
    public final boolean isVanilla() {
        return isVanilla;
    }

    public static final net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<Ingredient> VANILLA_SERIALIZER =
        new net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<Ingredient>() {
            @Override
            public com.mojang.serialization.MapCodec<? extends Ingredient> codec() {
                return VANILLA_MAP_CODEC;
            }

            @Override
            public void write(RegistryFriendlyByteBuf buffer, Ingredient value) {
                VANILLA_CONTENTS_STREAM_CODEC.encode(buffer, value);
            }

            @Override
            public Ingredient read(RegistryFriendlyByteBuf buffer) {
                return VANILLA_CONTENTS_STREAM_CODEC.decode(buffer);
            }
        };

    public net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<? extends Ingredient> serializer() {
        if (!isVanilla()) throw new IllegalStateException("Modders must implement Ingredient.codec in their custom Ingredients: " + getClass());
        return VANILLA_SERIALIZER;
    }

    @Override
    public String toString() {
        var buf = new StringBuilder();
        buf.append("Ingredient[");
        for (int x = 0; x < values.size(); x++) {
            if (x != 0)
                buf.append(", ");
            buf.append(values.get(x));
        }
        buf.append(']');
        return buf.toString();
    }
}
