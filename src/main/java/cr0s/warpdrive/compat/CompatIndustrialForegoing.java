package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatIndustrialForegoing implements IBlockTransformer {
	
	private static Class<?> classAxisAlignedBlock;
	private static Class<?> classBlockConveyor;
	private static Class<?> classBlockLabel;
	
	public static void register() {
		try {
			classAxisAlignedBlock = Class.forName("net.ndrei.teslacorelib.blocks.AxisAlignedBlock");
			classBlockConveyor = Class.forName("com.buuz135.industrial.proxy.block.BlockConveyor");
			classBlockLabel = Class.forName("com.buuz135.industrial.proxy.block.BlockLabel");
			
			WarpDriveConfig.registerBlockTransformer("IndustrialForegoing", new CompatIndustrialForegoing());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classAxisAlignedBlock.isInstance(block)
		    || classBlockConveyor.isInstance(block)
		    || classBlockLabel.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotFacing           = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private static final Map<String, String> rotFacingNames;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("north", "east");
		map.put("east", "south");
		map.put("south", "west");
		map.put("west", "north");
		rotFacingNames = Collections.unmodifiableMap(map);
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (classBlockConveyor.isInstance(block)) {
			// facing property
			if (nbtTileEntity.hasKey("Facing")) {
				final String facing = nbtTileEntity.getString("Facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setString("Facing", rotFacingNames.get(facing));
					break;
				case 2:
					nbtTileEntity.setString("Facing", rotFacingNames.get(rotFacingNames.get(facing)));
					break;
				case 3:
					nbtTileEntity.setString("Facing", rotFacingNames.get(rotFacingNames.get(rotFacingNames.get(facing))));
					break;
				default:
					break;
				}
			}
			
			// upgrades
			if (nbtTileEntity.hasKey("Upgrades")) {
				final NBTTagCompound tagCompoundUpgrades = nbtTileEntity.getCompoundTag("Upgrades");
				final Map<String, NBTBase> map = new HashMap<>();
				for (final String key : rotFacingNames.keySet()) {
					if (tagCompoundUpgrades.hasKey(key)) {
						final NBTBase tagBase = tagCompoundUpgrades.getTag(key);
						switch (rotationSteps) {
						case 1:
							map.put(rotFacingNames.get(key), tagBase);
							break;
						case 2:
							map.put(rotFacingNames.get(rotFacingNames.get(key)), tagBase);
							break;
						case 3:
							map.put(rotFacingNames.get(rotFacingNames.get(rotFacingNames.get(key))), tagBase);
							break;
						default:
							map.put(key, tagBase);
							break;
						}
						tagCompoundUpgrades.removeTag(key);
					}
				}
				if (!map.isEmpty()) {
					for (final Entry<String, NBTBase> entry : map.entrySet()) {
						tagCompoundUpgrades.setTag(entry.getKey(), entry.getValue());
					}
				}
			}
		}
		
		// vanilla facing
		switch (rotationSteps) {
		case 1:
			return rotFacing[metadata];
		case 2:
			return rotFacing[rotFacing[metadata]];
		case 3:
			return rotFacing[rotFacing[rotFacing[metadata]]];
		default:
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
