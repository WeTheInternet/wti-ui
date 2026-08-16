package net.wti.quest.api;

///
/// CompletionPolicy:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:37
public enum CompletionPolicy {
    /// All subquests must be completed (default)
    ALL_OF,
    /// Any subtask completion can enable the parent quest to be completed
    /// At least Math.max(1, QuestDefinition::getMinimumRequired) subtasks must be completed.
    ANY_OF,
    /// Each subtask completion adds weight (defined by ChildRef::getQuantity)
    /// The weight of completed subtasks must exceed the weight of QuestDefinition::getMinimumRequired.
    WEIGHTED
}
