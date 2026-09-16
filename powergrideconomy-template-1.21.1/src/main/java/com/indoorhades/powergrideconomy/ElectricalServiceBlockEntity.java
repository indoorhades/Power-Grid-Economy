package com.indoorhades.powergrideconomy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.UUID;

public class ElectricalServiceBlockEntity extends ElectricBlockEntity {
    public enum PlanType { NONE, TIME, ENERGY }

    private long serverTicks;
    private UUID ownerUuid;
    private UUID buyerUuid;
    private boolean idCardBound;
    private PlanType planType = PlanType.NONE;
    private long pricePerUnit;
    private long remainingTimeTicks;
    private long remainingEnergy;
    private long totalEnergyConsumed;
    private double energyRemainder;
    private boolean serviceEnabled = true;

    @Nullable
    private SwitchedWire positiveSwitch;
    @Nullable
    private SwitchedWire negativeSwitch;

    public ElectricalServiceBlockEntity(BlockPos pos, BlockState state) {
        super(PowerGridEconomy.ELECTRICAL_SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Power Grid circuit:
     * terminal 0 = positive input
     * terminal 1 = negative input
     * terminal 2 = positive output
     * terminal 3 = negative output
     *
     * The two switched wires are the physical service switch. When the service is
     * cut, both conductors are opened.
     */
    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(4);
        positiveSwitch = builder.connectSwitch(0.001f, builder.terminalNode(0), builder.terminalNode(2), serviceEnabled);
        negativeSwitch = builder.connectSwitch(0.001f, builder.terminalNode(1), builder.terminalNode(3), serviceEnabled);
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            tickServer();
        }
    }

    public void tickServer() {
        serverTicks++;
        updateSwitchState();

        if (planType == PlanType.TIME && remainingTimeTicks > 0) {
            remainingTimeTicks--;
            if (remainingTimeTicks == 0) {
                serviceEnabled = false;
                updateSwitchState();
                setChanged();
            }
        }

        if (planType == PlanType.ENERGY && serviceEnabled && remainingEnergy > 0 && positiveSwitch != null) {
            double watts = Math.max(0.0D, positiveSwitch.power());
            double whThisTick = watts / 72000.0D;
            if (whThisTick > 0.0D) {
                energyRemainder += whThisTick;
                long wholeWh = (long) energyRemainder;
                if (wholeWh > 0) {
                    energyRemainder -= wholeWh;
                    remainingEnergy = Math.max(0L, remainingEnergy - wholeWh);
                    totalEnergyConsumed += wholeWh;
                    if (remainingEnergy == 0) {
                        serviceEnabled = false;
                        updateSwitchState();
                    }
                    setChanged();
                }
            }
        }
    }

    private void updateSwitchState() {
        if (positiveSwitch != null) {
            positiveSwitch.setState(serviceEnabled);
        }
        if (negativeSwitch != null) {
            negativeSwitch.setState(serviceEnabled);
        }
    }

    public boolean isServiceEnabled() { return serviceEnabled; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public UUID getBuyerUuid() { return buyerUuid; }
    public boolean isIdCardBound() { return idCardBound; }
    public PlanType getPlanType() { return planType; }
    public long getPricePerUnit() { return pricePerUnit; }
    public long getRemainingTimeTicks() { return remainingTimeTicks; }
    public long getRemainingEnergy() { return remainingEnergy; }
    public long getTotalEnergyConsumed() { return totalEnergyConsumed; }

    public boolean canEdit(UUID playerUuid) {
        return !idCardBound || ownerUuid == null || ownerUuid.equals(playerUuid);
    }

    /**
     * Binds the service to the identity stored by the real Numismatics ID Card.
     * The IDCardItem API is intentionally kept in the block class, so this method
     * only receives the already validated stack/player pair.
     */
    public boolean bindOwnerFromCard(ItemStack card, UUID playerUuid) {
        if (idCardBound && ownerUuid != null && !ownerUuid.equals(playerUuid)) {
            return false;
        }
        ownerUuid = playerUuid;
        idCardBound = true;
        setChanged();
        return true;
    }

    public void setServiceEnabled(boolean enabled) {
        serviceEnabled = enabled;
        updateSwitchState();
        setChanged();
    }

    public Component getStatusMessage() {
        String status = serviceEnabled ? "ACTIVO" : "CORTADO";
        String plan = switch (planType) {
            case TIME -> "TIEMPO";
            case ENERGY -> "ENERGIA (Wh)";
            case NONE -> "SIN PLAN";
        };
        return Component.literal("Servicio eléctrico: " + status + " | Plan: " + plan + " | Power Grid Economy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        buyerUuid = tag.hasUUID("Buyer") ? tag.getUUID("Buyer") : null;
        idCardBound = tag.getBoolean("IdCardBound");
        try {
            planType = PlanType.valueOf(tag.getString("PlanType"));
        } catch (IllegalArgumentException ignored) {
            planType = PlanType.NONE;
        }
        pricePerUnit = Math.max(0L, tag.getLong("PricePerUnit"));
        remainingTimeTicks = Math.max(0L, tag.getLong("RemainingTimeTicks"));
        remainingEnergy = Math.max(0L, tag.getLong("RemainingEnergyWh"));
        totalEnergyConsumed = Math.max(0L, tag.getLong("TotalEnergyConsumedWh"));
        energyRemainder = Math.max(0.0D, tag.getDouble("EnergyRemainder"));
        serviceEnabled = tag.getBoolean("ServiceEnabled");
        updateSwitchState();
    }
}
