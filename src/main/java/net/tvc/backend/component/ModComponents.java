package net.tvc.backend.component;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Identifier;

import net.tvc.backend.BackendInstance;

import com.mojang.serialization.Codec;

import java.util.UUID;

public class ModComponents {
    static final DataComponentType<UUID> DUPE_ID = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(BackendInstance.MOD_ID, "dupe_id"),
        DataComponentType.<UUID>builder().persistent(null).build()
    );
    
    public static void initialize() {}
}