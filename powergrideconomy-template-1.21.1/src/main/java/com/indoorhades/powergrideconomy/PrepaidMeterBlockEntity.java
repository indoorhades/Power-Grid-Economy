package com.indoorhades.powergrideconomy;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.IDeductable;
import dev.ithundxr.createnumismatics.content.backend.ReasonHolder;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.List;
import java.util.UUID;

public class PrepaidMeterBlockEntity extends ElectricBlockEntity implements Container {
    private static final double TICK_SECONDS = 0.05;
    private static final double WATT_TICKS_TO_MWH = TICK_SECONDS / 3_600_000.0;
    private static final float MEASUREMENT_RESISTANCE = 1_000_000f;

    private final ItemStack[] inventory = new ItemStack[] {ItemStack.EMPTY};
    private ElectricityContract contract;
    private PlanType planType = PlanType.ENERGY;
    private long unitPrice = 1;
    private SwitchedWire positiveSwitch;
    private SwitchedWire negativeSwitch;
    private ElectricWire voltageShunt;
    private double lastPowerW;
    private double totalEnergyMWh;

    public PrepaidMeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(4);
        positiveSwitch = builder.connectSwitch(resistance("switch"), builder.terminalNode(0), builder.terminalNode(2), true);
        negativeSwitch = builder.connectSwitch(resistance("switch"), builder.terminalNode(1), builder.terminalNode(3), true);
        voltageShunt = builder.connect(MEASUREMENT_RESISTANCE, builder.terminalNode(0), builder.terminalNode(1));
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if (level == null || level.isClientSide || positiveSwitch == null || negativeSwitch == null || voltageShunt == null)
            return;

        long time = level.getGameTime();
        if (contract != null && !contract.active(time)) {
            contract = null;
            setPowerConnected(false);
            setChanged();
        }
        if (contract == null) {
            lastPowerW = 0;
            return;
        }

        setPowerConnected(true);
        double current = Math.abs(positiveSwitch.current());
        double voltage = Math.abs(voltageShunt.potentialDifference());
        lastPowerW = current * voltage;

        if (contract.type == PlanType.ENERGY) {
            double used = Math.min(contract.energyMWh, lastPowerW * WATT_TICKS_TO_MWH);
            contract.energyMWh = Math.max(0, contract.energyMWh - used);
            totalEnergyMWh += used;
            if (contract.energyMWh <= 1.0e-9) {
                contract = null;
                setPowerConnected(false);
            }
        }

