package com.indoorhades.powergrideconomy;

import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModContainer;

@Mod(value = PowerGridEconomy.MODID, dist = Dist.CLIENT)
public class PowerGridEconomyClient {
    public PowerGridEconomyClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(PowerGridEconomy.PREPAID_METER_MENU.get(), PrepaidMeterScreen::new));
    }
}
