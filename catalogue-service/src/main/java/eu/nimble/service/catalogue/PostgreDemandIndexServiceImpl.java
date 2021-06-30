package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.category.IndexCategoryService;
import eu.nimble.service.catalogue.model.category.Category;
import eu.nimble.service.catalogue.model.demand.DemandFacetResponse;
import eu.nimble.service.catalogue.model.demand.DemandFacetValue;
import eu.nimble.service.catalogue.persistence.util.DemandPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("!test")
public class PostgreDemandIndexServiceImpl implements DemandIndexService {
    private static final String QUERY_INSERT_INDEX_DATA = "UPDATE demand_type SET search_index = %s WHERE hjid = :hjid";
    private static final String QUERY_SELECT_DEMANDS = "SELECT hjid FROM demand_type WHERE search_index @@ to_tsquery('%s', '%s')";

    private static Map<String, String> postgreTsVectorLanguageMap = new HashMap<>();
    static {
        postgreTsVectorLanguageMap.put("de", "german");
        postgreTsVectorLanguageMap.put("en", "english");
        postgreTsVectorLanguageMap.put("es", "spanish");
        postgreTsVectorLanguageMap.put("tr", "turkish");
    }

    private static final Logger logger = LoggerFactory.getLogger(PostgreDemandIndexServiceImpl.class);

    @Autowired
    private IndexCategoryService indexCategoryService;

    @Override
    public void indexDemandText(DemandType demand) {
        Map<String, StringBuilder> languageBasedData = new HashMap<>();
        for (TextType title : demand.getTitle()) {
            StringBuilder indexData = languageBasedData.get(title.getLanguageID());
            if (languageBasedData.get(title.getLanguageID()) == null) {
                indexData = new StringBuilder();
                languageBasedData.put(title.getLanguageID(), indexData);
            }
            indexData.append(title.getValue()).append(" ");
        }
        for (TextType description : demand.getDescription()) {
            StringBuilder indexData = languageBasedData.get(description.getLanguageID());
            if (languageBasedData.get(description.getLanguageID()) == null) {
                indexData = new StringBuilder();
                languageBasedData.put(description.getLanguageID(), indexData);
            }
            indexData.append(description.getValue()).append(" ");
        }

        List<String> vectorParts = new ArrayList<>();
        for (Map.Entry<String, StringBuilder> indexData : languageBasedData.entrySet()) {
            String postgreLang = postgreTsVectorLanguageMap.get(indexData.getKey());
            if (postgreLang == null) {
                logger.warn("No language definition found for the demand data. lang: {}", indexData.getKey());
                vectorParts.add(String.format("to_tsvector('%s')", indexData.getValue().toString()));
            } else {
                vectorParts.add(String.format("to_tsvector('%s', '%s')", postgreLang, indexData.getValue().toString()));
            }
        }
        String indexData = String.join(" || ", vectorParts);

        String query = String.format(QUERY_INSERT_INDEX_DATA, indexData);
        new JPARepositoryFactory().forCatalogueRepository().executeUpdate(query, new String[]{"hjid"}, new Object[]{demand.getHjid()}, true);
    }

