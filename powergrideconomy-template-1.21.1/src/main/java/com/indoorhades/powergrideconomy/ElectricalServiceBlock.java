package com.indoorhades.powergrideconomy;

import com.mojang.serialization.MapCodec;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.HorizontalElectricBlock;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

/** Physical service switch with four real Power Grid terminals. */
public class ElectricalServiceBlock extends HorizontalElectricBlock {
    public static final MapCodec<ElectricalServiceBlock> CODEC = simpleCodec(ElectricalServiceBlock::new);

    private static final TerminalBoundingBox[] TERMINALS = new TerminalBoundingBox[] {
            new TerminalBoundingBox(Component.literal("Entrada +"), 1, 1, 0, 3, 3, 2),
            new TerminalBoundingBox(Component.literal("Entrada -"), 1, 5, 0, 3, 7, 2),
            new TerminalBoundingBox(Component.literal("Salida +"), 1, 5, 14, 3, 7, 16),
            new TerminalBoundingBox(Component.literal("Salida -"), 1, 1, 14, 3, 3, 16)
    };

    public ElectricalServiceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS, terminal -> switch (state.getValue(HORIZONTAL_FACING)) {
                    case NORTH -> terminal;
                    case SOUTH -> terminal.rotateAroundY(180);
                    case EAST -> terminal.rotateAroundY(90);
                    case WEST -> terminal.rotateAroundY(270);
                    default -> throw new IllegalStateException();
                }))
                .build());
    }

    @Override
    protected MapCodec<? extends HorizontalElectricBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof ElectricalServiceBlockEntity electrical) {
                electrical.tick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ElectricalServiceBlockEntity electrical)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown() && held.getItem() instanceof IDCardItem) {
            if (!electrical.canEdit(player.getUUID())) {
                player.displayClientMessage(Component.literal("Servicio protegido por otro propietario."), true);
                return InteractionResult.CONSUME;
            }

            if (IDCardItem.get(held) == null) {
                player.displayClientMessage(Component.literal("Primero vincula la ID Card con tu identidad."), true);
                return InteractionResult.CONSUME;
            }

            if (electrical.bindOwnerFromCard(held, player.getUUID())) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("ID Card vinculada al servicio."), true);
                return InteractionResult.CONSUME;
            }
        }

        player.displayClientMessage(electrical.getStatusMessage(), true);
        return InteractionResult.CONSUME;
    }
}
