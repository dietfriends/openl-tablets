package org.openl.types.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;

public class JavaOpenClassTest {

    @Test
    public void testGetComponentType() {
        assertEquals(JavaOpenClass.getOpenClass(String.class),
                JavaOpenClass.getOpenClass(String[].class).getComponentClass());

        assertEquals(JavaOpenClass.getOpenClass(String[].class),
                JavaOpenClass.getOpenClass(String[][].class).getComponentClass());

        assertNull(JavaOpenClass.getOpenClass(int.class).getComponentClass());

        assertEquals(JavaOpenClass.OBJECT, JavaOpenClass.getOpenClass(List.class).getComponentClass());
    }

    @Test
    public void testIsSimple() {
        IOpenClass clazz = JavaOpenClass.getOpenClass(String.class);
        assertTrue(clazz.isSimple());
    }

    @Test
    public void testResetClassLoader() {
        IOpenClass doubleValue = JavaOpenClass.getOpenClass(Double.class);
        IOpenClass myType = JavaOpenClass.getOpenClass(MyType.class);

        assertSame(doubleValue, JavaOpenClass.getOpenClass(Double.class));
        assertSame(myType, JavaOpenClass.getOpenClass(MyType.class));

        JavaOpenClassCache.getInstance().resetClassloader(Double.class.getClassLoader());
        JavaOpenClassCache.getInstance().resetClassloader(MyType.class.getClassLoader());

        assertSame(doubleValue, JavaOpenClass.getOpenClass(Double.class));
        assertNotSame(myType, JavaOpenClass.getOpenClass(MyType.class));
    }

