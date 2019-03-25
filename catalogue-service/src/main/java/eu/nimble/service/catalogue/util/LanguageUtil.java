package eu.nimble.service.catalogue.util;

import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;

import java.util.List;

public class LanguageUtil {

    public static String getValue(List<TextType> texts,String defaultLanguage){
        if(texts == null || texts.size() == 0){
            return null;
        }
        String englishValue = null;
        for(TextType text:texts){
            if(text.getLanguageID().contentEquals("en")){
                englishValue = text.getValue();
            }
            else if(text.getLanguageID().contentEquals(defaultLanguage)){
                return text.getValue();
            }
        }

        if(englishValue != null){
            return englishValue;
        }

        return texts.get(0).getValue();
    }
}
