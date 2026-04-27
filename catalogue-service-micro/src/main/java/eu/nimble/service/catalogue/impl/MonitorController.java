package eu.nimble.service.catalogue.impl;

import eu.nimble.service.catalogue.config.RoleConfig;
import eu.nimble.service.catalogue.persistence.util.MonitorPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.MonitorNotificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.WatchlistEntryType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * HCDP-04-02: Activity Monitor REST API.
 *
 * <p>Provides watchlist CRUD and notification inbox management.
 * State-change detection and anomaly logic run client-side in Angular (lazy, on-demand).
 * This controller only persists what Angular tells it to persist.</p>
 *
 * <p>All endpoints require a valid Bearer token (userId extracted from token by Angular).</p>
 */
@Controller
public class MonitorController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private IValidationUtil validationUtil;

    // ======================================================================
    // WATCHLIST
    // ======================================================================

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns all watchlist entries for the given user.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Watchlist retrieved successfully"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @GetMapping(value = "/monitor/watchlist", produces = "application/json")
    public ResponseEntity<List<WatchlistEntryType>> getWatchlist(
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @ApiParam(value = "Bearer token") @RequestHeader(value = "Authorization") String bearerToken) {

        logger.debug("[MONITOR] Getting watchlist for user: {}", userId);
        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<WatchlistEntryType> watchlist = MonitorPersistenceUtil.getWatchlistForUser(userId);
        logger.debug("[MONITOR] Found {} watchlist entries for user: {}", watchlist.size(), userId);
        return ResponseEntity.ok(watchlist);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Adds a new watchlist entry for a user.")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Watchlist entry created"),
        @ApiResponse(code = 409, message = "Already watching this target"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @PostMapping(value = "/monitor/watchlist",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<WatchlistEntryType> addWatchlistEntry(
            @RequestBody WatchlistEntryType entry,
            @RequestHeader(value = "Authorization") String bearerToken) {

        logger.debug("[MONITOR] Adding watchlist entry: type={}, target={}, user={}",
                entry.getWatchType(), entry.getTargetId(), entry.getUserId());
        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        // Deduplicate: return 409 if already watching
        WatchlistEntryType existing = MonitorPersistenceUtil.getWatchlistEntryByTarget(
                entry.getUserId(), entry.getWatchType(), entry.getTargetId());
        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(existing);
        }
        entry.setCreatedAt(new Date());
        if (entry.getLastAnomalyAlerted() == null) {
            entry.setLastAnomalyAlerted(false);
        }
        MonitorPersistenceUtil.saveWatchlistEntry(entry);
        logger.debug("[MONITOR] Watchlist entry saved with hjid: {}", entry.getHjid());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Updates state snapshot fields of a watchlist entry (lastSeenStatus, lastSeenBpCount, lastAnomalyAlerted).")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Updated successfully"),
        @ApiResponse(code = 404, message = "Entry not found"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @PutMapping(value = "/monitor/watchlist/{hjid}",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<WatchlistEntryType> updateWatchlistEntry(
            @ApiParam(value = "Watchlist entry HJID") @PathVariable("hjid") Long hjid,
            @RequestBody WatchlistEntryType patch,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        WatchlistEntryType existing = MonitorPersistenceUtil.getWatchlistEntry(hjid);
        if (existing == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Patch only the mutable state fields
        if (patch.getLastSeenStatus() != null)   existing.setLastSeenStatus(patch.getLastSeenStatus());
        if (patch.getLastSeenBpCount() != null)  existing.setLastSeenBpCount(patch.getLastSeenBpCount());
        if (patch.getLastAnomalyAlerted() != null) existing.setLastAnomalyAlerted(patch.getLastAnomalyAlerted());
        if (patch.getTargetLabel() != null)      existing.setTargetLabel(patch.getTargetLabel());
        MonitorPersistenceUtil.updateWatchlistEntry(existing);
        return ResponseEntity.ok(existing);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Removes a watchlist entry. Only the owning user can remove their own entries.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Deleted successfully"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @DeleteMapping(value = "/monitor/watchlist/{hjid}")
    public ResponseEntity<Void> removeWatchlistEntry(
            @ApiParam(value = "Watchlist entry HJID") @PathVariable("hjid") Long hjid,
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @RequestHeader(value = "Authorization") String bearerToken) {

        logger.debug("[MONITOR] Removing watchlist entry hjid={} for user={}", hjid, userId);
        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        MonitorPersistenceUtil.deleteWatchlistEntry(hjid, userId);
        return ResponseEntity.noContent().build();
    }

    // ======================================================================
    // NOTIFICATIONS (inbox)
    // ======================================================================

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns all undismissed notifications for the user, newest first.")
    @GetMapping(value = "/monitor/notifications", produces = "application/json")
    public ResponseEntity<List<MonitorNotificationType>> getNotifications(
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(MonitorPersistenceUtil.getNotificationsForUser(userId));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Returns the count of unread, undismissed notifications — used for the badge.")
    @GetMapping(value = "/monitor/badge-count", produces = "application/json")
    public ResponseEntity<Long> getBadgeCount(
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(MonitorPersistenceUtil.countUnreadForUser(userId));
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Creates a new notification (called by Angular after detecting a state change or anomaly).")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Notification created"),
        @ApiResponse(code = 401, message = "Unauthorized request")
    })
    @PostMapping(value = "/monitor/notifications",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<MonitorNotificationType> createNotification(
            @RequestBody MonitorNotificationType notification,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        notification.setCreatedAt(new Date());
        MonitorPersistenceUtil.saveNotification(notification);
        logger.debug("[MONITOR] Notification created: type={}, user={}", notification.getNotificationType(), notification.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Marks a single notification as read.")
    @PostMapping(value = "/monitor/notifications/{hjid}/read")
    public ResponseEntity<Void> markAsRead(
            @ApiParam(value = "Notification HJID") @PathVariable("hjid") Long hjid,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        MonitorPersistenceUtil.markOneRead(hjid, new Date());
        return ResponseEntity.ok().build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Dismisses a single notification (removes from inbox).")
    @PostMapping(value = "/monitor/notifications/{hjid}/dismiss")
    public ResponseEntity<Void> dismiss(
            @ApiParam(value = "Notification HJID") @PathVariable("hjid") Long hjid,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        MonitorPersistenceUtil.dismissOne(hjid, new Date());
        return ResponseEntity.ok().build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Marks all undismissed notifications as read for the user.")
    @PostMapping(value = "/monitor/notifications/read-all")
    public ResponseEntity<Void> markAllRead(
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        MonitorPersistenceUtil.markAllRead(userId, new Date());
        return ResponseEntity.ok().build();
    }

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "", notes = "Dismisses all notifications for the user (clears entire inbox).")
    @PostMapping(value = "/monitor/notifications/clear-all")
    public ResponseEntity<Void> clearAll(
            @ApiParam(value = "Keycloak user ID") @RequestParam(value = "userId") String userId,
            @RequestHeader(value = "Authorization") String bearerToken) {

        if (!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_CATALOGUE_READ)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        MonitorPersistenceUtil.dismissAll(userId, new Date());
        return ResponseEntity.ok().build();
    }
}
