package com.rexsaurus.hatchling.block;

import com.rexsaurus.hatchling.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/** Stores lifecycle generation for eggs laid by aliens. Player/worldgen eggs stay generation 0. */
public class ParasiteEggBlockEntity extends BlockEntity {
	private static final String GENERATION_KEY = "Generation";
	private int generation;

	public ParasiteEggBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PARASITE_EGG, pos, state);
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = Math.max(0, generation);
		this.markDirty();
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putInt(GENERATION_KEY, generation);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		generation = nbt.getInt(GENERATION_KEY);
	}
}
