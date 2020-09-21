package cr0s.warpdrive.command;

import cr0s.warpdrive.client.SkyBoxManager;
import cr0s.warpdrive.render.RenderSpaceSky;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.IClientCommand;

public class ClientCommandSkyBox extends CommandBase implements IClientCommand {
    @Override
    public String getName() {
        return "skybox";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/skybox <id>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            return;
        }

        if (!SkyBoxManager.skyboxes.containsKey(args[0])) {
            sender.sendMessage(new TextComponentString("Invalid ID!"));

            return;
        }

        SkyBoxManager.setSkyBox(args[0]);
        SkyBoxManager.saveSelectedSkyBox(args[0]);

        sender.sendMessage(new TextComponentString("Skybox is now " + SkyBoxManager.skyboxes.get(args[0]).getName()));
    }

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
        return false;
    }
}
