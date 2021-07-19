package com.sun.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * author sungw
 *
 * @description
 * @date 2021/5/26
 */
public class AAServlet implements Servlet{
    @Override
    public void init() {
        System.out.println("aaServlet...init");
    }
    @Override
    public void Service(InputStream is, OutputStream ops) throws IOException {
        System.out.println("aaServlet...service");
        ops.write("I am from AAServlet".getBytes());
        ops.flush();
    }
    @Override
    public void destory() {}
}
