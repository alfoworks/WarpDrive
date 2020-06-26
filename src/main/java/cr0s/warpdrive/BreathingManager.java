package cr0s.warpdrive;

import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.api.IBreathingHelmet;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.data.EnergyWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class BreathingManager {
	
	public static boolean isAirBlock(@Nonnull final Block block) {
		return block == WarpDrive.blockAirSource
		    || block == WarpDrive.blockAirFlow;
	}
	
	public static boolean onLivingJoinEvent(final EntityLivingBase entityLivingBase, final int x, final int y, final int z) {
		return true;
	}
	
	public static void onLivingUpdateEvent(final EntityLivingBase entityLivingBase, final int x, final int y, final int z) {
	
	}
	
	public static void onEntityLivingDeath(@Nonnull final EntityLivingBase entityLivingBase) {
	
	}
}
