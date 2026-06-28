package me.rpicpu.itemscan.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;

import me.rpicpu.itemscan.data.ViolationCategory;
import me.rpicpu.itemscan.data.ViolationResult;
import me.rpicpu.itemscan.utils.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class IllegalItemService {
    public static void checkContainer(Container container, ServerPlayer player, BlockPos pos, boolean isEnderChest) {
        if (!Config.get().enabled || player == null) {
            return;
        }

        Set<UUID> dupeIds = new HashSet<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<ViolationResult> violation = detectViolation(stack, player, pos, isEnderChest, dupeIds, false, container);
            if (violation.isEmpty()) {
                continue;
            }

            handleViolation(violation.get(), player, pos, container, slot, stack, isEnderChest);

            if (violation.get().category() == ViolationCategory.SEVERE && pos == null) {
                break;
            }
        }
    }

    @SuppressWarnings("null")
    private static Optional<ViolationResult> detectViolation(
        ItemStack stack,
        ServerPlayer player,
        BlockPos pos,
        boolean isEnderChest,
        Set<UUID> dupeIds,
        boolean insideShulker,
        Container parentContainer
    ) {
        @SuppressWarnings("null")
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (nbt.contains("immune") && nbt.getBoolean("immune").orElse(false)) {
            return Optional.empty();
        }

        if (Config.get().tracking.dupe && DupeTrackingService.isTrackable(stack)) {
            DupeTrackingService.track(stack, player, pos, isEnderChest);
            UUID dupeId = DupeTrackingService.getDupeId(stack);
            if (dupeId != null && !dupeIds.add(dupeId)) {
                return Optional.of(new ViolationResult(ViolationCategory.DUPE_UUID, "duplicate item UUID in scan batch"));
            }
        }

        Optional<ViolationResult> stackViolation = checkStackViolation(stack);
        if (stackViolation.isPresent()) {
            return stackViolation;
        }

        String itemId = stack.getItem().toString();
        if (!insideShulker && itemId.contains("shulker_box")) {
            checkShulkerContents(stack, player, pos, isEnderChest, dupeIds, parentContainer);
        }

        return Optional.empty();
    }

    @SuppressWarnings("null")
    private static Optional<ViolationResult> checkStackViolation(ItemStack stack) {
        int repairCost = stack.getOrDefault(DataComponents.REPAIR_COST, 0);
        String itemId = stack.getItem().toString();
        ItemEnchantments enchantments = stack.getEnchantments();

        if (repairCost == 0 && itemId.equals("minecraft:elytra") && !enchantments.isEmpty()) {
            return Optional.of(new ViolationResult(ViolationCategory.REPAIR_COST, "elytra with enchantments and zero repair cost"));
        }

        if (stack.getCount() > stack.getMaxStackSize()) {
            return Optional.of(new ViolationResult(ViolationCategory.SEVERE, "stack size exceeds maximum"));
        }

        if (Config.get().blacklistedItems.contains(itemId)) {
            return Optional.of(new ViolationResult(ViolationCategory.SEVERE, "blacklisted item id"));
        }

        for (String pattern : Config.get().blacklistedItemPatterns) {
            if (!pattern.isBlank() && itemId.contains(pattern)) {
                return Optional.of(new ViolationResult(ViolationCategory.SEVERE, "blacklisted item pattern: " + pattern));
            }
        }

        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            if (level > enchantment.value().getMaxLevel()
                || level < enchantment.value().getMinLevel()
                || !enchantment.value().isSupportedItem(stack)) {
                return Optional.of(new ViolationResult(ViolationCategory.SEVERE, "invalid enchantment level or compatibility"));
            }
        }

        return Optional.empty();
    }

    private static void checkShulkerContents(
        ItemStack shulker,
        ServerPlayer player,
        BlockPos pos,
        boolean isEnderChest,
        Set<UUID> dupeIds,
        Container parentContainer
    ) {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        List<ItemStack> items = contents.stream().toList();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            Optional<ViolationResult> violation = detectViolation(stack, player, pos, isEnderChest, dupeIds, true, parentContainer);
            if (violation.isEmpty()) {
                continue;
            }

            handleShulkerViolation(violation.get(), player, pos, shulker, slot, stack, isEnderChest, parentContainer);
            return;
        }
    }

    @SuppressWarnings("null")
    private static void handleViolation(
        ViolationResult violation,
        ServerPlayer player,
        BlockPos pos,
        Container container,
        int slot,
        ItemStack stack,
        boolean isEnderChest
    ) {
        int code = ReportService.reportViolation(
            violation.category(),
            player,
            stack,
            pos,
            isEnderChest,
            violation.reason()
        );
        ItemStack paper = ReportService.createNotificationPaper(ReportService.buildMessage(code, false));

        if (violation.category() == ViolationCategory.SEVERE) {
            if (pos == null) {
                wipePlayerStorage(player);
                player.getInventory().setItem(0, paper);
            } else {
                clearContainer(container);
                container.setItem(0, paper);
            }
            return;
        }

        container.setItem(slot, paper);
    }

    @SuppressWarnings("null")
    private static void handleShulkerViolation(
        ViolationResult violation,
        ServerPlayer player,
        BlockPos pos,
        ItemStack shulker,
        int slot,
        ItemStack stack,
        boolean isEnderChest,
        Container parentContainer
    ) {
        int code = ReportService.reportViolation(
            violation.category(),
            player,
            stack,
            pos,
            isEnderChest,
            violation.reason()
        );
        ItemStack paper = ReportService.createNotificationPaper(ReportService.buildMessage(code, false));

        if (violation.category() == ViolationCategory.SEVERE) {
            if (pos == null) {
                wipePlayerStorage(player);
                player.getInventory().setItem(0, paper);
            } else if (parentContainer != null) {
                clearContainer(parentContainer);
                parentContainer.setItem(0, paper);
            } else {
                shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(paper)));
            }
            return;
        }

        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        List<ItemStack> items = new java.util.ArrayList<>(contents.stream().toList());
        while (items.size() <= slot) {
            items.add(ItemStack.EMPTY);
        }
        items.set(slot, paper);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    private static void wipePlayerStorage(ServerPlayer player) {
        clearContainer(player.getInventory());
        clearContainer(player.getEnderChestInventory());
    }

    private static void clearContainer(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    public static void checkPlayerPosition(ServerPlayer player, int size, int innerSize) {
        BlockPos playerPos = player.getOnPos();
        ServerLevel world = player.level();

        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    if (Math.abs(x) <= innerSize && Math.abs(y) <= innerSize && Math.abs(z) <= innerSize) {
                        continue;
                    }

                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);

                    if (blockEntity instanceof Container container) {
                        checkContainer(container, player, pos, false);
                    }
                }
            }
        }
    }

    public static void checkPlayerInventory(ServerPlayer player) {
        checkContainer(player.getInventory(), player, null, false);
        checkContainer(player.getEnderChestInventory(), player, null, true);
    }
}
