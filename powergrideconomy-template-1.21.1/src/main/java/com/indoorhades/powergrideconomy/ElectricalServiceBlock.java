package com.indoorhades.powergrideconomy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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

/**
 * Power Grid Economy electrical service block.
 *
 * The four copper terminals are reserved for the real Power Grid connection
 * adapter. Service/contract state lives in ElectricalServiceBlockEntity.
 */
public class ElectricalServiceBlock extends BaseEntityBlock {
    public static final MapCodec<ElectricalServiceBlock> CODEC = simpleCodec(ElectricalServiceBlock::new);

    public ElectricalServiceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricalServiceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }

        return (tickLevel, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof ElectricalServiceBlockEntity electrical) {
                electrical.tickServer();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof ElectricalServiceBlockEntity electrical)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (player.isShiftKeyDown() && isNumismaticsIdCard(held)) {
            if (!electrical.canEdit(player.getUUID())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Este servicio está protegido por su propietario."), true);
                return InteractionResult.CONSUME;
            }

            if (electrical.bindOwner(player.getUUID())) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("ID Card vinculada. Eres el propietario del servicio."), true);
                return InteractionResult.CONSUME;
            }
        }

        player.displayClientMessage(electrical.getStatusMessage(), true);
        return InteractionResult.CONSUME;
    }

    private static boolean isNumismaticsIdCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "numismatics".equals(id.getNamespace()) && id.getPath().endsWith("_id_card");
    }
}
