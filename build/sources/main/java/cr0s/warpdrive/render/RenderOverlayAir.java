package cr0s.warpdrive.render;

import cr0s.warpdrive.config.WarpDriveConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class RenderOverlayAir {
	@SubscribeEvent
	public void onRender(@Nonnull final RenderGameOverlayEvent.Pre event) {

	}
	
	@SubscribeEvent
	public void onRender(@Nonnull final RenderGameOverlayEvent.Post event) {

	}
}