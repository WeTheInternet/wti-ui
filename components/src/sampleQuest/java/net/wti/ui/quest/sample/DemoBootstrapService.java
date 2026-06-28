package net.wti.ui.quest.sample;

import net.wti.model.api.WtiGroup;
import net.wti.model.api.WtiUser;
import net.wti.model.quest.api.QuestDefinitionStore;
import net.wti.model.user.core.UserGroupStore;
import net.wti.model.user.impl.InMemoryUserGroupStore;
import net.wti.model.user.impl.WtiGroupLoaderImpl;
import net.wti.model.user.impl.WtiUserLoaderImpl;
import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import net.wti.quest.model.impl.InMemoryQuestDefinitionStore;
import net.wti.quest.model.impl.NamespacedQuestDefinitionSourceImpl;
import net.wti.quest.model.impl.QuestDefinitionLoaderImpl;
import net.wti.time.api.ModelDay;
import xapi.fu.log.Log;
import xapi.jre.model.ModelServiceJre;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.time.X_Time;
import xapi.util.api.SuccessHandler;

import java.util.ArrayList;
import java.util.List;

///
/// DemoBootstrapService:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 10/04/2026 @ 10:01
public class DemoBootstrapService {

    public static final String DEFAULT_USER = "dad";

    public DemoBootstrapService() {
        ((ModelServiceJre)X_Model.getService()).getOrMakeModelManifest(LiveQuest.class);
        ((ModelServiceJre)X_Model.getService()).getOrMakeModelManifest(QuestDefinition.class);
        ((ModelServiceJre)X_Model.getService()).getOrMakeModelManifest(WtiUser.class);
        ((ModelServiceJre)X_Model.getService()).getOrMakeModelManifest(WtiGroup.class);
    }

    public List<LiveQuest> loadForToday(final ModelDay day) {
        final UserGroupStore userGroupStore = new InMemoryUserGroupStore();
        preloadUsersAndGroups(userGroupStore);

        final ModelKey userKey = WtiUser.newKey(DEFAULT_USER);
        final ModelKey rootKey = WtiGroup.newKey("root");
        final QuestDefinitionStore defStore = new InMemoryQuestDefinitionStore();
        preloadDefinitions(defStore, rootKey);

        final NamespacedQuestDefinitionSourceImpl source =
                new NamespacedQuestDefinitionSourceImpl(defStore, userGroupStore, rootKey);
        Log.tryLog(DemoBootstrapService.class, this, Log.LogLevel.INFO,
                "Loading definitions");

        final List<LiveQuest> out = new ArrayList<>();
        source.streamDefinitionsForUser(userKey, (ns, def) -> {
            if (def == null || !def.auto()) {
                return;
            }
            final LiveQuest live = X_Model.create(LiveQuest.class);
            live.setTitle(def.getTitle());
            live.setDescription(def.getDescription());
            live.setParentDayKey(ModelDay.newKey(day.getDayNum()));
            live.setCreatedAtMillis(X_Time.nowMillisLong());
            live.setDayIndex(day.getDayNum());
            live.setLiveKey(def.getKey().getId());
            live.setSourceDefinitionKey(def.getKey());
            live.setScheduleTemplateKey(def.getScheduleTemplateKey());
            live.setEffectivePriority(def.getPriority());
            live.setTags(def.getTags());
            X_Model.persist(live, SuccessHandler.noop());
            out.add(live);
        }, () -> {});

        return out;
    }

    private void preloadUsersAndGroups(final UserGroupStore store) {
        new WtiGroupLoaderImpl().loadInto(store);
        new WtiUserLoaderImpl().loadInto(store);
    }

    private void preloadDefinitions(final QuestDefinitionStore store, final ModelKey rootKey) {
        new QuestDefinitionLoaderImpl().loadFromClasspath((resource, def) -> {
            if (def != null) {
                ((InMemoryQuestDefinitionStore) store).put(rootKey, def);
            }
        });
    }
}