    @Override
    public List<DemandType> searchDemand(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,
                                         Integer pageNo, Integer limit,List<String> circularEconomyCertificates, List<String> otherCertificates) {

        DemandRetrievalQuery query = constructDemandRetrievalQuery(
                DemandQueryType.HJID, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry,circularEconomyCertificates,otherCertificates
        );
        List<BigInteger> results = new JPARepositoryFactory().forCatalogueRepository()
                .getEntities(query.query, query.queryParameters.toArray(new String[query.queryParameters.size()]), query.queryValues.toArray(), limit, pageNo * limit, true);
        if (results.size() > 0) {
            List<DemandType> demands = DemandPersistenceUtil.getDemandsForHjids(results.stream().map(BigInteger::longValue).collect(Collectors.toList()));
            // sort the results with the same order of the IDs, as ids are sorted by ranking
            List<DemandType> sortedDemands = new ArrayList<>();
            results.forEach(hjid -> {
                sortedDemands.add(demands.stream().filter(demand -> demand.getHjid() == hjid.longValue()).findFirst().get());
            });
            return sortedDemands;

        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public int getDemandCount(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,List<String> circularEconomyCertificates, List<String> otherCertificates) {
        DemandRetrievalQuery query = constructDemandRetrievalQuery(
                DemandQueryType.COUNT, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry,circularEconomyCertificates,otherCertificates
        );
        BigInteger count = new JPARepositoryFactory().forCatalogueRepository()
                .getSingleEntity(query.query, query.queryParameters.toArray(new String[query.queryParameters.size()]), query.queryValues.toArray(), true);
        return count.intValue();
    }

    @Override
    public List<DemandFacetResponse> getDemandFacets(String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,List<String> circularEconomyCertificates, List<String> otherCertificates) {
        List<DemandFacetResponse> facetResponses = new ArrayList<>();

        // category
        DemandRetrievalQuery query = constructDemandRetrievalQuery(DemandQueryType.CATEGORY, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, circularEconomyCertificates,otherCertificates);
        facetResponses.add(constructDemandFacetResponse("Category", query));
        // delivery country
        query = constructDemandRetrievalQuery(DemandQueryType.DELIVERY_COUNTRY, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, circularEconomyCertificates,otherCertificates);
        facetResponses.add(constructDemandFacetResponse("Delivery Country", query));
        // buyer country
        query = constructDemandRetrievalQuery(DemandQueryType.BUYER_COUNTRY, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, circularEconomyCertificates,otherCertificates);
        facetResponses.add(constructDemandFacetResponse("Buyer Country", query));
        // circular economy certificates
        query = constructDemandRetrievalQuery(DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, circularEconomyCertificates,otherCertificates);
        facetResponses.add(constructDemandFacetResponse("Sustainability / Environment Certificates", query));
        // other certificates
        query = constructDemandRetrievalQuery(DemandQueryType.OTHER_CERTIFICATES, queryTerm, lang, companyId, categoryUri, dueDate, buyerCountry, deliveryCountry, circularEconomyCertificates,otherCertificates);
        facetResponses.add(constructDemandFacetResponse("Demand Certificates", query));
        return facetResponses;
    }

    private DemandFacetResponse constructDemandFacetResponse(String facetName, DemandRetrievalQuery query ) {
        List<Object> dbResults = new JPARepositoryFactory().forCatalogueRepository()
                .getEntities(query.query, query.queryParameters.toArray(new String[query.queryParameters.size()]), query.queryValues.toArray(), true);
        List<DemandFacetValue> facetResults = new ArrayList<>();
        for (Object result : dbResults) {
            Object[] castedResult = (Object[]) result;
            facetResults.add(new DemandFacetValue((String) castedResult[0], ((BigInteger) castedResult[1]).intValue()));
        }
        return new DemandFacetResponse(facetName, facetResults);
    }

    private static DemandRetrievalQuery constructDemandRetrievalQuery(
            DemandQueryType queryType, String queryTerm, String lang, String companyId, String categoryUri, String dueDate, String buyerCountry, String deliveryCountry,List<String> circularEconomyCertificates, List<String> otherCertificates
    ) {
        StringBuilder query = new StringBuilder("SELECT");
        if (queryType == DemandQueryType.HJID) {
            query.append(" d.hjid");
        } else if (queryType == DemandQueryType.COUNT) {
            query.append(" COUNT(d.hjid)");
        } else if (queryType == DemandQueryType.BUYER_COUNTRY) {
            query.append(" c3.value_, COUNT(c3.value_)");
        } else if (queryType == DemandQueryType.DELIVERY_COUNTRY) {
            query.append(" c2.value_, COUNT(c2.value_)");
        } else if (queryType == DemandQueryType.CATEGORY) {
            query.append(" c4.uri, COUNT(c4.uri)");
        } else if (queryType == DemandQueryType.OTHER_CERTIFICATES){
            query.append(" c6.name_, COUNT(c6.name_)");
        } else if(queryType == DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES ){
            query.append(" c8.name_, COUNT(c8.name_)");
        }
        query.append(" FROM demand_type d");

        if (companyId != null) {
            // join metadata and owner company tables
            query.append(", metadata_type m, metadata_type_owner_company__0 o");
        } else {
            // if company id is not queried, join the metadata table just for id queries to be used in sorting
            if (queryType == DemandQueryType.HJID) {
                query.append(", metadata_type m");
            }
        }
        if (categoryUri != null) {
            // join code table
            query.append(", code_type c");
        }
        if (deliveryCountry != null || queryType == DemandQueryType.DELIVERY_COUNTRY) {
            // join code table
            query.append(", code_type c2");
        }
        if (buyerCountry != null || queryType == DemandQueryType.BUYER_COUNTRY) {
            // join code table
            query.append(", code_type c3");
        }
        if (otherCertificates != null || queryType == DemandQueryType.OTHER_CERTIFICATES) {
            // join code table
            query.append(", certificate_type c5, code_type c6");
        }
        if (circularEconomyCertificates != null || queryType == DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES) {
            // join code table
            query.append(", certificate_type c7, code_type c8");
        }

        // join with an other instance of code_type table to get all categories of the matching demands
        if (queryType == DemandQueryType.CATEGORY) {
            query.append(", code_type c4");
        }

        // ts_query query
        if (queryTerm != null) {
            if (lang == null) {
                lang = "simple";
            } else {
                lang = postgreTsVectorLanguageMap.get(lang);
                if (lang == null) {
                    lang = "simple";
                }
            }
            // construct ts query for query terms with multiple words
            if (queryTerm.contains(" ")) {
                queryTerm = String.join("|", queryTerm.split(" "));
            }
            query.append(", to_tsquery('").append(lang).append("','").append(queryTerm).append("') query");
        }

        // where statement
        if (queryTerm != null || companyId != null || categoryUri != null || dueDate != null || buyerCountry != null || deliveryCountry != null || otherCertificates != null || circularEconomyCertificates != null) {
            query.append(" WHERE");
        } else {
            if (queryType == DemandQueryType.HJID || queryType == DemandQueryType.CATEGORY || queryType == DemandQueryType.DELIVERY_COUNTRY || queryType == DemandQueryType.BUYER_COUNTRY || queryType == DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES
            || queryType == DemandQueryType.OTHER_CERTIFICATES) {
                query.append(" WHERE");
            }
        }
        boolean previousCriteria = false;
        List<String> queryParameters = new ArrayList<>();
        List<Object> queryValues = new ArrayList<>();

        if (companyId != null) {
            query.append(" m.hjid = d.metadata_demand_type_hjid").append(" AND");
            query.append(" m.hjid = o.owner_company_items_metadata_0").append(" AND");
            query.append(" o.item = :companyId");
            queryParameters.add("companyId");
            queryValues.add(companyId);
            previousCriteria = true;
        } else {
            if (queryType == DemandQueryType.HJID) {
                query.append(" m.hjid = d.metadata_demand_type_hjid");
                previousCriteria = true;
            }
        }

        if (categoryUri != null ) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.hjid = c.item_classification_code_dem_0").append(" AND");
            query.append(" c.uri = :categoryUri");
            queryParameters.add("categoryUri");
            queryValues.add(categoryUri);
        }

        if (queryType == DemandQueryType.CATEGORY) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.hjid = c4.item_classification_code_dem_0");
        }

        if (dueDate != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.due_date_item >= TO_DATE(:dueDate, 'YYYY-MM-DD')");
            queryParameters.add("dueDate");
            queryValues.add(dueDate);
        }

        if (deliveryCountry != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.delivery_country_demand_type_0 = c2.hjid").append(" AND");
            query.append(" c2.value_ = :deliveryCountry");
            queryParameters.add("deliveryCountry");
            queryValues.add(deliveryCountry);
        } else {
            if (queryType == DemandQueryType.DELIVERY_COUNTRY) {
                if (previousCriteria) {
                    query.append(" AND");
                }
                previousCriteria = true;
                query.append(" d.delivery_country_demand_type_0 = c2.hjid");
            }
        }

        if (buyerCountry != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.buyer_country_demand_type_hj_0 = c3.hjid").append(" AND");
            query.append(" c3.value_ = :buyerCountry");
            queryParameters.add("buyerCountry");
            queryValues.add(buyerCountry);
        } else {
            if (queryType == DemandQueryType.BUYER_COUNTRY) {
                if (previousCriteria) {
                    query.append(" AND");
                }
                previousCriteria = true;
                query.append(" d.buyer_country_demand_type_hj_0 = c3.hjid");
            }
        }

        if (circularEconomyCertificates != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.hjid = c7.certificate_demand_type_hjid").append(" AND");
            query.append(" c8.hjid = c7.certificate_type_code_certif_0").append(" AND");
            query.append(" c7.certificate_type = 'Circular Economy (Environment / Sustainability)'").append(" AND");
            query.append(" c8.name_ in :circularEconomyCertificates");
            queryParameters.add("circularEconomyCertificates");
            queryValues.add(circularEconomyCertificates);
        } else {
            if (queryType == DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES) {
                if (previousCriteria) {
                    query.append(" AND");
                }
                previousCriteria = true;
                query.append(" d.hjid = c7.certificate_demand_type_hjid").append(" AND");
                query.append(" c8.hjid = c7.certificate_type_code_certif_0").append(" AND");
                query.append(" c7.certificate_type = 'Circular Economy (Environment / Sustainability)'");
            }
        }

        if (otherCertificates != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            previousCriteria = true;
            query.append(" d.hjid = c5.certificate_demand_type_hjid").append(" AND");
            query.append(" c6.hjid = c5.certificate_type_code_certif_0").append(" AND");
            query.append(" c5.certificate_type != 'Circular Economy (Environment / Sustainability)'").append(" AND");
            query.append(" c6.name_ in :otherCertificates");
            queryParameters.add("otherCertificates");
            queryValues.add(otherCertificates);
        } else {
            if (queryType == DemandQueryType.OTHER_CERTIFICATES) {
                if (previousCriteria) {
                    query.append(" AND");
                }
                previousCriteria = true;
                query.append(" d.hjid = c5.certificate_demand_type_hjid").append(" AND");
                query.append(" c6.hjid = c5.certificate_type_code_certif_0").append(" AND");
                query.append(" c5.certificate_type != 'Circular Economy (Environment / Sustainability)'");
            }
        }

        if (queryTerm != null) {
            if (previousCriteria) {
                query.append(" AND");
            }
            query.append(" search_index @@ query");
        }

        // sorting is enabled if the query gets demand ids
        if (queryType.equals(DemandQueryType.HJID)) {
            if (queryTerm != null) {
                query.append(" ORDER BY ts_rank_cd(search_index, query) DESC, m.creation_date_item DESC");
            } else {
                query.append(" ORDER BY m.creation_date_item DESC");
            }
        } else if (queryType == DemandQueryType.DELIVERY_COUNTRY) {
            query.append(" GROUP BY c2.value_");
        } else if (queryType == DemandQueryType.BUYER_COUNTRY) {
            query.append(" GROUP BY c3.value_");
        } else if (queryType == DemandQueryType.CATEGORY) {
            query.append(" GROUP BY c4.uri");
        } else if (queryType == DemandQueryType.OTHER_CERTIFICATES)  {
            query.append(" GROUP BY c6.name_");
        } else if (queryType == DemandQueryType.CIRCULAR_ECONOMY_CERTIFICATES) {
            query.append(" GROUP BY c8.name_");
        }

        DemandRetrievalQuery result = new DemandRetrievalQuery();
        result.query = query.toString();
        result.queryParameters = queryParameters;
        result.queryValues = queryValues;
        return result;
    }

    private static class DemandRetrievalQuery {
        String query;
        List<String> queryParameters;
        List<Object> queryValues;
    }
}

enum DemandQueryType {
    COUNT,
    HJID,
    CATEGORY,
    BUYER_COUNTRY,
    OTHER_CERTIFICATES,
    CIRCULAR_ECONOMY_CERTIFICATES,
    DELIVERY_COUNTRY
}
