package cr0s.warpdrive.render.skybox;

import cr0s.warpdrive.render.RenderCommons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

public class CustomSkyBoxRenderer implements ISkyBoxRenderer {
    String displayName;
    ResourceLocation[] locations;

    public CustomSkyBoxRenderer(String id, String displayName) {
        this.displayName = displayName;

        ArrayList<ResourceLocation> locations = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            locations.add(new ResourceLocation("warpdrive", String.format("textures/alfosky/%s/%s.png", id, i)));
        }

        this.locations = locations.toArray(new ResourceLocation[]{});
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public void render(Tessellator tessellator, Minecraft mc, WorldClient world, float opacity, float partialTicks) {
        RenderCommons.renderSkyBox(tessellator, locations, opacity / 255, 1);
    }
}
