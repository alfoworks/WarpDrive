package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockParticlesInjector extends BlockAcceleratorControlPoint {
	
	public BlockParticlesInjector(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, true);
		
		setTranslationKey("warpdrive.atomic.particles_injector");
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE);
	}
	
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 0x8) != 0);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 8 : 0);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityParticlesInjector();
	}
}
