package com.raoulvdberge.refinedstorage.api.autocrafting.registry;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.CraftingTaskReadException;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

/**
 * A factory that creates a crafting task.
 * Register this factory in the {@link ICraftingTaskRegistry}.
 */
public interface ICraftingTaskFactory {
    /**
     * Returns a crafting task for a given pattern.
     *
     * @param network   the network
     * @param requested the request info
     * @param pattern   the pattern
     * @param quantity  the quantity
     * @return the crafting task
     */
    @Nonnull
    ICraftingTask create(INetwork network, ICraftingRequestInfo requested, int quantity, ICraftingPattern pattern);

    /**
     * Returns a crafting task for a given NBT tag.
     *
     * @param network the network
     * @param tag     the tag
     * @return the crafting task
     */
    ICraftingTask createFromNbt(INetwork network, NBTTagCompound tag) throws CraftingTaskReadException;
}
