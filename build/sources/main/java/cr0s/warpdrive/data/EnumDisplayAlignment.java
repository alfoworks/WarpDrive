package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum EnumDisplayAlignment implements IStringSerializable {
	
	TOP_LEFT          ("top_left"     , 0.0F, 0.0F),
	TOP_CENTER        ("top_center"   , 0.5F, 0.0F),
	TOP_RIGHT         ("top_right"    , 1.0F, 0.0F),
	MIDDLE_LEFT       ("middle_left"  , 0.0F, 0.5F),
	MIDDLE_CENTER     ("middle_center", 0.5F, 0.5F),
	MIDDLE_RIGHT      ("middle_right" , 1.0F, 0.5F),
	BOTTOM_LEFT       ("bottom_left"  , 0.0F, 1.0F),
	BOTTOM_CENTER     ("bottom_center", 0.5F, 1.0F),
	BOTTOM_RIGHT      ("bottom_right" , 1.0F, 1.0F);
	
	private final String name;
	public final float xRatio;
	public final float yRatio;
	
	EnumDisplayAlignment(final String name, final float xRatio, final float yRatio) {
		this.name = name;
		this.xRatio = xRatio;
		this.yRatio = yRatio;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
}
