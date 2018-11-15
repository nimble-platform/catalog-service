package eu.nimble.service.catalogue.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ClassUtils;
import org.jvnet.hyperjaxb3.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 02-Aug-18.
 */
public class HyperJaxbSerializationUtil {

    private static final Logger log = LoggerFactory.getLogger(HyperJaxbSerializationUtil.class);

    /**
     * A helper method to check built-in lists of the HyperJaxb-generated classes during the update operations.
     * HyperJaxb creates two lists for managing the ORM operations of lists of built-in types e.g. List<String>.
     * The lists are named with listName and listNameItems where the latter lists includes the hjid associations for
     * items of the lists. So, in an update operation clients are supposed to modify the latter list. However, the
     * latter is not included in the original XSD data model. So, we do not expect users to update it. Therefore, we
     * inject this method.
     * <p>
     * For example, let's consider {@link eu.nimble.service.model.ubl.commonaggregatecomponents.ItemPropertyType} class.
     * It contains a field named {@code value} and another one {@code valueItems}. The former one is the one that
     * corresponds to the field defined in the XSD schema. The latter one is the transient list, which is connected
     * to the ORM annotation to keep the list of "values". We expect users to modify the {@code value} field but
     * we should reflect the changes to the DB via the {@code valueItems} list
     * <p>
     * This method traverses the built-in lists of the given object recursively on all non-parameterized types and
     * lists and copy the values of the former list (i.e. the updated values) into the latter list so that the updates
     * will be reflected into the database. Note that the method checks only the list data structures.
     */
    public static <T> T checkBuiltInLists(String serializedObject, Class<T> klass) throws IOException {
        ObjectMapper objectMapperForTransientFields = new ObjectMapper();
        objectMapperForTransientFields.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, false);
        ObjectMapper standartObjectMapper = new ObjectMapper();
        standartObjectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);

        T originalObject = standartObjectMapper.readValue(serializedObject, klass);
        T originalObjectWithItemsLists = objectMapperForTransientFields.readValue(serializedObject, klass);

        checkBuiltInLists(originalObject, originalObjectWithItemsLists, new ArrayList<>());
        return originalObjectWithItemsLists;
    }

    private static <T> void checkBuiltInLists(Object originalObject, Object originalObjectWithItemsLists, List<Long> processedObjects) {
        // to prevent recursion, check whether the passed object is already processed
        // check only the objects with a hjid value
        Long hjid = getHjidValue(originalObjectWithItemsLists);
        if (hjid != null && processedObjects.contains(hjid)) {
            return;
        } else {
            processedObjects.add(hjid);
        }

        List<Field> fieldsToBeChecked = new ArrayList<>();
        // keeps the editable field names corresponding to the items hjid lists
        List<String> fieldsWithCorrespondingHjidItemsList = new ArrayList<>();
        Field[] fields = originalObjectWithItemsLists.getClass().getDeclaredFields();
        for (Field f : fields) {
            // discard static fields
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }

            Class type = f.getType();

            Type genericType = f.getGenericType();
            boolean targetedList = genericType instanceof ParameterizedType && f.getName().endsWith("Items");

            // check the field name
            // consider only parameterized types with "Items" suffix at the end of their names
            if (targetedList) {
                // retrieve the editable list e.g. ("value" field of ItemProperty class) from
                // the original object, which still has the values provided by the users
                List<T> editableList;
                List<T> editableListObjectWithItems;

                Field f2 = getNonItemsList(f, fields);
                f2.setAccessible(true);
                try {
                    editableList = (List<T>) f2.get(originalObject);
                    editableListObjectWithItems = (List<T>) f2.get(originalObjectWithItemsLists);
                    fieldsWithCorrespondingHjidItemsList.add(f2.getName());

                } catch (IllegalAccessException e) {
                    String msg = String.format("Failed to access %s field", f2.getName());
                    log.error(msg);
                    throw new RuntimeException(msg, e);
                }
                f2.setAccessible(false);

                if (editableList == null) {
                    log.warn("No original field corresponding to field: {}", f.getName());
                    continue;
                }

                // get the second list e.g. ("valueItems" field of ItemProperty class)
                List<Item<T>> hjList;
                f.setAccessible(true);
                try {
                    hjList = (List<Item<T>>) f.get(originalObjectWithItemsLists);
                    // no need to map the new items if the previous list is null
                    if (hjList == null) {
                        continue;
                    }

                } catch (IllegalAccessException e) {
                    String msg = String.format("Failed to initialize %s field", f.getName());
                    log.error(msg);
                    throw new RuntimeException(msg, e);
                }
                f.setAccessible(false);

                Class hjType = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                // the updated list is larger than the previous one
                // so, copy the first n (where n is the size of hjlist) values from editable list to hjlist
                // for the rest of the items, create new elements in the hjlist
                if (editableList.size() > hjList.size()) {
                    for (int i = 0; i < editableList.size(); i++) {
                        // update the existing ones
                        if (i < hjList.size()) {
                            Item<T> instance = hjList.get(i);
                            instance.setItem(editableList.get(i));

                            // create new items
                        } else {
                            try {
                                Item<T> instance = (Item<T>) hjType.newInstance();
                                instance.setItem(editableList.get(i));
                                hjList.add(instance);

                            } catch (InstantiationException | IllegalAccessException e) {
                                String msg = String.format("Failed to populate %s field", f.getName());
                                log.error(msg);
                                throw new RuntimeException(msg, e);
                            }
                        }
                    }

                    // the editable list is equal to smaller than the hjlist
                } else {
                    // first delete the excess items from the hjlist
                    hjList.subList(editableList.size(), hjList.size()).clear();
                    // then, update the existing ones
                    for (int i = 0; i < editableList.size(); i++) {

                        if (i < hjList.size()) {
                            Item<T> instance = hjList.get(i);
                            T copyValue = editableList.get(i);
                            instance.setItem(copyValue);
                        }
                    }
                }

                //hjList = new ArrayList<>(hjList);
                // clear the original list in order not to create duplicate values
                //editableListObjectWithItems = new ArrayList<>();
                try {
                    editableListObjectWithItems = new ArrayList<>();
                    f2.setAccessible(true);
                    f2.set(originalObjectWithItemsLists, editableListObjectWithItems);
                    f2.setAccessible(false);
                } catch (IllegalAccessException e) {
                    String msg = String.format("Failed to populate %s field", f2.getName());
                    log.error(msg);
                    throw new RuntimeException(msg, e);
                }
                System.out.println();


                // find the other fields that should be checked
            } else {
                // if the field is a list or array, get the type of inner elements
                if (type.getName().equals(List.class.getName()) || type.isArray()) {
                    if (type.getName().equals(List.class.getName())) {
                        type = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    } else {
                        type = type.getComponentType();
                    }
                }

                // exclude the primitive types and the targeted fields (i.e. the value and valueItems fields)
                if (!ClassUtils.isPrimitiveOrWrapper(type)) {
                    fieldsToBeChecked.add(f);
                }
            }
        }

        // apply the check on other suitable fields
        for (Field f : fieldsToBeChecked) {
            // if the field has a corresponding items hjid list (e.g. valueItems) then don't consider the field
            if(fieldsWithCorrespondingHjidItemsList.contains(f.getName())) {
                continue;
            }

            f.setAccessible(true);
            try {
                Object object = f.get(originalObject);
                Object objectWithItemsLists = f.get(originalObjectWithItemsLists);
                if (object != null) {
                    if (object instanceof List) {
                        List<Object> objectList = (List<Object>) object;
                        List<Object> objectListWithItemsLists = (List<Object>) objectWithItemsLists;
                        for (int i = 0; i < ((List<Object>) object).size(); i++) {
                            checkBuiltInLists(objectList.get(i), objectListWithItemsLists.get(i), processedObjects);
                        }
                    } else {
                        checkBuiltInLists(object, objectWithItemsLists, processedObjects);
                    }
                }

            } catch (IllegalAccessException e) {
                String msg = String.format("Failed to access %s field", f.getName());
                log.error(msg);
                throw new RuntimeException(msg, e);
            }
            f.setAccessible(false);
        }
    }

    /**
     * Gets the editable list corresponding to the items list (i.e. values list for valueItems)
     *
     * @param f
     * @param fields
     * @return
     */
    private static Field getNonItemsList(Field f, Field[] fields) {
        for (Field f2 : fields) {
            if (f2.getName().contentEquals(f.getName().substring(0, f.getName().indexOf("Items")))) {
                f2.setAccessible(true);
                return f2;
            }
        }
        return null;
    }

    private static Long getHjidValue(Object o) {
        Long hjid = null;
        Field[] fields = o.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals("hjid")) {
                f.setAccessible(true);
                try {
                    hjid = (Long) f.get(o);
                } catch (IllegalAccessException e) {
                    String msg = String.format("Failed to access hjid field");
                    log.error(msg);
                    throw new RuntimeException(msg, e);
                }
            }
        }
        return hjid;
    }
}
