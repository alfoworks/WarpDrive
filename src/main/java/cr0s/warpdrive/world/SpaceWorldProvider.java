package cr0s.warpdrive.world;

import com.google.common.collect.ImmutableList;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;

import javax.annotation.Nonnull;

import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

public class SpaceWorldProvider extends AbstractWorldProvider {
	private CelestialBody body;
	public SpaceWorldProvider() {
		super();
		
		biomeProvider = new BiomeProviderSingle(WarpDrive.biomeSpace);
		nether = false;
	}
	
	@Override
	protected void init() {
		super.init();
		
		world.setSeaLevel(0);
		final int dimension = this.getDimension();
		System.out.println("WARPDRIVE DIMENSION INFO");
		System.out.println(dimension);
		body = new CelestialBody("WarpDrive Space") {
			@Override
			public int getID() {
				return dimension;
			}

			@Override
			public String getUnlocalizedNamePrefix() {
				return "WD";
			}
		};
		body.setUnreachable();
	}
	
	@Nonnull 
	@Override
	public DimensionType getDimensionType() {
		return WarpDrive.dimensionTypeSpace;
	}
	
	@Override
	public boolean canRespawnHere() {
		return true;
	}
	
	@Override
	public boolean isSurfaceWorld() {
		return true;
	}
	
	@Override
	public int getAverageGroundLevel() {
		return 1;
	}
	
	@Override
	public double getHorizon() {
		return -256;
	}
	
	@Override
	public void updateWeather() {
		super.resetRainAndThunder();
	}
	
	@Nonnull
	@Override
	public Biome getBiomeForCoords(@Nonnull final BlockPos blockPos) {
		return WarpDrive.biomeSpace;
	}
	
	@Override
	public void setAllowedSpawnTypes(final boolean allowHostile, final boolean allowPeaceful) {
		super.setAllowedSpawnTypes(true, true);
	}
	
	@Override
	public float calculateCelestialAngle(final long time, final float partialTick) {
		// returns the clock angle: 0 is noon, 0.5 is midnight on the vanilla clock
		// daylight is required to enable IC2, NuclearCraft and EnderIO solar panels
		return 0.0F;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		final float ambient = 0.0F;
		
		for (int i = 0; i <= 15; ++i) {
			final float f1 = 1.0F - i / 15.0F;
			lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - ambient) + ambient;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean isSkyColored() {
		return false;
	}
		
	@Override
	public int getRespawnDimension(@Nonnull final EntityPlayerMP entityPlayerMP) {
		if (entityPlayerMP.world == null) {
			WarpDrive.logger.error(String.format("Invalid player passed to getRespawnDimension: %s", entityPlayerMP));
			return 0;
		}
		return CelestialObjectManager.getSpaceDimensionId(entityPlayerMP.world, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
	}
	
	@Nonnull
	@Override
	public IChunkGenerator createChunkGenerator() {
		return new SpaceChunkProvider(world, 45);
	}
	
	@Override
	public boolean canBlockFreeze(@Nonnull final BlockPos blockPos, final boolean byWater) {
		return true;
	}
	
	@Override
	public boolean isDaytime() {
		// true is required to enable GregTech solar boiler and Mekanism solar panels
		return true;
	}
	
	@Override
	public boolean canDoLightning(@Nonnull final Chunk chunk) {
		return false;
	}
	
	@Override
	public boolean canDoRainSnowIce(@Nonnull final Chunk chunk) {
		return false;
	}

	@Override
	public float getGravity() {
		return 0;
	}

	@Override
	public float getArrowGravity() {
		return 0;
	}

	@Override
	public double getMeteorFrequency() {
		return 0;
	}

	@Override
	public double getFuelUsageMultiplier() {
		return 0;
	}

	@Override
	public boolean canSpaceshipTierPass(int i) {
		return false;
	}

	@Override
	public float getFallDamageModifier() {
		return 0;
	}

	@Override
	public boolean hasNoAtmosphere() {
		return true;
	}

	@Override
	public float getSoundVolReductionAmount() {
		return 0;
	}

	@Override
	public boolean hasBreathableAtmosphere() {
		return false;
	}

	@Override
	public boolean netherPortalsOperational() {
		return false;
	}

	@Override
	public boolean isGasPresent(EnumAtmosphericGas enumAtmosphericGas) {
		return false;
	}

	@Override
	public float getThermalLevelModifier() {
		return 0;
	}

	@Override
	public float getWindLevel() {
		return 0;
	}

	@Override
	public float getSolarSize() {
		return 5.0F;
	}

	@Override
	public CelestialBody getCelestialBody() {
		// return null;
		return body;
	}

	@Override
	public boolean shouldDisablePrecipitation() {
		return false;
	}

	@Override
	public boolean shouldCorrodeArmor() {
		return false;
	}

	@Override
	public int getDungeonSpacing() {
		return 0;
	}

	@Override
	public ResourceLocation getDungeonChestType() {
		return null;
	}

	@Override
	public List<Block> getSurfaceBlocks() {
		return ImmutableList.of(Block.getBlockById(0));
	}
}