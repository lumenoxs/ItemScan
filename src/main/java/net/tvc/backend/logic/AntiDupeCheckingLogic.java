package net.tvc.backend.logic;

import net.tvc.backend.component.ModComponents;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class AntiDupeCheckingLogic {
    @SuppressWarnings("null")
    public static void setDupeId(ItemStack iStack, UUID dupeId) {
        // set dupe id
        iStack.set(ModComponents.DUPE_ID, dupeId);
    }

    @SuppressWarnings("null")
    public static UUID getDupeId(ItemStack iStack) {
        // get the dupe id
        UUID id = iStack.get(ModComponents.DUPE_ID);
        if (id == null) {
            createDupeId(iStack);
            return iStack.get(ModComponents.DUPE_ID);
        }
        return id;
    }

    @SuppressWarnings("null")
    public static void createDupeId(ItemStack iStack) {
        // add the dupe id to the nbt
        iStack.set(ModComponents.DUPE_ID, AntiDupeDB.register(iStack));
    }

    private static boolean isTrackable(ItemStack iStack) {
        String itemId = iStack.getItem().toString();
        return itemId.contains("diamond") || itemId.contains("netherite") || itemId.contains("shulker_box");
    }

    public static void track(ItemStack iStack, ServerPlayer player, BlockPos pos) {
        if (!isTrackable(iStack)) return;
        
        // get dupe id
        UUID dupeId = getDupeId(iStack);

        if (dupeId == null) {
            // register new item
            createDupeId(iStack);
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
