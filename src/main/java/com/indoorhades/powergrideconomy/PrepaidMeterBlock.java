package com.indoorhades.powergrideconomy;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.Rotation4ElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import net.minecraft.world.phys.shapes.VoxelShape;

@MethodsReturnNonnullByDefault
public class PrepaidMeterBlock extends Rotation4ElectricBlock implements IBE<PrepaidMeterBlockEntity> {
    // Tu modelo tiene cuatro puntas de cobre: 2 de entrada y 2 de salida.
    // En la orientación base: entrada = lado Z- y salida = lado Z+.
    // En cada lado: pin superior = positivo, pin inferior = negativo.
    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 7, 13, 0, 9, 15, 2)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 7, 9, 0, 9, 11, 2)
                    .withColor(IDecoratedTerminal.BLUE),
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 7, 13, 14, 9, 15, 16)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 7, 9, 14, 9, 11, 16)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    // Huella completa del modelo enviado: cuerpo + display + 4 puntas.
    private static final VoxelShape SHAPE = box(5, 0, 0, 11, 16, 16);

    public PrepaidMeterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Usa el sistema oficial de Rotation4ElectricBlock para que tanto el
        // modelo como los cuatro terminales giren juntos en las 4 rotaciones.
        BlockStateTerminalCollection terminals = Rotation4ElectricBlock.rotation4DownTerminals(this, TERMINALS, SHAPE);
        setTerminalCollection(terminals);
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
