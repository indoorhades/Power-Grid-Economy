package com.indoorhades.powergrideconomy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.UUID;

/**
 * Persistent service contract and real Power Grid electrical interface.
 * Terminal order: 0=positive input, 1=negative input, 2=positive output, 3=negative output.
 */
public class ElectricalServiceBlockEntity extends BlockEntity implements IElectricEntity {
    public enum PlanType { NONE, TIME, ENERGY }

    private static final long TICKS_PER_DAY = 24000L;
    private static final double TICKS_PER_HOUR = 72000.0D;
    private static final float SWITCH_RESISTANCE = 0.001F;

    private UUID ownerUuid;
    private UUID buyerUuid;
    private boolean idCardBound;
    private PlanType planType = PlanType.NONE;
    private long pricePerUnit;
    private long remainingTimeTicks;
    /** Energy balance is stored in Wh. */
    private long remainingEnergy;
    private long totalEnergyConsumed;
    private double energyRemainder;
    private boolean serviceEnabled = true;

    private transient SwitchedWire positiveSwitch;
    private transient SwitchedWire negativeSwitch;
    private transient IElectricNode outputPositiveNode;

    public ElectricalServiceBlockEntity(BlockPos pos, BlockState state) {
        super(PowerGridEconomy.ELECTRICAL_SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(4);
        positiveSwitch = builder.connectSwitch(SWITCH_RESISTANCE, builder.terminalNode(0), builder.terminalNode(2), serviceEnabled);
        negativeSwitch = builder.connectSwitch(SWITCH_RESISTANCE, builder.terminalNode(1), builder.terminalNode(3), serviceEnabled);
        outputPositiveNode = builder.terminalNode(2);
    }

    public void tickServer() {
        if (planType == PlanType.TIME && remainingTimeTicks > 0) {
            remainingTimeTicks--;
            if (remainingTimeTicks == 0) setServiceEnabled(false);
        }

        if (planType == PlanType.ENERGY && serviceEnabled && outputPositiveNode != null) {
            double voltage = Math.abs(outputPositiveNode.getVoltage());
            double current = Math.abs(outputPositiveNode.getCurrent());
            double watts = voltage * current;
            if (Double.isFinite(watts) && watts > 0.0D) {
                energyRemainder += watts / TICKS_PER_HOUR;
                long wholeWh = (long) energyRemainder;
                if (wholeWh > 0) {
                    energyRemainder -= wholeWh;
                    recordEnergyConsumed(wholeWh);
                }
            }
        }
    }

    /** Adds measured energy from Power Grid in Wh. */
    public void recordEnergyConsumed(long amountWh) {
        if (amountWh <= 0 || planType != PlanType.ENERGY || !serviceEnabled) return;
        totalEnergyConsumed = safeAdd(totalEnergyConsumed, amountWh);
        remainingEnergy = Math.max(0L, remainingEnergy - amountWh);
        if (remainingEnergy == 0L) setServiceEnabled(false);
        setChanged();
    }

    private void setServiceEnabled(boolean enabled) {
        serviceEnabled = enabled;
        if (positiveSwitch != null) positiveSwitch.setState(enabled);
        if (negativeSwitch != null) negativeSwitch.setState(enabled);
        setChanged();
    }

    public boolean canEdit(UUID playerUuid) { return !idCardBound || (ownerUuid != null && ownerUuid.equals(playerUuid)); }

    public boolean bindOwner(UUID playerUuid) {
        if (idCardBound && !canEdit(playerUuid)) return false;
        ownerUuid = playerUuid;
        idCardBound = true;
        setChanged();
        return true;
    }

    public void clearOwner() { ownerUuid = null; idCardBound = false; setChanged(); }

    public void configureTimePlan(long pricePerDay) {
        planType = PlanType.TIME;
        pricePerUnit = Math.max(0L, pricePerDay);
        remainingEnergy = 0L;
        setServiceEnabled(true);
    }

    public void configureEnergyPlan(long pricePerWh) {
        planType = PlanType.ENERGY;
        pricePerUnit = Math.max(0L, pricePerWh);
        remainingTimeTicks = 0L;
        setServiceEnabled(false);
    }

    public long calculateTimePrice(long days) { return safeMultiply(Math.max(0L, days), pricePerUnit); }
    public long calculateEnergyPrice(long energyWh) { return safeMultiply(Math.max(0L, energyWh), pricePerUnit); }

    public boolean activateTimePurchase(UUID buyer, long days) {
        if (planType != PlanType.TIME || days <= 0) return false;
        buyerUuid = buyer;
        remainingTimeTicks = safeAdd(remainingTimeTicks, safeMultiply(days, TICKS_PER_DAY));
        setServiceEnabled(true);
        return true;
    }

    public boolean activateEnergyPurchase(UUID buyer, long energyWh) {
        if (planType != PlanType.ENERGY || energyWh <= 0) return false;
        buyerUuid = buyer;
        remainingEnergy = safeAdd(remainingEnergy, energyWh);
        energyRemainder = 0.0D;
        setServiceEnabled(true);
        return true;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public UUID getBuyerUuid() { return buyerUuid; }
    public boolean isIdCardBound() { return idCardBound; }
    public PlanType getPlanType() { return planType; }
    public long getPricePerUnit() { return pricePerUnit; }
    public long getRemainingTimeTicks() { return remainingTimeTicks; }
    public long getRemainingEnergy() { return remainingEnergy; }
    public long getTotalEnergyConsumed() { return totalEnergyConsumed; }
    public boolean isServiceEnabled() { return serviceEnabled; }

    public Component getStatusMessage() {
        String status = serviceEnabled ? "ACTIVO" : "CORTADO";
        String plan = switch (planType) { case TIME -> "TIEMPO"; case ENERGY -> "ENERGIA (Wh)"; case NONE -> "SIN PLAN"; };
        return Component.literal("Servicio eléctrico: " + status + " | Plan: " + plan + " | Power Grid Economy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        if (buyerUuid != null) tag.putUUID("Buyer", buyerUuid);
        tag.putBoolean("IdCardBound", idCardBound);
        tag.putString("PlanType", planType.name());
        tag.putLong("PricePerUnit", pricePerUnit);
        tag.putLong("RemainingTimeTicks", remainingTimeTicks);
        tag.putLong("RemainingEnergyWh", remainingEnergy);
        tag.putLong("TotalEnergyConsumedWh", totalEnergyConsumed);
        tag.putDouble("EnergyRemainder", energyRemainder);
        tag.putBoolean("ServiceEnabled", serviceEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        super.loadAdditional(tag);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        buyerUuid = tag.hasUUID("Buyer") ? tag.getUUID("Buyer") : null;
        idCardBound = tag.getBoolean("IdCardBound");
        try { planType = PlanType.valueOf(tag.getString("PlanType")); } catch (IllegalArgumentException ignored) { planType = PlanType.NONE; }
        pricePerUnit = Math.max(0L, tag.getLong("PricePerUnit"));
        remainingTimeTicks = Math.max(0L, tag.getLong("RemainingTimeTicks"));
        remainingEnergy = Math.max(0L, tag.getLong("RemainingEnergyWh"));
        totalEnergyConsumed = Math.max(0L, tag.getLong("TotalEnergyConsumedWh"));
        energyRemainder = Math.max(0.0D, tag.getDouble("EnergyRemainder"));
        serviceEnabled = tag.getBoolean("ServiceEnabled");
    }

    private static long safeAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }

    private static long safeMultiply(long a, long b) {
        if (a <= 0 || b <= 0) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }
}
