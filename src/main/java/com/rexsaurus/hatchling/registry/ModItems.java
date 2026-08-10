package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.item.ParasiteEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static final Item PARASITE_EGG = Registry.register(
			Registries.ITEM,
			Identifier.of(Hatchling.MOD_ID, "parasite_egg"),
			new ParasiteEggItem(ModBlocks.PARASITE_EGG, new Item.Settings()));

	public static final Item CHITIN = Registry.register(
			Registries.ITEM,
			Identifier.of(Hatchling.MOD_ID, "chitin"),
			new Item(new Item.Settings()));

	public static final Item PARASITE_SPAWN_EGG = Registry.register(
			Registries.ITEM,
			Identifier.of(Hatchling.MOD_ID, "parasite_spawn_egg"),
			new SpawnEggItem(ModEntities.PARASITE, 0x7ea832, 0x3f5418, new Item.Settings()));

	public static final Item ALIEN_SPAWN_EGG = Registry.register(
			Registries.ITEM,
			Identifier.of(Hatchling.MOD_ID, "alien_spawn_egg"),
			new SpawnEggItem(ModEntities.ALIEN, 0x14180d, 0xc2708a, new Item.Settings()));

	public static final ItemGroup HATCHLING_GROUP = Registry.register(
			Registries.ITEM_GROUP,
			Identifier.of(Hatchling.MOD_ID, "main"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(PARASITE_EGG))
					.displayName(Text.translatable("itemGroup.hatchling.main"))
					.entries((displayContext, entries) -> {
						entries.add(PARASITE_EGG);
						entries.add(CHITIN);
						entries.add(PARASITE_SPAWN_EGG);
						entries.add(ALIEN_SPAWN_EGG);
					})
					.build());

	public static void register() {
		// Static init registers items and creative tab.
	}

	private ModItems() {
	}
}
