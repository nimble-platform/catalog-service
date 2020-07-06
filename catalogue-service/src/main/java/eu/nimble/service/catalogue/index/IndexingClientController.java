package eu.nimble.service.catalogue.index;

import eu.nimble.common.rest.indexing.IIndexingServiceClient;
import eu.nimble.common.rest.indexing.IIndexingServiceClientFallback;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.hystrix.HystrixFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@PropertySource("classpath:bootstrap.yml")
public class IndexingClientController {

    private IIndexingServiceClient nimbleIndexClient;

    private IIndexingServiceClient federatedIndexClient;

    private List<IIndexingServiceClient> clients;

    @Value("${nimble.indexing.url}")
    private String nimbleIndexUrl;

    @Value("${federated-index-enabled}")
    private boolean federatedIndexEnabled;

    @Value("${nimble.indexing.federated-index-url}")
    private String federatedIndexUrl;


    @Autowired
    IIndexingServiceClientFallback indexingFallback;

    public IndexingClientController() {

    }

    public IIndexingServiceClient getNimbleIndexClient() {
        if (nimbleIndexClient == null) {
            nimbleIndexClient = createIndexingClient(nimbleIndexUrl);
        }
        return nimbleIndexClient;
    }


    public IIndexingServiceClient getFederatedIndexClient() {
        if (federatedIndexEnabled && federatedIndexClient == null) {
            federatedIndexClient = createIndexingClient(federatedIndexUrl);
        }
        return federatedIndexClient;
    }

    public List<IIndexingServiceClient> getClients() {
        if (clients == null) {
            clients = new ArrayList<IIndexingServiceClient>();
            clients.add(getNimbleIndexClient());
            if (federatedIndexEnabled) {
                clients.add(getFederatedIndexClient());
            }
        }
        return clients;
    }

    private IIndexingServiceClient createIndexingClient(String url) {
        return HystrixFeign.builder().contract(new SpringMvcContract())
                .encoder(new Encoder.Default())
                .decoder(new Decoder.Default())
                .retryer(new Retryer.Default(1,100,3))
                .target(IIndexingServiceClient.class, url, indexingFallback);
    }

}
