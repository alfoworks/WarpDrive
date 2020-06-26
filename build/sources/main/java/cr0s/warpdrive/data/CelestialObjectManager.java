package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import net.minecraftforge.common.DimensionManager;

public class CelestialObjectManager extends XmlFileManager {
	
	private static final CelestialObjectManager SERVER = new CelestialObjectManager();
	private static final CelestialObjectManager CLIENT = new CelestialObjectManager();
	private HashMap<String, CelestialObject> celestialObjectsById = new HashMap<>();
	public CelestialObject[] celestialObjects = new CelestialObject[0];
	
	private double maxWorldBorder = 0.0D;
	
	// *** mixed statics ***
	
	public static void clearForReload(final boolean isRemote) {
		// create a new object instead of clearing, in case another thread is iterating through it
		(isRemote ? CLIENT : SERVER).celestialObjectsById = new HashMap<>();
	}
	
	public static CelestialObject get(final boolean isRemote, final String id) {
		return (isRemote ? CLIENT : SERVER).celestialObjectsById.get(id);
	}
	
	public static CelestialObject get(final World world, final int x, final int z) {
		if (world == null) {
			return null;
		}
		return (world.isRemote ? CLIENT : SERVER).get(world.provider.getDimension(), x, z);
	}
	
	public static CelestialObject get(final boolean isRemote, final int dimensionId, final int x, final int z) {
		return (isRemote ? CLIENT : SERVER).get(dimensionId, x, z);
	}
	
	public static CelestialObject getClosestChild(final World world, final int x, final int z) {
		double closestPlanetDistance = Double.POSITIVE_INFINITY;
		CelestialObject celestialObjectClosest = null;
		if (world != null) {
			for (final CelestialObject celestialObject : (world.isRemote ? CLIENT : SERVER).celestialObjects) {
				if (celestialObject.isHyperspace()) {
					continue;
				}
				final double distanceSquared = celestialObject.getSquareDistanceInParent(world.provider.getDimension(), x, z);
				if (distanceSquared <= 0.0D) {
					return celestialObject;
				} else if (closestPlanetDistance > distanceSquared) {
					closestPlanetDistance = distanceSquared;
					celestialObjectClosest = celestialObject;
				}
			}
		}
		return celestialObjectClosest;
	}
	
	public static boolean isInSpace(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject != null && celestialObject.isSpace();
	}
	
	public static boolean isInHyperspace(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject != null && celestialObject.isHyperspace();
	}
	
	public static boolean hasAtmosphere(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject == null || celestialObject.hasAtmosphere();
	}
	
	public static boolean isPlanet(final World world, final int x, final int z) {
		final CelestialObject celestialObject = get(world, x, z);
		return celestialObject == null
		    || (!celestialObject.isSpace() && !celestialObject.isHyperspace());
	}
	
