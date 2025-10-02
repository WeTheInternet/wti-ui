package net.wti.tasks.index

import net.wti.ui.demo.api.ModelRecurrence
import net.wti.ui.demo.api.ModelSettings
import net.wti.ui.demo.api.ModelTask
import net.wti.ui.demo.api.ModelTaskCompletion
import net.wti.ui.demo.api.ModelTaskDescription
import net.wti.ui.demo.api.ModelTimeRecord
import xapi.annotation.inject.SingletonOverride
import xapi.jre.model.ModelServiceJre
import xapi.model.X_Model
import xapi.model.service.ModelService
import xapi.platform.JrePlatform

@JrePlatform
@SingletonOverride(implFor = ModelService)
final class TestModelService extends ModelServiceJre {

    static TestModelService INSTANCE

    TestModelService() {
        INSTANCE = this
    }

    static void registerTypes() {
        X_Model.service // ensure the static service (us) is loaded
        assert INSTANCE == X_Model.service : "TestModelService did not win singleton injection"
        INSTANCE.getOrMakeModelManifest(ModelTask)
        INSTANCE.getOrMakeModelManifest(ModelRecurrence)
        INSTANCE.getOrMakeModelManifest(ModelTaskCompletion)
        INSTANCE.getOrMakeModelManifest(ModelTaskDescription)
        INSTANCE.getOrMakeModelManifest(ModelTimeRecord)
        INSTANCE.getOrMakeModelManifest(ModelSettings)
    }
}
