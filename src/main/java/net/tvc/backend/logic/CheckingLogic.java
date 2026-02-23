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
}
