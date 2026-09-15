package com.indoorhades.powergrideconomy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;

public class PrepaidMeterScreen extends AbstractContainerScreen<PrepaidMeterMenu> {
    public PrepaidMeterScreen(PrepaidMeterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.isEditor() ? 166 : 148;
    }

    @Override
    protected void init() {
        super.init();
        if (menu.isEditor()) {
            addRenderableWidget(Button.builder(Component.literal("Tipo"), b -> press(PrepaidMeterMenu.EDIT_TOGGLE_TYPE)).bounds(leftPos + 8, topPos + 8, 50, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(PrepaidMeterMenu.EDIT_PRICE_MINUS_1)).bounds(leftPos + 62, topPos + 8, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(PrepaidMeterMenu.EDIT_PRICE_PLUS_1)).bounds(leftPos + 94, topPos + 8, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(PrepaidMeterMenu.EDIT_PRICE_MINUS_10)).bounds(leftPos + 126, topPos + 8, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(PrepaidMeterMenu.EDIT_PRICE_PLUS_10)).bounds(leftPos + 8, topPos + 31, 40, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-100"), b -> press(PrepaidMeterMenu.EDIT_PRICE_MINUS_100)).bounds(leftPos + 50, topPos + 31, 48, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+100"), b -> press(PrepaidMeterMenu.EDIT_PRICE_PLUS_100)).bounds(leftPos + 100, topPos + 31, 48, 20).build());
            addRenderableWidget(Button.builder(Component.literal("GUARDAR"), b -> press(PrepaidMeterMenu.EDIT_SAVE)).bounds(leftPos + 108, topPos + 53, 60, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(PrepaidMeterMenu.BUY_MINUS_1)).bounds(leftPos + 8, topPos + 48, 35, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(PrepaidMeterMenu.BUY_PLUS_1)).bounds(leftPos + 45, topPos + 48, 35, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(PrepaidMeterMenu.BUY_MINUS_10)).bounds(leftPos + 82, topPos + 48, 35, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(PrepaidMeterMenu.BUY_PLUS_10)).bounds(leftPos + 119, topPos + 48, 35, 20).build());
            addRenderableWidget(Button.builder(Component.literal("-100"), b -> press(PrepaidMeterMenu.BUY_MINUS_100)).bounds(leftPos + 8, topPos + 71, 45, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+100"), b -> press(PrepaidMeterMenu.BUY_PLUS_100)).bounds(leftPos + 55, topPos + 71, 45, 20).build());
            addRenderableWidget(Button.builder(Component.literal("COMPRAR"), b -> press(PrepaidMeterMenu.BUY_CONFIRM)).bounds(leftPos + 104, topPos + 71, 64, 20).build());
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20252B);
        gui.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + (menu.isEditor() ? 61 : 44), 0xFF30363D);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderLabels(gui, mouseX, mouseY);
        renderTooltip(gui, mouseX, mouseY);
    }

    private void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        int x = leftPos + 8;
        int y = topPos + 3;
        if (menu.getMeter(Minecraft.getInstance().player) instanceof PrepaidMeterBlockEntity meter) {
            if (menu.isEditor()) {
                String type = meter.getPlanType() == PlanType.TIME ? "TIEMPO (días)" : "ENERGÍA (MWh)";
                gui.drawString(font, "Editor eléctrico", x, y, 0xFFFFFF, false);
                gui.drawString(font, "Plan: " + type, x, topPos + 57, 0xE0E0E0, false);
                gui.drawString(font, "Precio por unidad: " + meter.getUnitPrice() + " spurs", x, topPos + 69, 0xE0E0E0, false);
                gui.drawString(font, "Tarjeta ID: coloca una tarjeta de identidad", x, topPos + 92, 0xA0C8FF, false);
            } else {
                String unit = meter.getPlanType() == PlanType.TIME ? "días" : "MWh";
                long remaining = meter.getRemainingDisplay(Minecraft.getInstance().player);
                long total = menu.getQuantity();
                long price = total * meter.getUnitPrice();
                gui.drawString(font, "Plan eléctrico", x, y, 0xFFFFFF, false);
                gui.drawString(font, "Precio: " + meter.getUnitPrice() + " spurs / " + unit, x, topPos + 19, 0xE0E0E0, false);
                gui.drawString(font, "Tu saldo de energía: " + remaining + " " + unit, x, topPos + 31, 0xA0FFB0, false);
                gui.drawString(font, "Cantidad: " + total + " " + unit, x, topPos + 95, 0xFFFFFF, false);
                gui.drawString(font, "Total: " + price + " spurs", x, topPos + 107, 0xFFD36A, false);
                gui.drawString(font, "Estado: " + meter.getState(Minecraft.getInstance().player), x, topPos + 119, 0xFFFFFF, false);
            }
        }
    }
}
