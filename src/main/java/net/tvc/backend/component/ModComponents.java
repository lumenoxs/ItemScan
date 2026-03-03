package net.tvc.backend.component;

import java.util.UUID;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.UUIDUtil;
import com.mojang.serialization.Codec;

public class ModComponents {
    @SuppressWarnings("null")
    public static final DataComponentType<UUID> DUPE_ID = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("tvc-backend", "dupe_id"),
        DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC)
            .build()
    );

    @SuppressWarnings("null")
    public static final DataComponentType<Boolean> IMMUNE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("tvc-backend", "immune"),
        DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .build()
    );

    public static void initialize() {}
}