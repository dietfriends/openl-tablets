package org.openl.types.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ADynamicTest {

    @Test
    public void testIsAssignableFromNullOpenClass() {
        DummyDynamicClass d = new DummyDynamicClass("test");
        assertFalse(d.isAssignableFrom(NullOpenClass.the));
    }
}

class DummyDynamicClass extends ADynamicClass {

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return null;
    }

    public DummyDynamicClass(String name) {
        super(name, DynamicObject.class);
    }
}