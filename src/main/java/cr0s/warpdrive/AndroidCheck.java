package cr0s.warpdrive;

import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import net.minecraft.entity.player.EntityPlayer;

public class AndroidCheckHelper {
	public static boolean isAndroid(EntityPlayer player) {
		return MOPlayerCapabilityProvider.GetAndroidCapability(player) != null && MOPlayerCapabilityProvider.GetAndroidCapability(player).isAndroid();
	}
}
