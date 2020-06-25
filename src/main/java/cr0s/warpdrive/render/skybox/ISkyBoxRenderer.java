package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;

public interface ISkyBoxRenderer {
    public String getName();

    public void render(Tessellator tessellator, Minecraft mc, float opacity);
}
