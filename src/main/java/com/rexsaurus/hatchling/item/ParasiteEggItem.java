package com.rexsaurus.hatchling.item;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.ThrownParasiteEggEntity;
import com.rexsaurus.hatchling.registry.ModEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.block.Block;

/**
 * Places as a block when used on a block face ({@link BlockItem#useOnBlock}).
 * Throws when used in the air ({@link #use}).
 */
public class ParasiteEggItem extends BlockItem {
	public ParasiteEggItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;

		world.playSound(
				null,
				user.getX(), user.getY(), user.getZ(),
				SoundEvents.ENTITY_EGG_THROW,
				SoundCategory.PLAYERS,
				0.5f,
				0.6f);

		if (!world.isClient) {
			ThrownParasiteEggEntity thrown = new ThrownParasiteEggEntity(ModEntities.THROWN_PARASITE_EGG, user, world);
			thrown.setItem(stack);
			thrown.setVelocity(
					user,
					user.getPitch(),
					user.getYaw(),
					0.0f,
					(float) life.eggThrowVelocity,
					1.0f);
			world.spawnEntity(thrown);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		user.getItemCooldownManager().set(this, life.eggThrowCooldownTicks);
		stack.decrementUnlessCreative(1, user);
		return TypedActionResult.success(stack, world.isClient());
	}
}
