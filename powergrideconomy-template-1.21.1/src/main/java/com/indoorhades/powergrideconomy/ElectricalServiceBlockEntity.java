package com.indoorhades.powergrideconomy;

import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ElectricalServiceBlockEntity extends BlockEntity {
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

    public ElectricalServiceBlockEntity(BlockPos pos, BlockState state) {
        super(PowerGridEconomy.ELECTRICAL_SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        serverTicks++;
        if (planType == PlanType.TIME && remainingTimeTicks > 0) {
            remainingTimeTicks--;
            if (remainingTimeTicks == 0) {
                serviceEnabled = false;
                setChanged();
            }
        }
    }

    /**
     * Editing is unrestricted until an ID Card has bound an owner.
     * Once bound, only that owner's UUID may edit the service block.
     */
    public boolean canEdit(UUID playerUuid) {
        if (!idCardBound || ownerUuid == null) {
            return true;
        }
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Binds the service block to the player represented by the ID Card.
     * The card must already contain an identity supplied by Create: Numismatics.
     */
    public boolean bindOwnerFromCard(ItemStack stack, UUID playerUuid) {
        if (!(stack.getItem() instanceof IDCardItem)) {
            return false;
        }
        if (IDCardItem.get(stack) == null) {
            return false;
        }
        if (!canEdit(playerUuid)) {
            return false;
        }

        ownerUuid = playerUuid;
        idCardBound = true;
        setChanged();
        return true;
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
    }
}
