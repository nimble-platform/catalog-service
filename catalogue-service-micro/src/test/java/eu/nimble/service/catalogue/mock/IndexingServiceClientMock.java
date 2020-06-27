package eu.nimble.service.catalogue.mock;

import eu.nimble.common.rest.indexing.IIndexingServiceClient;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@Profile("test")
@Component
public class IndexingServiceClientMock implements IIndexingServiceClient {
    private static Logger log = LoggerFactory.getLogger(IndexingServiceClientMock.class);

    public Response setParty(@RequestHeader("Authorization") String bearerToken, @RequestBody String party){
        return null;
    }

    public Response removeParty(@RequestHeader("Authorization") String bearerToken, @RequestParam(value = "uri") String partyId){
        return null;
    }

    public Response setClass(@RequestHeader("Authorization") String bearerToken, @RequestBody String prop){
        return null;
    }

    public Response searchClass(@RequestHeader("Authorization") String bearerToken, @RequestBody String search){
        String response = null;
        try {
            response = getSearchClassResponse(search);
        } catch (IOException e) {
            String msg = String.format("Failed to get search class response for query: %s",search);
            log.error(msg,e);
            return null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response, Charset.defaultCharset()).build();
    }

    public Response selectClass(@RequestHeader("Authorization") String bearerToken, @RequestParam(value = "rows") String rows, @RequestParam("q") String q, @RequestParam(value = "fq",required = false) Set<String> fq){
        String response = null;
        try {
            response = getSelectClassResponse(q);
        } catch (IOException e) {
            String msg = String.format("Failed to select class for query: %s",q);
            log.error(msg,e);
            return null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response, Charset.defaultCharset()).build();
    }

    public Response postCatalogue(@RequestHeader("Authorization") String bearerToken, @RequestParam(value = "catalogueId") String catalogueId, @RequestBody String catalogueItems){
        return null;
    }

    public Response deleteCatalogue(@RequestHeader("Authorization") String bearerToken, @RequestParam("catalogueId") String catalogueId){
        return null;
    }

    public Response setItem(@RequestHeader("Authorization") String bearerToken, @RequestBody String prop){
        return null;
    }

    public Response deleteItem(@RequestHeader("Authorization") String bearerToken, @RequestParam("uri") String uri){
        return null;
    }

    @Override
    public Response clearItemIndex(String s) {
        return null;
    }

    public Response setProperty(@RequestHeader("Authorization") String bearerToken, @RequestBody String prop){
        return null;
    }

    public Response getProperties(@RequestHeader("Authorization") String bearerToken,@RequestParam(value = "uri",required = false) Set<String> uris, @RequestParam(value = "class",required = false) Set<String> classUris){
        String uri = new ArrayList<>(classUris).get(0);
        String response = null;
        try {
            response = getPropertiesResponse(uri);
        } catch (IOException e) {
            String msg = String.format("Failed to get properties for class uri: %s",uri);
            log.error(msg,e);
            return null;
        }
        return Response.builder().headers(new HashMap<>()).status(HttpStatus.OK.value()).body(response, Charset.defaultCharset()).build();
    }

    public Response searchItem(String s, String s1) {
        return null;
    }

    private String getSearchClassResponse(String query) throws IOException {
        if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AAC168#005\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-AAC168#005.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-BAA975#013\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-BAA975#013.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AKJ050#013\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-AKJ050#013.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-ACH237#011\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-ACH237#011.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AAA647#005\\\" OR id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AKJ049#008\\\" OR id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AJZ801#008\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_three_uris.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AAA647#005\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-AAA647#005.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AJZ801#008\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-AJZ801#008.json"));
        }
        else if (query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AKJ049#008\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_0173-1#01-AKJ049#008.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AAA647#005\\\" OR id:\\\"http://www.nimble-project.org/resource/eclass#0173-1#01-AJZ801#008\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_two_uris.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.aidimme.es/FurnitureSectorOntology.owl#RoadTransportService\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_road_transport_service.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.aidimme.es/FurnitureSectorOntology.owl#TransportService\\\" OR id:\\\"http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsService\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_transport_logistics_service.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.aidimme.es/FurnitureSectorOntology.owl#LogisticsService\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_logistics_service.json"));
        }
        else if(query.contentEquals("{\"rows\":2147483647,\"start\":0,\"q\":\"id:\\\"http://www.aidimme.es/FurnitureSectorOntology.owl#TransportService\\\" \"}")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/search_class_transport_service.json"));
        }
        return null;
    }

    private String getSelectClassResponse(String query) throws IOException {
        if(query.contentEquals("_text_:die AND((nameSpace:\"http://www.nimble-project.org/resource/eclass#\" AND level:4))")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/select_class_die.json"));
        }
        else if(query.contentEquals("_text_:warehouse AND((nameSpace:\"http://www.nimble-project.org/resource/eclass#\" AND level:4 AND code:14*))")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/select_class_warehouse.json"));
        }
        else if(query.contentEquals("_text_:mdf AND((nameSpace:\"http://www.nimble-project.org/resource/eclass#\" AND level:4 AND code:14*))")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/select_class_mdf.json"));
        }
        return null;
    }

    private String getPropertiesResponse(String uri) throws IOException {
        if(uri.contentEquals("http://www.nimble-project.org/resource/eclass#0173-1#01-BAA975#013")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/get_properties_0173-1#01-BAA975#013.json"));
        }
        else if(uri.contentEquals("http://www.nimble-project.org/resource/eclass#0173-1#01-AKJ050#013")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/get_properties_0173-1#01-AKJ050#013.json"));
        }
        else if(uri.contentEquals("http://www.nimble-project.org/resource/eclass#0173-1#01-ACH237#011")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/get_properties_0173-1#01-ACH237#011.json"));
        }
        else if(uri.contentEquals("http://www.aidimme.es/FurnitureSectorOntology.owl#RoadTransportService")){
            return IOUtils.toString(IndexingServiceClientMock.class.getResourceAsStream("/mock/indexing-service/get_properties_road_tranport_service.json"));
        }
        return null;
    }
}
