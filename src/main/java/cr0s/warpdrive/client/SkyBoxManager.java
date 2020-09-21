package cr0s.warpdrive.client;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.render.RenderSpaceSky;
import cr0s.warpdrive.render.skybox.ISkyBoxRenderer;

import java.util.HashMap;

public class SkyBoxManager {
    public static final HashMap<String, ISkyBoxRenderer> skyboxes = new HashMap<>();
    private static final String defaultId = "alfo";

    public static void addSkyBox(ISkyBoxRenderer skybox) {
        skyboxes.put(skybox.getID(), skybox);
    }

    public static void setSkyBox(String id) {
        if (!skyboxes.containsKey(id)) {
            id = defaultId;

            saveSelectedSkyBox(id);
        }

        RenderSpaceSky.getInstance().setRenderer(skyboxes.get(id));
    }

    public static void saveSelectedSkyBox(String id) {
        WarpDriveConfig.setSkyboxId(id);
    }
}
