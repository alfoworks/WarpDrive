package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

public interface ISkyBoxRenderer {
    public String getName();

    public void render(Tessellator tessellator, Minecraft mc, WorldClient world, float opacity, float partialTicks);
    
    public ResourceLocation getPreview();
}
