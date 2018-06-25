package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumLaserTreeFarmMode;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.common.Optional;

public class TileEntityLaserTreeFarm extends TileEntityAbstractMiner {
	
	private static final int    TREE_FARM_WARMUP_DELAY_TICKS = 40;
	private static final int    TREE_FARM_SCAN_DELAY_TICKS = 40;
	private static final int    TREE_FARM_HARVEST_LOG_DELAY_TICKS = 4;
	private static final int    TREE_FARM_BREAK_LEAF_DELAY_TICKS = 4;
	private static final int    TREE_FARM_SILKTOUCH_LEAF_DELAY_TICKS = 4;
	private static final int    TREE_FARM_TAP_TREE_WET_DELAY_TICKS = 4;
	private static final int    TREE_FARM_TAP_TREE_DRY_DELAY_TICKS = 1;
	private static final int    TREE_FARM_PLANT_DELAY_TICKS = 1;
	private static final int    TREE_FARM_LOW_POWER_DELAY_TICKS = 40;
	
	private static final int    TREE_FARM_ENERGY_PER_SURFACE = 1;
	private static final int    TREE_FARM_ENERGY_PER_WET_SPOT = 1;
	private static final double TREE_FARM_ENERGY_PER_LOG = 1;
	private static final double TREE_FARM_ENERGY_PER_LEAF = 1;
	private static final double TREE_FARM_SILKTOUCH_ENERGY_FACTOR = 2.0D;
	private static final int    TREE_FARM_ENERGY_PER_SAPLING = 1;
	
	// persistent properties
	private int radiusX_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
	private int radiusZ_requested = WarpDriveConfig.TREE_FARM_totalMaxRadius;
	private boolean breakLeaves = false;
	private boolean tapTrees = false;
	
	private boolean isFarming() {
		return currentState != STATE_IDLE;
	}
	private static final int STATE_IDLE = 0;
	private static final int STATE_WARMUP = 1;
	private static final int STATE_SCAN = 2;
	private static final int STATE_HARVEST = 3;
	private static final int STATE_TAP = 4;
	private static final int STATE_PLANT = 5;
	private int currentState = STATE_IDLE;
	
	// computed properties
	private int radiusX_actual = radiusX_requested;
	private int radiusZ_actual = radiusZ_requested;
	private boolean isPowered = false;
	
	private int delayTargetTicks = 0;
	
	private int totalHarvested = 0;
	
	private int delayTicks = 0;
	
	private LinkedList<BlockPos> soils;
	private int soilIndex = 0;
	private ArrayList<BlockPos> valuables;
	private int valuableIndex = 0;
	
	public TileEntityLaserTreeFarm() {
		super();
		laserOutputSide = EnumFacing.UP;
		peripheralName = "warpdriveLaserTreeFarm";
		addMethods(new String[] {
				"start",
				"stop",
				"radius",
				"state",
				"breakLeaves",
				"silktouch",
				"tapTrees"
		});
		laserMedium_maxCount = WarpDriveConfig.TREE_FARM_MAX_MEDIUMS_COUNT;
		CC_scripts = Arrays.asList("farm", "stop");
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		if (currentState == STATE_HARVEST || currentState == STATE_TAP || currentState == STATE_PLANT) {
			updateParameters();
			soils = scanSoils();
			valuables = new ArrayList<>(scanTrees());
		}
	}
	
	@SuppressWarnings("UnnecessaryReturnStatement")
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		IBlockState blockState = world.getBlockState(pos);
		if (currentState == STATE_IDLE) {
			delayTicks = 0;
			delayTargetTicks = TREE_FARM_WARMUP_DELAY_TICKS;
			updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.INACTIVE);
			
