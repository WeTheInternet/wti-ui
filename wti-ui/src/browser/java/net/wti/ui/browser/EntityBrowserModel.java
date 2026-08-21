package net.wti.ui.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Renderer-neutral state for browsing one entity type at a time.
///
/// Source and filtered order always follow caller order. Query evaluation
/// occurs only when the source or query changes; draw/layout paths consume the
/// resulting bounded page. Selection is stored by stable key and reconciled
/// as follows:
///
/// - a still-visible key is retained, even when its entity instance changes;
/// - when a selected entity disappears, the entry at the same filtered index
///   becomes current, falling back to the preceding final entry;
/// - explicit page changes select the first entry on the destination page;
/// - an empty result clears selection and reports zero pages.
///
/// The class has no Scene2D dependency. Callers which bind it to a Scene2D
/// actor must mutate the bound model on the render thread.
public final class EntityBrowserModel<E> {

    private final EntityBrowserKeyProvider<? super E> keyProvider;
    private final EntityBrowserMatcher<? super E> matcher;
    private final List<E> sourceEntries;
    private final List<E> filteredEntries;
    private final Map<String, E> entriesByKey;
    private final List<EntityBrowserListener<E>> listeners;

    private String query;
    private String selectedKey;
    private int pageSize;
    private int pageIndex;

    /// Creates an empty browser with caller-owned identity and matching policy.
    public EntityBrowserModel(
            final EntityBrowserKeyProvider<? super E> keyProvider,
            final EntityBrowserMatcher<? super E> matcher,
            final int pageSize
    ) {
        if (keyProvider == null) {
            throw new IllegalArgumentException("keyProvider cannot be null");
        }
        if (matcher == null) {
            throw new IllegalArgumentException("matcher cannot be null");
        }
        requirePositivePageSize(pageSize);
        this.keyProvider = keyProvider;
        this.matcher = matcher;
        this.pageSize = pageSize;
        this.sourceEntries = new ArrayList<>();
        this.filteredEntries = new ArrayList<>();
        this.entriesByKey = new LinkedHashMap<>();
        this.listeners = new ArrayList<>();
        this.query = "";
    }

    /// Replaces the source snapshot while preserving caller iteration order.
    ///
    /// The replacement is validated before current state is mutated. Null
    /// entities, null/empty keys, and duplicate keys are rejected explicitly.
    public void setEntries(final Iterable<? extends E> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        final List<E> replacement = new ArrayList<>();
        final Map<String, E> replacementByKey = new LinkedHashMap<>();
        for (final E entity : entries) {
            final String key = validatedKey(entity);
            if (replacementByKey.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate entity browser key: " + key);
            }
            replacement.add(entity);
            replacementByKey.put(key, entity);
        }

        final ReconcileState previous = reconcileState();
        sourceEntries.clear();
        sourceEntries.addAll(replacement);
        entriesByKey.clear();
        entriesByKey.putAll(replacementByKey);
        rebuildFiltered(previous);
    }

    /// Sets query text; null is normalized to the empty query.
    public void setQuery(final String query) {
        final String normalized = query == null ? "" : query;
        if (normalized.equals(this.query)) {
            return;
        }
        final ReconcileState previous = reconcileState();
        this.query = normalized;
        rebuildFiltered(previous);
    }

    /// Returns the exact current query text, or an empty string.
    public String getQuery() {
        return query;
    }

    /// Changes the maximum number of entries on one page.
    public void setPageSize(final int pageSize) {
        requirePositivePageSize(pageSize);
        if (this.pageSize == pageSize) {
            return;
        }
        final String previousKey = selectedKey;
        final int previousFirstIndex = pageIndex * this.pageSize;
        this.pageSize = pageSize;
        if (selectedKey != null) {
            final int selectedIndex = filteredIndexOf(selectedKey);
            pageIndex = selectedIndex < 0 ? 0 : selectedIndex / pageSize;
        } else {
            pageIndex = clampedPage(previousFirstIndex / pageSize);
        }
        fireContentsChanged();
        fireSelectionChangedIfNeeded(previousKey);
    }

    /// Returns the current positive page bound.
    public int getPageSize() {
        return pageSize;
    }

    /// Selects a zero-based page, clamping to current result bounds.
    ///
    /// Moving to another non-empty page selects its first visible entry.
    public boolean setPage(final int requestedPage) {
        final int nextPage = clampedPage(requestedPage);
        if (nextPage == pageIndex) {
            return false;
        }
        final String previousKey = selectedKey;
        pageIndex = nextPage;
        selectedKey = keyAtPageStart(nextPage);
        fireContentsChanged();
        fireSelectionChangedIfNeeded(previousKey);
        return true;
    }

