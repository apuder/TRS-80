package org.puder.trs80.tpk.annotator;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;

import org.jsonschema2pojo.AbstractAnnotator;
import org.puder.trs80.tpk.Base;

public class DBFlowAnnotator extends AbstractAnnotator {

    @Override
    public void propertyField(JFieldVar field, JDefinedClass clazz, String propertyName,
            JsonNode propertyNode) {
        clazz._extends(Base.class);
    }
}