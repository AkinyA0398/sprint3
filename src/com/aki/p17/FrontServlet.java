package com.aki.p17;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;

import annotation.AnnotationController;
import annotation.GetMethode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
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

    private boolean handleAnnotatedControllers(HttpServletRequest req, HttpServletResponse res, String path) {
        try {
            String controllerPackage = getServletConfig().getInitParameter("Controllers");
            if (controllerPackage == null) return false;

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL packageUrl = loader.getResource(controllerPackage.replace('.', '/'));
            if (packageUrl == null) return false;

            File dir = new File(URLDecoder.decode(packageUrl.getFile(), "UTF-8"));
            if (!dir.exists() || !dir.isDirectory()) return false;

            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(controllerPackage + "." + className);

                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                        String prefix = "/" + clazz.getAnnotation(AnnotationController.class).value();

                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GetMethode.class)) {
                                String methodPath = method.getAnnotation(GetMethode.class).value();
                                String fullPath = prefix + methodPath;

                                // Normalisation pour ignorer le '/' final
                                String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                                String normalizedFullPath = fullPath.endsWith("/") ? fullPath.substring(0, fullPath.length() - 1) : fullPath;

                                if (normalizedPath.equals(normalizedFullPath)) {
                                    PrintWriter out = res.getWriter();
                                    Object result = method.invoke(controllerInstance);
                                    res.setContentType("text/html;charset=UTF-8");
                                    out.println(result);
                                    return true;
                                }
                            }
                        }
                    }
                }
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

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
