package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mchange.v1.util.SimpleMapEntry;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Handler;

/**
 * Created by suat on 04-Jun-18.
 */
public class MigrationUtil {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MigrationUtil.class);
    private static final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmZjE0MzE0ZS02MGVlLTQ3NjgtYTYzZS03OGZmOTQzOWZjZGMiLCJleHAiOjE1MzczNDA4MTMsIm5iZiI6MCwiaWF0IjoxNTM3MzM3MjEzLCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiZGUwNWNkODAtMzJmYy00NmNjLWE3ZjgtZDQyYzU1YTIxYjExIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6Ijk1ODEyMzczLTdmNTAtNGVlYS1iZjU4LWRiNzk5MWNiY2E2NSIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbHAgY2VuayIsInByZWZlcnJlZF91c2VybmFtZSI6ImFscEBnbWFpbC5jb20iLCJnaXZlbl9uYW1lIjoiYWxwIiwiZmFtaWx5X25hbWUiOiJjZW5rIiwiZW1haWwiOiJhbHBAZ21haWwuY29tIn0.dSldcfxcPvO4eU2ntSniQbPKVRPyc6c9ls9iXA38fZSK9AEtwN72BupF9NYh5mwRUQYV7R5yHtSAWosIxOKIiD9xQ0fxrYF38OAQXFenqEe7j8HWF92qhK2l1NSMXPNJdHt33h8fNBZ_hbyB5bI_kToczOg3nikdxu8fjellcg023lPzEMtseQyHkuLCYCwgKF1IjRD5cUuRZyurs6V2HyFP7l-BvgIXHt_CwxnZ0-W6gjSx2N0PBRuKGzN68Ivx2wWguPwF1m1Q1n2H5ckAcbkY-gy6L43q2_bTM-pFMj2HbWkeOqiKMlyHCOsNpUAAvSEkZ4yjDy0k9jzlnpaDTQ";
    private static final String environment = "production";

    // connection parameters for 'Staging'
    private static String url;
    private static String username;
    private static String password;

    public static void main(String[] args) throws SQLException, InterruptedException, ClassNotFoundException {
        //printTablesWithValues(identityDbTables);
        //dropEmptyTables();
        //printDifferences();
        //printNumericDifferences();
        //moveData();
        //removeUnusedColumns();
    }

    private static void mergePartiesIntoSameInstance(HibernateUtility hibernateUtility) throws Exception{
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection(url,username, password);
            logger.info("Connection obtained");
            stmt = c.createStatement();

            // get references
            List<String> queries = new ArrayList<>();
            String sql = "SELECT tc.table_name, kcu.column_name\n" +
                    "AS foreign_table_name, ccu.column_name AS foreign_column_name\n" +
                    "FROM information_schema.table_constraints tc\n" +
                    "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name\n" +
                    "JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name\n" +
                    "WHERE constraint_type = 'FOREIGN KEY'\n" +
                    "AND ccu.table_name='party_type';";
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()){
                String tableName = resultSet.getString(1);
                String columnName = resultSet.getString(2);
                queries.add("update "+tableName+" set "+columnName+" = newValueHere where "+columnName+" in (hjidsQuery)");

            }
            logger.info("Obtained table metadata");

            // get party ids
            List<String> ids = new ArrayList<>();
            String query = "select distinct party_type.id  from party_type where party_type.id IS NOT NULL";
            resultSet = stmt.executeQuery(query);
            while (resultSet.next()){
                if(!resultSet.getString(1).equals("compID")){
                    ids.add(resultSet.getString(1));
                }
            }
            logger.info("Obtained party ids");

            String stringIds = ids.toString().replace("[","").replace("]","").replace(" ","");

            HttpResponse<JsonNode> response = Unirest.get(getIdentityServiceUrl() + "parties/"+stringIds)
                    .header("Authorization", token).asJson();

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            List<PartyType> parties = objectMapper.readValue(response.getBody().toString(),new TypeReference<List<PartyType>>(){});
            logger.info("Obtained parties");
            Map<String,String> map = new HashMap<>();
            for (PartyType partyType: parties){
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(partyType));
                JsonSerializationUtility.removeHjidFields(object);
                partyType = objectMapper.readValue(object.toString(), PartyType.class);
                HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(partyType);
                map.put(partyType.getPartyIdentification().get(0).getID(),partyType.getHjid().toString());
                logger.info("Persisted party with id: "+ partyType.getPartyIdentification().get(0).getID() + " with hjid: "+partyType.getHjid());
            }
            // traverse the map
            for (Map.Entry<String, String> entry : map.entrySet())
            {
                String hjidsQuery = "select party_type.hjid from party_type where party_type.id = '"+entry.getKey()+"'";
                for (String q:queries){
                    q = q.replace("newValueHere",entry.getValue()).replace("hjidsQuery",hjidsQuery);
                    stmt.executeUpdate(q);
                }
                logger.info("Updated references for company: "+ entry.getKey());
            }

            // delete other parties
            for(Map.Entry<String, String> entry : map.entrySet()){
                String hjidsQuery = "select party_type.hjid from party_type where party_type.id = '"+entry.getKey()+"' AND party_type.hjid != "+entry.getValue()+"";
                String deleteQuery = "delete from party_type where party_type.hjid in ("+hjidsQuery+")";
                stmt.executeUpdate(deleteQuery);
                logger.info("Deleted party with id: "+ entry.getKey());
            }

            String hjidsQuery = "select party_type.hjid from party_type where party_type.id = 'compID'";
            // delete party with id compID
            for(Map.Entry<String, String> entry : map.entrySet()){
                String deleteQuery = "delete from party_type where party_type.hjid in ("+hjidsQuery+")";
                stmt.executeUpdate(deleteQuery);
            }

            stmt.close();
            c.close();
        } catch ( Exception e ) {
            logger.error("",e);
            System.exit(0);
        } finally {
            try {
                if(stmt != null) {

                    stmt.close();
                }
                if(c != null) {
                    c.close();
                }
            }
            catch (Exception e){
                logger.error("",e);
            }

        }
    }

    private Map getConfigs() {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            PropertySource<?> applicationYamlPropertySource = loader.load(
                    "properties", new ClassPathResource("releases/r9/r9migration.yml"), null);

            Map map = ((MapPropertySource) applicationYamlPropertySource).getSource();

            String url = (String) map.get("hibernate.connection.url");
            url = url.replace("${DB_HOST}",System.getenv("DB_HOST")).replace("${DB_PORT}",System.getenv("DB_PORT"))
                .replace("${DB_DATABASE}",System.getenv("DB_DATABASE"));

            // set staging parameters
            MigrationUtil.url = url;
            MigrationUtil.username = System.getenv("DB_USERNAME");
            MigrationUtil.password = System.getenv("DB_PASSWORD");
            //

            map.put("hibernate.connection.url",url);
            map.put("hibernate.connection.username",MigrationUtil.username);
            map.put("hibernate.connection.password",MigrationUtil.password);


            return map;

        } catch (IOException e) {
            logger.error("", e);
            throw new RuntimeException();
        }
    }

    private static String getIdentityServiceUrl() {
        if(environment.contentEquals("production")) {
            return "https://nimble-platform.salzburgresearch.at/nimble/identity/";
        } else if(environment.contentEquals("staging")) {
            return "http://nimble-staging.salzburgresearch.at/identity/";
        }
        throw new RuntimeException();
    }
}
