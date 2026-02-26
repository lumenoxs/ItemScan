package net.tvc.backend.logic;

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
import net.tvc.backend.BackendInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class CheckingLogic {
    public static void checkItems(Container container, ServerPlayer player, BlockPos pos) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack iStack = container.getItem(i);
            if (!iStack.isEmpty()) {
                if (checkItem(iStack)) {
                    if (pos == null) {
                        ReportingLogic.saveInventoryReport(player, iStack);
                        BackendInstance.LOGGER.warn(player.getPlainTextName() + " had an illegal item");
                    } else {
                        ReportingLogic.saveStorageReport(player, iStack, pos);
                        BackendInstance.LOGGER.warn(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " had an illegal item");
                    }
                    container.removeItem(i, iStack.getCount());
                    container.setChanged();
                } else {
                    // Track or add dupe-id to valid items
                    trackOrTagItem(iStack, player, pos);
                }
            }
        }
    }

    public static boolean checkItem(ItemStack iStack) {
        boolean illegal = false;

        if (iStack.getCount() > iStack.getMaxStackSize()) {
            illegal = true;
            return illegal;
        }

        ItemEnchantments iEnchantments = iStack.getEnchantments();

        for (Holder<Enchantment> enchantment : iEnchantments.keySet()) {
            int level = iEnchantments.getLevel(enchantment);
            if (level > enchantment.value().getMaxLevel() || level < enchantment.value().getMinLevel() || enchantment.value().isSupportedItem(iStack) == false) {
                illegal = true;
                return illegal;
            }
        }

        return illegal;
    }

    public static void checkPosition(ServerPlayer player) {
        BlockPos pPos = player.getOnPos();
        ServerLevel world = player.level();

        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = new BlockPos(x+pPos.getX(), y+pPos.getY(), z+pPos.getZ());
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.getBlock() == Blocks.CHEST ||
                        blockState.getBlock() == Blocks.BARREL ||
                        blockState.getBlock() == Blocks.SHULKER_BOX) {
                        
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        Container bEntity;

                        if (blockEntity instanceof BarrelBlockEntity barrel) {
                            bEntity = barrel;
                        } else if (blockEntity instanceof ChestBlockEntity chest) {
                            bEntity = chest;
                        } else if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
                            bEntity = shulker;
                        } else {
                            continue;
                        }
                        
                        checkItems(bEntity, player, pos);
                    }
                }
            }
        }
    }

    public static void checkPlayer(ServerPlayer player) {
        checkItems(player.getInventory(), player, null);
    }

    /* ===================== DUPE TRACKING ===================== */

    private static boolean isDupeTrackableItem(ItemStack iStack) {
        String itemId = iStack.getItem().toString();
        return itemId.contains("diamond") || itemId.contains("netherite") || itemId.contains("shulker_box");
    }

    private static void trackOrTagItem(ItemStack iStack, ServerPlayer player, BlockPos pos) {
        if (!isDupeTrackableItem(iStack)) return;

        AntiDupeDB db = new AntiDupeDB();
        UUID dupeId = getDupeId(iStack);

        if (dupeId == null) {
            // Register new item
            dupeId = db.register(iStack);
            addDupeIdTag(iStack, dupeId);
        }

        // Update location and NBT
        String location = getLocationString(player, pos);
        String nbtString = getNBTString(iStack);

        db.updateLocation(dupeId, location);
        db.updateNBT(dupeId, nbtString);
    }

    private static UUID getDupeId(ItemStack iStack) {
        // Ensure we have a tag container even if it was previously empty.
        CompoundTag tag = iStack.getOrCreateTag();
        if (!tag.contains("tvc-backend:dupe-id")) {
            return null;
        }
        try {
            return tag.getUUID("tvc-backend:dupe-id");
        } catch (Exception e) {
            // if the stored value isn't a valid UUID, just treat it as absent
            return null;
        }
    }

    private static void addDupeIdTag(ItemStack iStack, UUID dupeId) {
        CompoundTag tag = iStack.getOrCreateTag();
        tag.putUUID("tvc-backend:dupe-id", dupeId);
    }

    private static String getLocationString(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            return "player:" + player.getName().getString() + " at X:" + 
                    Math.round(player.getX()) + " Y:" + 
                    Math.round(player.getY()) + " Z:" + 
                    Math.round(player.getZ());
        } else {
            // Determine container type
            ServerLevel world = player.level();
            BlockState blockState = world.getBlockState(pos);
            String containerType = "unknown";
            
            if (blockState.getBlock() == Blocks.CHEST) {
                containerType = "chest";
            } else if (blockState.getBlock() == Blocks.BARREL) {
                containerType = "barrel";
            } else if (blockState.getBlock() == Blocks.SHULKER_BOX) {
                containerType = "shulker";
            }

            return containerType + " at X:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ();
        }
    }

    private static String getNBTString(ItemStack iStack) {
        try {
            Object tagObj = iStack.getClass().getMethod("getTag").invoke(iStack);
            if (tagObj instanceof CompoundTag tag) {
                return tag.toString();
            }
        } catch (Exception e) {
            // No tag
        }
        return "{}";
    }
}
