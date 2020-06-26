package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumDisplayAlignment;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCommons {
	
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	private static final int TEXT_BORDER = 2;
	private static final float SCALE_UV = 0.00390625F;  // 1/256
	
	protected static int colorGradient(final float gradient, final int start, final int end) {
		return Math.max(0, Math.min(255, start + Math.round(gradient * (end - start))));
	}
	
	// from net.minecraft.client.gui.Gui
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v,
	                                            final int sizeX, final int sizeY, final int zLevel) {
		drawTexturedModalRect(x, y, u, v, sizeX, sizeY, zLevel, 1.0F, 1.0F, 1.0F, 1.0F);
	}
	protected static void drawTexturedModalRect(final int x, final int y, final int u, final int v,
	                                            final int sizeX, final int sizeY, final int zLevel,
	                                            final float red, final float green, final float blue, final float alpha) {
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		vertexBuffer.pos( x         , (y + sizeY), zLevel).tex(SCALE_UV * u          , SCALE_UV * (v + sizeY)).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos((x + sizeX), (y + sizeY), zLevel).tex(SCALE_UV * (u + sizeX), SCALE_UV * (v + sizeY)).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos((x + sizeX),  y         , zLevel).tex(SCALE_UV * (u + sizeX), SCALE_UV * v).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos( x         ,  y         , zLevel).tex(SCALE_UV * u          , SCALE_UV * v).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
	}
	
	public static int drawSplashAlarm(final int scaledWidth, final int scaledHeight, final String title, final String message) {
		// compute animation clock
		final double cycle = ((System.nanoTime() / 1000L) % 0x200000) / (double) 0x200000;
		
		// start rendering
		GlStateManager.pushMatrix();
		GlStateManager.scale(2.0F, 2.0F, 0.0F);
		
		int y = scaledHeight / 10;
		
		// bold title, single line, centered, with shadows
		final String textTitle = Commons.updateEscapeCodes("§l" + new TextComponentTranslation(title).getFormattedText());
		minecraft.fontRenderer.drawString(textTitle,
		                                  scaledWidth / 4.0F - minecraft.fontRenderer.getStringWidth(textTitle) / 2.0F,
		                                  y - minecraft.fontRenderer.FONT_HEIGHT,
		                                     Commons.colorARGBtoInt(230, 255, 32, 24),
		                                  true);
		
		// normal message, multi-lines, centered, without shadows
		final String textMessage = Commons.updateEscapeCodes(new TextComponentTranslation(message).getFormattedText());
		final int alpha = 160 + (int) (85.0D * Math.sin(cycle * 2 * Math.PI));
		
		final List<String> listMessages = minecraft.fontRenderer.listFormattedStringToWidth(textMessage, scaledWidth / 2);
		for (final String textLine : listMessages) {
			minecraft.fontRenderer.drawString(textLine,
			                                  scaledWidth / 4.0F - minecraft.fontRenderer.getStringWidth(textLine) / 2.0F,
			                                  y,
			                                  Commons.colorARGBtoInt(alpha, 192, 64, 48),
			                                  false);
			y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		GlStateManager.popMatrix();
		return alpha;
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String text,
	                           final float scale, final String formatPrefix, final int colorBackground, final int colorText, final boolean hasShadow,
	                           final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                           final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String text_formatted = Commons.updateEscapeCodes(formatPrefix + new TextComponentTranslation(text).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		final List<String> listLines = minecraft.fontRenderer.listFormattedStringToWidth(text_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final int scaled_box_height = listLines.size() * minecraft.fontRenderer.FONT_HEIGHT + 2 * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		final float red   = (colorBackground >> 16 & 255) / 255.0F;
		final float blue  = (colorBackground >> 8  & 255) / 255.0F;
		final float green = (colorBackground       & 255) / 255.0F;
		final float alpha = (colorBackground >> 24 & 255) / 255.0F;
		
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		
		// draw text
		for (final String textLine : listLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasShadow);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		GlStateManager.popMatrix();
	}
	
	public static void drawText(final int screen_width, final int screen_height, final String textHeader, final String textContent,
	                            final float scale, final String prefixHeader, final int colorBackground, final int colorText, final boolean hasHeaderShadow,
	                            @Nonnull final EnumDisplayAlignment enumScreenAnchor, final int xOffset, final int yOffset,
	                            @Nonnull final EnumDisplayAlignment enumTextAlignment, final float widthTextRatio, final int widthTextMin) {
		// prepare the string box content and dimensions
		final String header_formatted  = Commons.updateEscapeCodes(prefixHeader + new TextComponentTranslation(textHeader).getFormattedText());
		final String content_formatted = Commons.updateEscapeCodes(new TextComponentTranslation(textContent).getFormattedText());
		final int scaled_box_width = Math.max(widthTextMin, Math.round(widthTextRatio * screen_width)) + 2 * TEXT_BORDER;
		
		final List<String> listHeaderLines = 
			header_formatted.isEmpty() ? new ArrayList<>(0)
			                           : minecraft.fontRenderer.listFormattedStringToWidth(header_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final List<String> listContentLines =
			content_formatted.isEmpty() ? new ArrayList<>(0)
		                                : minecraft.fontRenderer.listFormattedStringToWidth(content_formatted, scaled_box_width - 2 * TEXT_BORDER);
		final boolean hasTileAndContent = listHeaderLines.size() > 0 && listContentLines.size() > 0;
		final int scaled_box_height = (listHeaderLines.size() + listContentLines.size()) * minecraft.fontRenderer.FONT_HEIGHT
		                            + (hasTileAndContent ? 3 : 1) * TEXT_BORDER;
		
		// compute the position
		final int screen_text_x = Math.round(screen_width  * enumScreenAnchor.xRatio + xOffset - enumTextAlignment.xRatio * scaled_box_width  * scale);
		final int screen_text_y = Math.round(screen_height * enumScreenAnchor.yRatio + yOffset - enumTextAlignment.yRatio * scaled_box_height * scale);
		
		// start rendering
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, scale, 0.0F);
		final int scaled_box_x  = Math.round(screen_text_x / scale - TEXT_BORDER);
		final int scaled_box_y  = Math.round(screen_text_y / scale - TEXT_BORDER);
		final int scaled_text_x = Math.round(screen_text_x / scale);
		int scaled_text_y       = Math.round(screen_text_y / scale);
		
		// draw background box
		GlStateManager.disableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		
		final float red   = (colorBackground >> 16 & 0xFF) / 255.0F;
		final float green = (colorBackground >> 8  & 0xFF) / 255.0F;
		final float blue  = (colorBackground       & 0xFF) / 255.0F;
		final float alpha = (colorBackground >> 24 & 0xFF) / 255.0F;
		
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y + scaled_box_height, -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x + scaled_box_width, scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		vertexBuffer.pos(scaled_box_x                   , scaled_box_y                    , -90.0D).color(red, green, blue, alpha).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		
		// draw text
		for (final String textLine : listHeaderLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, hasHeaderShadow);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		if (hasTileAndContent) {
			scaled_text_y += TEXT_BORDER;
		}
		for (final String textLine : listContentLines) {
			minecraft.fontRenderer.drawString(textLine, scaled_text_x, scaled_text_y, colorText, false);
			scaled_text_y += minecraft.fontRenderer.FONT_HEIGHT;
		}
		
		// close rendering
		// GlStateManager.enableTexture2D();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.popMatrix();
	}
	
	public static IModel getModel(final ResourceLocation resourceLocation) {
		final IModel model;
		try {
			model = ModelLoaderRegistry.getModel(resourceLocation);
		} catch (final Exception exception) {
			WarpDrive.logger.info(String.format("getModel %s", resourceLocation));
			throw new RuntimeException(exception);
		}
		return model;
	}
	
	public static void renderModelTESR(@Nonnull final List<BakedQuad> quads, @Nonnull final BufferBuilder renderer, final int brightness) {
		final int l1 = (brightness >> 0x10) & 0xFFFF;
		final int l2 = brightness & 0xFFFF;
		for (final BakedQuad quad : quads) {
			final int[] vData = quad.getVertexData();
			final VertexFormat format = quad.getFormat();
			final int size = format.getIntegerSize();
			final int uv = format.getUvOffsetById(0) / 4;
			// final int color = format.getColorOffset();
			for (int i = 0; i < 4; ++i) {
				renderer
						.pos(	Float.intBitsToFloat(vData[size * i    ]),
						        Float.intBitsToFloat(vData[size * i + 1]),
						        Float.intBitsToFloat(vData[size * i + 2]) )
						.color(255, 255, 255, 255)
						.tex(Float.intBitsToFloat(vData[size * i + uv]), Float.intBitsToFloat(vData[size * i + uv + 1]))
						.lightmap(l1, l2)
						.endVertex();
			}
		}
	}
}