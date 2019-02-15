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
        MigrationUtil script = new MigrationUtil();
        try{
            HibernateUtility hu = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME, script.getConfigs());

            migrateTextTypes(hu);

            System.exit(0);
        }
        catch (Exception e){
            logger.error("Failure", e);
        }
        //MarmottaSynchronizer sync = new MarmottaSynchronizer();
        /*sync.createStatusTable();
        String uuid = "catUuid";
        sync.addRecord(SyncStatus.UPDATE, uuid);
        List<SyncStatusRecord> records = sync.getStatusRecords();
        System.out.println(records.toString());
        sync.deleteStatusRecords(uuid);
        records = sync.getStatusRecords();
        System.out.println(records);
        System.out.println("done");*/

        //printTablesWithValues(identityDbTables);
        //dropEmptyTables();
        //printDifferences();
        //printNumericDifferences();
        //moveData();
        //removeUnusedColumns();
    }

    private static void handleParties(HibernateUtility hibernateUtility) throws Exception{
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

    private static void migrateTextTypes(HibernateUtility hibernateUtility){
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection(url,username, password);
            logger.info("Connection obtained");
            stmt = c.createStatement();

            // Handle CatalogueType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__catalogue_type_hjid","catalogue_type");
            logger.info("Handled CatalogueType name");
            // Handle CommunicationType value
            handleSingleTextType(hibernateUtility,stmt,"value_","value__communication_type_hj_0","communication_type");
            logger.info("Handled CommunicationType value");
            // Handle ContactType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__contact_type_hjid","contact_type");
            logger.info("Handled ContactType name");
            // Handle CountryType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__country_type_hjid","country_type");
            logger.info("Handled CountryType name");
            // Handle EventType description
            handleSingleTextType(hibernateUtility,stmt,"description","description_event_type_hjid","event_type");
            logger.info("Handled EventType description");
            // Handle TransportationServiceType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__transportation_service_0","transportation_service_type");
            logger.info("Handled TranportationServiceType name");
            // Handle ClassificationCategoryType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__classification_categor_0","classification_category_type");
            logger.info("Handled ClassificationCategoryType name");
            // Handle ClassificationSchemeType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__classification_scheme__0","classification_scheme_type");
            logger.info("Handled ClassificationSchemeType name");
            // Handle PriceOptionType name
            handleSingleTextType(hibernateUtility,stmt,"name_","name__price_option_type_hjid","price_option_type");
            logger.info("Handled PriceOptionType name");
            // Handle PriceOptionType special terms
            handleSingleTextType(hibernateUtility,stmt,"special_terms","special_terms_price_option_t_0","price_option_type");
            logger.info("Handled PriceOptionType special terms");


            // Handle CapabilityType description
            handleTextTypeList(hibernateUtility,stmt,"description_items_capability_0","capability_type_description__0","description_capability_type__0");
            logger.info("Handled Capability description");
            // Handle CompletedTaskType description
            handleTextTypeList(hibernateUtility,stmt,"description_items_completed__0","completed_task_type_descript_0","description_completed_task_t_0");
            logger.info("Handled CompletedTaskType description");
            // Handle Declaration description
            handleTextTypeList(hibernateUtility,stmt,"description_items_declaratio_0","declaration_type_description_0","description_declaration_type_0");
            logger.info("Handled Declaration description");
            // Handle ClassificationCategoryType description
            handleTextTypeList(hibernateUtility,stmt,"description_items_classifica_1","classification_category_type_0","description_classification_s_0");
            logger.info("Handled ClassificationCategoryType description");
            // Handle ClassificationSchemeType description
            handleTextTypeList(hibernateUtility,stmt,"description_items_classifica_0","classification_scheme_type_d_0","description_classification_s_0");
            logger.info("Handled ClassificationSchemeType description");
            // Handle ItemProperty value
            handleTextTypeList(hibernateUtility,stmt,"value_items_item_property_ty_0","item_property_type_value_item","value__item_property_type_hj_0");
            logger.info("Handled ItemProperty value");
            // Handle TradingTermType value
            handleTextTypeList(hibernateUtility,stmt,"value_items_trading_term_typ_0","trading_term_type_value_item","value__trading_term_type_hjid");
            logger.info("Handled TradingTermType value");


            // Handle TradingTermType description
            handleStringToTextTypeList(hibernateUtility,stmt,"description","trading_term_type","description_trading_term_typ_0");
            logger.info("Handled TradingTermType description");
            // Handle DeliveryTermsType special terms
            handleStringToTextTypeList(hibernateUtility,stmt,"special_terms","delivery_terms_type","special_terms_delivery_terms_0");
            logger.info("Handled DeliveryTermsType special terms");
            // Handle ItemPropertyType name
            handleStringToTextTypeList(hibernateUtility,stmt,"name_","item_property_type","name__item_property_type_hjid");
            logger.info("Handled ItemPropertyType name");
            // Handle PaymentMeansType instruction note
            handleStringToTextTypeList(hibernateUtility,stmt,"instruction_note","payment_means_type","instruction_note_payment_mea_0");
            logger.info("Handled PaymentMeansType instruction note");
            // Handle ShipmentType handling instructions
            handleStringToTextTypeList(hibernateUtility,stmt,"handling_instructions","shipment_type","handling_instructions_shipme_0");
            logger.info("Handled ShipmentType handling instructions");
            // Handle ItemType description
            handleStringToTextTypeList(hibernateUtility,stmt,"description","item_type","description_item_type_hjid");
            logger.info("Handled ItemType description");
            // Handle ItemType name
            handleStringToTextTypeList(hibernateUtility,stmt,"name_","item_type","name__item_type_hjid");
            logger.info("Handled ItemType name");
            // Handle DeclarationType name
            handleStringToTextTypeList(hibernateUtility,stmt,"name_","declaration_type","name__declaration_type_hjid");
            logger.info("Handled DeclarationType name");

            // Handle Party id and name
            String query = "select hjid,name_,id from party_type";

            ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()){
                Long hjid = resultSet.getLong(1);
                String name = resultSet.getString(2);
                String id = resultSet.getString(3);

                // get the party
                PartyType partyType =  hibernateUtility.load("FROM PartyType WHERE hjid = ?",hjid);
                // party name
                PartyNameType partyNameType = new PartyNameType();
                TextType textType = new TextType();
                textType.setValue(name);
                textType.setLanguageID("en");
                partyNameType.setName(textType);
                partyType.setPartyName(Arrays.asList(partyNameType));
                // party id
                PartyIdentificationType partyIdentificationType = new PartyIdentificationType();
                partyIdentificationType.setID(id);
                partyType.setPartyIdentification(Arrays.asList(partyIdentificationType));

                hibernateUtility.update(partyType);

                logger.info("Handled party with id: {}",id);
            }

            stmt.executeUpdate("ALTER TABLE catalogue_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE communication_type DROP COLUMN IF EXISTS value_");
            stmt.executeUpdate("ALTER TABLE contact_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE country_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE event_type DROP COLUMN IF EXISTS description");
            stmt.executeUpdate("ALTER TABLE transportation_service_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE classification_category_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE classification_scheme_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE price_option_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE price_option_type DROP COLUMN IF EXISTS special_terms");

            stmt.executeUpdate("ALTER TABLE capability_type_description__0 DROP COLUMN IF EXISTS description_items_capability_0");
            stmt.executeUpdate("ALTER TABLE completed_task_type_descript_0 DROP COLUMN IF EXISTS description_items_completed__0");
            stmt.executeUpdate("ALTER TABLE declaration_type_description_0 DROP COLUMN IF EXISTS description_items_declaratio_0");
            stmt.executeUpdate("ALTER TABLE classification_category_type_0 DROP COLUMN IF EXISTS description_items_classifica_1");
            stmt.executeUpdate("ALTER TABLE classification_scheme_type_d_0 DROP COLUMN IF EXISTS description_items_classifica_0");
            stmt.executeUpdate("ALTER TABLE item_property_type_value_item DROP COLUMN IF EXISTS value_items_item_property_ty_0");
            stmt.executeUpdate("ALTER TABLE trading_term_type_value_item DROP COLUMN IF EXISTS value_items_trading_term_typ_0");

            stmt.executeUpdate("ALTER TABLE trading_term_type  DROP COLUMN IF EXISTS description");
            stmt.executeUpdate("ALTER TABLE delivery_terms_type  DROP COLUMN IF EXISTS special_terms");
            stmt.executeUpdate("ALTER TABLE item_property_type  DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE payment_means_type  DROP COLUMN IF EXISTS instruction_note");
            stmt.executeUpdate("ALTER TABLE shipment_type  DROP COLUMN IF EXISTS handling_instructions");
            stmt.executeUpdate("ALTER TABLE item_type  DROP COLUMN IF EXISTS description");
            stmt.executeUpdate("ALTER TABLE item_type  DROP COLUMN IF EXISTS name_");

            stmt.executeUpdate("ALTER TABLE declaration_type  DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE party_type DROP COLUMN IF EXISTS name_");
            stmt.executeUpdate("ALTER TABLE party_type DROP COLUMN IF EXISTS id");
        }
        catch (Exception e){
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

    // List<String> to List<TextType>
    private static void handleTextTypeList(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String tableName,String referenceCol) throws Exception{
        Map<String,Long> map = new HashMap<>();
        String query = "select item,"+oldCol+" from " + tableName;

        ResultSet resultSet = stmt.executeQuery(query);
        while (resultSet.next()){
            String value = resultSet.getString(1);
            Long reference = resultSet.getLong(2);
            map.put(value,reference);
        }

        for(Map.Entry<String,Long> entry : map.entrySet()){
            TextType textType = new TextType();
            textType.setLanguageID("en");
            textType.setValue(entry.getKey());
            // persist TextType object
            hibernateUtility.persist(textType);

            query = "UPDATE text_type SET "+referenceCol+" = " + entry.getValue() + " WHERE hjid = " + textType.getHjid();
            stmt.executeUpdate(query);
        }
    }

    // String to List<TextType>
    private static void handleStringToTextTypeList(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String tableName,String referenceCol) throws Exception{
        Map<Long,String> map = new HashMap<>();
        String query = "select hjid,"+oldCol+" from " + tableName;

        ResultSet resultSet = stmt.executeQuery(query);
        while (resultSet.next()){
            Long hjid = resultSet.getLong(1);
            String name = resultSet.getString(2);
            map.put(hjid,name);
        }

        for(Map.Entry<Long,String> entry : map.entrySet()){
            TextType textType = new TextType();
            textType.setLanguageID("en");
            textType.setValue(entry.getValue());
            // persist TextType object
            hibernateUtility.persist(textType);

            query = "UPDATE text_type SET "+referenceCol+" = " + entry.getKey() + " WHERE hjid = " + textType.getHjid();
            stmt.executeUpdate(query);
        }
    }

    // String to TextType
    private static void handleSingleTextType(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String newCol,String tableName) throws Exception{
        Map<Long,String> map = new HashMap<>();
        String query = "select hjid,"+oldCol+" from " + tableName;

        ResultSet resultSet = stmt.executeQuery(query);
        while (resultSet.next()){
            Long hjid = resultSet.getLong(1);
            String name = resultSet.getString(2);
            map.put(hjid,name);
        }

        for(Map.Entry<Long,String> entry : map.entrySet()){
            TextType textType = new TextType();
            textType.setLanguageID("en");
            textType.setValue(entry.getValue());
            // persist TextType object
            hibernateUtility.persist(textType);

            query = "UPDATE "+tableName+" SET "+newCol+" = " + textType.getHjid() + " WHERE hjid = " + entry.getKey();
            stmt.executeUpdate(query);
        }
    }

    private static void moveData() throws ClassNotFoundException, SQLException {
        Connection connection2 = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");

        String selectValues = "SELECT hjid, %s from %s";
        String update = "UPDATE %s SET %s = %s WHERE hjid = %s";
        String initializedSelect;

        try {
            Statement s = connection2.createStatement();
            // dimension type
            initializedSelect = String.format(selectValues, "measurement_dimension_transp_2", "dimension_type");
            ResultSet rs = s.executeQuery(initializedSelect);

            Map<Integer, Integer> values = new HashMap<>();
            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
                int iVal = rs.getInt(2);
                if(!rs.wasNull()) {
                    values.put(rs.getInt(1), rs.getInt(2));
                }
            }

            String initializedUpdate;
            for (Map.Entry<Integer, Integer> e : values.entrySet()) {
                initializedUpdate = String.format(update, "dimension_type", "measurement_dimension_transp_0", e.getValue(), e.getKey());
                s.executeUpdate(initializedUpdate);
            }


            // transport_equipment_type
            initializedSelect = String.format(selectValues, "transport_equipment_transpor_0", "transport_equipment_type");
            rs = s.executeQuery(initializedSelect);

            values = new HashMap<>();
            ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
                int iVal = rs.getInt(2);
                if(!rs.wasNull()) {
                    values.put(rs.getInt(1), rs.getInt(2));
                }
            }

            initializedSelect = String.format(selectValues, "transport_equipment_transpor_1", "transport_equipment_type");
            rs = s.executeQuery(initializedSelect);

            Map<Integer, Integer> values2 = new HashMap<>();
            List<Integer> ids2 = new ArrayList<>();
            while (rs.next()) {
                ids2.add(rs.getInt(1));
                int iVal = rs.getInt(2);
                if(!rs.wasNull()) {
                    values2.put(rs.getInt(1), rs.getInt(2));
                }
            }

            for (Integer id : ids) {
                initializedUpdate = String.format(update, "transport_equipment_type", "transport_equipment_transpor_1", values.containsKey(id) ? values.get(id) : "NULL", id);
                s.executeUpdate(initializedUpdate);
            }
            for (Integer id : ids2) {
                initializedUpdate = String.format(update, "transport_equipment_type", "transport_equipment_transpor_0", values2.containsKey(id) ? values2.get(id) : "NULL", id);
                s.executeUpdate(initializedUpdate);
            }

        } finally {
            connection2.close();
        }
    }

    private static void removeUnusedColumns() throws ClassNotFoundException, SQLException {
        Connection connection = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb", "postgres", "nimble");
        Connection connection2 = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");

        // copy values from old foreign keys to new foreign keys
        // create missing fields
        // create foreign key if necessary for new field
        // remove redundant fields

        try {
            for (String table : filledList) {
                String query = "select column_name, data_type from information_schema.columns where table_name = '" + table + "'";
                Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery(query);
                Map<String, String> map1 = new HashMap<>();
                Map<String, String> map2 = new HashMap<>();
                while (rs.next()) {
                    map1.put(rs.getString(1), rs.getString(2));
                }

                s = connection2.createStatement();
                rs = s.executeQuery(query);
                while (rs.next()) {
                    map2.put(rs.getString(1), rs.getString(2));
                }

                System.out.println();
                System.out.println("TABLE: " + table);
                String dropColumnQuery = "ALTER TABLE %s DROP COLUMN %s";
                for(Map.Entry e : map2.entrySet()) {
                    // existence
                    if(!map1.containsKey(e.getKey())) {
                        System.out.println(e.getKey() + " does not exist in new schema");
                        String initializedQuery = String.format(dropColumnQuery, table, e.getKey());
                        Statement sdrop = connection2.createStatement();
                        sdrop.executeUpdate(initializedQuery);
                        sdrop.close();
                    }
                }

            }
        } finally {
            try {
                connection.close();
            } catch (Exception e) {

            }
            try {
                connection2.close();
            } catch (Exception e) {

            }
        }

    }

    private static void printNumericDifferences() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection connection = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb", "postgres", "nimble");

        Connection connection2 = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");

        try {
            for (String table : filledList) {
                String query = "select column_name, data_type from information_schema.columns where table_name = '" + table + "'";
                Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery(query);
                Map<String, String> map1 = new HashMap<>();
                Map<String, String> map2 = new HashMap<>();
                while (rs.next()) {
                    map1.put(rs.getString(1), rs.getString(2));
                }

                s = connection2.createStatement();
                rs = s.executeQuery(query);
                while(rs.next()) {
                    map2.put(rs.getString(1), rs.getString(2));
                }

                System.out.println();
                System.out.println("TABLE: " + table);

                // check columns
                for(Map.Entry<String, String> e : map1.entrySet()) {
                    String firstColumnName = e.getKey();
                    if(!map2.containsKey(e.getKey())) {
                        for(Map.Entry<String, String> e2 : map2.entrySet()) {
                            String secondColumnName = e2.getKey();
                            if(firstColumnName.substring(0, firstColumnName.length()-1).equals(secondColumnName.substring(0, secondColumnName.length()-1))) {
                                System.out.println(firstColumnName);
                                System.out.println(secondColumnName);
                            }
                        }
                    }
                }
            }
        } finally {
            try {
                connection.close();
            } catch (Exception e) {

            }
            try {
                connection2.close();
            } catch (Exception e) {

            }
        }
    }

    private static void printDifferences() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        Connection connection = null, connection2 = null;


        try {
            connection = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/ubldb", "postgres", "nimble");

            connection2 = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");

            Statement s = connection.createStatement();
            ResultSet rs1 = s.executeQuery("select table_name from information_schema.tables where table_schema = 'public'");
            List<String> tables = new ArrayList<>();
            while (rs1.next()) {
                tables.add(rs1.getString(1));
            }

            Statement s2= connection2.createStatement();
            ResultSet rs2 = s2.executeQuery("select table_name from information_schema.tables where table_schema = 'public'");
            List<String> tables2 = new ArrayList<>();
            while (rs2.next()) {
                tables2.add(rs2.getString(1));
            }

            System.out.println("NEW TABLES:");
            for(String table : tables2) {
                if(!tables.contains(table)) {
                    System.out.println(table);
                }
            }

            System.out.println("DIFFERENCES IN EXISTING TABLES");

            for (String table : tables) {
                String query = "select column_name, data_type from information_schema.columns where table_name = '" + table +"'";
                s = connection.createStatement();
                ResultSet rs = s.executeQuery(query);
                Map<String, String> map1 = new HashMap<>();
                Map<String, String> map2 = new HashMap<>();
                while(rs.next()) {
                    map1.put(rs.getString(1), rs.getString(2));
                }

                s = connection2.createStatement();
                rs = s.executeQuery(query);
                while(rs.next()) {
                    map2.put(rs.getString(1), rs.getString(2));
                }

                System.out.println();
                System.out.println("TABLE: " + table);

                // check columns
                for(Map.Entry e : map2.entrySet()) {
                    // existence
                    if(!map1.containsKey(e.getKey())) {
                        System.out.println(e.getKey() + " does not exist previously");
                    } else {
                        if(!e.getValue().equals(map1.get(e.getKey()))) {
                            System.out.println(e.getKey() + " types are different");
                        }
                    }
                }

                // check foreign keys
                Map<String, List<String>> foreignKeys1 = new HashMap<>();
                Map<String, List<String>> foreignKeys2 = new HashMap<>();
                query = "SELECT" +
                        " kcu.column_name," +
                        " ccu.table_name AS foreign_table_name," +
                        " ccu.column_name AS foreign_column_name" +
                        " FROM" +
                        " information_schema.table_constraints AS tc" +
                        " JOIN information_schema.key_column_usage AS kcu" +
                        " ON tc.constraint_name = kcu.constraint_name" +
                        " JOIN information_schema.constraint_column_usage AS ccu" +
                        " ON ccu.constraint_name = tc.constraint_name" +
                        " WHERE constraint_type = 'FOREIGN KEY' AND tc.table_name='" + table + "'";

                s = connection.createStatement();
                rs = s.executeQuery(query);
                while(rs.next()) {
                    List<String> fkInfo = new ArrayList<>();
                    fkInfo.add(rs.getString(2));
                    fkInfo.add(rs.getString(3));
                    foreignKeys1.put(rs.getString(1), fkInfo);
                }

                s = connection2.createStatement();
                rs = s.executeQuery(query);
                while(rs.next()) {
                    List<String> fkInfo = new ArrayList<>();
                    fkInfo.add(rs.getString(2));
                    fkInfo.add(rs.getString(3));
                    foreignKeys2.put(rs.getString(1), fkInfo);
                }

                for(Map.Entry<String, List<String>> e : foreignKeys2.entrySet()) {
                    if(!foreignKeys1.containsKey(e.getKey())) {
                        System.out.println(e.getKey() + " does not have foreign key");
                    } else {
                        String columnName = e.getKey();
                        String newReferencedEntity = e.getValue().get(0);
                        String newReferencedColumn = e.getValue().get(1);
                        String oldReferencedEntity = foreignKeys2.get(e.getKey()).get(0);
                        String oldReferencedColumn = foreignKeys2.get(e.getKey()).get(1);

                        if(!(newReferencedEntity.equals(oldReferencedEntity) && newReferencedColumn.equals(oldReferencedColumn))) {
                            System.out.println(e.getKey() + " has different referenced entities: " + newReferencedEntity + ", " + newReferencedColumn);
                            System.out.println(e.getKey() + " has different referenced entities: " + oldReferencedEntity + ", " + oldReferencedColumn);
//                            for(Map.Entry<String, List<String>> e2 : foreignKeys2.entrySet()) {
//                                if(newReferencedEntity.equals(e2.getValue().get(0)) && newReferencedColumn.equals(e2.getValue().get(1))) {
//                                    System.out.println("Original foreign key: " + e2.getKey());
//                                    break;
//                                }
//                            }
                        }
                    }
                }
            }
        } finally {
            try {
                connection.close();
            } catch (Exception e) {

            }
            try {
                connection2.close();
            } catch (Exception e) {

            }
        }
    }

    private static void dropEmptyTables() throws SQLException, ClassNotFoundException {
        List<String> filledList = new ArrayList<>();
        filledList.add("address_type");
        filledList.add("amount_type");
        filledList.add("attachment_type");
        filledList.add("binary_object_type");
        filledList.add("branch_type");
        filledList.add("card_account_type");
        filledList.add("catalogue_line_type");
        filledList.add("catalogue_line_type_warranty_0");
        filledList.add("catalogue_type");
        filledList.add("certificate_type");
        filledList.add("code_type");
        filledList.add("commodity_classification_type");
        filledList.add("consignment_type");
        filledList.add("contact_type");
        filledList.add("country_type");
        filledList.add("credit_account_type");
        filledList.add("customer_party_type");
        filledList.add("delivery_terms_type");
        filledList.add("delivery_type");
        filledList.add("dimension_type");
        filledList.add("document_reference_type");
        filledList.add("environmental_emission_type");
        filledList.add("financial_account_type");
        filledList.add("financial_institution_type");
        filledList.add("goods_item_type");
        filledList.add("item_identification_type");
        filledList.add("item_information_request_lin_0");
        filledList.add("item_information_request_typ_0");
        filledList.add("item_information_request_type");
        filledList.add("item_information_response_ty_0");
        filledList.add("item_information_response_ty_1");
        filledList.add("item_location_quantity_type");
        filledList.add("item_property_type");
        filledList.add("item_property_type_value_dec_0");
        filledList.add("item_property_type_value_item");
        filledList.add("item_type");
        filledList.add("line_item_type");
        filledList.add("line_reference_type");
        filledList.add("location_type");
        filledList.add("order_line_type");
        filledList.add("order_reference_type");
        filledList.add("order_response_simple_type");
        filledList.add("order_type");
        filledList.add("package_type");
        filledList.add("party_tax_scheme_type");
        filledList.add("party_type");
        filledList.add("payment_means_type");
        filledList.add("period_type");
        filledList.add("person_type");
        filledList.add("ppap_request_type");
        filledList.add("ppap_request_type_document_t_0");
        filledList.add("ppap_response_type");
        filledList.add("price_type");
        filledList.add("quality_indicator_type");
        filledList.add("quantity_type");
        filledList.add("quotation_line_type");
        filledList.add("quotation_type");
        filledList.add("quotation_type_note_item");
        filledList.add("request_for_quotation_line_t_0");
        filledList.add("request_for_quotation_type");
        filledList.add("request_for_quotation_type_n_0");
        filledList.add("sales_item_type");
        filledList.add("service_frequency_type");
        filledList.add("shipment_stage_type");
        filledList.add("shipment_type");
        filledList.add("supplier_party_type");
        filledList.add("tax_scheme_type");
        filledList.add("transport_equipment_type");
        filledList.add("transport_handling_unit_type");
        filledList.add("transport_means_type");
        filledList.add("transportation_service_type");

        List<String> allList = new ArrayList<>();
        allList.add("activity_data_line_type");
        allList.add("activity_property_type");
        allList.add("address_line_type");
        allList.add("address_type");
        allList.add("air_transport_type");
        allList.add("allowance_charge_type");
        allList.add("amount_type");
        allList.add("appeal_terms_type");
        allList.add("appeal_terms_type_descriptio_0");
        allList.add("attachment_type");
        allList.add("auction_terms_type");
        allList.add("auction_terms_type_condition_0");
        allList.add("auction_terms_type_descripti_0");
        allList.add("auction_terms_type_electroni_0");
        allList.add("auction_terms_type_justifica_0");
        allList.add("auction_terms_type_process_d_0");
        allList.add("awarding_criterion_response__1");
        allList.add("awarding_criterion_response__2");
        allList.add("awarding_criterion_response__3");
        allList.add("awarding_criterion_type");
        allList.add("awarding_criterion_type_calc_0");
        allList.add("awarding_criterion_type_desc_0");
        allList.add("awarding_criterion_type_mini_0");
        allList.add("awarding_criterion_type_weig_0");
        allList.add("awarding_terms_type");
        allList.add("awarding_terms_type_descript_0");
        allList.add("awarding_terms_type_low_tend_0");
        allList.add("awarding_terms_type_payment__0");
        allList.add("awarding_terms_type_prize_de_0");
        allList.add("awarding_terms_type_technica_0");
        allList.add("billing_reference_line_type");
        allList.add("billing_reference_type");
        allList.add("binary_object_type");
        allList.add("branch_type");
        allList.add("budget_account_line_type");
        allList.add("budget_account_type");
        allList.add("capability_type");
        allList.add("capability_type_description__0");
        allList.add("card_account_type");
        allList.add("catalogue_item_specification_0");
        allList.add("catalogue_line_type");
        allList.add("catalogue_line_type_note_item");
        allList.add("catalogue_line_type_warranty_0");
        allList.add("catalogue_pricing_update_lin_0");
        allList.add("catalogue_reference_type");
        allList.add("catalogue_request_line_type");
        allList.add("catalogue_request_line_type__0");
        allList.add("catalogue_type");
        allList.add("catalogue_type_description_i_0");
        allList.add("certificate_of_origin_applic_0");
        allList.add("certificate_of_origin_applic_1");
        allList.add("certificate_type");
        allList.add("certificate_type_remarks_item");
        allList.add("classification_category_type");
        allList.add("classification_category_type_0");
        allList.add("classification_scheme_type");
        allList.add("classification_scheme_type_d_0");
        allList.add("classification_scheme_type_n_0");
        allList.add("clause_type");
        allList.add("clause_type_content_item");
        allList.add("clause_type_note_item");
        allList.add("code_type");
        allList.add("commodity_classification_type");
        allList.add("communication_type");
        allList.add("completed_task_type");
        allList.add("completed_task_type_descript_0");
        allList.add("condition_type");
        allList.add("condition_type_description_i_0");
        allList.add("consignment_type");
        allList.add("consignment_type_carrier_ser_0");
        allList.add("consignment_type_customs_cle_0");
        allList.add("consignment_type_delivery_in_0");
        allList.add("consignment_type_forwarder_s_0");
        allList.add("consignment_type_handling_in_0");
        allList.add("consignment_type_haulage_ins_0");
        allList.add("consignment_type_information_0");
        allList.add("consignment_type_remarks_item");
        allList.add("consignment_type_special_ins_0");
        allList.add("consignment_type_special_ser_0");
        allList.add("consignment_type_summary_des_0");
        allList.add("consignment_type_tariff_desc_0");
        allList.add("consumption_average_type");
        allList.add("consumption_average_type_des_0");
        allList.add("consumption_correction_type");
        allList.add("consumption_correction_type__0");
        allList.add("consumption_history_type");
        allList.add("consumption_history_type_des_0");
        allList.add("consumption_line_type");
        allList.add("consumption_point_type");
        allList.add("consumption_point_type_descr_0");
        allList.add("consumption_report_reference_1");
        allList.add("consumption_report_type");
        allList.add("consumption_report_type_desc_0");
        allList.add("consumption_type");
        allList.add("contact_type");
        allList.add("contract_execution_requireme_0");
        allList.add("contract_execution_requireme_3");
        allList.add("contract_execution_requireme_4");
        allList.add("contract_extension_type");
        allList.add("contract_extension_type_opti_0");
        allList.add("contract_type");
        allList.add("contract_type_description_it_0");
        allList.add("contract_type_note_item");
        allList.add("contracting_activity_type");
        allList.add("contracting_party_type");
        allList.add("contracting_party_type_type");
        allList.add("corporate_registration_schem_0");
        allList.add("country_type");
        allList.add("credit_account_type");
        allList.add("credit_note_line_type");
        allList.add("credit_note_line_type_note_i_0");
        allList.add("customer_party_type");
        allList.add("customer_party_type_addition_0");
        allList.add("customs_declaration_type");
        allList.add("data_monitoring_clause_type");
        allList.add("data_monitoring_clause_type__0");
        allList.add("data_monitoring_clause_type__1");
        allList.add("data_monitoring_clause_type__2");
        allList.add("data_monitoring_clause_type__3");
        allList.add("data_monitoring_clause_type__4");
        allList.add("debit_note_line_type");
        allList.add("debit_note_line_type_note_it_0");
        allList.add("declaration_type");
        allList.add("declaration_type_description_0");
        allList.add("declaration_type_name_item");
        allList.add("delivery_terms_type");
        allList.add("delivery_type");
        allList.add("delivery_unit_type");
        allList.add("dependent_price_reference_ty_0");
        allList.add("despatch_advice_type");
        allList.add("despatch_advice_type_note_it_0");
        allList.add("despatch_line_type");
        allList.add("despatch_line_type_note_item");
        allList.add("despatch_line_type_outstandi_0");
        allList.add("despatch_type");
        allList.add("dimension_type");
        allList.add("dimension_type_description_i_0");
        allList.add("document_clause_type");
        allList.add("document_distribution_type");
        allList.add("document_reference_type");
        allList.add("document_response_type");
        allList.add("duty_type");
        allList.add("economic_operator_role_type");
        allList.add("economic_operator_role_type__0");
        allList.add("economic_operator_short_list_0");
        allList.add("economic_operator_short_list_2");
        allList.add("emission_calculation_method__0");
        allList.add("endorsement_type");
        allList.add("endorsement_type_remarks_item");
        allList.add("endorser_party_type");
        allList.add("energy_tax_report_type");
        allList.add("energy_water_supply_type");
        allList.add("environmental_emission_type");
        allList.add("environmental_emission_type__2");
        allList.add("evaluation_criterion_type");
        allList.add("evaluation_criterion_type_de_0");
        allList.add("evaluation_criterion_type_ex_0");
        allList.add("event_comment_type");
        allList.add("event_line_item_type");
        allList.add("event_tactic_enumeration_type");
        allList.add("event_tactic_type");
        allList.add("event_type");
        allList.add("event_type_description_item");
        allList.add("evidence_supplied_type");
        allList.add("evidence_type");
        allList.add("evidence_type_candidate_stat_0");
        allList.add("evidence_type_description_it_0");
        allList.add("exception_criteria_line_type");
        allList.add("exception_criteria_line_type_0");
        allList.add("exception_notification_line__0");
        allList.add("exception_notification_line__1");
        allList.add("exception_notification_line__2");
        allList.add("exchange_rate_type");
        allList.add("external_reference_type");
        allList.add("financial_account_type");
        allList.add("financial_guarantee_type");
        allList.add("financial_guarantee_type_des_0");
        allList.add("financial_institution_type");
        allList.add("forecast_exception_criterion_2");
        allList.add("forecast_exception_type");
        allList.add("forecast_line_type");
        allList.add("forecast_line_type_note_item");
        allList.add("forecast_revision_line_type");
        allList.add("forecast_revision_line_type__0");
        allList.add("forecast_revision_line_type__1");
        allList.add("framework_agreement_type");
        allList.add("framework_agreement_type_fre_0");
        allList.add("framework_agreement_type_jus_0");
        allList.add("goods_item_container_type");
        allList.add("goods_item_type");
        allList.add("goods_item_type_description__0");
        allList.add("hazardous_goods_transit_type");
        allList.add("hazardous_item_type");
        allList.add("hazardous_item_type_addition_0");
        allList.add("immobilized_security_type");
        allList.add("instruction_for_returns_line_0");
        allList.add("instruction_for_returns_line_1");
        allList.add("inventory_report_line_type");
        allList.add("inventory_report_line_type_n_0");
        allList.add("invoice_line_type");
        allList.add("invoice_line_type_note_item");
        allList.add("item_comparison_type");
        allList.add("item_identification_type");
        allList.add("item_information_request_lin_0");
        allList.add("item_information_request_typ_0");
        allList.add("item_information_request_type");
        allList.add("item_information_response_ty_0");
        allList.add("item_information_response_ty_1");
        allList.add("item_instance_type");
        allList.add("item_location_quantity_type");
        allList.add("item_location_quantity_type__0");
        allList.add("item_management_profile_type");
        allList.add("item_management_profile_type_0");
        allList.add("item_property_group_type");
        allList.add("item_property_range_type");
        allList.add("item_property_type");
        allList.add("item_property_type_value_dec_0");
        allList.add("item_property_type_value_item");
        allList.add("item_type");
        allList.add("language_type");
        allList.add("line_item_type");
        allList.add("line_reference_type");
        allList.add("line_response_type");
        allList.add("location_coordinate_type");
        allList.add("location_type");
        allList.add("lot_identification_type");
        allList.add("maritime_transport_type");
        allList.add("maritime_transport_type_ship_0");
        allList.add("meter_property_type");
        allList.add("meter_property_type_value_qu_0");
        allList.add("meter_reading_type");
        allList.add("meter_reading_type_meter_rea_0");
        allList.add("meter_type");
        allList.add("miscellaneous_event_type");
        allList.add("monetary_total_type");
        allList.add("notification_requirement_type");
        allList.add("on_account_payment_type");
        allList.add("on_account_payment_type_note_0");
        allList.add("order_line_reference_type");
        allList.add("order_line_type");
        allList.add("order_reference_type");
        allList.add("order_response_simple_type");
        allList.add("order_type");
        allList.add("ordered_shipment_type");
        allList.add("package_type");
        allList.add("party_identification_type");
        allList.add("party_legal_entity_type");
        allList.add("party_name_type");
        allList.add("party_tax_scheme_type");
        allList.add("party_type");
        allList.add("party_type_party_type_item");
        allList.add("payment_mandate_type");
        allList.add("payment_means_type");
        allList.add("payment_terms_type");
        allList.add("payment_terms_type_note_item");
        allList.add("payment_terms_type_payment_c_0");
        allList.add("payment_type");
        allList.add("performance_data_line_type");
        allList.add("performance_data_line_type_n_0");
        allList.add("period_type");
        allList.add("person_type");
        allList.add("person_type_role_item");
        allList.add("physical_attribute_type");
        allList.add("physical_attribute_type_desc_0");
        allList.add("pickup_type");
        allList.add("power_of_attorney_type");
        allList.add("power_of_attorney_type_descr_0");
        allList.add("ppap_request_type");
        allList.add("ppap_request_type_document_t_0");
        allList.add("ppap_response_type");
        allList.add("price_extension_type");
        allList.add("price_list_type");
        allList.add("price_type");
        allList.add("pricing_reference_type");
        allList.add("process_justification_type");
        allList.add("process_justification_type_d_0");
        allList.add("process_justification_type_p_0");
        allList.add("procurement_project_lot_type");
        allList.add("procurement_project_type");
        allList.add("procurement_project_type_des_0");
        allList.add("procurement_project_type_fee_0");
        allList.add("procurement_project_type_nam_0");
        allList.add("procurement_project_type_not_0");
        allList.add("project_reference_type");
        allList.add("promotional_event_line_item__0");
        allList.add("promotional_event_type");
        allList.add("promotional_specification_ty_0");
        allList.add("qualification_resolution_typ_0");
        allList.add("qualification_resolution_typ_1");
        allList.add("qualification_resolution_type");
        allList.add("qualifying_party_type");
        allList.add("qualifying_party_type_person_0");
        allList.add("quality_indicator_type");
        allList.add("quantity_type");
        allList.add("quotation_line_type");
        allList.add("quotation_line_type_note_item");
        allList.add("quotation_type");
        allList.add("quotation_type_note_item");
        allList.add("rail_transport_type");
        allList.add("receipt_advice_type");
        allList.add("receipt_advice_type_note_item");
        allList.add("receipt_line_type");
        allList.add("receipt_line_type_note_item");
        allList.add("receipt_line_type_reject_rea_0");
        allList.add("regulation_type");
        allList.add("related_item_type");
        allList.add("related_item_type_descriptio_0");
        allList.add("reminder_line_type");
        allList.add("reminder_line_type_note_item");
        allList.add("remittance_advice_line_type");
        allList.add("remittance_advice_line_type__0");
        allList.add("renewal_type");
        allList.add("request_for_quotation_line_t_0");
        allList.add("request_for_quotation_line_t_1");
        allList.add("request_for_quotation_type");
        allList.add("request_for_quotation_type_n_0");
        allList.add("request_for_tender_line_type");
        allList.add("request_for_tender_line_type_0");
        allList.add("requested_tender_total_type");
        allList.add("requested_tender_total_type__0");
        allList.add("response_type");
        allList.add("response_type_description_it_0");
        allList.add("result_of_verification_type");
        allList.add("retail_planned_impact_type");
        allList.add("road_transport_type");
        allList.add("sales_item_type");
        allList.add("secondary_hazard_type");
        allList.add("secondary_hazard_type_extens_0");
        allList.add("service_frequency_type");
        allList.add("service_provider_party_type");
        allList.add("service_provider_party_type__0");
        allList.add("shareholder_party_type");
        allList.add("shipment_stage_type");
        allList.add("shipment_type");
        allList.add("signature_type");
        allList.add("signature_type_note_item");
        allList.add("statement_line_type");
        allList.add("statement_line_type_note_item");
        allList.add("status_type");
        allList.add("status_type_description_item");
        allList.add("status_type_status_reason_it_0");
        allList.add("status_type_text_item");
        allList.add("stock_availability_report_li_0");
        allList.add("stock_availability_report_li_1");
        allList.add("stowage_type");
        allList.add("stowage_type_location_item");
        allList.add("subcontract_terms_type");
        allList.add("subcontract_terms_type_descr_0");
        allList.add("subscriber_consumption_type");
        allList.add("subscriber_consumption_type__0");
        allList.add("supplier_consumption_type");
        allList.add("supplier_consumption_type_de_0");
        allList.add("supplier_party_type");
        allList.add("supplier_party_type_addition_0");
        allList.add("tax_category_type");
        allList.add("tax_scheme_type");
        allList.add("tax_subtotal_type");
        allList.add("tax_total_type");
        allList.add("telecommunications_service_t_0");
        allList.add("telecommunications_supply_li_2");
        allList.add("telecommunications_supply_li_3");
        allList.add("telecommunications_supply_ty_0");
        allList.add("telecommunications_supply_ty_3");
        allList.add("temperature_type");
        allList.add("tender_line_type");
        allList.add("tender_line_type_note_item");
        allList.add("tender_line_type_warranty_in_0");
        allList.add("tender_preparation_type");
        allList.add("tender_preparation_type_desc_0");
        allList.add("tender_requirement_type");
        allList.add("tender_requirement_type_desc_0");
        allList.add("tender_result_type");
        allList.add("tender_result_type_descripti_0");
        allList.add("tendered_project_type");
        allList.add("tendered_project_type_fee_de_0");
        allList.add("tenderer_party_qualification_0");
        allList.add("tenderer_qualification_reque_0");
        allList.add("tenderer_qualification_reque_3");
        allList.add("tenderer_qualification_reque_4");
        allList.add("tenderer_requirement_type");
        allList.add("tenderer_requirement_type_de_0");
        allList.add("tenderer_requirement_type_na_0");
        allList.add("tendering_process_type");
        allList.add("tendering_process_type_descr_0");
        allList.add("tendering_process_type_negot_0");
        allList.add("tendering_terms_type");
        allList.add("tendering_terms_type_accepte_0");
        allList.add("tendering_terms_type_additio_0");
        allList.add("tendering_terms_type_funding_0");
        allList.add("tendering_terms_type_note_it_0");
        allList.add("tendering_terms_type_price_r_0");
        allList.add("trade_financing_type");
        allList.add("trading_terms_type");
        allList.add("trading_terms_type_informati_0");
        allList.add("transaction_conditions_type");
        allList.add("transaction_conditions_type__0");
        allList.add("transport_equipment_seal_type");
        allList.add("transport_equipment_type");
        allList.add("transport_equipment_type_dam_0");
        allList.add("transport_equipment_type_des_0");
        allList.add("transport_equipment_type_inf_0");
        allList.add("transport_equipment_type_ref_0");
        allList.add("transport_equipment_type_spe_0");
        allList.add("transport_event_type");
        allList.add("transport_event_type_descrip_0");
        allList.add("transport_execution_plan_req_2");
        allList.add("transport_execution_plan_req_3");
        allList.add("transport_execution_plan_req_4");
        allList.add("transport_execution_plan_req_5");
        allList.add("transport_execution_plan_typ_0");
        allList.add("transport_execution_plan_typ_1");
        allList.add("transport_execution_plan_typ_2");
        allList.add("transport_execution_plan_typ_3");
        allList.add("transport_execution_plan_type");
        allList.add("transport_execution_terms_ty_0");
        allList.add("transport_execution_terms_ty_1");
        allList.add("transport_execution_terms_ty_2");
        allList.add("transport_execution_terms_ty_3");
        allList.add("transport_handling_unit_type");
        allList.add("transport_handling_unit_type_2");
        allList.add("transport_handling_unit_type_3");
        allList.add("transport_handling_unit_type_4");
        allList.add("transport_means_type");
        allList.add("transport_means_type_registr_0");
        allList.add("transport_schedule_type");
        allList.add("transport_schedule_type_rema_0");
        allList.add("transportation_segment_type");
        allList.add("transportation_service_type");
        allList.add("transportation_service_type__0");
        allList.add("unstructured_price_type");
        allList.add("utility_item_type");
        allList.add("utility_item_type_descriptio_0");
        allList.add("web_site_access_type");
        allList.add("winning_party_type");
        allList.add("work_phase_reference_type");
        allList.add("work_phase_reference_type_wo_0");

        Class.forName("org.postgresql.Driver");
        Connection connection = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");

        try {
            for (String table : allList) {
                if (!filledList.contains(table)) {
                    String query = "DROP table " + table + " CASCADE";
                    System.out.println(query);
                    Statement s = connection.createStatement();
                    s.executeUpdate(query);
                    s.close();
                }
            }
        } finally {
            connection.close();
        }
    }

    private static void printTablesWithValues(List<String> tables) throws SQLException, ClassNotFoundException {
        Map<String, String> mapp = new HashMap<>();
        String selectQuery = "select * from %s";
        for(String table : tables ) {
            mapp.put(table, String.format(selectQuery, table));
        }


        Class.forName("org.postgresql.Driver");
        /*Connection connection = DriverManager
                .getConnection("jdbc:postgresql://sl-eu-lon-2-portal.5.dblayer.com:21113/ubldb", "admin", "LLSYYLRFECXKTCRK");*/
        /*Connection connection = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/ubldb2", "postgres", "nimble");*/
        Connection connection = DriverManager
                .getConnection("jdbc:postgresql://localhost:5432/identitydb", "postgres", "nimble");

        tables = new ArrayList<>();
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery("select * from information_schema.tables");
        while(rs.next()) {
            tables.add(rs.getString(1));
        }

        try {
            for (Map.Entry<String, String> e : mapp.entrySet()) {
                String query = e.getValue();
                s = connection.createStatement();
                rs = s.executeQuery(query);

                if (rs.next()) {
                    System.out.println(e.getKey());
                }
                rs.close();
                s.close();
            }
        } finally {
            connection.close();
        }
    }

    static List<String> filledList;
    static {
        filledList = new ArrayList<>();
        filledList.add("address_type");
        filledList.add("amount_type");
        filledList.add("attachment_type");
        filledList.add("binary_object_type");
        filledList.add("branch_type");
        filledList.add("card_account_type");
        filledList.add("catalogue_line_type");
        filledList.add("catalogue_line_type_warranty_0");
        filledList.add("catalogue_type");
        filledList.add("certificate_type");
        filledList.add("code_type");
        filledList.add("commodity_classification_type");
        filledList.add("consignment_type");
        filledList.add("contact_type");
        filledList.add("country_type");
        filledList.add("credit_account_type");
        filledList.add("customer_party_type");
        filledList.add("delivery_terms_type");
        filledList.add("delivery_type");
        filledList.add("dimension_type");
        filledList.add("document_reference_type");
        filledList.add("environmental_emission_type");
        filledList.add("financial_account_type");
        filledList.add("financial_institution_type");
        filledList.add("goods_item_type");
        filledList.add("item_identification_type");
        filledList.add("item_information_request_lin_0");
        filledList.add("item_information_request_typ_0");
        filledList.add("item_information_request_type");
        filledList.add("item_information_response_ty_0");
        filledList.add("item_information_response_ty_1");
        filledList.add("item_location_quantity_type");
        filledList.add("item_property_type");
        filledList.add("item_property_type_value_dec_0");
        filledList.add("item_property_type_value_item");
        filledList.add("item_type");
        filledList.add("line_item_type");
        filledList.add("line_reference_type");
        filledList.add("location_type");
        filledList.add("order_line_type");
        filledList.add("order_reference_type");
        filledList.add("order_response_simple_type");
        filledList.add("order_type");
        filledList.add("package_type");
        filledList.add("party_tax_scheme_type");
        filledList.add("party_type");
        filledList.add("payment_means_type");
        filledList.add("period_type");
        filledList.add("person_type");
        filledList.add("ppap_request_type");
        filledList.add("ppap_request_type_document_t_0");
        filledList.add("ppap_response_type");
        filledList.add("price_type");
        filledList.add("quality_indicator_type");
        filledList.add("quantity_type");
        filledList.add("quotation_line_type");
        filledList.add("quotation_type");
        filledList.add("quotation_type_note_item");
        filledList.add("request_for_quotation_line_t_0");
        filledList.add("request_for_quotation_type");
        filledList.add("request_for_quotation_type_n_0");
        filledList.add("sales_item_type");
        filledList.add("service_frequency_type");
        filledList.add("shipment_stage_type");
        filledList.add("shipment_type");
        filledList.add("supplier_party_type");
        filledList.add("tax_scheme_type");
        filledList.add("transport_equipment_type");
        filledList.add("transport_handling_unit_type");
        filledList.add("transport_means_type");
        filledList.add("transportation_service_type");
    }

    private static List<String> identityDbTables = new ArrayList<>();
    static {
        identityDbTables.add("activity_data_line_type");
        identityDbTables.add("activity_property_type");
        identityDbTables.add("address_line_type");
        identityDbTables.add("address_type");
        identityDbTables.add("air_transport_type");
        identityDbTables.add("allowance_charge_type");
        identityDbTables.add("amount_type");
        identityDbTables.add("appeal_terms_type");
        identityDbTables.add("appeal_terms_type_descriptio_0");
        identityDbTables.add("attachment_type");
        identityDbTables.add("auction_terms_type");
        identityDbTables.add("auction_terms_type_condition_0");
        identityDbTables.add("auction_terms_type_descripti_0");
        identityDbTables.add("auction_terms_type_electroni_0");
        identityDbTables.add("auction_terms_type_justifica_0");
        identityDbTables.add("auction_terms_type_process_d_0");
        identityDbTables.add("awarding_criterion_response__1");
        identityDbTables.add("awarding_criterion_response__2");
        identityDbTables.add("awarding_criterion_response__3");
        identityDbTables.add("awarding_criterion_type");
        identityDbTables.add("awarding_criterion_type_calc_0");
        identityDbTables.add("awarding_criterion_type_desc_0");
        identityDbTables.add("awarding_criterion_type_mini_0");
        identityDbTables.add("awarding_criterion_type_weig_0");
        identityDbTables.add("awarding_terms_type");
        identityDbTables.add("awarding_terms_type_descript_0");
        identityDbTables.add("awarding_terms_type_low_tend_0");
        identityDbTables.add("awarding_terms_type_payment__0");
        identityDbTables.add("awarding_terms_type_prize_de_0");
        identityDbTables.add("awarding_terms_type_technica_0");
        identityDbTables.add("billing_reference_line_type");
        identityDbTables.add("billing_reference_type");
        identityDbTables.add("binary_object_type");
        identityDbTables.add("branch_type");
        identityDbTables.add("budget_account_line_type");
        identityDbTables.add("budget_account_type");
        identityDbTables.add("capability_type");
        identityDbTables.add("capability_type_description__0");
        identityDbTables.add("card_account_type");
        identityDbTables.add("catalogue_item_specification_0");
        identityDbTables.add("catalogue_line_type");
        identityDbTables.add("catalogue_line_type_note_item");
        identityDbTables.add("catalogue_line_type_warranty_0");
        identityDbTables.add("catalogue_pricing_update_lin_0");
        identityDbTables.add("catalogue_reference_type");
        identityDbTables.add("catalogue_request_line_type");
        identityDbTables.add("catalogue_request_line_type__0");
        identityDbTables.add("certificate_of_origin_applic_0");
        identityDbTables.add("certificate_of_origin_applic_1");
        identityDbTables.add("certificate_type");
        identityDbTables.add("certificate_type_remarks_item");
        identityDbTables.add("classification_category_type");
        identityDbTables.add("classification_category_type_0");
        identityDbTables.add("classification_scheme_type");
        identityDbTables.add("classification_scheme_type_d_0");
        identityDbTables.add("classification_scheme_type_n_0");
        identityDbTables.add("clause_type");
        identityDbTables.add("clause_type_content_item");
        identityDbTables.add("code_type");
        identityDbTables.add("commodity_classification_type");
        identityDbTables.add("communication_type");
        identityDbTables.add("completed_task_type");
        identityDbTables.add("completed_task_type_descript_0");
        identityDbTables.add("condition_type");
        identityDbTables.add("condition_type_description_i_0");
        identityDbTables.add("consignment_type");
        identityDbTables.add("consignment_type_carrier_ser_0");
        identityDbTables.add("consignment_type_customs_cle_0");
        identityDbTables.add("consignment_type_delivery_in_0");
        identityDbTables.add("consignment_type_forwarder_s_0");
        identityDbTables.add("consignment_type_handling_in_0");
        identityDbTables.add("consignment_type_haulage_ins_0");
        identityDbTables.add("consignment_type_information_0");
        identityDbTables.add("consignment_type_remarks_item");
        identityDbTables.add("consignment_type_special_ins_0");
        identityDbTables.add("consignment_type_special_ser_0");
        identityDbTables.add("consignment_type_summary_des_0");
        identityDbTables.add("consignment_type_tariff_desc_0");
        identityDbTables.add("consumption_average_type");
        identityDbTables.add("consumption_average_type_des_0");
        identityDbTables.add("consumption_correction_type");
        identityDbTables.add("consumption_correction_type__0");
        identityDbTables.add("consumption_history_type");
        identityDbTables.add("consumption_history_type_des_0");
        identityDbTables.add("consumption_line_type");
        identityDbTables.add("consumption_point_type");
        identityDbTables.add("consumption_point_type_descr_0");
        identityDbTables.add("consumption_report_reference_1");
        identityDbTables.add("consumption_report_type");
        identityDbTables.add("consumption_report_type_desc_0");
        identityDbTables.add("consumption_type");
        identityDbTables.add("contact_type");
        identityDbTables.add("contract_execution_requireme_0");
        identityDbTables.add("contract_execution_requireme_3");
        identityDbTables.add("contract_execution_requireme_4");
        identityDbTables.add("contract_extension_type");
        identityDbTables.add("contract_extension_type_opti_0");
        identityDbTables.add("contract_type");
        identityDbTables.add("contract_type_description_it_0");
        identityDbTables.add("contract_type_note_item");
        identityDbTables.add("contracting_activity_type");
        identityDbTables.add("contracting_party_type");
        identityDbTables.add("contracting_party_type_type");
        identityDbTables.add("corporate_registration_schem_0");
        identityDbTables.add("country_type");
        identityDbTables.add("credit_account_type");
        identityDbTables.add("credit_note_line_type");
        identityDbTables.add("credit_note_line_type_note_i_0");
        identityDbTables.add("customer_party_type");
        identityDbTables.add("customer_party_type_addition_0");
        identityDbTables.add("customs_declaration_type");
        identityDbTables.add("debit_note_line_type");
        identityDbTables.add("debit_note_line_type_note_it_0");
        identityDbTables.add("declaration_type");
        identityDbTables.add("declaration_type_description_0");
        identityDbTables.add("declaration_type_name_item");
        identityDbTables.add("delivery_terms_type");
        identityDbTables.add("delivery_type");
        identityDbTables.add("delivery_unit_type");
        identityDbTables.add("dependent_price_reference_ty_0");
        identityDbTables.add("despatch_line_type");
        identityDbTables.add("despatch_line_type_note_item");
        identityDbTables.add("despatch_line_type_outstandi_0");
        identityDbTables.add("despatch_type");
        identityDbTables.add("dimension_type");
        identityDbTables.add("dimension_type_description_i_0");
        identityDbTables.add("document_distribution_type");
        identityDbTables.add("document_reference_type");
        identityDbTables.add("document_response_type");
        identityDbTables.add("duty_type");
        identityDbTables.add("economic_operator_role_type");
        identityDbTables.add("economic_operator_role_type__0");
        identityDbTables.add("economic_operator_short_list_0");
        identityDbTables.add("economic_operator_short_list_2");
        identityDbTables.add("emission_calculation_method__0");
        identityDbTables.add("endorsement_type");
        identityDbTables.add("endorsement_type_remarks_item");
        identityDbTables.add("endorser_party_type");
        identityDbTables.add("energy_tax_report_type");
        identityDbTables.add("energy_water_supply_type");
        identityDbTables.add("environmental_emission_type");
        identityDbTables.add("environmental_emission_type__2");
        identityDbTables.add("evaluation_criterion_type");
        identityDbTables.add("evaluation_criterion_type_de_0");
        identityDbTables.add("evaluation_criterion_type_ex_0");
        identityDbTables.add("event_comment_type");
        identityDbTables.add("event_line_item_type");
        identityDbTables.add("event_tactic_enumeration_type");
        identityDbTables.add("event_tactic_type");
        identityDbTables.add("event_type");
        identityDbTables.add("event_type_description_item");
        identityDbTables.add("evidence_supplied_type");
        identityDbTables.add("evidence_type");
        identityDbTables.add("evidence_type_candidate_stat_0");
        identityDbTables.add("evidence_type_description_it_0");
        identityDbTables.add("exception_criteria_line_type");
        identityDbTables.add("exception_criteria_line_type_0");
        identityDbTables.add("exception_notification_line__0");
        identityDbTables.add("exception_notification_line__1");
        identityDbTables.add("exception_notification_line__2");
        identityDbTables.add("exchange_rate_type");
        identityDbTables.add("external_reference_type");
        identityDbTables.add("financial_account_type");
        identityDbTables.add("financial_guarantee_type");
        identityDbTables.add("financial_guarantee_type_des_0");
        identityDbTables.add("financial_institution_type");
        identityDbTables.add("forecast_exception_criterion_2");
        identityDbTables.add("forecast_exception_type");
        identityDbTables.add("forecast_line_type");
        identityDbTables.add("forecast_line_type_note_item");
        identityDbTables.add("forecast_revision_line_type");
        identityDbTables.add("forecast_revision_line_type__0");
        identityDbTables.add("forecast_revision_line_type__1");
        identityDbTables.add("framework_agreement_type");
        identityDbTables.add("framework_agreement_type_fre_0");
        identityDbTables.add("framework_agreement_type_jus_0");
        identityDbTables.add("goods_item_container_type");
        identityDbTables.add("goods_item_type");
        identityDbTables.add("goods_item_type_description__0");
        identityDbTables.add("hazardous_goods_transit_type");
        identityDbTables.add("hazardous_item_type");
        identityDbTables.add("hazardous_item_type_addition_0");
        identityDbTables.add("immobilized_security_type");
        identityDbTables.add("instruction_for_returns_line_0");
        identityDbTables.add("instruction_for_returns_line_1");
        identityDbTables.add("inventory_report_line_type");
        identityDbTables.add("inventory_report_line_type_n_0");
        identityDbTables.add("invoice_line_type");
        identityDbTables.add("invoice_line_type_note_item");
        identityDbTables.add("item_comparison_type");
        identityDbTables.add("item_identification_type");
        identityDbTables.add("item_information_request_lin_0");
        identityDbTables.add("item_instance_type");
        identityDbTables.add("item_location_quantity_type");
        identityDbTables.add("item_location_quantity_type__0");
        identityDbTables.add("item_management_profile_type");
        identityDbTables.add("item_management_profile_type_0");
        identityDbTables.add("item_property_group_type");
        identityDbTables.add("item_property_range_type");
        identityDbTables.add("item_property_type");
        identityDbTables.add("item_property_type_value_dec_0");
        identityDbTables.add("item_property_type_value_item");
        identityDbTables.add("item_type");
        identityDbTables.add("language_type");
        identityDbTables.add("line_item_type");
        identityDbTables.add("line_reference_type");
        identityDbTables.add("line_response_type");
        identityDbTables.add("location_coordinate_type");
        identityDbTables.add("location_type");
        identityDbTables.add("lot_identification_type");
        identityDbTables.add("maritime_transport_type");
        identityDbTables.add("maritime_transport_type_ship_0");
        identityDbTables.add("meter_property_type");
        identityDbTables.add("meter_property_type_value_qu_0");
        identityDbTables.add("meter_reading_type");
        identityDbTables.add("meter_reading_type_meter_rea_0");
        identityDbTables.add("meter_type");
        identityDbTables.add("miscellaneous_event_type");
        identityDbTables.add("monetary_total_type");
        identityDbTables.add("notification_requirement_type");
        identityDbTables.add("on_account_payment_type");
        identityDbTables.add("on_account_payment_type_note_0");
        identityDbTables.add("order_line_reference_type");
        identityDbTables.add("order_line_type");
        identityDbTables.add("order_reference_type");
        identityDbTables.add("ordered_shipment_type");
        identityDbTables.add("package_type");
        identityDbTables.add("party_identification_type");
        identityDbTables.add("party_legal_entity_type");
        identityDbTables.add("party_name_type");
        identityDbTables.add("party_tax_scheme_type");
        identityDbTables.add("party_type");
        identityDbTables.add("party_type_party_type_item");
        identityDbTables.add("payment_mandate_type");
        identityDbTables.add("payment_means_type");
        identityDbTables.add("payment_terms_type");
        identityDbTables.add("payment_terms_type_note_item");
        identityDbTables.add("payment_terms_type_payment_c_0");
        identityDbTables.add("payment_type");
        identityDbTables.add("performance_data_line_type");
        identityDbTables.add("performance_data_line_type_n_0");
        identityDbTables.add("period_type");
        identityDbTables.add("person_type");
        identityDbTables.add("person_type_role_item");
        identityDbTables.add("physical_attribute_type");
        identityDbTables.add("physical_attribute_type_desc_0");
        identityDbTables.add("pickup_type");
        identityDbTables.add("power_of_attorney_type");
        identityDbTables.add("power_of_attorney_type_descr_0");
        identityDbTables.add("price_extension_type");
        identityDbTables.add("price_list_type");
        identityDbTables.add("price_type");
        identityDbTables.add("pricing_reference_type");
        identityDbTables.add("process_justification_type");
        identityDbTables.add("process_justification_type_d_0");
        identityDbTables.add("process_justification_type_p_0");
        identityDbTables.add("procurement_project_lot_type");
        identityDbTables.add("procurement_project_type");
        identityDbTables.add("procurement_project_type_des_0");
        identityDbTables.add("procurement_project_type_fee_0");
        identityDbTables.add("procurement_project_type_nam_0");
        identityDbTables.add("procurement_project_type_not_0");
        identityDbTables.add("project_reference_type");
        identityDbTables.add("promotional_event_line_item__0");
        identityDbTables.add("promotional_event_type");
        identityDbTables.add("promotional_specification_ty_0");
        identityDbTables.add("qualification_resolution_typ_0");
        identityDbTables.add("qualification_resolution_typ_1");
        identityDbTables.add("qualification_resolution_type");
        identityDbTables.add("qualifying_party_type");
        identityDbTables.add("qualifying_party_type_person_0");
        identityDbTables.add("quality_indicator_type");
        identityDbTables.add("quantity_type");
        identityDbTables.add("quotation_line_type");
        identityDbTables.add("quotation_line_type_note_item");
        identityDbTables.add("rail_transport_type");
        identityDbTables.add("receipt_line_type");
        identityDbTables.add("receipt_line_type_note_item");
        identityDbTables.add("receipt_line_type_reject_rea_0");
        identityDbTables.add("regulation_type");
        identityDbTables.add("related_item_type");
        identityDbTables.add("related_item_type_descriptio_0");
        identityDbTables.add("reminder_line_type");
        identityDbTables.add("reminder_line_type_note_item");
        identityDbTables.add("remittance_advice_line_type");
        identityDbTables.add("remittance_advice_line_type__0");
        identityDbTables.add("renewal_type");
        identityDbTables.add("request_for_quotation_line_t_0");
        identityDbTables.add("request_for_quotation_line_t_1");
        identityDbTables.add("request_for_tender_line_type");
        identityDbTables.add("request_for_tender_line_type_0");
        identityDbTables.add("requested_tender_total_type");
        identityDbTables.add("requested_tender_total_type__0");
        identityDbTables.add("response_type");
        identityDbTables.add("response_type_description_it_0");
        identityDbTables.add("result_of_verification_type");
        identityDbTables.add("retail_planned_impact_type");
        identityDbTables.add("road_transport_type");
        identityDbTables.add("sales_item_type");
        identityDbTables.add("secondary_hazard_type");
        identityDbTables.add("secondary_hazard_type_extens_0");
        identityDbTables.add("service_frequency_type");
        identityDbTables.add("service_provider_party_type");
        identityDbTables.add("service_provider_party_type__0");
        identityDbTables.add("shareholder_party_type");
        identityDbTables.add("shipment_stage_type");
        identityDbTables.add("shipment_type");
        identityDbTables.add("signature_type");
        identityDbTables.add("signature_type_note_item");
        identityDbTables.add("statement_line_type");
        identityDbTables.add("statement_line_type_note_item");
        identityDbTables.add("status_type");
        identityDbTables.add("status_type_description_item");
        identityDbTables.add("status_type_status_reason_it_0");
        identityDbTables.add("status_type_text_item");
        identityDbTables.add("stock_availability_report_li_0");
        identityDbTables.add("stock_availability_report_li_1");
        identityDbTables.add("stowage_type");
        identityDbTables.add("stowage_type_location_item");
        identityDbTables.add("subcontract_terms_type");
        identityDbTables.add("subcontract_terms_type_descr_0");
        identityDbTables.add("subscriber_consumption_type");
        identityDbTables.add("subscriber_consumption_type__0");
        identityDbTables.add("supplier_consumption_type");
        identityDbTables.add("supplier_consumption_type_de_0");
        identityDbTables.add("supplier_party_type");
        identityDbTables.add("supplier_party_type_addition_0");
        identityDbTables.add("tax_category_type");
        identityDbTables.add("tax_scheme_type");
        identityDbTables.add("tax_subtotal_type");
        identityDbTables.add("tax_total_type");
        identityDbTables.add("telecommunications_service_t_0");
        identityDbTables.add("telecommunications_supply_li_2");
        identityDbTables.add("telecommunications_supply_li_3");
        identityDbTables.add("telecommunications_supply_ty_0");
        identityDbTables.add("telecommunications_supply_ty_3");
        identityDbTables.add("temperature_type");
        identityDbTables.add("tender_line_type");
        identityDbTables.add("tender_line_type_note_item");
        identityDbTables.add("tender_line_type_warranty_in_0");
        identityDbTables.add("tender_preparation_type");
        identityDbTables.add("tender_preparation_type_desc_0");
        identityDbTables.add("tender_requirement_type");
        identityDbTables.add("tender_requirement_type_desc_0");
        identityDbTables.add("tender_result_type");
        identityDbTables.add("tender_result_type_descripti_0");
        identityDbTables.add("tendered_project_type");
        identityDbTables.add("tendered_project_type_fee_de_0");
        identityDbTables.add("tenderer_party_qualification_0");
        identityDbTables.add("tenderer_qualification_reque_0");
        identityDbTables.add("tenderer_qualification_reque_3");
        identityDbTables.add("tenderer_qualification_reque_4");
        identityDbTables.add("tenderer_requirement_type");
        identityDbTables.add("tenderer_requirement_type_de_0");
        identityDbTables.add("tenderer_requirement_type_na_0");
        identityDbTables.add("tendering_process_type");
        identityDbTables.add("tendering_process_type_descr_0");
        identityDbTables.add("tendering_process_type_negot_0");
        identityDbTables.add("tendering_terms_type");
        identityDbTables.add("tendering_terms_type_accepte_0");
        identityDbTables.add("tendering_terms_type_additio_0");
        identityDbTables.add("tendering_terms_type_funding_0");
        identityDbTables.add("tendering_terms_type_note_it_0");
        identityDbTables.add("tendering_terms_type_price_r_0");
        identityDbTables.add("trade_financing_type");
        identityDbTables.add("trading_terms_type");
        identityDbTables.add("trading_terms_type_informati_0");
        identityDbTables.add("transaction_conditions_type");
        identityDbTables.add("transaction_conditions_type__0");
        identityDbTables.add("transport_equipment_seal_type");
        identityDbTables.add("transport_equipment_type");
        identityDbTables.add("transport_equipment_type_dam_0");
        identityDbTables.add("transport_equipment_type_des_0");
        identityDbTables.add("transport_equipment_type_inf_0");
        identityDbTables.add("transport_equipment_type_ref_0");
        identityDbTables.add("transport_equipment_type_spe_0");
        identityDbTables.add("transport_event_type");
        identityDbTables.add("transport_event_type_descrip_0");
        identityDbTables.add("transport_execution_terms_ty_0");
        identityDbTables.add("transport_execution_terms_ty_1");
        identityDbTables.add("transport_execution_terms_ty_2");
        identityDbTables.add("transport_execution_terms_ty_3");
        identityDbTables.add("transport_handling_unit_type");
        identityDbTables.add("transport_handling_unit_type_2");
        identityDbTables.add("transport_handling_unit_type_3");
        identityDbTables.add("transport_handling_unit_type_4");
        identityDbTables.add("transport_means_type");
        identityDbTables.add("transport_means_type_registr_0");
        identityDbTables.add("transport_schedule_type");
        identityDbTables.add("transport_schedule_type_rema_0");
        identityDbTables.add("transportation_segment_type");
        identityDbTables.add("transportation_service_type");
        identityDbTables.add("transportation_service_type__0");
        identityDbTables.add("uaa_user");
        identityDbTables.add("unstructured_price_type");
        identityDbTables.add("user_invitation");
        identityDbTables.add("user_invitation_roleids");
        identityDbTables.add("utility_item_type");
        identityDbTables.add("utility_item_type_descriptio_0");
        identityDbTables.add("web_site_access_type");
        identityDbTables.add("winning_party_type");
        identityDbTables.add("work_phase_reference_type");
        identityDbTables.add("work_phase_reference_type_wo_0");
    }

    private static List<String> catalogTables = new ArrayList<>();
    static {
        catalogTables.add("activity_data_line_type");
        catalogTables.add("activity_property_type");
        catalogTables.add("address_line_type");
        catalogTables.add("address_type");
        catalogTables.add("air_transport_type");
        catalogTables.add("allowance_charge_type");
        catalogTables.add("amount_type");
        catalogTables.add("appeal_terms_type");
        catalogTables.add("appeal_terms_type_descriptio_0");
        catalogTables.add("attachment_type");
        catalogTables.add("auction_terms_type");
        catalogTables.add("auction_terms_type_condition_0");
        catalogTables.add("auction_terms_type_descripti_0");
        catalogTables.add("auction_terms_type_electroni_0");
        catalogTables.add("auction_terms_type_justifica_0");
        catalogTables.add("auction_terms_type_process_d_0");
        catalogTables.add("awarding_criterion_response__1");
        catalogTables.add("awarding_criterion_response__2");
        catalogTables.add("awarding_criterion_response__3");
        catalogTables.add("awarding_criterion_type");
        catalogTables.add("awarding_criterion_type_calc_0");
        catalogTables.add("awarding_criterion_type_desc_0");
        catalogTables.add("awarding_criterion_type_mini_0");
        catalogTables.add("awarding_criterion_type_weig_0");
        catalogTables.add("awarding_terms_type");
        catalogTables.add("awarding_terms_type_descript_0");
        catalogTables.add("awarding_terms_type_low_tend_0");
        catalogTables.add("awarding_terms_type_payment__0");
        catalogTables.add("awarding_terms_type_prize_de_0");
        catalogTables.add("awarding_terms_type_technica_0");
        catalogTables.add("billing_reference_line_type");
        catalogTables.add("billing_reference_type");
        catalogTables.add("binary_object_type");
        catalogTables.add("branch_type");
        catalogTables.add("budget_account_line_type");
        catalogTables.add("budget_account_type");
        catalogTables.add("capability_type");
        catalogTables.add("capability_type_description__0");
        catalogTables.add("card_account_type");
        catalogTables.add("catalogue_item_specification_0");
        catalogTables.add("catalogue_line_type");
        catalogTables.add("catalogue_line_type_note_item");
        catalogTables.add("catalogue_line_type_warranty_0");
        catalogTables.add("catalogue_pricing_update_lin_0");
        catalogTables.add("catalogue_reference_type");
        catalogTables.add("catalogue_request_line_type");
        catalogTables.add("catalogue_request_line_type__0");
        catalogTables.add("catalogue_type");
        catalogTables.add("catalogue_type_description_i_0");
        catalogTables.add("certificate_of_origin_applic_0");
        catalogTables.add("certificate_of_origin_applic_1");
        catalogTables.add("certificate_type");
        catalogTables.add("certificate_type_remarks_item");
        catalogTables.add("classification_category_type");
        catalogTables.add("classification_category_type_0");
        catalogTables.add("classification_scheme_type");
        catalogTables.add("classification_scheme_type_d_0");
        catalogTables.add("classification_scheme_type_n_0");
        catalogTables.add("clause_type");
        catalogTables.add("clause_type_content_item");
        catalogTables.add("clause_type_note_item");
        catalogTables.add("code_type");
        catalogTables.add("commodity_classification_type");
        catalogTables.add("communication_type");
        catalogTables.add("completed_task_type");
        catalogTables.add("completed_task_type_descript_0");
        catalogTables.add("condition_type");
        catalogTables.add("condition_type_description_i_0");
        catalogTables.add("consignment_type");
        catalogTables.add("consignment_type_carrier_ser_0");
        catalogTables.add("consignment_type_customs_cle_0");
        catalogTables.add("consignment_type_delivery_in_0");
        catalogTables.add("consignment_type_forwarder_s_0");
        catalogTables.add("consignment_type_handling_in_0");
        catalogTables.add("consignment_type_haulage_ins_0");
        catalogTables.add("consignment_type_information_0");
        catalogTables.add("consignment_type_remarks_item");
        catalogTables.add("consignment_type_special_ins_0");
        catalogTables.add("consignment_type_special_ser_0");
        catalogTables.add("consignment_type_summary_des_0");
        catalogTables.add("consignment_type_tariff_desc_0");
        catalogTables.add("consumption_average_type");
        catalogTables.add("consumption_average_type_des_0");
        catalogTables.add("consumption_correction_type");
        catalogTables.add("consumption_correction_type__0");
        catalogTables.add("consumption_history_type");
        catalogTables.add("consumption_history_type_des_0");
        catalogTables.add("consumption_line_type");
        catalogTables.add("consumption_point_type");
        catalogTables.add("consumption_point_type_descr_0");
        catalogTables.add("consumption_report_reference_1");
        catalogTables.add("consumption_report_type");
        catalogTables.add("consumption_report_type_desc_0");
        catalogTables.add("consumption_type");
        catalogTables.add("contact_type");
        catalogTables.add("contract_execution_requireme_0");
        catalogTables.add("contract_execution_requireme_3");
        catalogTables.add("contract_execution_requireme_4");
        catalogTables.add("contract_extension_type");
        catalogTables.add("contract_extension_type_opti_0");
        catalogTables.add("contract_type");
        catalogTables.add("contract_type_description_it_0");
        catalogTables.add("contract_type_note_item");
        catalogTables.add("contracting_activity_type");
        catalogTables.add("contracting_party_type");
        catalogTables.add("contracting_party_type_type");
        catalogTables.add("corporate_registration_schem_0");
        catalogTables.add("country_type");
        catalogTables.add("credit_account_type");
        catalogTables.add("credit_note_line_type");
        catalogTables.add("credit_note_line_type_note_i_0");
        catalogTables.add("customer_party_type");
        catalogTables.add("customer_party_type_addition_0");
        catalogTables.add("customs_declaration_type");
        catalogTables.add("data_monitoring_clause_type");
        catalogTables.add("data_monitoring_clause_type__0");
        catalogTables.add("data_monitoring_clause_type__1");
        catalogTables.add("data_monitoring_clause_type__2");
        catalogTables.add("data_monitoring_clause_type__3");
        catalogTables.add("data_monitoring_clause_type__4");
        catalogTables.add("debit_note_line_type");
        catalogTables.add("debit_note_line_type_note_it_0");
        catalogTables.add("declaration_type");
        catalogTables.add("declaration_type_description_0");
        catalogTables.add("declaration_type_name_item");
        catalogTables.add("delivery_terms_type");
        catalogTables.add("delivery_type");
        catalogTables.add("delivery_unit_type");
        catalogTables.add("dependent_price_reference_ty_0");
        catalogTables.add("despatch_advice_type");
        catalogTables.add("despatch_advice_type_note_it_0");
        catalogTables.add("despatch_line_type");
        catalogTables.add("despatch_line_type_note_item");
        catalogTables.add("despatch_line_type_outstandi_0");
        catalogTables.add("despatch_type");
        catalogTables.add("dimension_type");
        catalogTables.add("dimension_type_description_i_0");
        catalogTables.add("document_clause_type");
        catalogTables.add("document_distribution_type");
        catalogTables.add("document_reference_type");
        catalogTables.add("document_response_type");
        catalogTables.add("duty_type");
        catalogTables.add("economic_operator_role_type");
        catalogTables.add("economic_operator_role_type__0");
        catalogTables.add("economic_operator_short_list_0");
        catalogTables.add("economic_operator_short_list_2");
        catalogTables.add("emission_calculation_method__0");
        catalogTables.add("endorsement_type");
        catalogTables.add("endorsement_type_remarks_item");
        catalogTables.add("endorser_party_type");
        catalogTables.add("energy_tax_report_type");
        catalogTables.add("energy_water_supply_type");
        catalogTables.add("environmental_emission_type");
        catalogTables.add("environmental_emission_type__2");
        catalogTables.add("evaluation_criterion_type");
        catalogTables.add("evaluation_criterion_type_de_0");
        catalogTables.add("evaluation_criterion_type_ex_0");
        catalogTables.add("event_comment_type");
        catalogTables.add("event_line_item_type");
        catalogTables.add("event_tactic_enumeration_type");
        catalogTables.add("event_tactic_type");
        catalogTables.add("event_type");
        catalogTables.add("event_type_description_item");
        catalogTables.add("evidence_supplied_type");
        catalogTables.add("evidence_type");
        catalogTables.add("evidence_type_candidate_stat_0");
        catalogTables.add("evidence_type_description_it_0");
        catalogTables.add("exception_criteria_line_type");
        catalogTables.add("exception_criteria_line_type_0");
        catalogTables.add("exception_notification_line__0");
        catalogTables.add("exception_notification_line__1");
        catalogTables.add("exception_notification_line__2");
        catalogTables.add("exchange_rate_type");
        catalogTables.add("external_reference_type");
        catalogTables.add("financial_account_type");
        catalogTables.add("financial_guarantee_type");
        catalogTables.add("financial_guarantee_type_des_0");
        catalogTables.add("financial_institution_type");
        catalogTables.add("forecast_exception_criterion_2");
        catalogTables.add("forecast_exception_type");
        catalogTables.add("forecast_line_type");
        catalogTables.add("forecast_line_type_note_item");
        catalogTables.add("forecast_revision_line_type");
        catalogTables.add("forecast_revision_line_type__0");
        catalogTables.add("forecast_revision_line_type__1");
        catalogTables.add("framework_agreement_type");
        catalogTables.add("framework_agreement_type_fre_0");
        catalogTables.add("framework_agreement_type_jus_0");
        catalogTables.add("goods_item_container_type");
        catalogTables.add("goods_item_type");
        catalogTables.add("goods_item_type_description__0");
        catalogTables.add("hazardous_goods_transit_type");
        catalogTables.add("hazardous_item_type");
        catalogTables.add("hazardous_item_type_addition_0");
        catalogTables.add("immobilized_security_type");
        catalogTables.add("instruction_for_returns_line_0");
        catalogTables.add("instruction_for_returns_line_1");
        catalogTables.add("inventory_report_line_type");
        catalogTables.add("inventory_report_line_type_n_0");
        catalogTables.add("invoice_line_type");
        catalogTables.add("invoice_line_type_note_item");
        catalogTables.add("item_comparison_type");
        catalogTables.add("item_identification_type");
        catalogTables.add("item_information_request_lin_0");
        catalogTables.add("item_information_request_typ_0");
        catalogTables.add("item_information_request_type");
        catalogTables.add("item_information_response_ty_0");
        catalogTables.add("item_information_response_ty_1");
        catalogTables.add("item_instance_type");
        catalogTables.add("item_location_quantity_type");
        catalogTables.add("item_location_quantity_type__0");
        catalogTables.add("item_management_profile_type");
        catalogTables.add("item_management_profile_type_0");
        catalogTables.add("item_property_group_type");
        catalogTables.add("item_property_range_type");
        catalogTables.add("item_property_type");
        catalogTables.add("item_property_type_value_dec_0");
        catalogTables.add("item_property_type_value_item");
        catalogTables.add("item_type");
        catalogTables.add("language_type");
        catalogTables.add("line_item_type");
        catalogTables.add("line_reference_type");
        catalogTables.add("line_response_type");
        catalogTables.add("location_coordinate_type");
        catalogTables.add("location_type");
        catalogTables.add("lot_identification_type");
        catalogTables.add("maritime_transport_type");
        catalogTables.add("maritime_transport_type_ship_0");
        catalogTables.add("meter_property_type");
        catalogTables.add("meter_property_type_value_qu_0");
        catalogTables.add("meter_reading_type");
        catalogTables.add("meter_reading_type_meter_rea_0");
        catalogTables.add("meter_type");
        catalogTables.add("miscellaneous_event_type");
        catalogTables.add("monetary_total_type");
        catalogTables.add("notification_requirement_type");
        catalogTables.add("on_account_payment_type");
        catalogTables.add("on_account_payment_type_note_0");
        catalogTables.add("order_line_reference_type");
        catalogTables.add("order_line_type");
        catalogTables.add("order_reference_type");
        catalogTables.add("order_response_simple_type");
        catalogTables.add("order_type");
        catalogTables.add("ordered_shipment_type");
        catalogTables.add("package_type");
        catalogTables.add("party_identification_type");
        catalogTables.add("party_legal_entity_type");
        catalogTables.add("party_name_type");
        catalogTables.add("party_tax_scheme_type");
        catalogTables.add("party_type");
        catalogTables.add("party_type_party_type_item");
        catalogTables.add("payment_mandate_type");
        catalogTables.add("payment_means_type");
        catalogTables.add("payment_terms_type");
        catalogTables.add("payment_terms_type_note_item");
        catalogTables.add("payment_terms_type_payment_c_0");
        catalogTables.add("payment_type");
        catalogTables.add("performance_data_line_type");
        catalogTables.add("performance_data_line_type_n_0");
        catalogTables.add("period_type");
        catalogTables.add("person_type");
        catalogTables.add("person_type_role_item");
        catalogTables.add("physical_attribute_type");
        catalogTables.add("physical_attribute_type_desc_0");
        catalogTables.add("pickup_type");
        catalogTables.add("power_of_attorney_type");
        catalogTables.add("power_of_attorney_type_descr_0");
        catalogTables.add("ppap_request_type");
        catalogTables.add("ppap_request_type_document_t_0");
        catalogTables.add("ppap_response_type");
        catalogTables.add("price_extension_type");
        catalogTables.add("price_list_type");
        catalogTables.add("price_type");
        catalogTables.add("pricing_reference_type");
        catalogTables.add("process_justification_type");
        catalogTables.add("process_justification_type_d_0");
        catalogTables.add("process_justification_type_p_0");
        catalogTables.add("procurement_project_lot_type");
        catalogTables.add("procurement_project_type");
        catalogTables.add("procurement_project_type_des_0");
        catalogTables.add("procurement_project_type_fee_0");
        catalogTables.add("procurement_project_type_nam_0");
        catalogTables.add("procurement_project_type_not_0");
        catalogTables.add("project_reference_type");
        catalogTables.add("promotional_event_line_item__0");
        catalogTables.add("promotional_event_type");
        catalogTables.add("promotional_specification_ty_0");
        catalogTables.add("qualification_resolution_typ_0");
        catalogTables.add("qualification_resolution_typ_1");
        catalogTables.add("qualification_resolution_type");
        catalogTables.add("qualifying_party_type");
        catalogTables.add("qualifying_party_type_person_0");
        catalogTables.add("quality_indicator_type");
        catalogTables.add("quantity_type");
        catalogTables.add("quotation_line_type");
        catalogTables.add("quotation_line_type_note_item");
        catalogTables.add("quotation_type");
        catalogTables.add("quotation_type_note_item");
        catalogTables.add("rail_transport_type");
        catalogTables.add("receipt_advice_type");
        catalogTables.add("receipt_advice_type_note_item");
        catalogTables.add("receipt_line_type");
        catalogTables.add("receipt_line_type_note_item");
        catalogTables.add("receipt_line_type_reject_rea_0");
        catalogTables.add("regulation_type");
        catalogTables.add("related_item_type");
        catalogTables.add("related_item_type_descriptio_0");
        catalogTables.add("reminder_line_type");
        catalogTables.add("reminder_line_type_note_item");
        catalogTables.add("remittance_advice_line_type");
        catalogTables.add("remittance_advice_line_type__0");
        catalogTables.add("renewal_type");
        catalogTables.add("request_for_quotation_line_t_0");
        catalogTables.add("request_for_quotation_line_t_1");
        catalogTables.add("request_for_quotation_type");
        catalogTables.add("request_for_quotation_type_n_0");
        catalogTables.add("request_for_tender_line_type");
        catalogTables.add("request_for_tender_line_type_0");
        catalogTables.add("requested_tender_total_type");
        catalogTables.add("requested_tender_total_type__0");
        catalogTables.add("response_type");
        catalogTables.add("response_type_description_it_0");
        catalogTables.add("result_of_verification_type");
        catalogTables.add("retail_planned_impact_type");
        catalogTables.add("road_transport_type");
        catalogTables.add("sales_item_type");
        catalogTables.add("secondary_hazard_type");
        catalogTables.add("secondary_hazard_type_extens_0");
        catalogTables.add("service_frequency_type");
        catalogTables.add("service_provider_party_type");
        catalogTables.add("service_provider_party_type__0");
        catalogTables.add("shareholder_party_type");
        catalogTables.add("shipment_stage_type");
        catalogTables.add("shipment_type");
        catalogTables.add("signature_type");
        catalogTables.add("signature_type_note_item");
        catalogTables.add("statement_line_type");
        catalogTables.add("statement_line_type_note_item");
        catalogTables.add("status_type");
        catalogTables.add("status_type_description_item");
        catalogTables.add("status_type_status_reason_it_0");
        catalogTables.add("status_type_text_item");
        catalogTables.add("stock_availability_report_li_0");
        catalogTables.add("stock_availability_report_li_1");
        catalogTables.add("stowage_type");
        catalogTables.add("stowage_type_location_item");
        catalogTables.add("subcontract_terms_type");
        catalogTables.add("subcontract_terms_type_descr_0");
        catalogTables.add("subscriber_consumption_type");
        catalogTables.add("subscriber_consumption_type__0");
        catalogTables.add("supplier_consumption_type");
        catalogTables.add("supplier_consumption_type_de_0");
        catalogTables.add("supplier_party_type");
        catalogTables.add("supplier_party_type_addition_0");
        catalogTables.add("tax_category_type");
        catalogTables.add("tax_scheme_type");
        catalogTables.add("tax_subtotal_type");
        catalogTables.add("tax_total_type");
        catalogTables.add("telecommunications_service_t_0");
        catalogTables.add("telecommunications_supply_li_2");
        catalogTables.add("telecommunications_supply_li_3");
        catalogTables.add("telecommunications_supply_ty_0");
        catalogTables.add("telecommunications_supply_ty_3");
        catalogTables.add("temperature_type");
        catalogTables.add("tender_line_type");
        catalogTables.add("tender_line_type_note_item");
        catalogTables.add("tender_line_type_warranty_in_0");
        catalogTables.add("tender_preparation_type");
        catalogTables.add("tender_preparation_type_desc_0");
        catalogTables.add("tender_requirement_type");
        catalogTables.add("tender_requirement_type_desc_0");
        catalogTables.add("tender_result_type");
        catalogTables.add("tender_result_type_descripti_0");
        catalogTables.add("tendered_project_type");
        catalogTables.add("tendered_project_type_fee_de_0");
        catalogTables.add("tenderer_party_qualification_0");
        catalogTables.add("tenderer_qualification_reque_0");
        catalogTables.add("tenderer_qualification_reque_3");
        catalogTables.add("tenderer_qualification_reque_4");
        catalogTables.add("tenderer_requirement_type");
        catalogTables.add("tenderer_requirement_type_de_0");
        catalogTables.add("tenderer_requirement_type_na_0");
        catalogTables.add("tendering_process_type");
        catalogTables.add("tendering_process_type_descr_0");
        catalogTables.add("tendering_process_type_negot_0");
        catalogTables.add("tendering_terms_type");
        catalogTables.add("tendering_terms_type_accepte_0");
        catalogTables.add("tendering_terms_type_additio_0");
        catalogTables.add("tendering_terms_type_funding_0");
        catalogTables.add("tendering_terms_type_note_it_0");
        catalogTables.add("tendering_terms_type_price_r_0");
        catalogTables.add("trade_financing_type");
        catalogTables.add("trading_terms_type");
        catalogTables.add("trading_terms_type_informati_0");
        catalogTables.add("transaction_conditions_type");
        catalogTables.add("transaction_conditions_type__0");
        catalogTables.add("transport_equipment_seal_type");
        catalogTables.add("transport_equipment_type");
        catalogTables.add("transport_equipment_type_dam_0");
        catalogTables.add("transport_equipment_type_des_0");
        catalogTables.add("transport_equipment_type_inf_0");
        catalogTables.add("transport_equipment_type_ref_0");
        catalogTables.add("transport_equipment_type_spe_0");
        catalogTables.add("transport_event_type");
        catalogTables.add("transport_event_type_descrip_0");
        catalogTables.add("transport_execution_plan_req_2");
        catalogTables.add("transport_execution_plan_req_3");
        catalogTables.add("transport_execution_plan_req_4");
        catalogTables.add("transport_execution_plan_req_5");
        catalogTables.add("transport_execution_plan_typ_0");
        catalogTables.add("transport_execution_plan_typ_1");
        catalogTables.add("transport_execution_plan_typ_2");
        catalogTables.add("transport_execution_plan_typ_3");
        catalogTables.add("transport_execution_plan_type");
        catalogTables.add("transport_execution_terms_ty_0");
        catalogTables.add("transport_execution_terms_ty_1");
        catalogTables.add("transport_execution_terms_ty_2");
        catalogTables.add("transport_execution_terms_ty_3");
        catalogTables.add("transport_handling_unit_type");
        catalogTables.add("transport_handling_unit_type_2");
        catalogTables.add("transport_handling_unit_type_3");
        catalogTables.add("transport_handling_unit_type_4");
        catalogTables.add("transport_means_type");
        catalogTables.add("transport_means_type_registr_0");
        catalogTables.add("transport_schedule_type");
        catalogTables.add("transport_schedule_type_rema_0");
        catalogTables.add("transportation_segment_type");
        catalogTables.add("transportation_service_type");
        catalogTables.add("transportation_service_type__0");
        catalogTables.add("unstructured_price_type");
        catalogTables.add("utility_item_type");
        catalogTables.add("utility_item_type_descriptio_0");
        catalogTables.add("web_site_access_type");
        catalogTables.add("winning_party_type");
        catalogTables.add("work_phase_reference_type");
        catalogTables.add("work_phase_reference_type_wo_0Tables");
    }

    private Map getConfigs() {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            PropertySource<?> applicationYamlPropertySource = loader.load(
                    "properties", new ClassPathResource("releases/r5/r5migration.yml"), null);

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
