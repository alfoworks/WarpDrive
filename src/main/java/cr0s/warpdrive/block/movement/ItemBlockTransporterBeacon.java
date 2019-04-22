package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IItemTransporterBeacon;
import cr0s.warpdrive.api.computer.ITransporterCore;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.UUID;

public class ItemBlockTransporterBeacon extends ItemBlockAbstractBase implements IItemTransporterBeacon {
	
	public ItemBlockTransporterBeacon(final Block block) {
		super(block, false, false);
		
		setMaxStackSize(1);
		setMaxDamage(100 * 8);
	}
	
	private static String getTransporterName(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return "";
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return "";
		}
		final String name = tagCompound.getString("name");
		final UUID uuid = new UUID(tagCompound.getLong("uuidMost"), tagCompound.getLong("uuidLeast"));
		if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
			return "";
		}
		return name;
	}
	
	private static ItemStack setTransporterName(final ItemStack itemStack, final String name) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		if ( name == null
		  || name.isEmpty() ) {
			tagCompound.removeTag("name");
		} else {
			tagCompound.setString("name", name);
		}
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	private static UUID getTransporterSignature(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return null;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return null;
		}
		final UUID uuid = new UUID(tagCompound.getLong("uuidMost"), tagCompound.getLong("uuidLeast"));
		if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
			return null;
		}
		return uuid;
	}
	
	private static ItemStack setTransporterSignature(final ItemStack itemStack, final UUID uuid) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		if (uuid == null) {
			tagCompound.removeTag("uuidMost");
			tagCompound.removeTag("uuidLeast");
		} else {
			tagCompound.setLong("uuidMost", uuid.getMostSignificantBits());
			tagCompound.setLong("uuidLeast", uuid.getLeastSignificantBits());
		}
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	private static int getEnergy(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return 0;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return 0;
		}
		if (tagCompound.hasKey(EnergyWrapper.TAG_ENERGY)) {
			return tagCompound.getInteger(EnergyWrapper.TAG_ENERGY);
		}
		return 0;
	}
	
	private static ItemStack setEnergy(@Nonnull final ItemStack itemStack, final int energy) {
		if (!(itemStack.getItem() instanceof ItemBlockTransporterBeacon)) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		tagCompound.setInteger(EnergyWrapper.TAG_ENERGY, energy);
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	private static ItemStack updateDamage(@Nonnull final ItemStack itemStack, final int energy, final boolean isActive) {
		final int maxDamage = itemStack.getMaxDamage();
		final int metadataEnergy = maxDamage - maxDamage * energy / WarpDriveConfig.TRANSPORTER_BEACON_MAX_ENERGY_STORED;
		final int metadataNew = (metadataEnergy & ~0x3) + (isActive ? 2 : 0);
		if (metadataNew != itemStack.getItemDamage()) {
			itemStack.setItemDamage(metadataNew);
			return itemStack;
		} else {
			return null;
		}
	}
	
	// ITransporterBeacon overrides
	@Override
	public boolean isActive(@Nonnull final ItemStack itemStack) {
		return getEnergy(itemStack) > WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK;
	}
	
	// Item overrides
	@Override
	public void onUpdate(final ItemStack itemStack, final World world, final Entity entity, final int indexSlot, final boolean isHeld) {
		if (entity instanceof EntityPlayer) {
			final EntityPlayer entityPlayer = (EntityPlayer) entity;
			final ItemStack itemStackCheck = entityPlayer.inventory.getStackInSlot(indexSlot);
			if (itemStackCheck != itemStack) {
				WarpDrive.logger.error(String.format("Invalid item selection: possible dup tentative from %s",
				                                     entityPlayer));
				return;
			}
			
			// consume energy
			final int energy =  getEnergy(itemStack) - WarpDriveConfig.TRANSPORTER_BEACON_ENERGY_PER_TICK;
			if ( isHeld
			  && energy >= 0 ) {
				final ItemStack itemStackNew = setEnergy(itemStack, energy);
				updateDamage(itemStackNew, energy, true);
				((EntityPlayer) entity).inventory.setInventorySlotContents(indexSlot, itemStackNew);
				
			} else if (itemStack.getItemDamage() != 0) {// (still shows with energy but has none)
				final ItemStack itemStackNew = updateDamage(itemStack, energy, false);
				if (itemStackNew != null) {
					((EntityPlayer) entity).inventory.setInventorySlotContents(indexSlot, itemStackNew);
				}
			}
		}
		super.onUpdate(itemStack, world, entity, indexSlot, isHeld);
	}
	
	/* @TODO 1.10 is the upstream 1.7.10 bug fixed?
	@Override
	public boolean onItemUseFirst(final ItemStack itemStack, final EntityPlayer entityPlayer,
	                              final World world, final int x, final int y, final int z,
	                              final int side, final float hitX, final float hitY, final float hitZ) {
		// itemStack is constantly updated for energy updates
		// in net.minecraft.network.NetHandlerPlayServer.processPlayerBlockPlacement(NetHandlerPlayServer.java:657), a NPE appears randomly due to bad multithreading in upstream
		// consequently, we prevent to use the item on any tile entity other than a TransporterCore
		
		// allows block placement while sneaking
		if (entityPlayer.isSneaking()) {
			return false;
		}
		
		// allows non-tile entities or transporter core
		final TileEntity tileEntity = world.getTileEntity(x, y, z);
		if ( tileEntity == null
		  || tileEntity instanceof ITransporterCore ) {
			return false;
		}
		
		// allow if beacon is disabled
		if (!isActive(itemStack)) {
			return false;
		}
		
		// forbid everything else
		return true;
	}
	/**/
	
	@Nonnull
	@Override
	public EnumActionResult onItemUse(@Nonnull final EntityPlayer entityPlayer,
	                                  @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumHand hand,
	                                  @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return EnumActionResult.FAIL;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		if (itemStackHeld.isEmpty()) {
			return EnumActionResult.FAIL;
		}
		
		// check if clicked block can be interacted with
		// final Block block = world.getBlock(x, y, z);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		
		if (!(tileEntity instanceof ITransporterCore)) {
			return super.onItemUse(entityPlayer, world, blockPos, hand, facing, hitX, hitY, hitZ);
		}
		if (!entityPlayer.canPlayerEdit(blockPos, facing, itemStackHeld)) {
			return EnumActionResult.FAIL;
		}
		
		final UUID uuidBeacon = getTransporterSignature(itemStackHeld);
		final String nameBeacon = getTransporterName(itemStackHeld);
		final UUID uuidTransporter = ((ITransporterCore) tileEntity).getUUID();
		if (entityPlayer.isSneaking()) {// update transporter signature
			final String nameTransporter = ((ITransporterCore) tileEntity).getStarMapName();
			
			if ( uuidTransporter == null
			  || nameTransporter == null
			  || nameTransporter.isEmpty() ) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.get_missing"));
				
			} else if (uuidTransporter.equals(uuidBeacon)) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.get_same",
				                                                                  nameTransporter));
				
			} else {
				final ItemStack itemStackNew = setTransporterName(itemStackHeld, nameTransporter);
				setTransporterSignature(itemStackNew, uuidTransporter);
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.get",
				                                                                  nameTransporter));
				world.playSound(entityPlayer.posX + 0.5D, entityPlayer.posY + 0.5D, entityPlayer.posZ + 0.5D,
				                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS,
				                1.0F, 1.8F + 0.2F * world.rand.nextFloat(), false);
			}
			
		} else {// apply signature to transporter
			final Object[] remoteLocation = ((ITransporterCore) tileEntity).remoteLocation(new Object[] { });
			UUID uuidRemoteLocation;
			if ( remoteLocation == null
			  || remoteLocation.length != 1
			  || !(remoteLocation[0] instanceof String) ) {
				uuidRemoteLocation = null;
			} else {
				try {
					uuidRemoteLocation = UUID.fromString((String) remoteLocation[0]);
				} catch (final IllegalArgumentException exception) {// it's a player name
					uuidRemoteLocation = null;
				}
			}
			
			if (uuidBeacon == null) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.set_missing",
				                                                                  nameBeacon));
				
			} else if (uuidBeacon.equals(uuidTransporter)) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.set_self",
				                                                                  nameBeacon));
				
			} else if (uuidBeacon.equals(uuidRemoteLocation)) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.set_same",
				                                                                  nameBeacon));
				
			} else {
				((ITransporterCore) tileEntity).remoteLocation(new Object[] { uuidBeacon });
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.transporter_signature.set",
				                                                                  nameBeacon));
				world.playSound(entityPlayer.posX + 0.5D, entityPlayer.posY + 0.5D, entityPlayer.posZ + 0.5D,
				                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS,
				                1.0F, 1.2F + 0.2F * world.rand.nextFloat(), false);
			}
		}
		
		return EnumActionResult.SUCCESS;
	}
}
