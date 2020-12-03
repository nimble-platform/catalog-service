package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.persistence.util.DemandPersistenceUtil;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DemandType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import io.swagger.models.auth.In;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<DemandType> searchDemand(String queryTerm, String lang, Integer pageNo, Integer limit) {
        if (lang == null) {
            lang = "simple";
        } else {
            lang = postgreTsVectorLanguageMap.get(lang);
            if (lang == null) {
                lang = "simple";
            }
        }
        String query = String.format(QUERY_SELECT_DEMANDS, lang, queryTerm);
        List<BigInteger> results = new JPARepositoryFactory().forCatalogueRepository().getEntities(query, null, null, limit, pageNo * limit, true);
        List<DemandType> demands = DemandPersistenceUtil.getDemandsForHjids(results.stream().map(bi -> bi.longValue()).collect(Collectors.toList()));
        return demands;
    }
}
