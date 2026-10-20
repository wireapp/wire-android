package com.wire.metro.measure;

import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;

/** Selected constructor ENTRY facts plus bounded per-window identities at normal RETURN. */
public final class ConstructorAgent {
    public static void premain(String selection, Instrumentation instrumentation) throws Exception {
        if (Probe.class.getClassLoader() != null) throw new IllegalStateException("Probe must be bootstrap-loaded with -Xbootclasspath/a:probe.jar");
        Set<String> selected = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of(selection))) {
            String s = line.strip(); if (!s.isEmpty() && !s.startsWith("#")) selected.add(s.replace('.', '/'));
        }
        for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
            if (selected.contains(loaded.getName().replace('.', '/'))) throw new IllegalStateException("selected class loaded before agent: " + loaded.getName());
        }
        Probe.installed();
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                    ProtectionDomain domain, byte[] input) {
                if (!selected.contains(name)) return null;
                if (redefining != null) {
                    Probe.error(name + ": selected class redefined; constructor coverage no longer certified");
                    return null;
                }
                try {
                    ClassReader reader = new ClassReader(input);
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String method, String desc, String sig, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, method, desc, sig, exceptions);
                            if (!method.equals("<init>")) return mv;
                            int id = Probe.register(name.replace('/', '.') + "#<init>" + desc,
                                    loader == null ? "bootstrap" : loader.getClass().getName());
                            return new MethodVisitor(Opcodes.ASM9, mv) {
                                @Override public void visitInsn(int opcode) {
                                    if (opcode == Opcodes.RETURN) {
                                        mv.visitVarInsn(Opcodes.ALOAD, 0); // Initialized at a verifier-valid constructor RETURN.
                                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wire/metro/measure/Probe", "returned", "(Ljava/lang/Object;)V", false);
                                    }
                                    super.visitInsn(opcode);
                                }
                                @Override public void visitCode() {
                                    super.visitCode();
                                    // No reference to uninitialized this; runs BEFORE super()/this().
                                    mv.visitLdcInsn(id);
                                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/wire/metro/measure/Probe", "hit", "(I)V", false);
                                }
                            };
                        }
                    }, 0);
                    return writer.toByteArray();
                } catch (Throwable failure) {
                    Probe.error(name + ": " + failure);
                    return null; // finish() makes the measurement fail; never silently report partial counts.
                }
            }
        });
    }
}