        if (level.getGameTime() % 20 == 0)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        setUnsaved();
    }

    private void setPowerConnected(boolean connected) {
        positiveSwitch.setState(connected);
        negativeSwitch.setState(connected);
    }

    public UUID getOwnerId() {
        ItemStack stack = inventory[0];
        return IDCardItem.isBound(stack) ? IDCardItem.get(stack) : null;
    }

    public boolean canEdit(Player player) {
        UUID owner = getOwnerId();
        return owner == null || owner.equals(player.getUUID());
    }

    public void openEditor(Player player) {
        ((IPlayerExtension) player).openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new PrepaidMeterMenu(id, inv, worldPosition, true, this),
                Component.literal("Editor eléctrico")
        ), buf -> { buf.writeBlockPos(worldPosition); buf.writeBoolean(true); });
    }

    public void openBuyer(Player player) {
        ((IPlayerExtension) player).openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new PrepaidMeterMenu(id, inv, worldPosition, false, this),
                Component.literal("Plan eléctrico")
        ), buf -> { buf.writeBlockPos(worldPosition); buf.writeBoolean(false); });
    }

    public PlanType getPlanType() { return planType; }
    public long getUnitPrice() { return unitPrice; }
    public double getLastPowerW() { return lastPowerW; }
    public double getTotalEnergyMWh() { return totalEnergyMWh; }

    public void setPlanType(PlanType type) {
        planType = type;
        contract = null;
        setPowerConnected(false);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void adjustPrice(long delta) {
        unitPrice = Math.max(0, Math.min(1_000_000_000L, unitPrice + delta));
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public long getRemainingDisplay(Player player) {
        if (contract == null || !contract.playerId.equals(player.getUUID()) || level == null) return 0;
        return contract.type == PlanType.TIME
                ? Math.max(0, (contract.expiresAt - level.getGameTime()) / (20L * 60L * 60L * 24L))
                : (long) Math.ceil(contract.energyMWh);
    }

    public String getState(Player player) {
        if (contract == null || !contract.playerId.equals(player.getUUID())) return "SIN_PLAN";
        return contract.active(level.getGameTime()) ? "ACTIVO" : "AGOTADO";
    }

    public boolean purchase(Player buyer, long quantity) {
        if (level == null || level.isClientSide || quantity <= 0 || unitPrice < 0)
            return false;
        if (contract != null && !contract.playerId.equals(buyer.getUUID()) && contract.active(level.getGameTime()))
            return false;
        if (quantity > Integer.MAX_VALUE / Math.max(1L, unitPrice))
            return false;

        int price = (int) (quantity * unitPrice);
        ItemStack paymentCard = findPaymentCard(buyer, price);
        if (paymentCard.isEmpty()) return false;

        ReasonHolder reason = new ReasonHolder();
        IDeductable deductable = IDeductable.get(paymentCard, buyer, reason);
        if (deductable == null || deductable.getMaxWithdrawal() < price || !deductable.deduct(price, reason))
            return false;

        UUID owner = getOwnerId();
        if (owner != null) {
            var ownerAccount = Numismatics.BANK.getAccount(owner);
            if (ownerAccount != null) ownerAccount.deposit(price);
        }

        long now = level.getGameTime();
        if (planType == PlanType.TIME) {
            long ticks = Math.multiplyExact(quantity, 24L * 60L * 60L * 20L);
            long start = contract != null && contract.playerId.equals(buyer.getUUID()) && contract.expiresAt > now ? contract.expiresAt : now;
            contract = new ElectricityContract(buyer.getUUID(), PlanType.TIME, 0, start + ticks);
        } else {
            double energy = quantity + (contract != null && contract.playerId.equals(buyer.getUUID()) ? contract.energyMWh : 0);
            contract = new ElectricityContract(buyer.getUUID(), PlanType.ENERGY, energy, 0);
        }

        setPowerConnected(true);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    private ItemStack findPaymentCard(Player player, int price) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            IDeductable d = IDeductable.get(stack, player, ReasonHolder.IGNORED);
            if (d != null && d.getMaxWithdrawal() >= price) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        planType = PlanType.values()[Math.max(0, Math.min(PlanType.values().length - 1, tag.getInt("PlanType")))];
        unitPrice = Math.max(0, tag.getLong("UnitPrice"));
        totalEnergyMWh = tag.getDouble("TotalEnergyMWh");
        inventory[0] = ItemStack.parseOptional(registries, tag.getCompound("OwnerCard"));
        contract = tag.contains("Contract", Tag.TAG_COMPOUND) ? ElectricityContract.load(tag.getCompound("Contract")) : null;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("PlanType", planType.ordinal());
        tag.putLong("UnitPrice", unitPrice);
        tag.putDouble("TotalEnergyMWh", totalEnergyMWh);
        tag.put("OwnerCard", inventory[0].save(registries));
        if (contract != null) tag.put("Contract", contract.save());
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return inventory[0].isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? inventory[0] : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = slot == 0 ? inventory[0].split(amount) : ItemStack.EMPTY; setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack result = slot == 0 ? inventory[0] : ItemStack.EMPTY; if (slot == 0) inventory[0] = ItemStack.EMPTY; return result; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) { inventory[0] = stack; setChanged(); } }
    @Override public boolean stillValid(Player player) { return player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) < 64; }
    @Override public void clearContent() { inventory[0] = ItemStack.EMPTY; setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.getItem() instanceof IDCardItem; }
}
