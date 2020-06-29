package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class CustomSkyBoxRenderer implements ISkyBoxRenderer {
    String displayName;
    String initialPath;

    public CustomSkyBoxRenderer(String id, String displayName) {
        this.displayName = displayName;
        this.initialPath = String.format("textures/alfosky/%s/", id);
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public void render(Tessellator tessellator, Minecraft mc, float opacity) {
        GlStateManager.disableFog();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderHelper.disableStandardItemLighting();

        BufferBuilder bufferbuilder = tessellator.getBuffer();

        for (int i = 0; i < 6; i++) {
            mc.renderEngine.bindTexture(new ResourceLocation("warpdrive", initialPath + i + ".png"));

            GlStateManager.pushMatrix();

            if (i == 0) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            } else if (i == 1) {
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            } else if (i == 2) {
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            } else if (i == 3) {
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            } else if (i == 4) {
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            } else {
                GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            }

            double quadSize = 10;

            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(-quadSize, -quadSize, -quadSize).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
            bufferbuilder.pos(-quadSize, -quadSize, quadSize).tex(0.0D, 1).color(255, 255, 255, 255).endVertex();
            bufferbuilder.pos(quadSize, -quadSize, quadSize).tex(1, 1).color(255, 255, 255, 255).endVertex();
            bufferbuilder.pos(quadSize, -quadSize, -quadSize).tex(1, 0.0D).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
    }
}
