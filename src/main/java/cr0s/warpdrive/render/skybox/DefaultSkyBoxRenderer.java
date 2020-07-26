package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Random;

public class DefaultSkyBoxRenderer implements ISkyBoxRenderer {

    private static final float ALPHA_TOLERANCE = 1.0F / 256.0F;
    private static float starBrightness = 0.0F;
    private static final int callListRoot = GLAllocation.generateDisplayLists(3);
    private static final int callListStars = callListRoot;

    @Override
    public String getName() {
        return "WarpDrive Default";
    }

    @Override
    public void render(Tessellator tessellator, Minecraft mc, WorldClient world, float opacity, float partialTicks) {
        final float alphaBase = 1.0F; // - world.getRainStrength(partialTicks);

        // draw stars
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableAlpha();
        float starBrightness = 0.2F;
        if (world.provider != null) {
            starBrightness = world.getStarBrightness(partialTicks);
        }
        if (starBrightness > 0.0F) {
            renderStars_cached((float) opacity / 255);
        }
    }
    
    @Override
    public ResourceLocation getPreview() {
        return new ResourceLocation("");
    }
    
    private static int getStarColorRGB(@Nonnull final Random rand) {
        final double colorType = rand.nextDouble();
        final float hue;
        final float saturation;
        float brightness = 1.0F - 0.8F * rand.nextFloat();  // distance effect

        if (colorType <= 0.08D) {// 8% light blue (young star)
            hue = 0.48F + 0.08F * rand.nextFloat();
            saturation = 0.18F + 0.22F * rand.nextFloat();

        } else if (colorType <= 0.24D) {// 22% pure white (early age)
            hue = 0.126F + 0.040F * rand.nextFloat();
            saturation = 0.00F + 0.15F * rand.nextFloat();
            brightness *= 0.95F;

        } else if (colorType <= 0.45D) {// 21% yellow white
            hue = 0.126F + 0.040F * rand.nextFloat();
            saturation = 0.15F + 0.15F * rand.nextFloat();
            brightness *= 0.90F;

        } else if (colorType <= 0.67D) {// 22% yellow
            hue = 0.126F + 0.040F * rand.nextFloat();
            saturation = 0.80F + 0.15F * rand.nextFloat();
            if (rand.nextInt(3) == 1) {// yellow giant
                brightness *= 0.90F;
            } else {
                brightness *= 0.85F;
            }

        } else if (colorType <= 0.92D) {// 25% orange
            hue = 0.055F + 0.055F * rand.nextFloat();
            saturation = 0.85F + 0.15F * rand.nextFloat();
            if (rand.nextInt(3) == 1) {// (orange giant)
                brightness *= 0.90F;
            } else {
                brightness *= 0.80F;
            }

        } else {// red (mostly giants)
            hue = 0.95F + 0.05F * rand.nextFloat();
            if (rand.nextInt(3) == 1) {// (red giant)
                saturation = 0.80F + 0.20F * rand.nextFloat();
                brightness *= 0.95F;
            } else {
                saturation = 0.70F + 0.20F * rand.nextFloat();
                brightness *= 0.65F;
            }
        }
        return Color.HSBtoRGB(hue, saturation, brightness);
    }


    private void renderStars_direct(final float brightness) {
        final Random rand = new Random(10842L);
        final boolean hasMoreStars = rand.nextBoolean() || rand.nextBoolean();
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder vertexBuffer = tessellator.getBuffer();

        final double renderRangeMax = 10.0D;
        for (int indexStars = 0; indexStars < (hasMoreStars ? 20000 : 2000); indexStars++) {
            double randomX;
            double randomY;
            double randomZ;
            double randomLength;
            do {
                randomX = rand.nextDouble() * 2.0D - 1.0D;
                randomY = rand.nextDouble() * 2.0D - 1.0D;
                randomZ = rand.nextDouble() * 2.0D - 1.0D;
                randomLength = randomX * randomX + randomY * randomY + randomZ * randomZ;
            } while (randomLength >= 1.0D || randomLength <= 0.90D);

            final double renderSize = 0.020F + 0.0025F * Math.log(1.1D - rand.nextDouble());

            // forcing Z-order
            randomLength = 1.0D / Math.sqrt(randomLength);
            randomX *= randomLength;
            randomY *= randomLength;
            randomZ *= randomLength;

            // scaling
            final double x0 = randomX * renderRangeMax;
            final double y0 = randomY * renderRangeMax;
            final double z0 = randomZ * renderRangeMax;

            // angles
            final double angleH = Math.atan2(randomX, randomZ);
            final double angleV = Math.atan2(Math.sqrt(randomX * randomX + randomZ * randomZ), randomY);
            final double angleS = rand.nextDouble() * Math.PI * 2.0D;

            // colorization
            final int rgb = getStarColorRGB(rand);
            final float fRed   = brightness * ((rgb >> 16) & 0xFF) / 255.0F;
            final float fGreen = brightness * ((rgb >> 8) & 0xFF) / 255.0F;
            final float fBlue  = brightness * (rgb & 0xFF) / 255.0F;
            final float fAlpha = 1.0F;

            // pre-computations
            final double sinH = Math.sin(angleH);
            final double cosH = Math.cos(angleH);
            final double sinV = Math.sin(angleV);
            final double cosV = Math.cos(angleV);
            final double sinS = Math.sin(angleS);
            final double cosS = Math.cos(angleS);

            vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int indexVertex = 0; indexVertex < 4; indexVertex++) {
                final double valZero = 0.0D;
                final double offset1 = ((indexVertex     & 2) - 1) * renderSize;
                final double offset2 = ((indexVertex + 1 & 2) - 1) * renderSize;
                final double valV = offset1 * cosS - offset2 * sinS;
                final double valH = offset2 * cosS + offset1 * sinS;
                final double y1 = valV * sinV + valZero * cosV;
                final double valD = valZero * sinV - valV * cosV;
                final double x1 = valD * sinH - valH * cosH;
                final double z1 = valH * sinH + valD * cosH;
                vertexBuffer.pos(x0 + x1, y0 + y1, z0 + z1).color(fRed, fGreen, fBlue, fAlpha).endVertex();
            }
            tessellator.draw();
        }

    }

    private void renderStars_cached(final float brightness) {
        if (Math.abs(starBrightness - brightness) > ALPHA_TOLERANCE) {
            starBrightness = brightness;
            GlStateManager.pushMatrix();
            GlStateManager.glNewList(callListStars, GL11.GL_COMPILE);
            renderStars_direct(brightness);
            GlStateManager.glEndList();
            GlStateManager.popMatrix();
        }
        GlStateManager.callList(callListStars);
    }
}
