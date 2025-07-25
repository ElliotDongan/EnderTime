package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LiquidBlock extends Block implements BucketPickup {
    private static final Codec<FlowingFluid> FLOWING_FLUID = BuiltInRegistries.FLUID
        .byNameCodec()
        .comapFlatMap(
            p_309784_ -> p_309784_ instanceof FlowingFluid flowingfluid
                ? DataResult.success(flowingfluid)
                : DataResult.error(() -> "Not a flowing fluid: " + p_309784_),
            p_311315_ -> (Fluid)p_311315_
        );
    public static final MapCodec<LiquidBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360438_ -> p_360438_.group(FLOWING_FLUID.fieldOf("fluid").forGetter(p_312827_ -> p_312827_.fluid), propertiesCodec()).apply(p_360438_, LiquidBlock::new)
    );
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    @Deprecated // Use getFluid
    private final FlowingFluid fluid;
    private final List<FluidState> stateCache;
    public static final VoxelShape SHAPE_STABLE = Block.column(16.0, 0.0, 8.0);
    public static final ImmutableList<Direction> POSSIBLE_FLOW_DIRECTIONS = ImmutableList.of(Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST);

    @Override
    public MapCodec<LiquidBlock> codec() {
        return CODEC;
    }

    @Deprecated  // Forge: Use the constructor that takes a supplier
    public LiquidBlock(FlowingFluid p_54694_, BlockBehaviour.Properties p_54695_) {
        super(p_54695_);
        this.fluid = p_54694_;
        this.stateCache = Lists.newArrayList();
        this.stateCache.add(p_54694_.getSource(false));

        for (int i = 1; i < 8; i++) {
            this.stateCache.add(p_54694_.getFlowing(8 - i, false));
        }

        this.stateCache.add(p_54694_.getFlowing(8, true));
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
        fluidStateCacheInitialized = true;
        supplier = () -> p_54694_;
    }

    /**
     * @param p_54694_ A fluid supplier such as {@link net.minecraftforge.registries.RegistryObject<FlowingFluid>}
     */
    public LiquidBlock(java.util.function.Supplier<? extends FlowingFluid> p_54694_, BlockBehaviour.Properties p_54695_) {
        super(p_54695_);
        this.fluid = null;
        this.stateCache = Lists.newArrayList();
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, Integer.valueOf(0)));
        this.supplier = p_54694_;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState p_54760_, BlockGetter p_54761_, BlockPos p_54762_, CollisionContext p_54763_) {
        return p_54763_.isAbove(SHAPE_STABLE, p_54762_, true)
                && p_54760_.getValue(LEVEL) == 0
                && p_54763_.canStandOnFluid(p_54761_.getFluidState(p_54762_.above()), p_54760_.getFluidState())
            ? SHAPE_STABLE
            : Shapes.empty();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_54732_) {
        return p_54732_.getFluidState().isRandomlyTicking();
    }

    @Override
    protected void randomTick(BlockState p_221410_, ServerLevel p_221411_, BlockPos p_221412_, RandomSource p_221413_) {
        p_221410_.getFluidState().randomTick(p_221411_, p_221412_, p_221413_);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_54745_) {
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState p_54704_, PathComputationType p_54707_) {
        return !this.fluid.is(FluidTags.LAVA);
    }

    @Override
    protected FluidState getFluidState(BlockState p_54765_) {
        int i = p_54765_.getValue(LEVEL);
        if (!fluidStateCacheInitialized) initFluidStateCache();
        return this.stateCache.get(Math.min(i, 8));
    }

    @Override
    protected boolean skipRendering(BlockState p_54716_, BlockState p_54717_, Direction p_54718_) {
        return p_54717_.getFluidState().getType().isSame(this.fluid);
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_54738_) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState p_54720_, LootParams.Builder p_287727_) {
        return Collections.emptyList();
    }

    @Override
    protected VoxelShape getShape(BlockState p_54749_, BlockGetter p_54750_, BlockPos p_54751_, CollisionContext p_54752_) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState p_54754_, Level p_54755_, BlockPos p_54756_, BlockState p_54757_, boolean p_54758_) {
        if (!net.minecraftforge.fluids.FluidInteractionRegistry.canInteract(p_54755_, p_54756_)) {
            p_54755_.scheduleTick(p_54756_, p_54754_.getFluidState().getType(), this.fluid.getTickDelay(p_54755_));
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_54723_,
        LevelReader p_363921_,
        ScheduledTickAccess p_363294_,
        BlockPos p_54727_,
        Direction p_54724_,
        BlockPos p_54728_,
        BlockState p_54725_,
        RandomSource p_364601_
    ) {
        if (p_54723_.getFluidState().isSource() || p_54725_.getFluidState().isSource()) {
            p_363294_.scheduleTick(p_54727_, p_54723_.getFluidState().getType(), this.fluid.getTickDelay(p_363921_));
        }

        return super.updateShape(p_54723_, p_363921_, p_363294_, p_54727_, p_54724_, p_54728_, p_54725_, p_364601_);
    }

    @Override
    protected void neighborChanged(BlockState p_54709_, Level p_54710_, BlockPos p_54711_, Block p_54712_, @Nullable Orientation p_368724_, boolean p_54714_) {
        if (!net.minecraftforge.fluids.FluidInteractionRegistry.canInteract(p_54710_, p_54711_)) {
            p_54710_.scheduleTick(p_54711_, p_54709_.getFluidState().getType(), this.fluid.getTickDelay(p_54710_));
        }
    }

    @Deprecated // FORGE: Use FluidInteractionRegistry#canInteract instead
    private boolean shouldSpreadLiquid(Level p_54697_, BlockPos p_54698_, BlockState p_54699_) {
        if (this.fluid.is(FluidTags.LAVA)) {
            boolean flag = p_54697_.getBlockState(p_54698_.below()).is(Blocks.SOUL_SOIL);

            for (Direction direction : POSSIBLE_FLOW_DIRECTIONS) {
                BlockPos blockpos = p_54698_.relative(direction.getOpposite());
                if (p_54697_.getFluidState(blockpos).is(FluidTags.WATER)) {
                    Block block = p_54697_.getFluidState(p_54698_).isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                    p_54697_.setBlockAndUpdate(p_54698_, block.defaultBlockState());
                    this.fizz(p_54697_, p_54698_);
                    return false;
                }

                if (flag && p_54697_.getBlockState(blockpos).is(Blocks.BLUE_ICE)) {
                    p_54697_.setBlockAndUpdate(p_54698_, Blocks.BASALT.defaultBlockState());
                    this.fizz(p_54697_, p_54698_);
                    return false;
                }
            }
        }

        return true;
    }

    private void fizz(LevelAccessor p_54701_, BlockPos p_54702_) {
        p_54701_.levelEvent(1501, p_54702_, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54730_) {
        p_54730_.add(LEVEL);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity p_397497_, LevelAccessor p_153772_, BlockPos p_153773_, BlockState p_153774_) {
        if (p_153774_.getValue(LEVEL) == 0) {
            p_153772_.setBlock(p_153773_, Blocks.AIR.defaultBlockState(), 11);
            return new ItemStack(this.fluid.getBucket());
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return this.fluid.getPickupSound();
    }

    private final java.util.function.Supplier<? extends net.minecraft.world.level.material.Fluid> supplier;
    public FlowingFluid getFluid() {
        return (FlowingFluid)supplier.get();
    }

    private boolean fluidStateCacheInitialized = false;
    protected synchronized void initFluidStateCache() {
        if (fluidStateCacheInitialized == false) {
            this.stateCache.add(getFluid().getSource(false));

            for (int i = 1; i < 8; ++i) {
                this.stateCache.add(getFluid().getFlowing(8 - i, false));
            }

            this.stateCache.add(getFluid().getFlowing(8, true));
            fluidStateCacheInitialized = true;
        }
    }
}
