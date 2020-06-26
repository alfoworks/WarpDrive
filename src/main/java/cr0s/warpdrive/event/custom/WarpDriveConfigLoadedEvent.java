package cr0s.warpdrive.event.custom;

import net.minecraftforge.fml.common.eventhandler.Event;

/*
Создан для интеграции с AFMSpaceUnionMod.
 */
public class WarpDriveConfigLoadedEvent extends Event {

    @Override
    public boolean isCancelable() {
        return false;
    }
}
