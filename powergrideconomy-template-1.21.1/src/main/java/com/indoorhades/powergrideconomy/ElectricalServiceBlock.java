package com.indoorhades.powergrideconomy;

import com.mojang.serialization.MapCodec;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Physical service switch with four Power Grid terminals. */
public class ElectricalServiceBlock extends BaseEntityBlock {
    public static final MapCodec<ElectricalServiceBlock> CODEC = simpleCodec(ElectricalServiceBlock::new);

    public ElectricalServiceBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricalServiceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (tickLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof ElectricalServiceBlockEntity electrical) electrical.tickServer();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ElectricalServiceBlockEntity electrical)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown() && held.getItem() instanceof IDCardItem) {
            if (!electrical.canEdit(player.getUUID())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Servicio protegido por otro propietario."), true);
                return InteractionResult.CONSUME;
            }

            if (IDCardItem.get(held) == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Primero vincula la ID Card con tu identidad."), true);
                return InteractionResult.CONSUME;
            }

            if (electrical.bindOwnerFromCard(held, player.getUUID())) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("ID Card vinculada al servicio."), true);
                return InteractionResult.CONSUME;
            }
        }

        player.displayClientMessage(electrical.getStatusMessage(), true);
        return InteractionResult.CONSUME;
    }
}
