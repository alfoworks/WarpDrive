package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumGasColor implements IStringSerializable {
	
	BLUE      ("blue"),
	RED	      ("red"),
	GREEN     ("green"),
	YELLOW    ("yellow"),
	DARK      ("dark"),
	DARKNESS  ("darkness"),
	WHITE	  ("white"),
	MILK      ("milk"),
	ORANGE    ("orange"),
	SIREN     ("siren"),
	GRAY      ("gray"),
	VIOLET    ("violet");
	
	private final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumGasColor> ID_MAP = new HashMap<>();
	
	static {
		length = EnumGasColor.values().length;
		for (final EnumGasColor gasColor : values()) {
			ID_MAP.put(gasColor.ordinal(), gasColor);
		}
	}
	
	EnumGasColor(final String unlocalizedName) {
		this.unlocalizedName = unlocalizedName;
	}
	
	public static EnumGasColor get(final int damage) {
		return ID_MAP.get(damage);
	}

	public String getTranslationKey() {
		return unlocalizedName;
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
}
