package org.openl.itest.db;

import org.openl.rules.ruleservice.storelogdata.Converter;

public class NoConvertorString implements Converter<String, String> {
    @Override
    public String apply(String value) {
        return value;
    }
}
