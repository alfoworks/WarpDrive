package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public interface ISkyBoxRenderer {
    public String getName();

    public String getID();

    public void render(Tessellator tessellator, Minecraft mc, WorldClient world, float opacity, float partialTicks);
    
    @Nonnull
    public ResourceLocation getPreview();
}
