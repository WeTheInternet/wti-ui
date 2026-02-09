package net.wti.ui.quest.sample;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/// LiveQuestDemo
///
/// Desktop launcher for the LiveQuest sample view.
/// Uses AbstractSampleApp (LiveQuestDemoApp) to build a Stage with
/// a DefaultLiveQuestView populated by seeded LiveQuest instances.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:36
public final class LiveQuestDemo {

    private LiveQuestDemo() {
        // utility
    }

    public static void main(final String[] args) {
        final Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("LifeQuest - LiveQuest Demo");
        config.setWindowedMode(900, 600);
        config.useVsync(true);
        new Lwjgl3Application(new LiveQuestDemoApp(), config);
    }
}