	public static double getGravity(@Nonnull final Entity entity) {
		final CelestialObject celestialObject = get(entity.world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posZ));
		return celestialObject == null ? 1.0D : celestialObject.getGravity();
	}
	
	public static int getSpaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in space?
		if (celestialObject.isSpace()) {
			return celestialObject.dimensionId;
		}
		// coming from hyperspace?
		if (celestialObject.isHyperspace()) {
			celestialObject = getClosestChild(world, x, z);
			return celestialObject == null ? 0 : celestialObject.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null ? 0 : celestialObject.dimensionId;
	}
	
	public static int getHyperspaceDimensionId(final World world, final int x, final int z) {
		CelestialObject celestialObject = get(world, x, z);
		if (celestialObject == null) {
			return world.provider.getDimension();
		}
		// already in hyperspace?
		if (celestialObject.isHyperspace()) {
			return celestialObject.dimensionId;
		}
		// coming from space?
		if (celestialObject.isSpace()) {
			return celestialObject.parent.dimensionId;
		}
		// coming from a planet?
		while (celestialObject != null && !celestialObject.isSpace()) {
			celestialObject = celestialObject.parent;
		}
		return celestialObject == null || celestialObject.parent == null ? 0 : celestialObject.parent.dimensionId;
	}
	
	public static int getDimensionId(final String stringDimension, final Entity entity) {
		switch (stringDimension.toLowerCase()) {
		case "world":
		case "overworld":
		case "0":
			return 0;
		case "nether":
		case "thenether":
		case "-1":
			return -1;
		case "s":
		case "space":
			return getSpaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		case "h":
		case "hyper":
		case "hyperspace":
			return getHyperspaceDimensionId(entity.world, (int) entity.posX, (int) entity.posZ);
		default:
			try {
				return Integer.parseInt(stringDimension);
			} catch (final Exception exception) {
				// exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.info(String.format("Invalid dimension %s, expecting integer or overworld/nether/end/theend/space/hyper/hyperspace",
				                                    stringDimension));
			}
		}
		return 0;
	}
	
	public static double getMaxWorldBorder(final World world) {
		return (world.isRemote ? CLIENT : SERVER).getMaxWorldBorder();
	}
	
	// *** server side only ***
	
	public static void onFMLInitialization() {
		// only create dimensions if we own them
		for (final CelestialObject celestialObject : SERVER.celestialObjects) {
			if (!celestialObject.isVirtual()) {
				switch (celestialObject.provider) {
				case CelestialObject.PROVIDER_SPACE:
					if (celestialObject.isSpace()) {
						DimensionManager.registerDimension(celestialObject.dimensionId, WarpDrive.dimensionTypeSpace);
					} else {
						WarpDrive.logger.error(String.format("Only a space dimension can be provided by WarpDriveSpace. Dimension %d is not one of those.",
						                                     celestialObject.dimensionId));
					}
					break;
					
				case CelestialObject.PROVIDER_HYPERSPACE:
					if (celestialObject.isHyperspace()) {
						DimensionManager.registerDimension(celestialObject.dimensionId, WarpDrive.dimensionTypeHyperSpace);
					} else {
						WarpDrive.logger.error(String.format("Only an hyperspace dimension can be provided by WarpDriveHyperspace. Dimension %d is not one of those.",
						                                     celestialObject.dimensionId));
					}
					break;
					
				case CelestialObject.PROVIDER_OTHER:
					// nothing
					break;
					
				default:
					WarpDrive.logger.error(String.format("Unknown dimension provider %s for dimension %d, ignoring...",
					                                     celestialObject.provider,
					                                     celestialObject.dimensionId));
					break;
				}
			}
		}
	}
	
	public static void load(final File dir) {
		SERVER.load(dir, "celestialObjects", "celestialObject");
		SERVER.rebuildAndValidate(false);
	}
	
	// @TODO add a proper API
	public static void updateInRegistry(final CelestialObject celestialObject) {
		SERVER.addOrUpdateInRegistry(celestialObject, true);
		SERVER.rebuildAndValidate(true);
	}
	
	public static NBTBase writeClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		final NBTTagList nbtTagList = new NBTTagList();
		if (celestialObject != null) {
			// add current with all direct parents
			CelestialObject celestialObjectParent = celestialObject;
			while (celestialObjectParent != null) {
				nbtTagList.appendTag(celestialObjectParent.writeToNBT(new NBTTagCompound()));
				celestialObjectParent = celestialObjectParent.parent;
			}
			
			// add all children
			for (final CelestialObject celestialObjectChild : SERVER.celestialObjects) {
				// keep only direct children
				if (!celestialObjectChild.parentId.equals(celestialObject.id)) {
					continue;
				}
				nbtTagList.appendTag(celestialObjectChild.writeToNBT(new NBTTagCompound()));
			}
		}
		return nbtTagList;
	}
	
	public static boolean onOpeningNetherPortal(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		// prevent creating a portal outside the world border
		final CelestialObject celestialObjectPortal = get(world, blockPos.getX(), blockPos.getZ());
		if (celestialObjectPortal != null) {
			if (!celestialObjectPortal.isInsideBorder(blockPos.getX(), blockPos.getZ()) ) {
				final EntityPlayer entityPlayer = world.getClosestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10.0D, false);
				if (entityPlayer != null) {
					entityPlayer.sendStatusMessage(
							new WarpDriveText(Commons.getStyleWarning(), "warpdrive.world_border.portal_denied"), true);
				}
				WarpDrive.logger.info(String.format("Nether portal opening cancelled %s for player %s: portal entry is outside the world border",
				                                    entityPlayer == null ? "-null-" : entityPlayer.getName(),
				                                    Commons.format(world, blockPos) ));
				return false;
			}
			
			// @TODO prevent creating a portal in specific dimensions
		}
		
		// prevent creating a portal leading outside the world border
		final boolean isInTheNether = world.provider.getDimension() == -1;
		final CelestialObject celestialObjectExit = get(false, isInTheNether ? 0 : -1, 0, 0);
		if (celestialObjectExit != null) {
			final double factor = isInTheNether ? 8.0D : 1 / 8.0D;
			final int xExit = (int) Math.floor(blockPos.getX() * factor);
			final int zExit = (int) Math.floor(blockPos.getZ() * factor);
			if ( Math.abs(xExit - celestialObjectExit.dimensionCenterX) > celestialObjectExit.borderRadiusX
			  || Math.abs(zExit - celestialObjectExit.dimensionCenterZ) > celestialObjectExit.borderRadiusZ ) {
				final EntityPlayer entityPlayer = world.getClosestPlayer(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 10.0D, false);
				if (entityPlayer != null) {
					entityPlayer.sendStatusMessage(
							new WarpDriveText(Commons.getStyleWarning(), "warpdrive.world_border.portal_denied"), true);
				}
				WarpDrive.logger.info(String.format("Nether portal opening cancelled for player %s %s: portal exit is outside the world border",
				                                    entityPlayer == null ? "-null-" : entityPlayer.getName(),
				                                    Commons.format(world, blockPos) ));
				return false;
			}
		}
		
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("Opening Nether portal %s",
			                                    Commons.format(world, blockPos) ));
		}
		
		return true;
	}
	
	// *** client side only ***
	
	@SideOnly(Side.CLIENT)
	public static void readClientSync(final NBTTagList nbtTagList) {
		clearForReload(true);
		if (nbtTagList != null && nbtTagList.tagCount() > 0) {
			for (int index = 0; index < nbtTagList.tagCount(); index++) {
				final CelestialObject celestialObject = new CelestialObject(nbtTagList.getCompoundTagAt(index));
				CLIENT.addOrUpdateInRegistry(celestialObject, false);
			}
		}
		CLIENT.rebuildAndValidate(true);
	}
	
	@SideOnly(Side.CLIENT)
	public static CelestialObject[] getRenderStack() {
		return CLIENT.celestialObjects;
	}
	
	@SuppressWarnings("unused") // Core mod
	@SideOnly(Side.CLIENT)
	public static WorldBorder World_getWorldBorder(@Nonnull final World world) {
		final WorldBorder worldBorder = world.getWorldBorder();
		final EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
		if ( entityPlayer == null
		  || entityPlayer.world != world ) {
			return worldBorder;
		}
		final CelestialObject celestialObject = get(world, (int) entityPlayer.posX, (int) entityPlayer.posZ);
		if (celestialObject == null) {
			return worldBorder;
		}
		return celestialObject.getWorldBorder();
	}
	
	// *** non-static methods ***
	
	private void addOrUpdateInRegistry(@Nonnull final CelestialObject celestialObject, final boolean isUpdating) {
		final CelestialObject celestialObjectExisting = celestialObjectsById.get(celestialObject.id);
		if (celestialObjectExisting == null || isUpdating) {
			celestialObjectsById.put(celestialObject.id, celestialObject);
		} else {
			WarpDrive.logger.warn(String.format("Celestial object %s is already defined, keeping original definition", celestialObject.id));
		}
	}
	
	private void rebuildAndValidate(final boolean isRemote) {
		// optimize execution speed by flattening the data structure
		final int count = celestialObjectsById.size();
		final CelestialObject[] celestialObjectsTemp = new CelestialObject[count];
		int index = 0;
		for (final CelestialObject celestialObject : celestialObjectsById.values()) {
			celestialObjectsTemp[index++] = celestialObject;
			celestialObject.resolveParent(celestialObjectsById.get(celestialObject.parentId));
		}
		
		// check overlapping regions
		int countErrors = 0;
		int countHyperspace = 0;
		int countSpace = 0;
		double maxWorldBorderTemp = 0.0D;
		for (int indexCelestialObject1 = 0; indexCelestialObject1 < count; indexCelestialObject1++) {
			final CelestialObject celestialObject1 = celestialObjectsTemp[indexCelestialObject1];
			celestialObject1.lateUpdate();
			
			// stats
			if (celestialObject1.isHyperspace()) {
				countHyperspace++;
			} else if (celestialObject1.isSpace()) {
				countSpace++;
			}
			final AxisAlignedBB worldBorderArea1 = celestialObject1.getWorldBorderArea();
			maxWorldBorderTemp = Math.max(maxWorldBorderTemp, 2 * Math.max(Math.max(Math.abs(worldBorderArea1.minX), Math.abs(worldBorderArea1.minZ)),
			                                                               Math.max(Math.abs(worldBorderArea1.maxX), Math.abs(worldBorderArea1.maxZ)) ) );
			
			// validate coordinates
			if (!celestialObject1.isVirtual()) {
				if ( celestialObject1.parent == null
				  || celestialObject1.parent.dimensionId != celestialObject1.dimensionId ) {// not hyperspace
					final CelestialObject celestialObjectParent = get(celestialObject1.parentId);
					if (celestialObjectParent == null) {
						if ( !isRemote
						  && celestialObject1.parentId != null
						  && !celestialObject1.parentId.isEmpty() ) {
							countErrors++;
							WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s refers to unknown parent %s",
							                                     countErrors,
							                                     celestialObject1.id,
							                                     celestialObject1.parentId));
						}
					} else if ( celestialObject1.parentCenterX - celestialObject1.borderRadiusX < celestialObjectParent.dimensionCenterX - celestialObjectParent.borderRadiusX 
					         || celestialObject1.parentCenterZ - celestialObject1.borderRadiusZ < celestialObjectParent.dimensionCenterZ - celestialObjectParent.borderRadiusZ
					         || celestialObject1.parentCenterX + celestialObject1.borderRadiusX > celestialObjectParent.dimensionCenterX + celestialObjectParent.borderRadiusX
					         || celestialObject1.parentCenterZ + celestialObject1.borderRadiusZ > celestialObjectParent.dimensionCenterZ + celestialObjectParent.borderRadiusZ ) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s is outside its parent border.\n%s\n%s\n%s's area in parent %s is outside %s's border %s",
						                                     countErrors,
						                                     celestialObject1.id,
						                                     celestialObject1,
						                                     celestialObjectParent,
						                                     celestialObject1.id,
						                                     celestialObject1.getAreaInParent(),
						                                     celestialObjectParent.id,
						                                     celestialObjectParent.getWorldBorderArea() ));
					}
				}
				if ( celestialObject1.dimensionCenterX - celestialObject1.borderRadiusX < -30000000
				  || celestialObject1.dimensionCenterZ - celestialObject1.borderRadiusZ < -30000000
				  || celestialObject1.dimensionCenterX + celestialObject1.borderRadiusX >= 30000000
				  || celestialObject1.dimensionCenterZ + celestialObject1.borderRadiusZ >= 30000000 ) {
					countErrors++;
					WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nCelestial object %s is outside the game border +/-30000000.\n%s\n%s border is %s",
					                                     countErrors,
					                                     celestialObject1.id,
					                                     celestialObject1,
					                                     celestialObject1.id,
					                                     celestialObject1.getWorldBorderArea() ));
				}
			}
			
			// validate against other celestial objects
			for (int indexCelestialObject2 = indexCelestialObject1 + 1; indexCelestialObject2 < count; indexCelestialObject2++) {
				final CelestialObject celestialObject2 = celestialObjectsTemp[indexCelestialObject2];
				// are they overlapping in a common parent dimension?
				if ( !celestialObject1.isHyperspace()
				  && !celestialObject2.isHyperspace()
				  && celestialObject1.parent != null
				  && celestialObject2.parent != null
				  && celestialObject1.parent.dimensionId == celestialObject2.parent.dimensionId ) {
					final AxisAlignedBB areaInParent1 = celestialObject1.getAreaInParent();
					final AxisAlignedBB areaInParent2 = celestialObject2.getAreaInParent();
					if (areaInParent1.intersects(areaInParent2)) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nOverlapping parent areas detected in dimension %d between %s and %s\nArea1 %s from %s\nArea2 %s from %s", 
						                                     countErrors, 
						                                     celestialObject1.parent.dimensionId, 
						                                     celestialObject1.id, 
						                                     celestialObject2.id,
						                                     areaInParent1,
						                                     celestialObject1,
						                                     areaInParent2,
						                                     celestialObject2 ));
					}
				}
				// are they in the same dimension?
				if ( !celestialObject1.isVirtual()
				  && !celestialObject2.isVirtual()
				  && celestialObject1.dimensionId == celestialObject2.dimensionId ) {
					final AxisAlignedBB worldBorderArea2 = celestialObject2.getWorldBorderArea();
					if (worldBorderArea1.intersects(worldBorderArea2)) {
						countErrors++;
						WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nOverlapping areas detected in dimension %d between %s and %s\nArea1 %s from %s\nArea2 %s from %s",
						                                     countErrors,
						                                     celestialObject1.dimensionId,
						                                     celestialObject1.id,
						                                     celestialObject2.id,
						                                     worldBorderArea1,
						                                     celestialObject1,
						                                     worldBorderArea2,
						                                     celestialObject2 ));
					}
				}
			}
		}
		
		if (!isRemote && countHyperspace == 0) {
			countErrors++;
			WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nAt least one hyperspace celestial object should be defined!",
			                                     countErrors ));
		} else if (!isRemote && countSpace == 0) {
			countErrors++;
			WarpDrive.logger.error(String.format("CelestialObjects validation error #%d\nAt least one space celestial object should be defined!",
			                                     countErrors ));
		}
		
		if (WarpDriveConfig.G_ENFORCE_VALID_CELESTIAL_OBJECTS) {
			if (countErrors == 1) {
				throw new RuntimeException("Invalid celestial objects definition: update your configuration to fix this validation error, search your logs for 'CelestialObjects validation error' to get more details.");
			} else if (countErrors > 0) {
				throw new RuntimeException(String.format(
						"Invalid celestial objects definition: update your configuration to fix those %d validation errors, search your logs for 'CelestialObjects validation error' to get more details.",
						countErrors));
			}
		} else {
			FMLLog.bigWarning("Invalid celestial objects definition: bad things will happen, fix those before reporting any issue!");
		}
		
		
		// We're not checking invalid dimension id, so they can be pre-allocated (see MystCraft)
		
		// delay setting the array so the render thread can rely on its content
		celestialObjects = celestialObjectsTemp;
		maxWorldBorder = maxWorldBorderTemp;
	}
	
	@Override
	protected void parseRootElement(final String location, final Element elementCelestialObject) throws InvalidXmlException {
		parseCelestiaObjectElement(location, elementCelestialObject, "");
	}
	
	private void parseCelestiaObjectElement(final String location, final Element elementCelestialObject, final String parentId) throws InvalidXmlException {
		final CelestialObject celestialObjectRead = new CelestialObject(location, parentId, elementCelestialObject);
		
		addOrUpdateInRegistry(celestialObjectRead, false);
		
		// look for optional child element(s)
		final List<Element> listChildren = XmlFileManager.getChildrenElementByTagName(elementCelestialObject, "celestialObject");
		if (!listChildren.isEmpty()) {
			for (int indexElement = 0; indexElement < listChildren.size(); indexElement++) {
				final Element elementChild = listChildren.get(indexElement);
				final String locationChild = String.format("%s Celestial object %s > child %d/%d",
				                                           location, celestialObjectRead.id, indexElement + 1, listChildren.size());
				parseCelestiaObjectElement(locationChild, elementChild, celestialObjectRead.id);
			}
		}
	}
	
	public CelestialObject get(final String id) {
		return celestialObjectsById.get(id);
	}
	
	public CelestialObject get(final int dimensionId, final int x, final int z) {
		double distanceClosest = Double.POSITIVE_INFINITY;
		CelestialObject celestialObjectClosest = null;
		for (final CelestialObject celestialObject : celestialObjects) {
			if ( celestialObject != null
			  && !celestialObject.isVirtual() 
			  && dimensionId == celestialObject.dimensionId ) {
				final double distanceSquared = celestialObject.getSquareDistanceOutsideBorder(x, z);
				if (distanceSquared <= 0) {
					return celestialObject;
				} else if (distanceClosest > distanceSquared) {
					distanceClosest = distanceSquared;
					celestialObjectClosest = celestialObject;
				}
			}
		}
		return celestialObjectClosest;
	}
	
	public double getMaxWorldBorder() {
		return maxWorldBorder < 1000 ? 6.0E7D : maxWorldBorder;
	}
}
