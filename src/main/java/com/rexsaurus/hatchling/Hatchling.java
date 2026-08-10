package com.rexsaurus.hatchling;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.registry.ModBlocks;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.registry.ModItems;
import com.rexsaurus.hatchling.registry.ModSounds;
import com.rexsaurus.hatchling.registry.ModWorldgen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hatchling implements ModInitializer {
	public static final String MOD_ID = "hatchling";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HatchlingConfig.load();

		ModSounds.register();
		ModBlocks.register();
		ModEntities.register();
		ModItems.register();
		ModWorldgen.register();

		FabricDefaultAttributeRegistry.register(ModEntities.PARASITE,
				com.rexsaurus.hatchling.entity.ParasiteEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.ALIEN,
				com.rexsaurus.hatchling.entity.AlienEntity.createAttributes());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				registerCommands(dispatcher));

		LOGGER.info("Hatchling initialized — the infestation begins.");
	}

	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("hatchling")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("reload")
						.executes(ctx -> {
							HatchlingConfig.load();
							ctx.getSource().sendFeedback(
									() -> Text.literal("Reloaded hatchling.json"), true);
							return 1;
						})));
	}
}
