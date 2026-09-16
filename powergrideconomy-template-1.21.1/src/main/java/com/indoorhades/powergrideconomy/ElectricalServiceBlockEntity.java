package com.indoorhades.powergrideconomy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Persistent service contract for an Electrical Service Block.
 *
 * Power Grid consumption is intentionally exposed through recordEnergyConsumed(),
 * so the eventual Power Grid adapter can feed real measured consumption here
 * without inventing an electrical API.
 */
public class ElectricalServiceBlockEntity extends BlockEntity {
    public enum PlanType {
        NONE,
        TIME,
        ENERGY
    }

    private static final long TICKS_PER_DAY = 24000L;

    private UUID ownerUuid;
    private UUID buyerUuid;
    private boolean idCardBound;

    private PlanType planType = PlanType.NONE;
    private long pricePerUnit;
    private long remainingTimeTicks;
    private long remainingEnergy;
    private long totalEnergyConsumed;
    private boolean serviceEnabled = true;

    public ElectricalServiceBlockEntity(BlockPos pos, BlockState state) {
        super(PowerGridEconomy.ELECTRICAL_SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        if (planType == PlanType.TIME && remainingTimeTicks > 0) {
            remainingTimeTicks--;
            if (remainingTimeTicks == 0) {
                serviceEnabled = false;
                setChanged();
            }
        }
    }

    /**
     * Adds the real energy consumed by the Power Grid adapter.
     * The value is in the project's configured abstract energy unit.
     */
    public void recordEnergyConsumed(long amount) {
        if (amount <= 0 || planType != PlanType.ENERGY || !serviceEnabled) {
            return;
        }

        totalEnergyConsumed = safeAdd(totalEnergyConsumed, amount);
        remainingEnergy = Math.max(0L, remainingEnergy - amount);

        if (remainingEnergy == 0L) {
            serviceEnabled = false;
        }
        setChanged();
    }

    public boolean canEdit(UUID playerUuid) {
        return !idCardBound || (ownerUuid != null && ownerUuid.equals(playerUuid));
    }

    public boolean bindOwner(UUID playerUuid) {
        if (idCardBound && !canEdit(playerUuid)) {
            return false;
        }
        ownerUuid = playerUuid;
        idCardBound = true;
        setChanged();
        return true;
    }

    public void clearOwner() {
        ownerUuid = null;
        idCardBound = false;
        setChanged();
    }

    public void configureTimePlan(long pricePerDay) {
        planType = PlanType.TIME;
        pricePerUnit = Math.max(0L, pricePerDay);
        remainingEnergy = 0L;
        setChanged();
    }

    public void configureEnergyPlan(long pricePerEnergyUnit) {
        planType = PlanType.ENERGY;
        pricePerUnit = Math.max(0L, pricePerEnergyUnit);
        remainingTimeTicks = 0L;
        setChanged();
    }

    public long calculateTimePrice(long days) {
        return safeMultiply(Math.max(0L, days), pricePerUnit);
    }

    public long calculateEnergyPrice(long energy) {
        return safeMultiply(Math.max(0L, energy), pricePerUnit);
    }

    public boolean activateTimePurchase(UUID buyer, long days) {
        if (planType != PlanType.TIME || days <= 0) {
            return false;
        }
        buyerUuid = buyer;
        remainingTimeTicks = safeAdd(remainingTimeTicks, safeMultiply(days, TICKS_PER_DAY));
        serviceEnabled = true;
        setChanged();
        return true;
    }

    public boolean activateEnergyPurchase(UUID buyer, long energy) {
        if (planType != PlanType.ENERGY || energy <= 0) {
            return false;
        }
        buyerUuid = buyer;
        remainingEnergy = safeAdd(remainingEnergy, energy);
        serviceEnabled = true;
        setChanged();
        return true;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getBuyerUuid() {
        return buyerUuid;
    }

    public boolean isIdCardBound() {
        return idCardBound;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public long getPricePerUnit() {
        return pricePerUnit;
    }

    public long getRemainingTimeTicks() {
        return remainingTimeTicks;
    }

    public long getRemainingEnergy() {
        return remainingEnergy;
    }

    public long getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }

    public boolean isServiceEnabled() {
        return serviceEnabled;
    }

    public Component getStatusMessage() {
        String status = serviceEnabled ? "ACTIVO" : "CORTADO";
        String plan = switch (planType) {
            case TIME -> "TIEMPO";
            case ENERGY -> "ENERGIA";
            case NONE -> "SIN PLAN";
        };
        return Component.literal("Servicio eléctrico: " + status + " | Plan: " + plan
                + " | Power Grid Economy");
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
        tag.putLong("RemainingEnergy", remainingEnergy);
        tag.putLong("TotalEnergyConsumed", totalEnergyConsumed);
        tag.putBoolean("ServiceEnabled", serviceEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        super.loadAdditional(tag);
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
        remainingEnergy = Math.max(0L, tag.getLong("RemainingEnergy"));
        totalEnergyConsumed = Math.max(0L, tag.getLong("TotalEnergyConsumed"));
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
