package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.MonitorNotificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.WatchlistEntryType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * HCDP-04-02: Persistence helper for Activity Monitor (watchlist + notification inbox).
 * All queries are against the catalogue (UBL) repository — same DB as DemandType/DemandResponseType.
 *
 * NOTE: All mutations use executeUpdate() (JPQL bulk updates) rather than updateEntity().
 * The updateEntity() path does not reliably flush in this JPA setup; JPQL updates do.
 */
public class MonitorPersistenceUtil {

    // ------------------------------------------------------------------ Watchlist read
    private static final String QUERY_GET_WATCHLIST_FOR_USER =
            "SELECT w FROM WatchlistEntryType w WHERE w.userId = :userId ORDER BY w.createdAt DESC";

    private static final String QUERY_GET_WATCHLIST_ENTRY =
            "SELECT w FROM WatchlistEntryType w WHERE w.hjid = :hjid";

    private static final String QUERY_GET_WATCHLIST_ENTRY_BY_TARGET =
            "SELECT w FROM WatchlistEntryType w WHERE w.userId = :userId AND w.watchType = :watchType AND w.targetId = :targetId";

    // ------------------------------------------------------------------ Watchlist write
    private static final String QUERY_UPDATE_WATCHLIST =
            "UPDATE WatchlistEntryType w SET " +
            "w.lastSeenStatus = :lastSeenStatus, " +
            "w.lastSeenBpCount = :lastSeenBpCount, " +
            "w.lastAnomalyAlerted = :lastAnomalyAlerted, " +
            "w.targetLabel = :targetLabel " +
            "WHERE w.hjid = :hjid";

    private static final String QUERY_DELETE_WATCHLIST_ENTRY =
            "DELETE FROM WatchlistEntryType w WHERE w.hjid = :hjid AND w.userId = :userId";

    public static List<WatchlistEntryType> getWatchlistForUser(String userId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(
                QUERY_GET_WATCHLIST_FOR_USER,
                new String[]{"userId"}, new Object[]{userId});
    }

    public static WatchlistEntryType getWatchlistEntry(Long hjid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(
                QUERY_GET_WATCHLIST_ENTRY,
                new String[]{"hjid"}, new Object[]{hjid});
    }

    public static WatchlistEntryType getWatchlistEntryByTarget(String userId, String watchType, String targetId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(
                QUERY_GET_WATCHLIST_ENTRY_BY_TARGET,
                new String[]{"userId", "watchType", "targetId"}, new Object[]{userId, watchType, targetId});
    }

    public static void saveWatchlistEntry(WatchlistEntryType entry) {
        new JPARepositoryFactory().forCatalogueRepository().persistEntity(entry);
    }

    /**
     * Updates mutable state fields of an existing watchlist entry using a JPQL bulk update.
     * Passes all four mutable fields from the already-patched entity object.
     */
    public static void updateWatchlistEntry(WatchlistEntryType entry) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_UPDATE_WATCHLIST,
                new String[]{"lastSeenStatus", "lastSeenBpCount", "lastAnomalyAlerted", "targetLabel", "hjid"},
                new Object[]{entry.getLastSeenStatus(), entry.getLastSeenBpCount(),
                             entry.getLastAnomalyAlerted(), entry.getTargetLabel(), entry.getHjid()});
    }

    public static void deleteWatchlistEntry(Long hjid, String userId) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_DELETE_WATCHLIST_ENTRY,
                new String[]{"hjid", "userId"}, new Object[]{hjid, userId});
    }

    // ------------------------------------------------------------------ Notifications read
    private static final String QUERY_GET_NOTIFICATIONS_FOR_USER =
            "SELECT n FROM MonitorNotificationType n WHERE n.userId = :userId AND n.dismissedAt IS NULL ORDER BY n.createdAt DESC";

    private static final String QUERY_COUNT_UNREAD_FOR_USER =
            "SELECT COUNT(n.hjid) FROM MonitorNotificationType n WHERE n.userId = :userId AND n.dismissedAt IS NULL AND n.readAt IS NULL";

    private static final String QUERY_GET_NOTIFICATION =
            "SELECT n FROM MonitorNotificationType n WHERE n.hjid = :hjid";

    // ------------------------------------------------------------------ Notifications write (all JPQL bulk)
    private static final String QUERY_MARK_ONE_READ =
            "UPDATE MonitorNotificationType n SET n.readAt = :now WHERE n.hjid = :hjid AND n.readAt IS NULL";

    private static final String QUERY_DISMISS_ONE =
            "UPDATE MonitorNotificationType n SET n.dismissedAt = :now WHERE n.hjid = :hjid AND n.dismissedAt IS NULL";

    private static final String QUERY_SET_READ_IF_NULL_FOR_ONE =
            "UPDATE MonitorNotificationType n SET n.readAt = :now WHERE n.hjid = :hjid AND n.readAt IS NULL";

    private static final String QUERY_MARK_ALL_READ =
            "UPDATE MonitorNotificationType n SET n.readAt = :now WHERE n.userId = :userId AND n.dismissedAt IS NULL AND n.readAt IS NULL";

    private static final String QUERY_DISMISS_ALL =
            "UPDATE MonitorNotificationType n SET n.dismissedAt = :now WHERE n.userId = :userId AND n.dismissedAt IS NULL";

    public static List<MonitorNotificationType> getNotificationsForUser(String userId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(
                QUERY_GET_NOTIFICATIONS_FOR_USER,
                new String[]{"userId"}, new Object[]{userId});
    }

    public static long countUnreadForUser(String userId) {
        Long count = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(
                QUERY_COUNT_UNREAD_FOR_USER,
                new String[]{"userId"}, new Object[]{userId});
        return count != null ? count : 0L;
    }

    public static MonitorNotificationType getNotification(Long hjid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(
                QUERY_GET_NOTIFICATION,
                new String[]{"hjid"}, new Object[]{hjid});
    }

    public static void saveNotification(MonitorNotificationType notification) {
        new JPARepositoryFactory().forCatalogueRepository().persistEntity(notification);
    }

    /** Marks a single notification as read (idempotent — no-op if already read). */
    public static void markOneRead(Long hjid, java.util.Date now) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_MARK_ONE_READ,
                new String[]{"hjid", "now"}, new Object[]{hjid, now});
    }

    /**
     * Dismisses a single notification and also sets readAt if still unread.
     * Two separate queries — both are idempotent.
     */
    public static void dismissOne(Long hjid, java.util.Date now) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_DISMISS_ONE,
                new String[]{"hjid", "now"}, new Object[]{hjid, now});
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_SET_READ_IF_NULL_FOR_ONE,
                new String[]{"hjid", "now"}, new Object[]{hjid, now});
    }

    public static void markAllRead(String userId, java.util.Date now) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_MARK_ALL_READ,
                new String[]{"userId", "now"}, new Object[]{userId, now});
    }

    public static void dismissAll(String userId, java.util.Date now) {
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(
                QUERY_DISMISS_ALL,
                new String[]{"userId", "now"}, new Object[]{userId, now});
    }
}
