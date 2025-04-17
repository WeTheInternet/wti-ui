package net.wti.ui.demo.jre;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.wti.ui.demo.common.DemoApp;

/// DemoAppJre:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 11:42
public class DemoAppJre {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 640);
//        config.setDecorated(false);

        new Lwjgl3Application(new DemoApp(), config);
    }
}
