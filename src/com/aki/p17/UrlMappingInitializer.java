package com.aki.p17;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import annotation.AnnotationController;
import annotation.GetMethode;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class UrlMappingInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String controllerPackage = context.getInitParameter("Controllers");
        if (controllerPackage == null) return;

        Map<String, String[]> urlMappings = scanControllers(controllerPackage);
        context.setAttribute("urlMappings", urlMappings);
        System.out.println("URL Mappings scanned and loaded:");
        for (Map.Entry<String, String[]> entry : urlMappings.entrySet()) {
            System.out.println("URL: " + entry.getKey() + " -> Class: " + entry.getValue()[0] + ", Method: " + entry.getValue()[1]);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }

    private Map<String, String[]> scanControllers(String controllerPackage) {
        Map<String, String[]> urlMappings = new HashMap<>();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources(controllerPackage.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File dir = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
                    if (dir.exists() && dir.isDirectory()) {
                        scanDirectory(dir, controllerPackage, urlMappings);
                    }
                } else if (resource.getProtocol().equals("jar")) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.getName().startsWith(controllerPackage.replace('.', '/')) && entry.getName().endsWith(".class")) {
                                String className = entry.getName().replace('/', '.').replace(".class", "");
                                try {
                                    Class<?> clazz = Class.forName(className);
                                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                                        String prefix = "/" + clazz.getAnnotation(AnnotationController.class).value();
                                        for (Method method : clazz.getDeclaredMethods()) {
                                            if (method.isAnnotationPresent(GetMethode.class)) {
                                                String methodPath = method.getAnnotation(GetMethode.class).value();
                                                String fullPath = prefix + "/" + methodPath;
                                                urlMappings.put(fullPath, new String[]{clazz.getSimpleName(), method.getName()});
                                            }
                                        }
                                    }
                                } catch (ClassNotFoundException e) {
                                    // Ignore classes that can't be loaded
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return urlMappings;
    }

    private void scanDirectory(File dir, String packageName, Map<String, String[]> urlMappings) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), urlMappings);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        String prefix = "/" + clazz.getAnnotation(AnnotationController.class).value();
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GetMethode.class)) {
                                String methodPath = method.getAnnotation(GetMethode.class).value();
                                String fullPath = prefix + "/" + methodPath;
                                urlMappings.put(fullPath, new String[]{clazz.getSimpleName(), method.getName()});
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Ignore classes that can't be loaded
                }
            }
        }
    }
}
