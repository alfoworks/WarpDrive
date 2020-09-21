package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObject.RenderData;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.render.skybox.*;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cr0s.warpdrive.world.SpaceWorldProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.IRenderHandler;

import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.opengl.GL11;

public class RenderSpaceSky extends IRenderHandler {
	
	private static RenderSpaceSky INSTANCE = null;

	public ISkyBoxRenderer currentRenderer;
	private ISkyBoxRenderer nextRenderer;

	private int anim = 0;
	private long timeSinceAnimStarted;
	
	public static RenderSpaceSky getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RenderSpaceSky();
		}
		return INSTANCE;
	}

	public void setRenderer(ISkyBoxRenderer renderer) {
		if (currentRenderer == null) {
			currentRenderer = renderer;
		} else {
			nextRenderer = renderer;

			anim = -1;
			timeSinceAnimStarted = System.currentTimeMillis();
		}
	}
	
	@Override
	public void render(final float partialTicks, @Nonnull final WorldClient world, @Nonnull final Minecraft mc) {
		final Vec3d vec3Player = mc.player.getPositionEyes(partialTicks);
		final CelestialObject celestialObject = world.provider == null ? null
				: CelestialObjectManager.get(world, (int) vec3Player.x, (int) vec3Player.z);

		final Tessellator tessellator = Tessellator.getInstance();

		GlStateManager.depthMask(false);

		// draw stars
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.disableAlpha();

		float a = 255;

		if (timeSinceAnimStarted != -1) {
			if (anim == 1) {
				a = Math.min(System.currentTimeMillis() - timeSinceAnimStarted, 255);

				if (a == 255) {
					timeSinceAnimStarted = -1;
					anim = 0;
				}
			} else if (anim == -1) {
				a = Math.max(0, 255 - (System.currentTimeMillis() - timeSinceAnimStarted));

				if (a == 0) {
					timeSinceAnimStarted = System.currentTimeMillis();
					anim = 1;
					currentRenderer = nextRenderer;
					nextRenderer = null;
				}
			}
		}

		currentRenderer.render(tessellator, mc, world, a, partialTicks);

		// enable texture with alpha blending
		GlStateManager.enableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();

		// Planets
		if (celestialObject != null && celestialObject.opacityCelestialObjects > 0.0F) {
			final Vector3 vectorPlayer = GlobalRegionManager.getUniversalCoordinates(celestialObject, vec3Player.x, vec3Player.y, vec3Player.z);
			for (final CelestialObject celestialObjectChild : CelestialObjectManager.getRenderStack()) {
				if (celestialObject == celestialObjectChild) {
					continue;
				}
				if (!celestialObject.id.equals(celestialObjectChild.parentId)) {
					continue;
				}
				renderCelestialObject(tessellator,
						celestialObjectChild,
						celestialObject.opacityCelestialObjects,
						vectorPlayer);
			}
		}

		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.enableFog();

		GlStateManager.enableTexture2D();
		GlStateManager.depthMask(true);
	}
	
	static final double PLANET_FAR = 1786.0D;
	static final double PLANET_APPROACHING = 512.0D;
	static final double PLANET_ORBIT = 128.0D;
	
	private static void renderCelestialObject(final Tessellator tessellator, final CelestialObject celestialObject,
	                                          final float alphaSky, final Vector3 vectorPlayer) {
		// @TODO compute relative coordinates for rendering on celestialObject
		
		// get universal coordinates
		final Vector3 vectorCenter = GlobalRegionManager.getUniversalCoordinates(
				celestialObject,
		        celestialObject.dimensionCenterX,
		        64,
		        celestialObject.dimensionCenterZ );
		final Vector3 vectorBorderPos = GlobalRegionManager.getUniversalCoordinates(
				celestialObject,
				celestialObject.dimensionCenterX + celestialObject.borderRadiusX,
				64,
				celestialObject.dimensionCenterZ + celestialObject.borderRadiusZ );
		if (vectorCenter == null || vectorBorderPos == null) {// probably an invalid celestial object tree
			return;
		}
		final double borderRadiusX = vectorBorderPos.x - vectorCenter.x;
		final double borderRadiusZ = vectorBorderPos.z - vectorCenter.z;
		
		// compute distances
		final double distanceToBorder;
		{
			final double dx = Math.abs(vectorPlayer.x - vectorCenter.x) - borderRadiusX;
			final double dz = Math.abs(vectorPlayer.z - vectorCenter.z) - borderRadiusZ;
			// are we in orbit?
			if ((dx <= 0.0D) && (dz <= 0.0D)) {
				distanceToBorder = 0.0D;
			} else {
				// do the maths
				final double dxOutside = Math.max(0.0D, dx);
				final double dzOutside = Math.max(0.0D, dz);
				distanceToBorder = Math.sqrt(dxOutside * dxOutside + dzOutside * dzOutside);
			}
		}
		
		final double distanceToCenterX = vectorCenter.x - vectorPlayer.x;
		final double distanceToCenterZ = vectorCenter.z - vectorPlayer.z;
		final double distanceToCenter = Math.sqrt(distanceToCenterX * distanceToCenterX + distanceToCenterZ * distanceToCenterZ);
		
		// transition values
		// distance              far   approaching  orbit
		// world border         1.000     1.000     1.000
		// PLANET_FAR           1.000     1.000     1.000
		// PLANET_APPROACHING   0.000     1.000     1.000
		// PLANET_ORBIT         0.000     0.000     1.000
		// in orbit             0.000     0.000     0.000
		final double transitionFar         = (Math.max(PLANET_APPROACHING, Math.min(PLANET_FAR, distanceToBorder)) - PLANET_APPROACHING) / (PLANET_FAR - PLANET_APPROACHING);
		final double transitionApproaching = (Math.max(PLANET_ORBIT, Math.min(PLANET_APPROACHING, distanceToBorder)) - PLANET_ORBIT) / (PLANET_APPROACHING - PLANET_ORBIT);
		final double transitionOrbit       = Math.max(0.0D, Math.min(PLANET_ORBIT, distanceToBorder)) / PLANET_ORBIT;
		
		// relative position above celestialObject
		final double offsetX = (1.0 - transitionOrbit) * (distanceToCenterX / borderRadiusX);
		final double offsetZ = (1.0 - transitionOrbit) * (distanceToCenterZ / borderRadiusZ);
		
		// simulating a non-planar universe...
		final double planetY_far = (celestialObject.dimensionId + 99 % 100 - 50) * Math.log(distanceToCenter) / 1.0D;
		final double planetY = planetY_far * transitionApproaching;
		
		// render range is only used for Z-ordering
		double renderRange = 9.0D + 0.5D * (distanceToCenter / Math.max(borderRadiusX, borderRadiusZ));
		
		// render size is 1 at space border range
		// render size is 10 at approaching range
		// render size is 90 at orbit range
		// render size is min(1000, celestialObject border) at orbit range
		final double renderSize = 5.00D / 1000.0D * Math.min(1000.0D, Math.max(borderRadiusX, borderRadiusZ)) * (1.0D - transitionOrbit)
								+ 2.50D * (transitionOrbit < 1.0D ? transitionOrbit : (1.0D - transitionApproaching))
								+ 0.25D * (transitionApproaching < 1.0D ? transitionApproaching : (1.0D - transitionFar))
								+ 0.10D * transitionFar;
		
		// angles
		final double angleH = Math.atan2(distanceToCenterX, distanceToCenterZ);
		final double angleV_far = Math.atan2(distanceToCenter, planetY);
		final double angleV = Math.PI * (1.0D - transitionOrbit) + angleV_far * transitionOrbit;
		final double angleS = 0.15D * celestialObject.dimensionId * transitionApproaching // + (world.getTotalWorldTime() + partialTicks) * Math.PI / 6000.0D;
							+ angleH * (1.0D - transitionApproaching);
		
		if ( WarpDriveConfig.LOGGING_RENDERING
		  && celestialObject.dimensionId == 1
		  && (Minecraft.getSystemTime() / 10) % 100 == 0) {
			WarpDrive.logger.info(String.format("transition Far %.2f Approaching %.2f Orbit %.2f distanceToCenter %.3f %.3f offset %.3f %.3f angle H %.3f V_far %.3f V %.3f S %.3f",
				transitionFar, transitionApproaching, transitionOrbit, distanceToCenterX, distanceToCenterZ, offsetX, offsetZ, angleH, angleV_far, angleV, angleS));
		}
		
		// pre-computations
		final double sinH = Math.sin(angleH);
		final double cosH = Math.cos(angleH);
		final double sinV = Math.sin(angleV);
		final double cosV = Math.cos(angleV);
		final double sinS = Math.sin(angleS);
		final double cosS = Math.cos(angleS);
		
		GlStateManager.pushMatrix();
		
		// GlStateManager.enableBlend();    // by caller
		final double time = Minecraft.getSystemTime() / 1000.0D;
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		for (final RenderData renderData : celestialObject.setRenderData) {
			// compute texture offsets for clouds animation 
			final float offsetU = (float) ( Math.signum(renderData.periodU) * ((time / Math.abs(renderData.periodU)) % 1.0D) );
			final float offsetV = (float) ( Math.signum(renderData.periodV) * ((time / Math.abs(renderData.periodV)) % 1.0D) );
			
			// apply rendering parameters
			if (renderData.texture != null) {
				GlStateManager.enableTexture2D();
				Minecraft.getMinecraft().getTextureManager().bindTexture(renderData.resourceLocation);
				vertexBuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			} else {
				GlStateManager.disableTexture2D();
				vertexBuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
			}
			if (renderData.isAdditive) {
				GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
			} else {
				GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			}
			
			// draw current layer
			for (int indexVertex = 0; indexVertex < 4; indexVertex++) {
				final double offset1 = ((indexVertex & 2) - 1) * renderSize;
				final double offset2 = ((indexVertex + 1 & 2) - 1) * renderSize;
				final double valV = offset1 * cosS - offset2 * sinS;
				final double valH = offset2 * cosS + offset1 * sinS;
				final double y = valV * sinV + renderRange * cosV;
				final double valD = renderRange * sinV - valV * cosV;
				final double x = valD * sinH - valH * cosH + renderSize * offsetX;
				final double z = valH * sinH + valD * cosH + renderSize * offsetZ;
				vertexBuffer.pos(x, y, z);
				if (renderData.texture != null) {
					vertexBuffer.tex((indexVertex & 2) / 2 + offsetU, (indexVertex + 1 & 2) / 2 + offsetV);
				}
				vertexBuffer.color(renderData.red, renderData.green, renderData.blue, renderData.alpha * alphaSky).endVertex();
			}
			tessellator.draw();
			
			// slight offset to get volumetric illusion
			renderRange -= 0.25D;
		}
		
		// restore settings
		GlStateManager.enableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		GlStateManager.popMatrix();
	}
}