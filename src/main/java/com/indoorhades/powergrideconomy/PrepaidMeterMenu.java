package com.indoorhades.powergrideconomy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;

public class PrepaidMeterMenu extends AbstractContainerMenu {
    public static final int EDIT_TOGGLE_TYPE = 0;
    public static final int EDIT_PRICE_MINUS_1 = 1;
    public static final int EDIT_PRICE_PLUS_1 = 2;
    public static final int EDIT_PRICE_MINUS_10 = 3;
    public static final int EDIT_PRICE_PLUS_10 = 4;
    public static final int EDIT_PRICE_MINUS_100 = 5;
    public static final int EDIT_PRICE_PLUS_100 = 6;
    public static final int EDIT_SAVE = 7;
    public static final int BUY_MINUS_1 = 10;
    public static final int BUY_PLUS_1 = 11;
    public static final int BUY_MINUS_10 = 12;
    public static final int BUY_PLUS_10 = 13;
    public static final int BUY_MINUS_100 = 14;
    public static final int BUY_PLUS_100 = 15;
    public static final int BUY_CONFIRM = 16;

    private final BlockPos pos;
    private final boolean editor;
    private final Container container;
    private long quantity = 1;

    public PrepaidMeterMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos(), data.readBoolean(), null);
    }

    public PrepaidMeterMenu(int id, Inventory inventory, BlockPos pos, boolean editor, PrepaidMeterBlockEntity meter) {
        super(PowerGridEconomy.PREPAID_METER_MENU.get(), id);
        this.pos = pos;
        this.editor = editor;
        this.container = meter == null ? new SimpleContainer(1) : meter;

        if (editor) {
            addSlot(new Slot(container, 0, 80, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof IDCardItem; }
            });
        }
        addPlayerInventory(inventory);
    }

    private void addPlayerInventory(Inventory inv) {
        int y = editor ? 84 : 66;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, y + 58));
    }

    public PrepaidMeterBlockEntity getMeter(Player player) {
        return player != null && player.level().getBlockEntity(pos) instanceof PrepaidMeterBlockEntity meter ? meter : null;
    }

    public BlockPos getPos() { return pos; }
    public boolean isEditor() { return editor; }
    public long getQuantity() { return quantity; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level().getBlockEntity(pos) instanceof PrepaidMeterBlockEntity meter)) return false;

        if (editor) {
            if (!meter.canEdit(player)) return false;
            switch (id) {
                case EDIT_TOGGLE_TYPE -> meter.setPlanType(meter.getPlanType() == PlanType.TIME ? PlanType.ENERGY : PlanType.TIME);
                case EDIT_PRICE_MINUS_1 -> meter.adjustPrice(-1);
                case EDIT_PRICE_PLUS_1 -> meter.adjustPrice(1);
                case EDIT_PRICE_MINUS_10 -> meter.adjustPrice(-10);
                case EDIT_PRICE_PLUS_10 -> meter.adjustPrice(10);
                case EDIT_PRICE_MINUS_100 -> meter.adjustPrice(-100);
                case EDIT_PRICE_PLUS_100 -> meter.adjustPrice(100);
                case EDIT_SAVE -> { player.closeContainer(); return true; }
                default -> { return false; }
            }
            return true;
        }

        switch (id) {
            case BUY_MINUS_1 -> quantity = Math.max(1, quantity - 1);
            case BUY_PLUS_1 -> quantity = Math.min(1_000_000_000L, quantity + 1);
            case BUY_MINUS_10 -> quantity = Math.max(1, quantity - 10);
            case BUY_PLUS_10 -> quantity = Math.min(1_000_000_000L, quantity + 10);
            case BUY_MINUS_100 -> quantity = Math.max(1, quantity - 100);
            case BUY_PLUS_100 -> quantity = Math.min(1_000_000_000L, quantity + 100);
            case BUY_CONFIRM -> {
                if (meter.purchase(player, quantity)) {
                    player.displayClientMessage(Component.literal("Plan eléctrico comprado correctamente."), true);
                    quantity = 1;
                } else {
                    player.displayClientMessage(Component.literal("No se pudo cobrar el plan. Usa una tarjeta bancaria de Numismatics con saldo suficiente."), true);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof PrepaidMeterBlockEntity meter && meter.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!editor) return ItemStack.EMPTY;
        if (index == 0) {
            ItemStack stack = slots.get(0).getItem().copy();
            if (stack.isEmpty() || !moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            slots.get(0).set(ItemStack.EMPTY);
            return stack;
        }
        if (index < slots.size() && slots.get(index).hasItem() && slots.get(index).getItem().getItem() instanceof IDCardItem) {
            if (moveItemStackTo(slots.get(index).getItem(), 0, 1, false)) return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }
}
