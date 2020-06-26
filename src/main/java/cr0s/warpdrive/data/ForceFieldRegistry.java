package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldRelay;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Thread safe registry of all known force field blocks, grouped by frequency, for use in main and calculation threads
 * 
 */
public class ForceFieldRegistry {
	
	private static final TIntObjectHashMap<CopyOnWriteArraySet<RegistryEntry>> registry = new TIntObjectHashMap<>(16);
	private static int countAdd = 0;
	private static int countRemove = 0;
	private static int countRead = 0;
	
	private static final class RegistryEntry {
		public final GlobalPosition globalPosition;
		public final boolean isRelay;
		
		RegistryEntry(@Nonnull final GlobalPosition globalPosition, final boolean isRelay) {
			this.globalPosition = globalPosition;
			this.isRelay = isRelay;
		}
		
		RegistryEntry(@Nonnull final TileEntity tileEntity) {
			this(new GlobalPosition(tileEntity), tileEntity instanceof TileEntityForceFieldRelay);
		}
		
		@Override
		public boolean equals(final Object object) {
			if (this == object) {
				return true;
			}
			if (object == null) {
				return false;
			}
			if (object instanceof TileEntity) {
				final TileEntity tileEntity = (TileEntity) object;
				return globalPosition.equals(tileEntity)
				    && isRelay == (tileEntity instanceof TileEntityForceFieldRelay);
			}
			if (getClass() != object.getClass()) {
				return false;
			}
			final RegistryEntry that = (RegistryEntry) object;
			return isRelay == that.isRelay
			    && globalPosition.equals(that.globalPosition);
		}
		
		@Override
		public int hashCode() {
			return globalPosition.hashCode();
		}
	}
	