    @Test
    public void testGetMethod() {
        IOpenClass myType = JavaOpenClass.getOpenClass(MyType.class);
        assertNotNull(myType);
        IOpenMethod myTypeMethod1 = myType.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.DOUBLE});
        IOpenMethod myTypeMethod2 = myType.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class)});
        IOpenMethod myTypeMethod3 = myType.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE});

        assertNotNull(myTypeMethod1);
        assertNotNull(myTypeMethod2);
        assertNotNull(myTypeMethod3);

        assertNotEquals(myTypeMethod1, myTypeMethod2);
        assertNotEquals(myTypeMethod2, myTypeMethod3);
        assertNotEquals(myTypeMethod1, myTypeMethod3);

        assertSame(myTypeMethod1,
                myType.getMethod("method1", new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.DOUBLE}));
        assertSame(myTypeMethod2,
                myType.getMethod("method1",
                        new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class)}));
        assertSame(myTypeMethod3, myType.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE}));

        IOpenClass extended = JavaOpenClass.getOpenClass(Extended.class);
        assertNotNull(extended);
        IOpenMethod extended1 = extended.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.DOUBLE});
        IOpenMethod extended2 = extended.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class)});
        IOpenMethod extended3 = extended.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE});

        assertNotNull(extended1);
        assertNotNull(extended2);
        assertNotNull(extended3);

        assertNotEquals(extended1, extended2);
        assertNotEquals(extended2, extended3);
        assertNotEquals(extended1, extended3);

        assertSame(extended1,
                extended.getMethod("method1", new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.DOUBLE}));
        assertSame(extended2,
                extended.getMethod("method1",
                        new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class), JavaOpenClass.getOpenClass(Double.class)}));
        assertSame(extended3, extended.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE}));

        assertNotEquals(myTypeMethod1, extended1);
        assertSame(myTypeMethod2, extended2);
        assertSame(myTypeMethod3, extended3);

        assertNull(myType.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.DOUBLE}));
        assertNull(myType.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.getOpenClass(Double.class)}));
        assertNull(myType.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Double.class)}));
        assertNull(myType.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE, JavaOpenClass.DOUBLE}));
        assertNull(myType.getMethod("method1", new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class)}));
        assertNull(myType.getMethod("method", new IOpenClass[]{JavaOpenClass.BYTE}));
        assertNull(myType.getMethod("method11", new IOpenClass[]{JavaOpenClass.BYTE}));

        assertNotNull(extended.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.DOUBLE}));
        assertNull(extended.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.getOpenClass(Double.class)}));
        assertNull(extended.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Double.class)}));
        assertNull(extended.getMethod("method1", new IOpenClass[]{JavaOpenClass.BYTE, JavaOpenClass.DOUBLE}));
        assertNull(extended.getMethod("method1", new IOpenClass[]{JavaOpenClass.getOpenClass(Byte.class)}));
        assertNull(extended.getMethod("method", new IOpenClass[]{JavaOpenClass.BYTE}));
        assertNull(extended.getMethod("method11", new IOpenClass[]{JavaOpenClass.BYTE}));
    }

    @Test
    public void interfaceOpenClassMethodsTest() {
        IOpenClass openClass = JavaOpenClass.getOpenClass(MyInterface.class);
        IOpenMethod method1 = openClass.getMethod("method1",
                new IOpenClass[]{JavaOpenClass.INT, JavaOpenClass.DOUBLE});
        assertNotNull(method1);
        for (Method method : Object.class.getMethods()) {
            IOpenClass[] params = new IOpenClass[method.getParameterTypes().length];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                params[i] = JavaOpenClass.getOpenClass(method.getParameterTypes()[i]);
            }
            IOpenMethod m = openClass.getMethod(method.getName(), params);
            assertNotNull(m);
        }
    }

    @Test
    public void superClassBeanFieldsTest() {
        // when
        JavaOpenClass beanAOpenClass = JavaOpenClass.getOpenClass(BeanX.class);
        IOpenField openField = beanAOpenClass.getField("B");
        assertNotNull(openField);
        // then
        Collection<IOpenField> fields = beanAOpenClass.getFields();
        assertNotNull(beanAOpenClass.getFields());
        assertEquals(6, fields.size());
        assertNotNull(beanAOpenClass.getField("B"));
        assertNotNull(beanAOpenClass.getField("Ba"));
        assertNotNull(beanAOpenClass.getField("cc"));
        assertNotNull(beanAOpenClass.getField("BB"));
        assertNotNull(beanAOpenClass.getField("gg"));
        for (IOpenField it : fields) {
            if ("class".equals(it.getName())) {
                continue;
            }
            assertTrue(it.isWritable());
            assertTrue(it.isReadable());
        }
    }

    @Test
    public void testIsAssignableFromNullOpenClass() {
        JavaOpenClass beanAOpenClass = JavaOpenClass.getOpenClass(BeanX.class);
        assertFalse(beanAOpenClass.isAssignableFrom(NullOpenClass.the));
    }

    @Test
    public void interfaceBeanFieldsTest() {
        JavaOpenClass beanAOpenClass = JavaOpenClass.getOpenClass(BeanXInterface.class);
        IOpenField openField = beanAOpenClass.getField("ba");
        assertNotNull(openField);
        Collection<IOpenField> fields = beanAOpenClass.getFields();
        assertNotNull(fields);
        assertEquals(5, fields.size());
        assertNotNull(beanAOpenClass.getField("b"));
        assertNotNull(beanAOpenClass.getField("ba"));
        assertNotNull(beanAOpenClass.getField("BB"));
        assertNotNull(beanAOpenClass.getField("x"));
    }

    public static class MyType {
        public void method1(int i, double j) {
        }

        public void method1(Byte i, Double j) {
        }

        public void method1(byte i) {
        }
    }

    public static class Extended extends MyType {
        @Override
        public void method1(int i, double j) {
        }

        public void method1(Integer i, double j) {
        }
    }

    public interface MyInterface {
        void method1(int i, double j);
    }

    public interface IBeanA {
        int getGg();
    }

    public static abstract class BeanA implements IBeanA {
        private int B;
        private int Ba;
        private int BB;
        private int cc;
        private int gg;

        public BeanA() {
        }

        public int getB() {
            return this.B;
        }

        public void setB(int b) {
            this.B = b;
        }

        public int getBa() {
            return this.Ba;
        }

        public void setBa(int ba) {
            this.Ba = ba;
        }

        public int getBB() {
            return this.BB;
        }

        public void setBB(int BB) {
            this.BB = BB;
        }

        public int getCc() {
            return cc;
        }

        public void setCc(int cc) {
            this.cc = cc;
        }

        public void setGg(int gg) {
            this.gg = gg;
        }
    }

    public static class BeanX extends BeanA implements BeanCInterface {
        public BeanX() {
        }

        @Override
        public int getGg() {
            return 0;
        }
    }

    public interface BeanCInterface {
        int getCc();
    }

    public interface BeanAInterface {
        int getB();

        void setB(int b);

        int getBa();

        void setBa(int ba);

        int getBB();

        void setBB(int BB);
    }

    public interface BeanXInterface extends BeanAInterface {
        int getX();

        void setX(int x);
    }
}
