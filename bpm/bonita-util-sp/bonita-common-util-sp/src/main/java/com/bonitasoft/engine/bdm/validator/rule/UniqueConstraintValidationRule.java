/*******************************************************************************
 * Copyright (C) 2014 Bonitasoft S.A.
 * Bonitasoft is a trademark of Bonitasoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * Bonitasoft, 32 rue Gustave Eiffel 38000 Grenoble
 * or Bonitasoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.bdm.validator.rule;

import com.bonitasoft.engine.bdm.UniqueConstraint;
import com.bonitasoft.engine.bdm.validator.SQLNameValidator;
import com.bonitasoft.engine.bdm.validator.ValidationStatus;

public class UniqueConstraintValidationRule implements ValidationRule {

    private static final int MAX_CONSTRAINTNAME_LENGTH = 25;

    private final SQLNameValidator sqlNameValidator;

    public UniqueConstraintValidationRule() {
        sqlNameValidator = new SQLNameValidator(MAX_CONSTRAINTNAME_LENGTH);
    }

    @Override
    public boolean appliesTo(final Object modelElement) {
        return modelElement instanceof UniqueConstraint;
    }

    @Override
    public ValidationStatus checkRule(final Object modelElement) {
        if (!appliesTo(modelElement)) {
            throw new IllegalArgumentException(UniqueConstraintValidationRule.class.getName() + " doesn't handle validation for "
                    + modelElement.getClass().getName());
        }
        final UniqueConstraint uc = (UniqueConstraint) modelElement;
        final ValidationStatus status = new ValidationStatus();
        final String name = uc.getName();
        if (name == null || name.isEmpty()) {
            status.addError("A unique constraint must have name");
            return status;
        }
        final boolean isValid = sqlNameValidator.isValid(name);
        if (!isValid) {
            status.addError(name + " is not a valid SQL identifier");
        }

        if (uc.getFieldNames().isEmpty()) {
            status.addError(name + " unique constraint must have at least one field declared");
        }

        return status;
    }

}