	@Nonnull
	public static Set<TileEntity> getTileEntities(final int beamFrequency, @Nullable final WorldServer world, @Nonnull final BlockPos blockPos) {
		countRead++;
		if (WarpDriveConfig.LOGGING_FORCE_FIELD_REGISTRY) {
			if (countRead % 1000 == 0) {
				WarpDrive.logger.info(String.format("ForceFieldRegistry stats: read %d add %d remove %d => %.1f",
				                                    countRead, countAdd, countRemove, ((float) countRead) / (countRemove + countRead + countAdd)));
			}
		}
		
		// sanity checks
		if (world == null) {
			WarpDrive.logger.warn(String.format("ForceFieldRegistry:getTileEntities called with no world for beam frequency %d %s",
			                                    beamFrequency, Commons.format(null, blockPos) ));
			return new CopyOnWriteArraySet<>();
		}
		
		final CopyOnWriteArraySet<RegistryEntry> setRegistryEntries = registry.get(beamFrequency);
		if (setRegistryEntries == null) {
			return new CopyOnWriteArraySet<>();
		}
		
		// find all relevant tiles by world and frequency
		// we delay calls to getTileEntity so we only load the required ones 
		int range2;
		final int maxRange2 = ForceFieldSetup.FORCEFIELD_RELAY_RANGE * ForceFieldSetup.FORCEFIELD_RELAY_RANGE;
		
		// first loop is to keep the relays in range, and all the potentials in the same dimension
		
		// we keep relays in range as starting point
		final Set<RegistryEntry> setRegistryEntryNonRelays = new HashSet<>();
		final Set<RegistryEntry> setRegistryEntryRelays = new HashSet<>();
		Set<RegistryEntry> setRegistryEntryToIterate = new HashSet<>();
		for (final RegistryEntry registryEntry : setRegistryEntries) {
			// skip if it's in another dimension
			if (registryEntry.globalPosition.dimensionId != world.provider.getDimension()) {
				continue;
			}
			
			if (registryEntry.isRelay) {
				range2 = (registryEntry.globalPosition.x - blockPos.getX()) * (registryEntry.globalPosition.x - blockPos.getX())
				       + (registryEntry.globalPosition.y - blockPos.getY()) * (registryEntry.globalPosition.y - blockPos.getY())
				       + (registryEntry.globalPosition.z - blockPos.getZ()) * (registryEntry.globalPosition.z - blockPos.getZ());
				if (range2 <= maxRange2) {
					// remember relay entry in range
					setRegistryEntryToIterate.add(registryEntry);
				} else {
					// remember relay entry in the same world
					setRegistryEntryRelays.add(registryEntry);
				}
			} else {
				// remember non-relay entry in the same world
				setRegistryEntryNonRelays.add(registryEntry);
			}
		}
		
		// if no relay was found, we just return the block given initially
		if (setRegistryEntryToIterate.isEmpty()) {
			final Set<TileEntity> setResult = new HashSet<>(1);
			setResult.add(world.getTileEntity(blockPos));
			return setResult;
		}
		
		// find all relays in that network
		Set<RegistryEntry>       setRegistryEntryToIterateNext;
		final Set<RegistryEntry> setRegistryEntryRelaysInRange = new HashSet<>();
		final Set<TileEntity>    setTileEntityRelaysInRange    = new HashSet<>();
		while(!setRegistryEntryToIterate.isEmpty()) {
			setRegistryEntryToIterateNext = new HashSet<>();
			for (final RegistryEntry registryEntryCurrent : setRegistryEntryToIterate) {
				
				// get tile entity and validate beam frequency
				final TileEntity tileEntityCurrent = world.getTileEntity(registryEntryCurrent.globalPosition.getBlockPos());
				if ( (!(tileEntityCurrent instanceof IBeamFrequency))
				  || ((IBeamFrequency) tileEntityCurrent).getBeamFrequency() != beamFrequency
				  || !(tileEntityCurrent instanceof TileEntityForceFieldRelay) ) {
					// block no longer exist => remove from registry
					WarpDrive.logger.info(String.format("Removing invalid ForceFieldRegistry relay entry for beam frequency %d %s: %s",
					                                    beamFrequency,
					                                    Commons.format(world, registryEntryCurrent.globalPosition.getBlockPos()),
					                                    tileEntityCurrent ));
					countRemove++;
					setRegistryEntries.remove(registryEntryCurrent);
					if (WarpDriveConfig.LOGGING_FORCE_FIELD_REGISTRY) {
						printRegistry("removed");
					}
					continue;
				}
				
				// save a validated relay
				setRegistryEntryRelaysInRange.add(registryEntryCurrent);
				setTileEntityRelaysInRange.add(tileEntityCurrent);
				
				// find all relays in range
				for (final RegistryEntry registryEntryRelay : setRegistryEntryRelays) {
					
					if ( !setRegistryEntryRelaysInRange.contains(registryEntryRelay)
					  && !setRegistryEntryToIterate.contains(registryEntryRelay)
					  && !setRegistryEntryToIterateNext.contains(registryEntryRelay) ) {
						range2 = (tileEntityCurrent.getPos().getX() - registryEntryRelay.globalPosition.x) * (tileEntityCurrent.getPos().getX() - registryEntryRelay.globalPosition.x)
						       + (tileEntityCurrent.getPos().getY() - registryEntryRelay.globalPosition.y) * (tileEntityCurrent.getPos().getY() - registryEntryRelay.globalPosition.y)
						       + (tileEntityCurrent.getPos().getZ() - registryEntryRelay.globalPosition.z) * (tileEntityCurrent.getPos().getZ() - registryEntryRelay.globalPosition.z);
						if (range2 <= maxRange2) {
							// add a relay entry in range
							setRegistryEntryToIterateNext.add(registryEntryRelay);
						}
					}
				}
			}
			
			setRegistryEntryToIterate = setRegistryEntryToIterateNext;
		}
		
		// find all projectors in range of that network
		final Set<RegistryEntry> setRegistryEntryResults = new HashSet<>(setTileEntityRelaysInRange.size() + 5);
		final Set<TileEntity> setTileEntityResults = new HashSet<>(setTileEntityRelaysInRange.size() + 5);
		for (final TileEntity tileEntityRelayInRange : setTileEntityRelaysInRange) {
			for (final RegistryEntry registryEntryNonRelay : setRegistryEntryNonRelays) {
				if (!setRegistryEntryResults.contains(registryEntryNonRelay)) {
					range2 = (tileEntityRelayInRange.getPos().getX() - registryEntryNonRelay.globalPosition.x) * (tileEntityRelayInRange.getPos().getX() - registryEntryNonRelay.globalPosition.x)
					       + (tileEntityRelayInRange.getPos().getY() - registryEntryNonRelay.globalPosition.y) * (tileEntityRelayInRange.getPos().getY() - registryEntryNonRelay.globalPosition.y)
					       + (tileEntityRelayInRange.getPos().getZ() - registryEntryNonRelay.globalPosition.z) * (tileEntityRelayInRange.getPos().getZ() - registryEntryNonRelay.globalPosition.z);
					if (range2 <= maxRange2) {
						
						// get tile entity and validate beam frequency
						final TileEntity tileEntity = world.getTileEntity(registryEntryNonRelay.globalPosition.getBlockPos());
						if ( (tileEntity instanceof IBeamFrequency)
						  && ((IBeamFrequency) tileEntity).getBeamFrequency() == beamFrequency ) {
							// add a non-relay in range
							setRegistryEntryResults.add(registryEntryNonRelay);
							setTileEntityResults.add(tileEntity);
						} else {
							// block no longer exist => remove from registry
							WarpDrive.logger.info(String.format("Removing invalid ForceFieldRegistry non-relay entry for beam frequency %d %s: %s",
							                                    beamFrequency,
							                                    Commons.format(world, registryEntryNonRelay.globalPosition.getBlockPos()),
							                                    tileEntity ));
							countRemove++;
							setRegistryEntries.remove(registryEntryNonRelay);
							if (WarpDriveConfig.LOGGING_FORCE_FIELD_REGISTRY) {
								printRegistry("removed");
							}
						}
					}
				}
			}
		}
		
		setTileEntityResults.addAll(setTileEntityRelaysInRange);
		return setTileEntityResults;
	}
	
