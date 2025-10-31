package com.aki.controllers;

import annotation.AnnotationController;
import annotation.GetMethode;

@AnnotationController("test")
public class TestController {

    @GetMethode("hello")
    public String hello() {
        return "<h1>Hello from TestController!</h1>";
    }

    @GetMethode("list")
    public String list() {
        return "<h1>List from TestController!</h1>";
    }
}