    /// Moves to the next page when one exists.
    public boolean nextPage() {
        return setPage(pageIndex + 1);
    }

    /// Moves to the previous page when one exists.
    public boolean previousPage() {
        return setPage(pageIndex - 1);
    }

    /// Returns the zero-based current page index.
    public int getPageIndex() {
        return pageIndex;
    }

    /// Returns zero for an empty result.
    public int getPageCount() {
        if (filteredEntries.isEmpty()) {
            return 0;
        }
        return (filteredEntries.size() + pageSize - 1) / pageSize;
    }

    /// Returns the number of entries matching the current query.
    public int getResultCount() {
        return filteredEntries.size();
    }

    /// Returns true when the current query has no results.
    public boolean isEmpty() {
        return filteredEntries.isEmpty();
    }

    /// Returns an immutable copy of the caller-ordered source snapshot.
    public List<E> getEntries() {
        return immutableCopy(sourceEntries);
    }

    /// Returns an immutable copy of all current filtered results.
    public List<E> getFilteredEntries() {
        return immutableCopy(filteredEntries);
    }

    /// Returns an immutable copy of the current bounded page.
    public List<E> getPageEntries() {
        if (filteredEntries.isEmpty()) {
            return Collections.emptyList();
        }
        final int from = pageIndex * pageSize;
        final int to = Math.min(from + pageSize, filteredEntries.size());
        return Collections.unmodifiableList(new ArrayList<>(filteredEntries.subList(from, to)));
    }

    /// Returns the selected stable key, or null when selection is empty.
    public String getSelectedKey() {
        return selectedKey;
    }

    /// Returns the selected entity from the latest source snapshot.
    public E getSelectedEntity() {
        return selectedKey == null ? null : entriesByKey.get(selectedKey);
    }

    E getEntry(final String stableKey) {
        return stableKey == null ? null : entriesByKey.get(stableKey);
    }

    String keyOf(final E entity) {
        return validatedKey(entity);
    }

    /// Selects a visible stable key and follows it to its containing page.
    public boolean setSelectedKey(final String stableKey) {
        if (stableKey == null) {
            return clearSelection();
        }
        final int filteredIndex = filteredIndexOf(stableKey);
        if (filteredIndex < 0) {
            return false;
        }
        final String previousKey = selectedKey;
        final int previousPage = pageIndex;
        selectedKey = stableKey;
        pageIndex = filteredIndex / pageSize;
        if (previousPage != pageIndex) {
            fireContentsChanged();
        }
        fireSelectionChangedIfNeeded(previousKey);
        return !same(previousKey, selectedKey) || previousPage != pageIndex;
    }

    /// Clears navigation selection without emitting activation.
    public boolean clearSelection() {
        if (selectedKey == null) {
            return false;
        }
        final String previousKey = selectedKey;
        selectedKey = null;
        fireSelectionChangedIfNeeded(previousKey);
        return true;
    }

    /// Moves selection through the complete filtered result and follows pages.
    ///
    /// Downward grid movement retains the logical column where possible. If
    /// that column is absent from an incomplete final row, it lands on the
    /// final entry in that row.
    public boolean moveSelection(
            final EntityBrowserDirection direction,
            final int columns
    ) {
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be greater than zero");
        }
        if (filteredEntries.isEmpty()) {
            return clearSelection();
        }

