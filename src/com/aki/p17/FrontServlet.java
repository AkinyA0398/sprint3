package com.aki.p17;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    private Map<String, String[]> urlMappings; // URL -> [className, methodName]

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        urlMappings = new HashMap<>();
        scanControllers();
    }

    private void scanControllers() {
        try {
            String controllerPackage = getServletConfig().getInitParameter("Controllers");
            if (controllerPackage == null) return;

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources(controllerPackage.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File dir = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
                    if (dir.exists() && dir.isDirectory()) {
                        scanDirectory(dir, controllerPackage);
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
    }

    private void scanDirectory(File dir, String packageName) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
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

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Check for /list endpoint
        if (path.equals("/list")) {
            listUrls(req, res);
            return;
        }

        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            // Vérifie si un contrôleur annoté peut traiter la requête
            if (!handleAnnotatedControllers(req, res, path)) {
                customServe(req, res);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        service(req, res);
    }

    private boolean handleAnnotatedControllers(HttpServletRequest req, HttpServletResponse res, String path) {
        try {
            String controllerPackage = getServletConfig().getInitParameter("Controllers");
            if (controllerPackage == null || urlMappings == null) return false;

            // Normalisation pour ignorer le '/' final
            String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

            if (urlMappings.containsKey(normalizedPath)) {
                String[] details = urlMappings.get(normalizedPath);
                String className = details[0];
                String methodName = details[1];

                Class<?> clazz = Class.forName(controllerPackage + "." + className);
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Method method = clazz.getMethod(methodName);

                PrintWriter out = res.getWriter();
                Object result = method.invoke(controllerInstance);
                res.setContentType("text/html;charset=UTF-8");
                out.println(result);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false; // Aucun contrôleur ne correspond
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = """
                <html>
                    <head><title>Resource Not Found</title></head>
                    <body>
                        <h1>Unknown resource</h1>
                        <p>The requested URL was not found: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(uri);

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void listUrls(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            res.setContentType("text/html;charset=UTF-8");
            out.println("<html><head><title>URL Mappings</title></head><body>");
            out.println("<h1>All Mapped URLs</h1>");
            out.println("<table border='1'><tr><th>URL</th><th>Supported</th><th>Class</th><th>Method</th></tr>");

            for (Map.Entry<String, String[]> entry : urlMappings.entrySet()) {
                String url = entry.getKey();
                String[] details = entry.getValue();
                String className = details[0];
                String methodName = details[1];
                out.println("<tr><td>" + url + "</td><td>Yes</td><td>" + className + "</td><td>" + methodName + "</td></tr>");
            }

            out.println("</table></body></html>");
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
