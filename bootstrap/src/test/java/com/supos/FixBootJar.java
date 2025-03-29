package com.supos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

// fix jar build by spring-boot-maven-plugin, add directory JarEntry
public class FixBootJar {
    public static void main(String[] args) throws IOException {
        try (JarFile jarFile = new JarFile("adp-lite-1.jar")) {

            LinkedHashSet<String> names = new LinkedHashSet<>();
            HashSet<String> dirs = new HashSet<>();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                names.add(name);
                if (name.endsWith("/")) {
                    dirs.add(name);
                }
            }
            File targetFile = new File(jarFile.getName() + ".lite.jar");
            if (targetFile.exists()) {
                targetFile.delete();
            }
            try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(targetFile))) {
                for (String name : names) {
                    ZipEntry jarEntry = jarFile.getEntry(name);
                    if (name.endsWith(".class")) {
                        String classFile = name;
                        int lastX = classFile.lastIndexOf('/');
                        if (lastX > 0) {
                            String classFileDir = classFile.substring(0, lastX);
                            int sp = classFileDir.indexOf('/');
                            while (sp > 0) {
                                String pDir = classFileDir.substring(0, sp + 1);
                                if (dirs.add(pDir)) {
                                    System.out.println("addDir: " + pDir);
                                    jarOutputStream.putNextEntry(new JarEntry(pDir));
                                    jarOutputStream.flush();
                                }
                                sp = classFileDir.indexOf('/', sp + 1);
                            }
                        }
                    }
                    jarOutputStream.putNextEntry(jarEntry);
                    BootJarToLibJar.copy(jarFile.getInputStream(jarEntry), jarOutputStream);
                }
            }
            // test
            try (JarFile jarFileTar = new JarFile(targetFile)) {
                Enumeration<JarEntry> itr = jarFileTar.entries();
                while (itr.hasMoreElements()) {
                    JarEntry jarEntry = itr.nextElement();
                    String name = jarEntry.getName();
                    System.out.println("entry: " + name);
                }
            }
        }
    }
}
