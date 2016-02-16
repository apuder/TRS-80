package org.puder.trs80.tpk.annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JType;

import org.jsonschema2pojo.AbstractAnnotator;

import java.util.ArrayList;
import java.util.List;

public class ORMLiteAnnotator extends AbstractAnnotator {

    @Override
    public void propertyOrder(JDefinedClass clazz, JsonNode propertiesNode) {
        clazz.annotate(DatabaseTable.class).param("tableName", clazz.name());
    }

    @Override
    public void propertyField(JFieldVar field, JDefinedClass clazz, String propertyName,
            JsonNode propertyNode) {
        field.annotate(DatabaseField.class);
        if (field.type().erasure().equals(field.type().owner().ref(List.class))) {
            Class indexClazz = String.class;
            JType indexType = clazz.getPackage().owner().ref(indexClazz);
            field.type(clazz.getPackage().owner().ref(ArrayList.class).narrow(indexType));
        }
    }
}