package com.indoorhades.powergrideconomy;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

@MethodsReturnNonnullByDefault
public class PrepaidMeterBlock extends Rotation4ElectricBlock implements IBE<PrepaidMeterBlockEntity> {
    private final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 3, 1, 0.5, 7, 4, 3).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 9, 1, 0.5, 13, 4, 3).withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 3, 1, 13, 7, 4, 15.5).withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 9, 1, 13, 13, 4, 15.5).withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 8, 14);

    public PrepaidMeterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        var shaper = VoxelShaper.forDirectional(SHAPE, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS, terminal -> {
                    var facing = state.getValue(FACING);
                    terminal = switch (facing) {
                        case DOWN -> terminal;
                        case UP -> terminal.rotateAroundX(180);
                        case EAST -> terminal.rotateAroundZ(90).rotateAroundY(180);
                        case WEST -> terminal.rotateAroundZ(90);
                        case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                        case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                    };
                    var rotation = state.getValue(ROTATION);
                    return terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                }))
                .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                .build());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        PrepaidMeterBlockEntity meter = getBlockEntity(level, pos);
        if (meter == null)
            return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!meter.canEdit(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Solo el propietario puede editar este medidor."), true);
                return InteractionResult.FAIL;
            }
            meter.openEditor(player);
            return InteractionResult.SUCCESS;
        }

        meter.openBuyer(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<PrepaidMeterBlockEntity> getBlockEntityClass() {
        return PrepaidMeterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PrepaidMeterBlockEntity> getBlockEntityType() {
        return PowerGridEconomy.PREPAID_METER_BE.get();
    }
}
