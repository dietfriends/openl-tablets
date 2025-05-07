package org.openl.rules.convertor;

import java.lang.reflect.Array;

import org.openl.binding.IBindingContext;
import org.openl.types.IOpenClass;
import org.openl.util.MessageUtils;

class String2ClassConvertor implements IString2DataConvertor<Class<?>>, IString2DataConverterWithContext<Class<?>> {

    public static final String ARRAY_SUFIX = "[]";

    @Override
    public Class<?> parse(String data, String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> parse(String data, String format, IBindingContext cxt) {
        if (data == null) {
            return null;
        }

        String typeName;
        if (data.endsWith(ARRAY_SUFIX)) {
            typeName = data.substring(0, data.length() - 2);
        } else {
            typeName = data;
        }

        IOpenClass openClass = cxt.findType(typeName);

        if (openClass == null) {
            throw new IllegalArgumentException(MessageUtils.getTypeNotFoundMessage(data));
        }

        Class<?> clazz = openClass.getInstanceClass();
        if (data.endsWith(ARRAY_SUFIX)) {
            clazz = Array.newInstance(clazz, 0).getClass();
        }
        return clazz;
    }
}