	public static void updateInRegistry(@Nonnull final IBeamFrequency tileEntity) {
		assert tileEntity instanceof TileEntity;
		
		countRead++;
		CopyOnWriteArraySet<RegistryEntry> setRegistryEntries = registry.get(tileEntity.getBeamFrequency());
		if (setRegistryEntries == null) {
			setRegistryEntries = new CopyOnWriteArraySet<>();
		}
		for (final RegistryEntry registryEntry : setRegistryEntries) {
			if (registryEntry.equals(tileEntity)) {
				// already registered
				return;
			}
		}
		// not found => add
		countAdd++;
		setRegistryEntries.add(new RegistryEntry((TileEntity) tileEntity));
		registry.put(tileEntity.getBeamFrequency(), setRegistryEntries);
		if (WarpDriveConfig.LOGGING_FORCE_FIELD_REGISTRY) {
			printRegistry("added");
		}
	}
	
	public static void removeFromRegistry(@Nonnull final IBeamFrequency tileEntity) {
		assert tileEntity instanceof TileEntity;
		
		countRead++;
		final CopyOnWriteArraySet<RegistryEntry> setRegistryEntries = registry.get(tileEntity.getBeamFrequency());
		if (setRegistryEntries == null) {
			// noting to remove
			return;
		}
		for (final RegistryEntry registryEntry : setRegistryEntries) {
			if (registryEntry.equals(tileEntity)) {
				// found it, remove and exit
				countRemove++;
				setRegistryEntries.remove(registryEntry);
				return;
			}
		}
		// not found => ignore it
	}
	
	public static void printRegistry(final String trigger) {
		WarpDrive.logger.info(String.format("Force field registry (%d entries after %s):",
		                                    registry.size(), trigger ));
		
		registry.forEachEntry((beamFrequency, relayOrProjectors) -> {
			final StringBuilder message = new StringBuilder();
			for (final RegistryEntry registryEntry : relayOrProjectors) {
				if (message.length() > 0) {
					message.append(", ");
				}
				message.append(Commons.format(registryEntry.globalPosition));
			}
			WarpDrive.logger.info(String.format("- %d entries at beam frequency %d : %s",
			                                    relayOrProjectors.size(),
			                                    beamFrequency,
			                                    message ));
			return true;
		});
	}
}
