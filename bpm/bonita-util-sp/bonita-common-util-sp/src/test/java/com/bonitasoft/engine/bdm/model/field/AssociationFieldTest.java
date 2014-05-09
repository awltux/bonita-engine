/*******************************************************************************
 * Copyright (C) 2014 Bonitasoft S.A.
 * Bonitasoft is a trademark of Bonitasoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.bdm.model.field;

import static com.bonitasoft.engine.bdm.model.assertion.FieldAssert.assertThat;
import static com.bonitasoft.engine.bdm.model.builder.BusinessObjectBuilder.aBO;
import static com.bonitasoft.engine.bdm.model.builder.FieldBuilder.aBooleanField;

import org.junit.Test;

import com.bonitasoft.engine.bdm.model.BusinessObject;
import com.bonitasoft.engine.bdm.model.field.AssociationField.Type;

/**
 * @author Colin PUY
 */
public class AssociationFieldTest {

    private BusinessObject aBo = aBO("boName").withField(aBooleanField("aField").build()).build();

    @Test
    public void should_not_be_marshallizable_without_name_type_and_reference() throws Exception {
        AssociationField field = new AssociationField();
        field.setName("aName");
        field.setType(Type.AGGREGATION);

        assertThat(field).cannotBeMarshalled();

        //
        field = new AssociationField();
        field.setType(Type.AGGREGATION);
        field.setReference(aBo);

        assertThat(field).cannotBeMarshalled();

        //
        field = new AssociationField();
        field.setReference(aBo);
        field.setName("aName");

        assertThat(field).cannotBeMarshalled();
    }

    @Test
    public void should_be_marshallizable_with_only_name_type_and_reference() throws Exception {
        AssociationField field = new AssociationField();
        field.setName("aName");
        field.setType(Type.AGGREGATION);
        field.setReference(aBo);

        assertThat(field).canBeMarshalled();
    }
}
