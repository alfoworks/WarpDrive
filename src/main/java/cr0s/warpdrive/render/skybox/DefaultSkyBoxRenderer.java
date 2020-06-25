package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;

public class DefaultSkyBoxRenderer implements ISkyBoxRenderer {
    @Override
    public String getName() {
        return "WarpDrive Default";
    }

    @Override
    public void render(Tessellator tessellator, Minecraft mc, float opacity) {

    }
}
