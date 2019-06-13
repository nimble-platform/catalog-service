package eu.nimble.service.catalogue.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.UblUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by suat on 13-Jun-19.
 */
public class LoggerUtil {
    public static Map<String, String> getMDCParamMapForCatalogueLine(CatalogueLineType line, CatalogueEvent catalogueEvent) {
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", catalogueEvent.getActivity());
        paramMap.put("productId", line.getID());

        if(line.getGoodsItem() != null && line.getGoodsItem().getItem() != null) {
            ItemType item = line.getGoodsItem().getItem();
            if(item.getName() != null) {
                String itemName = UblUtil.getText(item.getName());
                paramMap.put("productName", itemName);
            }
            if(item.getManufacturerParty() != null){
                PartyType manufacturer = item.getManufacturerParty();
                if(manufacturer.getPartyName() != null) {
                    String partyName = UblUtil.getName(manufacturer);
                    paramMap.put("companyName", partyName);
                }
                if(manufacturer.getPartyIdentification() != null) {
                    String partyId = UblUtil.getId(manufacturer.getPartyIdentification());
                    paramMap.put("companyId", partyId);
                }

            }
        }

        return paramMap;
    }
}
