package com.indoorhades.powergrideconomy;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import org.slf4j.Logger;

@Mod(PowerGridEconomy.MODID)
public class PowerGridEconomy {
    public static final String MODID = "powergrideconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> PREPAID_METER = BLOCKS.register("prepaid_meter",
            () -> new PrepaidMeterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.METAL)));
    public static final DeferredItem<BlockItem> PREPAID_METER_ITEM = ITEMS.registerSimpleBlockItem("prepaid_meter", PREPAID_METER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrepaidMeterBlockEntity>> PREPAID_METER_BE = BLOCK_ENTITIES.register("prepaid_meter",
            () -> BlockEntityType.Builder.of(PrepaidMeterBlockEntity::new, PREPAID_METER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<PrepaidMeterMenu>> PREPAID_METER_MENU = MENUS.register("prepaid_meter",
            () -> IMenuTypeExtension.create(PrepaidMeterMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.powergrideconomy"))
            .icon(() -> PREPAID_METER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(PREPAID_METER_ITEM.get()))
            .build());

    public PowerGridEconomy(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        TABS.register(modEventBus);
        LOGGER.info("Power Grid Economy loading with prepaid electricity and Create: Numismatics integration");
    }
}
