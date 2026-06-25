package net.tvc.backend.services;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

import net.minecraft.nbt.CompoundTag;
import net.tvc.backend.utils.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IllegalItemService {
    
    @SuppressWarnings("unused")
    private static String getItemSignature(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        StringBuilder sig = new StringBuilder();
        
        // item type
        sig.append(stack.getItem());
        
        // durability
        if (stack.isDamageableItem()) sig.append("|d:").append(stack.getDamageValue());
        
        // enchants
        sig.append("|e:").append(stack.getEnchantments());
        
        // trims
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) sig.append("|t:").append(trim);
        
        return sig.toString();
    }
    
    private static List<ItemStack> getItems(Container container) {
        List<ItemStack> list = new ArrayList<>();
        
        for (int i = 0; i < container.getContainerSize(); i++) list.add(container.getItem(i));
        
        return list;
    }
    
    private static List<ItemStack> getItems(ItemStack iStack) {
        ItemContainerContents contents = iStack.get(DataComponents.CONTAINER);
        
        if (contents == null) {
            return List.of();
        }
        
        return contents.stream().toList();
    }
    
    private static void checkShulkerItems(ItemStack iStack, ServerPlayer player, BlockPos pos) {
        checkItems(getItems(iStack), null, null);
    }
    
    public static void checkItems(List<ItemStack> items, ServerPlayer player, BlockPos pos) {
        if (!Config.get().enabled) {
            return;
        }

        Set<UUID> dupeIds = new HashSet<>();
        
        // loop through items
        for (ItemStack iStack : items) {
            if (!iStack.isEmpty()) {
                boolean isDuplicate = false;

                if (Config.get().tracking.dupe && DupeTrackingService.isTrackable(iStack)) {
                    DupeTrackingService.track(iStack, player, pos);
                    isDuplicate = !dupeIds.add(DupeTrackingService.getDupeId(iStack));
                }

                if (isDuplicate || checkItem(iStack, player, pos)) {
                    if (pos == null) ReportService.saveInventoryReport(player, iStack);
                    else ReportService.saveStorageReport(player, iStack, pos);
                    iStack.setCount(0);
                }
            }
        }
    }
    
    @SuppressWarnings("null")
    public static boolean checkItem(ItemStack iStack, ServerPlayer player, BlockPos pos) {
        CompoundTag nbt = iStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("immune") && nbt.getBoolean("immune").get()) return false;
        
        int rCost = iStack.get(DataComponents.REPAIR_COST);
        String itemId = iStack.getItem().toString();
        ItemEnchantments iEnchantments = iStack.getEnchantments();
        
        // scan shulker items
        if (itemId.contains("shulker_box")) checkShulkerItems(iStack, player, pos);
        
        // repair cost
        if (rCost == 0) {
            if (itemId.equals("minecraft:elytra") && iEnchantments.size() != 0) {
                return true;
            }
        }
        
        // stacks
        if (iStack.getCount() > iStack.getMaxStackSize()) {
            return true;
        }
        
        // item IDs
        if (Config.get().blacklistedItems.contains(itemId)) {
            return true;
        }

        for (String pattern : Config.get().blacklistedItemPatterns) {
            if (!pattern.isBlank() && itemId.contains(pattern)) {
                return true;
            }
        }
        
        // enchantments
        for (Holder<Enchantment> enchantment : iEnchantments.keySet()) {
            int level = iEnchantments.getLevel(enchantment);
            if (level > enchantment.value().getMaxLevel() || level < enchantment.value().getMinLevel()
                || enchantment.value().isSupportedItem(iStack) == false) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void checkPlayerPosition(ServerPlayer player, int size, int innerSize) {
        BlockPos pPos = player.getOnPos();
        ServerLevel world = player.level();
        
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    
                    // skip inner cube
                    if (Math.abs(x) <= innerSize &&
                    Math.abs(y) <= innerSize &&
                    Math.abs(z) <= innerSize) {
                        continue;
                    }
                    
                    BlockPos pos = pPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    
                    // clean container check
                    if (blockEntity instanceof Container bEntity) {
                        checkItems(getItems(bEntity), player, pos);
                    }
                }
            }
        }
    }
    
    public static void checkPlayerInventory(ServerPlayer player) {
        // check inventory
        checkItems(getItems(player.getInventory()), player, null);
        checkItems(getItems(player.getEnderChestInventory()), player, null);
    }
}
