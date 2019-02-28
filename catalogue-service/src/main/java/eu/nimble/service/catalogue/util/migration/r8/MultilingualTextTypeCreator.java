package eu.nimble.service.catalogue.util.migration.r8;

import eu.nimble.service.catalogue.util.migration.DBConnector;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 27-Feb-19.
 */
public class MultilingualTextTypeCreator extends DBConnector {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MultilingualTextTypeCreator.class);

    public static void main(String[] args) {
        new MultilingualTextTypeCreator().migrateTextTypes();
        logger.info("Text type migration script completed");
    }

    private void migrateTextTypes(){
        HibernateUtility hibernateUtility = getHibernateUtility();
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection(url,username, password);
            logger.info("Connection obtained");
            stmt = c.createStatement();
            String removeDanglingItemProperties = "delete from item_property_type_value_item where value_items_item_property_ty_0 is null";
            stmt.executeUpdate(removeDanglingItemProperties);

            // Handle CatalogueType name
            try {
                handleSingleTextType(hibernateUtility, stmt, "name_", "name__catalogue_type_hjid", "catalogue_type");
                logger.info("Handled CatalogueType name");
            } catch (Exception e) {
                logger.info("Failed to update catalogue_type table", e);
            }
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

            try {
                stmt.executeUpdate("ALTER TABLE catalogue_type DROP COLUMN IF EXISTS name_");
            } catch (Exception e) {
                logger.info("Failed to alter catalogue type", e);
            }
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
    private void handleTextTypeList(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String tableName,String referenceCol) throws Exception{
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
    private void handleStringToTextTypeList(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String tableName,String referenceCol) throws Exception{
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
    private void handleSingleTextType(HibernateUtility hibernateUtility,Statement stmt,String oldCol,String newCol,String tableName) throws Exception{
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

}
