package cr0s.warpdrive.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name(value = "WarpDriveCore") // The readable mod name
@IFMLLoadingPlugin.MCVersion(value = "1.12.2")
@IFMLLoadingPlugin.TransformerExclusions(value = "cr0s.warpdrive.core.")
@IFMLLoadingPlugin.SortingIndex(value = 1001) // > 1000 to work with srg names
public class FMLLoadingPlugin implements IFMLLoadingPlugin, IFMLCallHook {
	
	public static File location;
	public static final Logger logger = LogManager.getLogger("WarpDriveCore");
	private boolean debugLog = false;
	
	@Override
	public String[] getASMTransformerClass() {
		return new String[] { ClassTransformer.class.getName() };
	}
	
	@Override
	public String getModContainerClass() {
		return MyDummyModContainer.class.getName();
	}
	
	@Override
	public String getSetupClass() {
		return getClass().getName();
	}
	
	@Override
	public void injectData(final Map<String, Object> data) {
		location = (File) data.get("coremodLocation");
		if (debugLog) {
			logger.info("injectData:");
			for (final Entry<String, Object> entry : data.entrySet()) {
				logger.info(String.format("- %s = %s",
				                          entry.getKey(), entry.getValue()));
			}
		}
	}
	
	@Override
	public Void call() {
		if (debugLog) { logger.info("call()"); }
		return null;
	}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}