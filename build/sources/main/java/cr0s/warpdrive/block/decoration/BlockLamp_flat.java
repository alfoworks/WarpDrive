package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockLamp_flat extends BlockAbstractLamp {
	
	private static final AxisAlignedBB AABB_DOWN  = new AxisAlignedBB(0.00D, 0.84D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_UP    = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 0.16D, 1.00D);
	private static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.00D, 0.00D, 0.84D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 1.00D, 0.16D);
	private static final AxisAlignedBB AABB_WEST  = new AxisAlignedBB(0.84D, 0.00D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_EAST  = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 0.16D, 1.00D, 1.00D);
	
	public BlockLamp_flat(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, "warpdrive.decoration.lamp_flat");
	}
	
	@Nonnull
	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		switch (blockState.getValue(BlockProperties.FACING)) {
			case DOWN : return AABB_DOWN ;
			case UP   : return AABB_UP   ;
			case NORTH: return AABB_NORTH;
			case SOUTH: return AABB_SOUTH;
			case WEST : return AABB_WEST ;
			case EAST : return AABB_EAST ;
			default   : return AABB_UP;
		}
	}
}