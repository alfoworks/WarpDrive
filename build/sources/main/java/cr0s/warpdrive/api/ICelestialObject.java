package cr0s.warpdrive.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

public interface ICelestialObject {
	
	// also known as unique id
	String getName();
	
	String getDisplayName();
	
	String getDescription();
	
	NBTTagCompound getTag();
	
	boolean isVirtual();
	
	boolean isHyperspace();
	
	boolean isSpace();
	
	double getGravity();
	
	boolean hasAtmosphere();
	
	AxisAlignedBB getWorldBorderArea();
	
	AxisAlignedBB getAreaToReachParent();
	
	AxisAlignedBB getAreaInParent();
	
	/**
	 * Verify that the given area is fully contained within the border.
	 * It's up to caller to verify if this celestial object is matched.
	 *
	 * @param aabb bounding box that should fit within border
	 * @return true if we're fully inside the border
	 */
	boolean isInsideBorder(final AxisAlignedBB aabb);
	
	/**
	 * Verify that the given position is within the border.
	 * It's up to caller to verify if this celestial object is matched.
	 *
	 * @param x coordinates inside the celestial object
	 * @param z coordinates inside the celestial object
	 * @return true if we're fully inside the border
	 */
	boolean isInsideBorder(final double x, final double z);
	
	/**
	 * Check if given position is in this object orbit.
	 *
	 * @param dimensionId current position in parent dimension
	 * @param x current position in parent dimension
	 * @param z current position in parent dimension
	 * @return true if we're in orbit of the object
	 */
	boolean isInOrbit(final int dimensionId, final double x, final double z);
}
