/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.base.INamedThing;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */
public class AmbiguousFieldException extends OpenlNotCheckedException {

    private final Collection<IOpenField> matchingFields;

    private final String fieldName;

    public AmbiguousFieldException(String fieldName, List<IOpenField> matchingFields) {
        this.fieldName = fieldName;
        this.matchingFields = Collections.unmodifiableList(matchingFields);
    }

    public Collection<IOpenField> getMatchingFields() {
        return matchingFields;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        sb.append("Field '").append(fieldName).append("' is ambiguous");
        if (matchingFields != null) {
            sb.append(":\nMatching fields:\n");
            boolean first = true;
            Set<IOpenClass> openClasses = new HashSet<>();
            for (IOpenField f : matchingFields) {
                openClasses.add(f.getDeclaringClass());
            }
            for (IOpenField f : matchingFields) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(openClasses.size() == 1 ? "" : f.getDeclaringClass().getDisplayName(INamedThing.SHORT) + ".").append(f.getDisplayName(INamedThing.SHORT));
                first = false;
            }
        } else {
            sb.append(".");
        }
        return sb.toString();
    }

}