        final int currentIndex = selectedKey == null
                ? -1
                : filteredIndexOf(selectedKey);
        final int nextIndex;
        if (currentIndex < 0) {
            nextIndex = Math.min(pageIndex * pageSize, filteredEntries.size() - 1);
        } else {
            nextIndex = movedIndex(currentIndex, direction, columns);
        }
        if (nextIndex == currentIndex) {
            return false;
        }
        return setSelectedKey(validatedKey(filteredEntries.get(nextIndex)));
    }

    /// Emits one explicit activation for the selected entity, if any.
    public boolean activateSelection() {
        final E entity = getSelectedEntity();
        if (entity == null) {
            return false;
        }
        final List<EntityBrowserListener<E>> snapshot = listenerSnapshot();
        for (final EntityBrowserListener<E> listener : snapshot) {
            listener.activated(this, selectedKey, entity);
        }
        return true;
    }

    /// Adds an intent listener once; duplicate registration is ignored.
    public void addListener(final EntityBrowserListener<E> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /// Removes a previously registered listener.
    public void removeListener(final EntityBrowserListener<E> listener) {
        listeners.remove(listener);
    }

    private void rebuildFiltered(final ReconcileState previous) {
        filteredEntries.clear();
        for (final E entity : sourceEntries) {
            if (query.isEmpty() || matcher.matches(entity, query)) {
                filteredEntries.add(entity);
            }
        }

        if (previous.selectedKey != null) {
            final int retainedIndex = filteredIndexOf(previous.selectedKey);
            if (retainedIndex >= 0) {
                selectedKey = previous.selectedKey;
                pageIndex = retainedIndex / pageSize;
            } else if (filteredEntries.isEmpty()) {
                selectedKey = null;
                pageIndex = 0;
            } else {
                final int fallbackIndex = Math.min(
                        Math.max(0, previous.selectedFilteredIndex),
                        filteredEntries.size() - 1
                );
                selectedKey = validatedKey(filteredEntries.get(fallbackIndex));
                pageIndex = fallbackIndex / pageSize;
            }
        } else {
            selectedKey = null;
            pageIndex = clampedPage(previous.pageIndex);
        }

        fireContentsChanged();
        fireSelectionChangedIfNeeded(previous.selectedKey);
    }

    private ReconcileState reconcileState() {
        return new ReconcileState(
                selectedKey,
                selectedKey == null ? -1 : filteredIndexOf(selectedKey),
                pageIndex
        );
    }

    private int movedIndex(
            final int currentIndex,
            final EntityBrowserDirection direction,
            final int columns
    ) {
        switch (direction) {
            case LEFT:
                return Math.max(0, currentIndex - 1);
            case RIGHT:
                return Math.min(filteredEntries.size() - 1, currentIndex + 1);
            case UP:
                return Math.max(0, currentIndex - columns);
            case DOWN:
                final int candidate = currentIndex + columns;
                if (candidate < filteredEntries.size()) {
                    return candidate;
                }
                final int finalRowStart =
                        ((filteredEntries.size() - 1) / columns) * columns;
                return currentIndex < finalRowStart
                        ? filteredEntries.size() - 1
                        : currentIndex;
            case HOME:
                return 0;
            case END:
                return filteredEntries.size() - 1;
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    private int filteredIndexOf(final String stableKey) {
        for (int i = 0; i < filteredEntries.size(); i++) {
            if (stableKey.equals(validatedKey(filteredEntries.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private String keyAtPageStart(final int page) {
        if (filteredEntries.isEmpty()) {
            return null;
        }
        final int index = Math.min(page * pageSize, filteredEntries.size() - 1);
        return validatedKey(filteredEntries.get(index));
    }

    private int clampedPage(final int requestedPage) {
        final int pageCount = getPageCount();
        if (pageCount == 0) {
            return 0;
        }
        return Math.max(0, Math.min(requestedPage, pageCount - 1));
    }

    private String validatedKey(final E entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity browser entries cannot be null");
        }
        final String key = keyProvider.keyOf(entity);
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Entity browser keys cannot be null or empty");
        }
        return key;
    }

    private void fireContentsChanged() {
        final List<EntityBrowserListener<E>> snapshot = listenerSnapshot();
        for (final EntityBrowserListener<E> listener : snapshot) {
            listener.contentsChanged(this);
        }
    }

    private void fireSelectionChangedIfNeeded(final String previousKey) {
        if (same(previousKey, selectedKey)) {
            return;
        }
        final E selectedEntity = getSelectedEntity();
        final List<EntityBrowserListener<E>> snapshot = listenerSnapshot();
        for (final EntityBrowserListener<E> listener : snapshot) {
            listener.selectionChanged(this, previousKey, selectedKey, selectedEntity);
        }
    }

    private List<EntityBrowserListener<E>> listenerSnapshot() {
        return new ArrayList<>(listeners);
    }

    private static void requirePositivePageSize(final int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
    }

    private static boolean same(final String left, final String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static <T> List<T> immutableCopy(final List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static final class ReconcileState {
        private final String selectedKey;
        private final int selectedFilteredIndex;
        private final int pageIndex;

        private ReconcileState(
                final String selectedKey,
                final int selectedFilteredIndex,
                final int pageIndex
        ) {
            this.selectedKey = selectedKey;
            this.selectedFilteredIndex = selectedFilteredIndex;
            this.pageIndex = pageIndex;
        }
    }
}
