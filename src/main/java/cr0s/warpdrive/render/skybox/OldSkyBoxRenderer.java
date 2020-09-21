package cr0s.warpdrive.render.skybox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Random;

public class OldSkyBoxRenderer implements ISkyBoxRenderer {
	
	public static final int callListStars = GLAllocation.generateDisplayLists(3);
	
	{
		GL11.glPushMatrix();
		GL11.glNewList(callListStars, GL11.GL_COMPILE);
		renderStars();
		GL11.glEndList();
		GL11.glPopMatrix();
	}
	
	@Override
	public String getName() {
		return I18n.format("warpdrive.skybox.names.old");
	}

	@Override
	public String getID() {
		return "old";
	}

	@Override
	public void render(Tessellator tessellator, Minecraft mc, WorldClient world, float opacity, float partialTicks) {
		GL11.glCallList(callListStars);
	}
	
	private void renderStars() {
		final Random rand = new Random(10842L);
		final boolean hasMoreStars = rand.nextBoolean() || rand.nextBoolean();
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();

		for (int indexStars = 0; indexStars < (hasMoreStars ? 20000 : 6000); indexStars++) {
			double randomX = rand.nextDouble() * 2.0D - 1.0D;
			double randomY = rand.nextDouble() * 2.0D - 1.0D;
			double randomZ = rand.nextDouble() * 2.0D - 1.0D;
			final double lambda = 1.2D;
			final double renderSize = 0.10F + 0.03F * Math.log(1.0D - rand.nextDouble()) / (-lambda); // random.nextFloat() * 0.5F;
			double randomLength = randomX * randomX + randomY * randomY + randomZ * randomZ;
			
			if (randomLength < 1.0D && randomLength > 0.01D) {
				// forcing Z-order
				randomLength = 1.0D / Math.sqrt(randomLength);
				randomX *= randomLength;
				randomY *= randomLength;
				randomZ *= randomLength;
				
				// scaling
				final double x0 = randomX * 100.0D;
				final double y0 = randomY * 100.0D;
				final double z0 = randomZ * 100.0D;
				
				// angles
				final double angleH = Math.atan2(randomX, randomZ);
				final double angleV = Math.atan2(Math.sqrt(randomX * randomX + randomZ * randomZ), randomY);
				final double angleS = rand.nextDouble() * Math.PI * 2.0D;
				
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
					
					vertexBuffer.pos(x0 + x1, y0 + y1, z0 + z1).color(255, 255, 255, 255).endVertex();
				}
				tessellator.draw();
			}
		}
	}
	
	@Nonnull
	@Override
	public ResourceLocation getPreview() {
		return new ResourceLocation("warpdrive", "textures/celestial/skybox_preview_old.png");
	}
}
