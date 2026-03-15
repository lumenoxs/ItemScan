package net.tvc.backend.logic;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;

public class AntiDupeCheckingLogic {
    @SuppressWarnings("null")
    public static void setDupeId(ItemStack iStack, UUID dupeId) {
        // set dupe id
        CompoundTag nbt = iStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        nbt.putString("identifier", dupeId.toString());
        CustomData.set(DataComponents.CUSTOM_DATA, iStack, nbt);
    }

    @SuppressWarnings("null")
    public static UUID getDupeId(ItemStack iStack) {
        // get the dupe id
        CompoundTag nbt = iStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID id = nbt.contains("identifier") ? UUID.fromString(nbt.getString("identifier").get()) : null;
        return id;
    }

    public static void createDupeId(ItemStack iStack) {
        // add the dupe id to the nbt
        setDupeId(iStack, AntiDupeDB.register(iStack));
    }

    public static boolean isTrackable(ItemStack iStack) {
        String itemId = iStack.getItem().toString();
        return (itemId.contains("diamond") || itemId.contains("netherite") || itemId.contains("shulker_box"))
                && !itemId.equals("minecraft:diamond") && !itemId.contains("block") && !itemId.contains("ore")
                && !itemId.contains("ingot") && !itemId.contains("scrap") && !itemId.contains("upgrade");
    }

    public static void track(ItemStack iStack, ServerPlayer player, BlockPos pos) {
        if (!isTrackable(iStack))
            return;

        // get dupe id
        UUID dupeId = getDupeId(iStack);

        if (dupeId == null) {
            createDupeId(iStack);
            dupeId = getDupeId(iStack);
        }

        // update location and nbt
        String location = getLocationString(player, pos);
        String nbtString = getNBTString(iStack);

        AntiDupeDB.updateLocation(dupeId, location);
        AntiDupeDB.updateNBT(dupeId, nbtString);
    }

    private static String getNBTString(ItemStack iStack) {
        return iStack.getTags().toString();
    }

    private static String getLocationString(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            return "player:" + player.getName().getString() + " at X:" +
                    Math.round(player.getX()) + " Y:" +
                    Math.round(player.getY()) + " Z:" +
                    Math.round(player.getZ());
        } else {
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
}
