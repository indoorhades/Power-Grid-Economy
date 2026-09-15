package com.indoorhades.powergrideconomy;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class ElectricityContract {
    public final UUID playerId;
    public final PlanType type;
    public double energyMWh;
    public long expiresAt;

    public ElectricityContract(UUID playerId, PlanType type, double energyMWh, long expiresAt) {
        this.playerId = playerId;
        this.type = type;
        this.energyMWh = energyMWh;
        this.expiresAt = expiresAt;
    }

    public boolean active(long gameTime) {
        return type == PlanType.ENERGY ? energyMWh > 1.0e-9 : gameTime < expiresAt;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Player", playerId);
        tag.putInt("Type", type.ordinal());
        tag.putDouble("EnergyMWh", energyMWh);
        tag.putLong("ExpiresAt", expiresAt);
        return tag;
    }

    public static ElectricityContract load(CompoundTag tag) {
        UUID player = tag.getUUID("Player");
        PlanType type = PlanType.values()[Math.max(0, Math.min(PlanType.values().length - 1, tag.getInt("Type")))];
        return new ElectricityContract(player, type, tag.getDouble("EnergyMWh"), tag.getLong("ExpiresAt"));
    }
}
