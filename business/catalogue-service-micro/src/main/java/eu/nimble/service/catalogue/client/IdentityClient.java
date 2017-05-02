package eu.nimble.service.catalogue.client;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by Johannes Innerbichler on 02/05/17.
 * Feign client for identity microservice.
 */
@FeignClient(name = "identity-service", url = "localhost:9096", fallback = IdentityClientFallback.class)  // for debuging
//@FeignClient(name = "identity-service") // use this declaration for integration to MS infrastructure
public interface IdentityClient {
    @RequestMapping(method = RequestMethod.GET, value = "/party/{partyId}", produces = "application/json")
    PartyType getParty(@PathVariable("partyId") String storeId);
}

/**
 * Fallback if identity service is not reachable.
 */
@Component
class IdentityClientFallback implements IdentityClient {
    @Override
    public PartyType getParty(String storeId) {
        return null;
    }
}