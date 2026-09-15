package com.indoorhades.powergrideconomy;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(PowerGridEconomy.MODID)
public class PowerGridEconomy {
    public static final String MODID = "powergrideconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    /** The new electrical service block. */
    public static final DeferredBlock<ElectricalServiceBlock> ELECTRICAL_SERVICE_BLOCK = BLOCKS.register(
            "electrical_service_block",
            () -> new ElectricalServiceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .destroyTime(3.0f)
                    .explosionResistance(6.0f)));

    /** Inventory item for the electrical service block. */
    public static final DeferredItem<BlockItem> ELECTRICAL_SERVICE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("electrical_service_block", ELECTRICAL_SERVICE_BLOCK);

    /** Block entity that will hold owner, plan and consumption data. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricalServiceBlockEntity>>
            ELECTRICAL_SERVICE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "electrical_service_block",
                    () -> BlockEntityType.Builder.of(
                            ElectricalServiceBlockEntity::new,
                            ELECTRICAL_SERVICE_BLOCK.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.powergrideconomy"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ELECTRICAL_SERVICE_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(ELECTRICAL_SERVICE_BLOCK_ITEM.get()))
                    .build());

    public PowerGridEconomy(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Power Grid Economy: electrical service block registered");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ELECTRICAL_SERVICE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Power Grid Economy server systems ready");
    }
}