			// force start if no computer control is available
			if (!WarpDriveConfig.isComputerCraftLoaded && !WarpDriveConfig.isOpenComputersLoaded) {
				breakLeaves = true;
				enableSilktouch = false;
				tapTrees = true;
				start();
			}
			return;
		}
		
		delayTicks++;
		
		updateParameters();
		
		// Scanning
		if (currentState == STATE_WARMUP) {
			updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
			if (delayTicks >= delayTargetTicks) {
				delayTicks = 0;
				delayTargetTicks = TREE_FARM_SCAN_DELAY_TICKS;
				currentState = STATE_SCAN;
				updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
				return;
			}
			
		} else if (currentState == STATE_SCAN) {
			final int energyCost = TREE_FARM_ENERGY_PER_SURFACE * (1 + 2 * radiusX_actual) * (1 + 2 * radiusZ_actual);
			if (delayTicks == 1) {
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.debug("Scan pre-tick");
				}
				
				// validate environment: clearance above
				final IBlockState blockStateAbove = world.getBlockState(pos.up());
				final Block blockAbove = blockStateAbove.getBlock();
				if ( !isLog(blockAbove)
				  && !isLeaf(blockAbove)
				  && !blockAbove.isAir(blockStateAbove, world, pos.up()) ) {
					PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5, new Vector3(this).translate(0.5F),
					                                      new Vector3(0.0D, 0.0D, 0.0D),
					                                      1.0F, 1.0F, 1.0F,
					                                      1.0F, 1.0F, 1.0F,
					                                      32);
					
					currentState = STATE_WARMUP;	// going back to warmup state to show the animation when it'll be back online
					delayTicks = 0;
					delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
					return;
				}
				
				// check power level
				isPowered = laserMedium_consumeExactly(energyCost, true);
				if (!isPowered) {
					currentState = STATE_WARMUP;	// going back to warmup state to show the animation when it'll be back online
					delayTicks = 0;
					delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
					return;
				} else {
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
				}
				
				// show current layer
				final int age = Math.max(40, 2 * TREE_FARM_SCAN_DELAY_TICKS);
				final double xMax = pos.getX() + radiusX_actual + 1.0D;
				final double xMin = pos.getX() - radiusX_actual + 0.0D;
				final double zMax = pos.getZ() + radiusZ_actual + 1.0D;
				final double zMin = pos.getZ() - radiusZ_actual + 0.0D;
				final double y = pos.getY() + world.rand.nextInt(9);
				PacketHandler.sendBeamPacket(world, new Vector3(xMin, y, zMin), new Vector3(xMax, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMax, y, zMin), new Vector3(xMax, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMax, y, zMax), new Vector3(xMin, y, zMax), 0.3F, 0.0F, 1.0F, age, 0, 50);
				PacketHandler.sendBeamPacket(world, new Vector3(xMin, y, zMax), new Vector3(xMin, y, zMin), 0.3F, 0.0F, 1.0F, age, 0, 50);
				
			} else if (delayTicks >= delayTargetTicks) {
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.debug("Scan tick");
				}
				delayTicks = 0;
				
				// consume power
				isPowered = laserMedium_consumeExactly(energyCost, false);
				if (!isPowered) {
					delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
					return;
				} else {
					delayTargetTicks = TREE_FARM_SCAN_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
				}
				
				// scan
				soils = scanSoils();
				soilIndex = 0;
				
				valuables = new ArrayList<>(scanTrees());
				valuableIndex = 0;
				if (!valuables.isEmpty()) {
					world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 4F, 1F);
					currentState = tapTrees ? STATE_TAP : STATE_HARVEST;
					delayTargetTicks = TREE_FARM_HARVEST_LOG_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_POWERED);
					return;
					
				} else if (soils != null && !soils.isEmpty()) {
					world.playSound(null, pos, SoundEvents.LASER_HIGH, SoundCategory.BLOCKS, 4F, 1F);
					currentState = STATE_PLANT;
					delayTargetTicks = TREE_FARM_PLANT_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_POWERED);
					return;
					
				} else {
					world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4F, 1F);
					currentState = STATE_WARMUP;
					delayTargetTicks = TREE_FARM_WARMUP_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
				}
			}
		} else if (currentState == STATE_HARVEST || currentState == STATE_TAP) {
			if (delayTicks >= delayTargetTicks) {
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.debug("Harvest/tap tick");
				}
				delayTicks = 0;
				
				// harvesting done => plant
				if (valuables == null || valuableIndex >= valuables.size()) {
					valuableIndex = 0;
					currentState = STATE_PLANT;
					delayTargetTicks = TREE_FARM_PLANT_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_POWERED);
					return;
				}
				
				// get current block
				final BlockPos valuable = valuables.get(valuableIndex);
				final IBlockState blockStateValuable = world.getBlockState(valuable);
				valuableIndex++;
				boolean isLog = isLog(blockStateValuable.getBlock());
				boolean isLeaf = isLeaf(blockStateValuable.getBlock());
				
				// check area protection
				if (isBlockBreakCanceled(null, world, valuable)) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(this + " Harvesting cancelled at (" + valuable.getX() + " " + valuable.getY() + " " + valuable.getZ() + ")");
					}
					// done with this block
					return;
				}
				
				// save the rubber producing blocks in tapping mode
				if (currentState == STATE_TAP) {
					if (blockStateValuable.getBlock().isAssociatedBlock(WarpDriveConfig.IC2_rubberWood)) {
						final int metadata = blockStateValuable.getBlock().getMetaFromState(blockStateValuable);
						if (metadata >= 2 && metadata <= 5) {
							if (WarpDriveConfig.LOGGING_COLLECTION) {
								WarpDrive.logger.info("Tap found rubber wood wet-spot at " + valuable + " with metadata " + metadata);
							}
							
							// consume power
							final int energyCost = TREE_FARM_ENERGY_PER_WET_SPOT;
							isPowered = laserMedium_consumeExactly(energyCost, false);
							if (!isPowered) {
								delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
								updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_LOW_POWER);
								return;
							} else {
								delayTargetTicks = TREE_FARM_TAP_TREE_WET_DELAY_TICKS;
								updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_POWERED);
							}
							
							final ItemStack resin = WarpDriveConfig.IC2_Resin.copy();
							resin.setCount( (int) Math.round(Math.random() * 4) );
							if (addToConnectedInventories(resin)) {
								stop();
							}
							totalHarvested += resin.getCount();
							final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * TREE_FARM_HARVEST_LOG_DELAY_TICKS));
							PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(valuable).translate(0.5D),
									0.8F, 0.8F, 0.2F, age, 0, 50);
							
							world.setBlockState(valuable, blockStateValuable.getBlock().getStateFromMeta(metadata + 6), 3);
							// done with this block
							return;
							
						} else if (metadata != 0 && metadata != 1) {
							delayTargetTicks = TREE_FARM_TAP_TREE_DRY_DELAY_TICKS;
							// done with this block
							return;
						}
					}
				}
				
				if (isLog || (breakLeaves && isLeaf)) {// actually break the block?
					// consume power
					double energyCost = isLog ? TREE_FARM_ENERGY_PER_LOG : TREE_FARM_ENERGY_PER_LEAF;
					if (enableSilktouch) {
						energyCost *= TREE_FARM_SILKTOUCH_ENERGY_FACTOR;
					}
					isPowered = laserMedium_consumeExactly((int) Math.round(energyCost), false);
					if (!isPowered) {
						delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
						updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_LOW_POWER);
						return;
					} else {
						delayTargetTicks = isLog ? TREE_FARM_HARVEST_LOG_DELAY_TICKS : enableSilktouch ? TREE_FARM_SILKTOUCH_LEAF_DELAY_TICKS : TREE_FARM_BREAK_LEAF_DELAY_TICKS;
						updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.FARMING_POWERED);
					}
					
					totalHarvested++;
					final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
					PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(valuable).translate(0.5D),
							0.2F, 0.7F, 0.4F, age, 0, 50);
					world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4F, 1F);
					
					harvestBlock(valuable);
				}
			}
		} else if (currentState == STATE_PLANT) {
			if (delayTicks >= delayTargetTicks) {
				if (WarpDriveConfig.LOGGING_COLLECTION) {
					WarpDrive.logger.debug("Plant final tick");
				}
				delayTicks = 0;
				
				// planting done => scan
				if (soils == null || soilIndex >= soils.size()) {
					soilIndex = 0;
					currentState = STATE_SCAN;
					delayTargetTicks = TREE_FARM_SCAN_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
					return;
				}
				
				// get current block
				final BlockPos soil = soils.get(soilIndex);
				final BlockPos blockPosPlant = soil.add(0, 1, 0);
				final IBlockState blockStateSoil = world.getBlockState(soil);
				soilIndex++;
				final Collection<IInventory> inventories = Commons.getConnectedInventories(this);
				if (inventories == null || inventories.isEmpty()) {
					currentState = STATE_WARMUP;
					delayTargetTicks = TREE_FARM_WARMUP_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_LOW_POWER);
					return;
				}
				
				int slotIndex = 0;
				int plantableCount = 0;
				ItemStack itemStack = null;
				IBlockState plant = null;
				IInventory inventory = null;
				for (final IInventory inventoryLoop : inventories) {
					if (plant == null) {
						slotIndex = 0;
					}
					while (slotIndex < inventoryLoop.getSizeInventory() && plant == null) {
						itemStack = inventoryLoop.getStackInSlot(slotIndex);
						if (itemStack.isEmpty()) {
							slotIndex++;
							continue;
						}
						final Block blockFromItem = Block.getBlockFromItem(itemStack.getItem());
						if ( !(itemStack.getItem() instanceof IPlantable)
						  && !(blockFromItem instanceof IPlantable) ) {
							slotIndex++;
							continue;
						}
						plantableCount++;
						final IPlantable plantable = (IPlantable) ((itemStack.getItem() instanceof IPlantable) ? itemStack.getItem() : blockFromItem);
						if (itemStack.getItem() instanceof ItemBlock) {
							final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
							final int metadata = itemBlock.getMetadata(itemStack.getMetadata());
							final Block block = itemBlock.getBlock();
							plant = block.getStateForPlacement(world, blockPosPlant, EnumFacing.UP,
							                                   0.5F, 0.0F, 0.5F, metadata,
							                                   null, EnumHand.MAIN_HAND);
						} else {
							plant = plantable.getPlant(world, blockPosPlant);
						}
						if (WarpDriveConfig.LOGGING_COLLECTION) {
							WarpDrive.logger.info("Slot " + slotIndex + " as " + itemStack + " which plantable " + plantable + " as block " + plant);
						}
						
						if (!blockStateSoil.getBlock().canSustainPlant(blockStateSoil, world, soil, EnumFacing.UP, plantable)) {
							plant = null;
							slotIndex++;
							continue;
						}
						
						if (!plant.getBlock().canPlaceBlockAt(world, blockPosPlant)) {
							plant = null;
							slotIndex++;
							continue;
						}
						
						inventory = inventoryLoop;
					}
				}
				
				// no plantable found at all, back to scanning
				if (plantableCount <= 0) {
					currentState = STATE_SCAN;
					delayTargetTicks = TREE_FARM_SCAN_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.SCANNING_POWERED);
					return;
				}
				
				// no sapling found for this soil, moving on...
				if (inventory == null || plant == null || itemStack == null) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.debug("No sapling found");
					}
					return;
				}
				
				// check area protection
				if (isBlockPlaceCanceled(null, world, blockPosPlant, plant)) {
					if (WarpDriveConfig.LOGGING_COLLECTION) {
						WarpDrive.logger.info(this + " Planting cancelled at (" + blockPosPlant.getX() + " " + blockPosPlant.getY() + " " + blockPosPlant.getZ() + ")");
					}
					// done with this block
					return;
				}
				
				// consume power
				final int energyCost = TREE_FARM_ENERGY_PER_SAPLING;
				isPowered = laserMedium_consumeExactly(energyCost, false);
				if (!isPowered) {
					delayTargetTicks = TREE_FARM_LOW_POWER_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_LOW_POWER);
					return;
				} else {
					delayTargetTicks = TREE_FARM_PLANT_DELAY_TICKS;
					updateBlockState(blockState, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.PLANTING_POWERED);
				}
				
				itemStack.shrink(1);
				inventory.setInventorySlotContents(slotIndex, itemStack);
				
				// totalPlanted++;
				final int age = Math.max(10, Math.round((4 + world.rand.nextFloat()) * WarpDriveConfig.MINING_LASER_MINE_DELAY_TICKS));
				PacketHandler.sendBeamPacket(world, laserOutput, new Vector3(blockPosPlant).translate(0.5D),
						0.2F, 0.7F, 0.4F, age, 0, 50);
				world.playSound(null, pos, SoundEvents.LASER_LOW, SoundCategory.BLOCKS, 4F, 1F);
				world.setBlockState(blockPosPlant, plant, 3);
			}
		}
	}
	
	@Override
	protected void stop() {
		super.stop();
		currentState = STATE_IDLE;
		updateBlockState(null, BlockLaserTreeFarm.MODE, EnumLaserTreeFarmMode.INACTIVE);
	}
	
	private void updateParameters() {
		final int maxScanRadius = WarpDriveConfig.TREE_FARM_MAX_SCAN_RADIUS_NO_LASER_MEDIUM
		                        + cache_laserMedium_count * WarpDriveConfig.TREE_FARM_MAX_SCAN_RADIUS_PER_LASER_MEDIUM;
		radiusX_actual = Math.min(radiusX_requested, maxScanRadius);
		radiusZ_actual = Math.min(radiusZ_requested, maxScanRadius);
	}
	
	private static boolean isSoil(final Block block) {
		return Dictionary.BLOCKS_SOILS.contains(block);
	}
	
	private static boolean isLog(final Block block) {
		return Dictionary.BLOCKS_LOGS.contains(block);
	}
	
	private static boolean isLeaf(final Block block) {
		return Dictionary.BLOCKS_LEAVES.contains(block);
	}
	
	private LinkedList<BlockPos> scanSoils() {
		final int xMin = pos.getX() - radiusX_actual;
		final int xMax = pos.getX() + radiusX_actual;
		final int yMin = pos.getY();
		final int yMax = pos.getY() + 8;
		final int zMin = pos.getZ() - radiusZ_actual;
		final int zMax = pos.getZ() + radiusZ_actual;
		
		final LinkedList<BlockPos> soilPositions = new LinkedList<>();
		
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					final BlockPos blockPos = new BlockPos(x, y, z);
					if (world.isAirBlock(blockPos.add(0, 1, 0))) {
						final Block block = world.getBlockState(blockPos).getBlock();
						if (isSoil(block)) {
							if (WarpDriveConfig.LOGGING_COLLECTION) {
								WarpDrive.logger.info("Found soil at " + x + " " + y + " " + z);
							}
							soilPositions.add(blockPos);
						}
					}
				}
			}
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info("Found " + soilPositions.size() + " soils");
		}
		return soilPositions;
	}
	
	private Collection<BlockPos> scanTrees() {
		final int xMin = pos.getX() - radiusX_actual;
		final int xMax = pos.getX() + radiusX_actual;
		final int yMin = pos.getY() + 1;
		final int yMax = pos.getY() + 1 + (tapTrees ? 8 : 0);
		final int zMin = pos.getZ() - radiusZ_actual;
		final int zMax = pos.getZ() + radiusZ_actual;
		
		Collection<BlockPos> logPositions = new HashSet<>();
		
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					final BlockPos blockPos = new BlockPos(x, y, z);
					final Block block = world.getBlockState(blockPos).getBlock();
					if (isLog(block)) {
						if (!logPositions.contains(blockPos)) {
							if (WarpDriveConfig.LOGGING_COLLECTION) {
								WarpDrive.logger.info("Found tree base at " + x + "," + y + "," + z);
							}
							logPositions.add(blockPos);
						}
					}
				}
			}
		}
		if (!logPositions.isEmpty()) {
			@SuppressWarnings("unchecked")
			final HashSet<Block> whitelist = (HashSet<Block>) Dictionary.BLOCKS_LOGS.clone();
			if (breakLeaves) {
				whitelist.addAll(Dictionary.BLOCKS_LEAVES);
			}
			final int maxLogDistance = WarpDriveConfig.TREE_FARM_MAX_LOG_DISTANCE
			                         + cache_laserMedium_count * WarpDriveConfig.TREE_FARM_MAX_LOG_DISTANCE_PER_MEDIUM;
			logPositions = Commons.getConnectedBlocks(world, logPositions, Commons.UP_DIRECTIONS, whitelist, maxLogDistance);
		}
		if (WarpDriveConfig.LOGGING_COLLECTION) {
			WarpDrive.logger.info("Found " + logPositions.size() + " valuables");
		}
		return logPositions;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setInteger("radiusX", radiusX_requested);
		tagCompound.setInteger("radiusZ", radiusZ_requested);
		tagCompound.setBoolean("breakLeaves", breakLeaves);
		tagCompound.setBoolean("tapTrees", tapTrees);
		tagCompound.setInteger("currentState", currentState);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		radiusX_requested = tagCompound.getInteger("radiusX");
		radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, radiusX_requested);
		radiusZ_requested = tagCompound.getInteger("radiusZ");
		radiusZ_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, radiusZ_requested);
		
		breakLeaves     = tagCompound.getBoolean("breakLeaves");
		tapTrees        = tagCompound.getBoolean("tapTrees");
		currentState    = tagCompound.getInteger("currentState");
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] start(final Context context, final Arguments arguments) {
		return start();
	}
	
	@SuppressWarnings("SameReturnValue")
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] stop(final Context context, final Arguments arguments) {
		stop();
		return null;
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		return state();
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] radius(final Context context, final Arguments arguments) {
		return radius(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] breakLeaves(final Context context, final Arguments arguments) {
		return breakLeaves(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] silktouch(final Context context, final Arguments arguments) {
		return silktouch(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] tapTrees(final Context context, final Arguments arguments) {
		return tapTrees(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// Common OC/CC methods
	private Object[] start() {
		if (isFarming()) {
			return new Object[] { false, "Already started" };
		}
		
		totalHarvested = 0;
		delayTicks = 0;
		currentState = STATE_WARMUP;
		return new Boolean[] { true };
	}
	
	private Object[] state() {
		final int energy = laserMedium_getEnergyStored();
		final String status = getStatusHeaderInPureText();
		final Integer retValuables, retValuablesIndex;
		if (isFarming() && valuables != null) {
			retValuables = valuables.size();
			retValuablesIndex = valuableIndex;
			
			return new Object[] { status, isFarming(), energy, totalHarvested, retValuablesIndex, retValuables };
		}
		return new Object[] { status, isFarming(), energy, totalHarvested, 0, 0 };
	}
	
	private Object[] radius(final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[0]));
				radiusZ_requested = radiusX_requested;
				markDirty();
			} else if (arguments.length == 2) {
				radiusX_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[0]));
				radiusZ_requested = Commons.clamp(1, WarpDriveConfig.TREE_FARM_totalMaxRadius, Commons.toInt(arguments[1]));
				markDirty();
			}
		} catch(final NumberFormatException exception) {
			radiusX_requested = WarpDriveConfig.TREE_FARM_MAX_SCAN_RADIUS_NO_LASER_MEDIUM;
			radiusZ_requested = WarpDriveConfig.TREE_FARM_MAX_SCAN_RADIUS_NO_LASER_MEDIUM;
		}
		return new Integer[] { radiusX_requested, radiusZ_requested };
	}
	
	private Object[] breakLeaves(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				breakLeaves = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { breakLeaves };
			}
		}
		return new Object[] { breakLeaves };
	}
	
	private Object[] silktouch(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				enableSilktouch = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { enableSilktouch };
			}
		}
		return new Object[] { enableSilktouch };
	}
	
	private Object[] tapTrees(final Object[] arguments) {
		if (arguments.length == 1 && arguments[0] != null) {
			try {
				tapTrees = Commons.toBool(arguments[0]);
				markDirty();
			} catch (final Exception exception) {
				return new Object[] { tapTrees };
			}
		}
		return new Object[] { tapTrees };
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "start":
			return start();
			
		case "stop":
			stop();
			return null;
			
		case "state":
			return state();
			
		case "radius":
			return radius(arguments);
			
		case "breakLeaves":
			return breakLeaves(arguments);
			
		case "silktouch":
			return silktouch(arguments);
			
		case "tapTrees":
			return tapTrees(arguments);
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
	
	@Override
	public ITextComponent getStatusHeader() {
		final int energy = laserMedium_getEnergyStored();
		String state = "IDLE (not farming)";
		if (currentState == STATE_IDLE) {
			state = "IDLE (not farming)";
		} else if (currentState == STATE_WARMUP) {
			state = "Warming up...";
		} else if (currentState == STATE_SCAN) {
			if (breakLeaves) {
				state = "Scanning all";
			} else {
				state = "Scanning logs";
			}
		} else if (currentState == STATE_HARVEST) {
			if (breakLeaves) {
				state = "Harvesting all";
			} else {
				state = "Harvesting logs";
			}
			if (enableSilktouch) {
				state = state + " with silktouch";
			}
		} else if (currentState == STATE_TAP) {
			if (breakLeaves) {
				state = "Tapping trees, harvesting all";
			} else {
				state = "Tapping trees, harvesting logs";
			}
			if (enableSilktouch) {
				state = state + " with silktouch";
			}
		} else if (currentState == STATE_PLANT) {
			state = "Planting trees";
		}
		if (energy <= 0) {
			state = state + " - Out of energy";
		} else if (((currentState == STATE_SCAN) || (currentState == STATE_HARVEST) || (currentState == STATE_TAP)) && !isPowered) {
			state = state + " - Not enough power";
		}
		return new TextComponentString(state);
	}
}
