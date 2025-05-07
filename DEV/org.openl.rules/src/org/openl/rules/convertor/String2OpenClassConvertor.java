package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.types.IOpenClass;
import org.openl.util.MessageUtils;

class String2OpenClassConvertor implements IString2DataConvertor<IOpenClass>, IString2DataConverterWithContext<IOpenClass> {

    public static final String ARRAY_SUFFIX = "[]";

    @Override
    public IOpenClass parse(String data, String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass parse(String data, String format, IBindingContext cxt) {
        if (data == null) {
            return null;
        }

        String typeName;
        if (data.endsWith(ARRAY_SUFFIX)) {
            typeName = data.substring(0, data.length() - 2);
        } else {
            typeName = data;
        }

        IOpenClass openClass = cxt.findType(typeName);

        if (openClass == null) {
            throw new IllegalArgumentException(MessageUtils.getTypeNotFoundMessage(data));
        }

        if (data.endsWith(ARRAY_SUFFIX)) {
            openClass = openClass.getAggregateInfo().getIndexedAggregateType(openClass);
        }
        return openClass;
    }
}
