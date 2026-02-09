package net.wti.quest.api;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.PersistenceStrategy;
import xapi.annotation.model.Persistent;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;

/// ChildRef
///
/// Definition-level composition relationship.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:42
@IsModel(
        modelType = ChildRef.MODEL_CHILD_REF,
        persistence = @Persistent(strategy = PersistenceStrategy.Remote)
)
public interface ChildRef extends Model {

    String MODEL_CHILD_REF = "qchild";

    /// Key of the child QuestDefinition.
    ModelKey getChildDefinitionKey();
    ChildRef setChildDefinitionKey(ModelKey key);

    ChildRole getRole();
    ChildRef setRole(ChildRole role);

    ChildStartPolicy getStartPolicy();
    ChildRef setStartPolicy(ChildStartPolicy policy);

    /// Used only when startPolicy == AFTER_N_SIBLINGS_COMPLETE
    Integer getStartAfterNSiblingsComplete();
    ChildRef setStartAfterNSiblingsComplete(Integer n);

    /// Optional quantity (quotas / counts).
    Integer getQuantity();
    ChildRef setQuantity(Integer quantity);

    /// If true, child inherits parent tags; extra tags can be added in additionalTags.
    Boolean getInheritTags();
    ChildRef setInheritTags(Boolean inherit);

    /// Additional tags applied on top of inherited tags.
    String[] getAdditionalTags();
    ChildRef setAdditionalTags(String[] tags);

}
