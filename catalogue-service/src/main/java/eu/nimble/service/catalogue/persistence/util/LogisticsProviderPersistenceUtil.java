package eu.nimble.service.catalogue.persistence.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.LogisticsProviderType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * HCDP-05-04: Persistence helper for the Logistics Provider directory.
 *
 * <p>Backs {@code LogisticsProvidersController}. Queries against the catalogue
 * (UBL) repository — same DB as the rest of the platform (ubldb). Read-only
 * from the application side; writes happen only via the seed script.</p>
 */
public class LogisticsProviderPersistenceUtil {

    private static final String QUERY_GET_ACTIVE_PROVIDERS =
            "SELECT p FROM LogisticsProviderType p WHERE p.active = true ORDER BY p.name ASC";

    private static final String QUERY_GET_BY_SLUG =
            "SELECT p FROM LogisticsProviderType p WHERE p.slug = :slug";

    public static List<LogisticsProviderType> getActiveProviders() {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(
                QUERY_GET_ACTIVE_PROVIDERS,
                new String[]{}, new Object[]{});
    }

    public static LogisticsProviderType getBySlug(String slug) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(
                QUERY_GET_BY_SLUG,
                new String[]{"slug"}, new Object[]{slug});
    }
}
