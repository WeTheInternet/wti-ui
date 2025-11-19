package net.wti.time.impl;

///
/// DayService:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/11/2025 @ 22:21

import net.wti.time.api.DayIndex;
import net.wti.time.api.ModelDay;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.time.api.TimeZoneInfo;

/// DayService
///
/// Service for computing and caching ModelDay instances.
/// Provides create-if-missing semantics for ModelDay objects.
///
/// Can be used as compute-only (no persistence) or wired to persist
/// ModelDay instances to storage in the future.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2025-11-18
public class DayService {

    private final DayIndexService indexService;
    private final MapLike<String, ModelDay> cache;

    public DayService(DayIndexService indexService) {
        this.indexService = indexService;
        this.cache = X_Jdk.mapOrderedKeyConcurrent();
    }

    /// Gets or creates a ModelDay for the given DayIndex using default zone and rolloverHour.
    public ModelDay getOrCreateModelDay(DayIndex dayIndex) {
        return getOrCreateModelDay(dayIndex, indexService.getDefaultZone(), indexService.getDefaultRolloverHour());
    }

    /// Gets or creates a ModelDay for the given DayIndex with custom zone.
    public ModelDay getOrCreateModelDay(DayIndex dayIndex, TimeZoneInfo zone) {
        return getOrCreateModelDay(dayIndex, zone, indexService.getDefaultRolloverHour());
    }

    /// Gets or creates a ModelDay for the given DayIndex with custom zone and rolloverHour.
    /// Uses cache to avoid recomputing.
    public ModelDay getOrCreateModelDay(DayIndex dayIndex, TimeZoneInfo zone, int rolloverHour) {
        String cacheKey = makeCacheKey(dayIndex, zone, rolloverHour);
        return cache.computeIfAbsent(cacheKey, k -> indexService.createModelDay(dayIndex, zone, rolloverHour));
    }

    /// Gets or creates a ModelDay for the given epoch millis using default zone and rolloverHour.
    public ModelDay getOrCreateModelDay(double epochMillis) {
        DayIndex dayIndex = indexService.computeDayIndex(epochMillis);
        return getOrCreateModelDay(dayIndex);
    }

    /// Gets or creates a ModelDay for the given epoch millis with custom zone.
    public ModelDay getOrCreateModelDay(double epochMillis, TimeZoneInfo zone) {
        DayIndex dayIndex = indexService.computeDayIndex(epochMillis, zone);
        return getOrCreateModelDay(dayIndex, zone);
    }

    /// Gets or creates a ModelDay for the given epoch millis with custom zone and rolloverHour.
    public ModelDay getOrCreateModelDay(double epochMillis, TimeZoneInfo zone, int rolloverHour) {
        DayIndex dayIndex = indexService.computeDayIndex(epochMillis, zone, rolloverHour);
        return getOrCreateModelDay(dayIndex, zone, rolloverHour);
    }

    /// Returns the ModelDay for today using default zone and rolloverHour.
    public ModelDay today() {
        return getOrCreateModelDay(indexService.today());
    }

    /// Returns the ModelDay for today using custom zone.
    public ModelDay today(TimeZoneInfo zone) {
        return getOrCreateModelDay(indexService.today(zone), zone);
    }

    /// Clears the cache. Useful for testing.
    public void clearCache() {
        cache.clear();
    }

    /// Returns the number of cached ModelDay instances.
    public int getCacheSize() {
        return cache.size();
    }

    private static String makeCacheKey(DayIndex dayIndex, TimeZoneInfo zone, int rolloverHour) {
        return dayIndex.getDayNum() + "|" + zone.getId() + "|" + rolloverHour;
    }
}
