package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.render.RenderSpaceSky;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

public class A extends AbstractCommand {

    @Nonnull
    @Override
    public String getName() {
        return "a";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull final ICommandSender commandSender) {
        return "/a";
    }

    @Override
    public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] params) {
        if (params.length < 1) {
            return;
        }

        if (params[0].equalsIgnoreCase("c")) {
            RenderSpaceSky.crap.clear();
            return;
        }

        int side = Integer.parseInt(params[0]);

        RenderSpaceSky.crap.add(side);
    }
}