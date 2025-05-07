package org.openl.gen.writers;

import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.openl.gen.FieldDescription;

/**
 * Generates a equals(Object) method. This method uses only JRE classes for deep comparing.
 *
 * @author Yury Molchan
 */
public class EqualsWriter extends DefaultBeanByteCodeWriter {

    /**
     * @param beanNameWithPackage name of the class being generated with package, symbol '/' is used as separator<br>
     *                            (e.g. <code>my/test/TestClass</code>)
     * @param allFields           collection of fields for current class and parent`s ones.
     */
    public EqualsWriter(String beanNameWithPackage, Map<String, FieldDescription> allFields) {
        super(beanNameWithPackage, null, allFields);
    }

    @Override
    public void write(ClassWriter classWriter) {
        MethodVisitor mv;
        mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);

        trueIfTheSame(mv);
        falseIfNull(mv);
        falseIfDifferentClassNames(mv);
        doCast(mv); // CastType other = (CastType) arg0;

        Label retFalse = new Label();
        // comparing by fields
        for (Map.Entry<String, FieldDescription> field : getBeanFields().entrySet()) {
            String fieldName = field.getKey();
            FieldDescription fd = field.getValue();
            String typeDescriptor = fd.getTypeDescriptor();
            String typeName = fd.getTypeName();

            mv.visitVarInsn(Opcodes.ALOAD, 0); // this.fieldName
            mv.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, typeDescriptor);

            mv.visitVarInsn(Opcodes.ALOAD, 2); // other.fieldName
            mv.visitFieldInsn(Opcodes.GETFIELD, getBeanNameWithPackage(), fieldName, typeDescriptor);

            compareNE(mv, typeName, retFalse);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }

        mv.visitInsn(Opcodes.ICONST_1);// true
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(retFalse);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitMaxs(0, 0);
    }

    private void compareNE(MethodVisitor mv, String type, Label gotoIfNotEqual) {
        if ("double".equals(type)) {
            invoke(mv, "java/lang/Double", "compare", "(DD)I");
            mv.visitJumpInsn(Opcodes.IFNE, gotoIfNotEqual);
        } else if ("float".equals(type)) {
            invoke(mv, "java/lang/Float", "compare", "(FF)I");
            mv.visitJumpInsn(Opcodes.IFNE, gotoIfNotEqual);
        } else if ("long".equals(type)) {
            mv.visitInsn(Opcodes.LCMP);
            mv.visitJumpInsn(Opcodes.IFNE, gotoIfNotEqual);
        } else if ("int".equals(type) || "short".equals(type) || "byte".equals(type) || "char".equals(type)) {
            // No conversions
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, gotoIfNotEqual);
        } else if ("boolean".equals(type)) {
            // No conversions
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, gotoIfNotEqual);
        } else if (type.charAt(0) == '[' && type.length() == 2) { // Array of primitives
            invoke(mv, "java/util/Arrays", "equals", "(" + type + type + ")Z");
            mv.visitJumpInsn(Opcodes.IFEQ, gotoIfNotEqual);
        } else if (type.startsWith("[L")) { // Array of objects
            invoke(mv, "java/util/Arrays", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z");
            mv.visitJumpInsn(Opcodes.IFEQ, gotoIfNotEqual);
        } else if (type.startsWith("[[")) { // Multi array
            invoke(mv, "java/util/Arrays", "deepEquals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z");
            mv.visitJumpInsn(Opcodes.IFEQ, gotoIfNotEqual);
        } else {
            invoke(mv, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
            mv.visitJumpInsn(Opcodes.IFEQ, gotoIfNotEqual);

        }
    }

    private void doCast(MethodVisitor mv) {
        // cast
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, getBeanNameWithPackage());
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{getBeanNameWithPackage()}, 0, null);
    }

    private void falseIfDifferentClassNames(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, endif); // this.class != other.class
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    private void falseIfNull(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNONNULL, endif); // other == null
        mv.visitInsn(Opcodes.ICONST_0);// false
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    private void trueIfTheSame(MethodVisitor mv) {
        Label endif = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IF_ACMPNE, endif); // this == other
        mv.visitInsn(Opcodes.ICONST_1);// true
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(endif);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
    }

    private static void invoke(MethodVisitor mv, String clazz, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, clazz, methodName, descriptor, false);
    }
}
