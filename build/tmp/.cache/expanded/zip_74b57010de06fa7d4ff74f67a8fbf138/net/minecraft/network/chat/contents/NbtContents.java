package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public class NbtContents implements ComponentContents {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<NbtContents> CODEC = RecordCodecBuilder.mapCodec(
        p_326081_ -> p_326081_.group(
                Codec.STRING.fieldOf("nbt").forGetter(NbtContents::getNbtPath),
                Codec.BOOL.lenientOptionalFieldOf("interpret", false).forGetter(NbtContents::isInterpreting),
                ComponentSerialization.CODEC.lenientOptionalFieldOf("separator").forGetter(NbtContents::getSeparator),
                DataSource.CODEC.forGetter(NbtContents::getDataSource)
            )
            .apply(p_326081_, NbtContents::new)
    );
    public static final ComponentContents.Type<NbtContents> TYPE = new ComponentContents.Type<>(CODEC, "nbt");
    private final boolean interpreting;
    private final Optional<Component> separator;
    private final String nbtPathPattern;
    private final DataSource dataSource;
    @Nullable
    protected final NbtPathArgument.NbtPath compiledNbtPath;

    public NbtContents(String p_237395_, boolean p_237396_, Optional<Component> p_237397_, DataSource p_237398_) {
        this(p_237395_, compileNbtPath(p_237395_), p_237396_, p_237397_, p_237398_);
    }

    private NbtContents(String p_237389_, @Nullable NbtPathArgument.NbtPath p_237390_, boolean p_237391_, Optional<Component> p_237392_, DataSource p_237393_) {
        this.nbtPathPattern = p_237389_;
        this.compiledNbtPath = p_237390_;
        this.interpreting = p_237391_;
        this.separator = p_237392_;
        this.dataSource = p_237393_;
    }

    @Nullable
    private static NbtPathArgument.NbtPath compileNbtPath(String p_237410_) {
        try {
            return new NbtPathArgument().parse(new StringReader(p_237410_));
        } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
        }
    }

    public String getNbtPath() {
        return this.nbtPathPattern;
    }

    public boolean isInterpreting() {
        return this.interpreting;
    }

    public Optional<Component> getSeparator() {
        return this.separator;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public boolean equals(Object p_237430_) {
        return this == p_237430_
            ? true
            : p_237430_ instanceof NbtContents nbtcontents
                && this.dataSource.equals(nbtcontents.dataSource)
                && this.separator.equals(nbtcontents.separator)
                && this.interpreting == nbtcontents.interpreting
                && this.nbtPathPattern.equals(nbtcontents.nbtPathPattern);
    }

    @Override
    public int hashCode() {
        int i = this.interpreting ? 1 : 0;
        i = 31 * i + this.separator.hashCode();
        i = 31 * i + this.nbtPathPattern.hashCode();
        return 31 * i + this.dataSource.hashCode();
    }

    @Override
    public String toString() {
        return "nbt{" + this.dataSource + ", interpreting=" + this.interpreting + ", separator=" + this.separator + "}";
    }

    @Override
    public MutableComponent resolve(@Nullable CommandSourceStack p_237401_, @Nullable Entity p_237402_, int p_237403_) throws CommandSyntaxException {
        if (p_237401_ != null && this.compiledNbtPath != null) {
            Stream<Tag> stream = this.dataSource.getData(p_237401_).flatMap(p_237417_ -> {
                try {
                    return this.compiledNbtPath.get(p_237417_).stream();
                } catch (CommandSyntaxException commandsyntaxexception) {
                    return Stream.empty();
                }
            });
            if (this.interpreting) {
                RegistryOps<Tag> registryops = p_237401_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                Component component = DataFixUtils.orElse(ComponentUtils.updateForEntity(p_237401_, this.separator, p_237402_, p_237403_), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR);
                return stream.flatMap(p_389923_ -> {
                    try {
                        Component component1 = ComponentSerialization.CODEC.parse(registryops, p_389923_).getOrThrow();
                        return Stream.of(ComponentUtils.updateForEntity(p_237401_, component1, p_237402_, p_237403_));
                    } catch (Exception exception) {
                        LOGGER.warn("Failed to parse component: {}", p_389923_, exception);
                        return Stream.of();
                    }
                }).reduce((p_237420_, p_237421_) -> p_237420_.append(component).append(p_237421_)).orElseGet(Component::empty);
            } else {
                Stream<String> stream1 = stream.map(NbtContents::asString);
                return ComponentUtils.updateForEntity(p_237401_, this.separator, p_237402_, p_237403_)
                    .map(
                        p_237415_ -> stream1.map(Component::literal)
                            .reduce((p_237424_, p_237425_) -> p_237424_.append(p_237415_).append(p_237425_))
                            .orElseGet(Component::empty)
                    )
                    .orElseGet(() -> Component.literal(stream1.collect(Collectors.joining(", "))));
            }
        } else {
            return Component.empty();
        }
    }

    private static String asString(Tag p_396919_) {
        return p_396919_ instanceof StringTag(String s) ? s : p_396919_.toString();
    }

    @Override
    public ComponentContents.Type<?> type() {
        return TYPE;
    }
}