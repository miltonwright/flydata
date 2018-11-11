package com.github.miltonwright.fly;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class App {
    private static class Handler extends AbstractHandler {
        @Override
        public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {
            if (!"/flyavganger".equals(path) || !"GET".equals(httpRequest.getMethod())) {
                return;
            }

            httpResponse.setContentType("text/plain");
            request.setHandled(true);
        }
    }

    public static Server createServer(int port) throws Exception {
        Server server = new Server(port);
        server.setHandler(new Handler());
        server.start();
        return server;
    }

    public static void main(String[] args) throws Exception {
        Server server = createServer(Integer.getInteger("com.github.miltonwright.fly.port", 8080));
        server.join();
    }
}