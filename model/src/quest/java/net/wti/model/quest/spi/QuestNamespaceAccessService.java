package net.wti.model.quest.spi;

///
/// QuestNamespaceAccessService:
///
///
import xapi.model.api.ModelKey;

/// Namespace membership helper.
///
/// Namespace is the single ACL source of truth.
public interface QuestNamespaceAccessService {

    /// True when user can read definitions in the given namespace.
    /// Implementation should support user namespace, group namespace, and root namespace.
    boolean canAccessNamespace(ModelKey userKey, ModelKey namespaceKey);
}
