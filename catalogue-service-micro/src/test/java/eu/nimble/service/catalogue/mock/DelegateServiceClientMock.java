package eu.nimble.service.catalogue.mock;

import eu.nimble.common.rest.delegate.IDelegateClient;
import feign.Response;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

@Profile("test")
@Component
public class DelegateServiceClientMock implements IDelegateClient {

    @Override
    public Response addFederatedMetadataToCollaborationGroup(String bearerToken, String federationId, String documentId, String body, String partyId, String delegateId) {
        return null;
    }

    @Override
    public Response getGroupIdTuple(String bearerToken, String federationId, String documentId,String partyId,String delegateId) {
        return null;
    }

    @Override
    public Response getOrderDocument(String bearerToken, String processInstanceId, String orderResponseId, String delegateId) {
        return null;
    }

    @Override
    public Response getParty(String bearerToken,Long partyId, boolean includeRoles,String delegateId) {
        return null;
    }

    @Override
    public Response getParty(String bearerToken, String partyIds, boolean includeRoles, List<String> delegateIds) {
        return null;
    }

    @Override
    public Response getPerson(String bearerToken, String personId, String delegateId) {
        return null;
    }

    @Override
    public Response getPersonViaToken(String s, String s1, String s2) {
        return null;
    }

    @Override
    public Response getPartyByPersonID(String s, String s1, String s2) {
        return null;
    }

    @Override
    public Response getCollaborationGroup(String bearerToken,String id, String delegateId) {
        return null;
    }

    @Override
    public Response unMergeCollaborationGroup(String bearerToken, String groupId, String delegateId) {
        return null;
    }

    @Override
    public Response getCatalogLineByHjid(String bearerToken,Long hjid) {
        return null;
    }

    @Override
    public Response getExpectedOrders(String bearerToken,Boolean forAll, List<String> unShippedOrderIds) {
        return null;
    }

    @Override
    public Response getFederationId() {
        return Response.builder().headers(new HashMap<>()).status(200).body("TEST_INSTANCE",Charset.defaultCharset()).build();
    }

}