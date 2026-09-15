package com.indoorhades.powergrideconomy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent data container for the electrical service block.
 *
 * The owner, plan, purchased time/energy and consumption accounting will be
 * added here as the Numismatics and Power Grid APIs are integrated.
 */
public class ElectricalServiceBlockEntity extends BlockEntity {
    private long serverTicks;
    private boolean serviceEnabled = true;

    public ElectricalServiceBlockEntity(BlockPos pos, BlockState state) {
        super(PowerGridEconomy.ELECTRICAL_SERVICE_BLOCK_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        serverTicks++;
    }

    public boolean isServiceEnabled() {
        return serviceEnabled;
    }

    public Component getStatusMessage() {
        return Component.literal(serviceEnabled
                ? "Servicio eléctrico: ACTIVO | Power Grid Economy"
                : "Servicio eléctrico: CORTADO | Power Grid Economy");
    }
}
