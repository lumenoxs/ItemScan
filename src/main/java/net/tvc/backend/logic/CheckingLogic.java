package net.tvc.backend.logic;

import net.tvc.backend.BackendInstance;
import net.tvc.backend.component.ModComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.core.component.DataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.net.URI;

public class CheckingLogic {
    private static final String[] BLACKLISTED_ITEMS = {
        "minecraft:command_block",
        "minecraft:chain_command_block",
        "minecraft:repeating_command_block",
        "minecraft:command_block_minecart",
        "minecraft:structure_block",
        "minecraft:jigsaw",
        "minecraft:structure_void",
        "minecraft:barrier",
        "minecraft:light",
        "minecraft:debug_stick",
        "minecraft:knowledge_book",
        "minecraft:spawner",
        "minecraft:end_portal"
    };

    @SuppressWarnings("null")
    public static void checkItems(Container container, ServerPlayer player, BlockPos pos) {
        // loop through items in container
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack iStack = container.getItem(i);
            if (!iStack.isEmpty()) {
                // check item
                if (checkItem(iStack)) {
                    Integer code;
                    TextColor DARK_RED = TextColor.fromRgb(0xAA0000);
                    TextColor RED = TextColor.fromRgb(0xFF5555);
                    TextColor GOLD = TextColor.fromRgb(0xFFAA00);
                    if (pos == null) {
                        // inventory report
                        code = ReportingLogic.saveInventoryReport(player, iStack);
                        MutableComponent line1 = Component.literal("An automated scan has successfully found and removed illegal item(s) in your inventory");
                        MutableComponent line2 = Component.literal("\nFor more info, go here: ");
                        MutableComponent link = Component.literal("https://truevanilla.net/wiki/A.I.I.D.A./Inventory");
                        MutableComponent line3 = Component.literal("\nCode: ");
                        MutableComponent codeText = Component.literal(code.toString());

                        line1.setStyle(line1.getStyle().withColor(DARK_RED));
                        line2.setStyle(line2.getStyle().withColor(RED));
                        link.setStyle(link.getStyle().withColor(RED).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://truevanilla.net/wiki/A.I.I.D.A./Inventory"))));
                        line3.setStyle(line3.getStyle().withColor(GOLD));
                        codeText.setStyle(codeText.getStyle().withColor(GOLD).withClickEvent(new ClickEvent.CopyToClipboard(code.toString())).withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy!"))));

                        MutableComponent message = Component.literal("").append(line1).append(line2).append(link).append(line3).append(codeText);
                        player.sendSystemMessage(message);
                        iStack.setCount(0);
                        BackendInstance.LOGGER.warn(player.getPlainTextName() + " had an illegal item");
                    } else {
                        // chest report
                        code = ReportingLogic.saveStorageReport(player, iStack, pos);
                        MutableComponent line1 = Component.literal("An automated scan has successfully found and removed illegal item(s) in a storage block near you");
                        MutableComponent line2 = Component.literal("\nFor more info, go here: ");
                        MutableComponent link = Component.literal("https://truevanilla.net/wiki/A.I.I.D.A./Storage");
                        MutableComponent line3 = Component.literal("\nCode: ");
                        MutableComponent codeText = Component.literal(code.toString());

                        line1.setStyle(line1.getStyle().withColor(DARK_RED));
                        line2.setStyle(line2.getStyle().withColor(RED));
                        link.setStyle(link.getStyle().withColor(RED).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://truevanilla.net/wiki/A.I.I.D.A./Storage"))));
                        line3.setStyle(line3.getStyle().withColor(GOLD));
                        codeText.setStyle(codeText.getStyle().withColor(GOLD).withClickEvent(new ClickEvent.CopyToClipboard(code.toString())).withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy!"))));

                        MutableComponent message = Component.literal("").append(line1).append(line2).append(link).append(line3).append(codeText);
                        player.sendSystemMessage(message);
                        BackendInstance.LOGGER.warn(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " had an illegal item");
                    }
                    container.removeItem(i, iStack.getCount());
                    container.setChanged();
                } else {
                    // anti dupe id tracking
                    AntiDupeCheckingLogic.track(iStack, player, pos);
                }
            }
        }
    }

    @SuppressWarnings("null")
    public static boolean checkItem(ItemStack iStack) {
        if (iStack.getOrDefault(ModComponents.IMMUNE, false)) {
            return false;
        }
        
        int rCost = iStack.get(DataComponents.REPAIR_COST);
        String itemId = iStack.getItem().toString();
        ItemEnchantments iEnchantments = iStack.getEnchantments();

        // repair cost
        if (rCost == 0) {
            if (itemId.equals("minecraft:elytra") && iEnchantments.size() != 0) {
                return true;
            }
        }

        // incompatible enchantments
        List<Holder<Enchantment>> enchantList = new ArrayList<>(iEnchantments.keySet());

        for (int i = 0; i < enchantList.size(); i++) {
            for (int j = i + 1; j < enchantList.size(); j++) {
                if (!Enchantment.areCompatible(enchantList.get(i), enchantList.get(j))) {
                    return true;
                }
            }
        }

        // stacks
        if (iStack.getCount() > iStack.getMaxStackSize()) {
            return true;
        }
        
        // item IDs
        if (Arrays.asList(BLACKLISTED_ITEMS).contains(itemId) || itemId.contains("spawn_egg")) {
            return true;
        }

        // enchantments
        for (Holder<Enchantment> enchantment : iEnchantments.keySet()) {
            int level = iEnchantments.getLevel(enchantment);
            if (level > enchantment.value().getMaxLevel() || level < enchantment.value().getMinLevel() || enchantment.value().isSupportedItem(iStack) == false) {
                return true;
            }
        }

        return false;
    }

    public static void checkPosition(ServerPlayer player) {
        // get location
        BlockPos pPos = player.getOnPos();
        ServerLevel world = player.level();

        // check 4x3x4 area around the player for containers, and scan them
        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    // get block pos
                    BlockPos pos = new BlockPos(x+pPos.getX(), y+pPos.getY(), z+pPos.getZ());
                    BlockState blockState = world.getBlockState(pos);
                    // if container
                    if (blockState.getBlock() == Blocks.CHEST ||
                        blockState.getBlock() == Blocks.BARREL ||
                        blockState.getBlock() == Blocks.SHULKER_BOX) {
                        
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        Container bEntity;
                            
                        // casting
                        if (blockEntity instanceof BarrelBlockEntity barrel) {
                            bEntity = barrel;
                        } else if (blockEntity instanceof ChestBlockEntity chest) {
                            bEntity = chest;
                        } else if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
                            bEntity = shulker;
                        } else {
                            continue;
                        }
                        
                        // check items
                        checkItems(bEntity, player, pos);
                    }
                }
            }
        }
    }

    public static void checkPlayer(ServerPlayer player) {
        // check inventory
        checkItems(player.getInventory(), player, null);
    }
}
