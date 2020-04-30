package cr0s.warpdrive.compat;

import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import net.minecraft.entity.player.EntityPlayer;

public class CompatMatterOverdrive {
	public static boolean isAndroid(EntityPlayer player) {
		AndroidPlayer playerCapability = MOPlayerCapabilityProvider.GetAndroidCapability(player);
		
		return playerCapability != null && playerCapability.isAndroid();
	}
}
