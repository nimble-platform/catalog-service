package eu.nimble.service.catalogue.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by suat on 25-Jan-19.
 */
@Configuration
//@EnableSolrRepositories(basePackages = {
//        "eu.nimble.indexing.repository"
//})
public class SolrClientConfig {

    @Value("${nimble.indexing.solr.url}")
    private String solrUrl;

    @Bean
    public SolrClient solrClient() {
        return new HttpSolrClient(solrUrl);
    }
}
