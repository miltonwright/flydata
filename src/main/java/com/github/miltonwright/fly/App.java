package com.github.miltonwright.fly;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

public class App {

    private static final CacheLoader<String, List<Flight>> FLIGHT_LOADER = new CacheLoader<String, List<Flight>>() {
        @Override
        public List<Flight> load(String arg) throws Exception {
            return ImmutableList.of();
        }
    };

    private static class Handler extends AbstractHandler {
        private final LoadingCache<String, List<Flight>> flightCache;

        private Handler(CacheLoader<String, List<Flight>> cacheLoader) {
            flightCache =  CacheBuilder.newBuilder().refreshAfterWrite(Duration.ofMinutes(3)).build(cacheLoader);
        }

        @Override
        public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {
            if (!"/flyavganger".equals(path) || !"GET".equals(httpRequest.getMethod())) {
                return;
            }

            httpResponse.setContentType("text/plain");
            try (PrintWriter out = new PrintWriter(httpResponse.getOutputStream())) {
                for (Flight f : flightCache.getUnchecked("")) {
                    out.println(f.flightId);
                }
            } finally {
                request.setHandled(true);
            }
        }
    }

    public static class Flight {
        String flightId;
    }

    public static Server createServer(int port, CacheLoader<String, List<Flight>> cacheLoader) throws Exception {
        Server server = new Server(port);
        server.setHandler(new Handler(cacheLoader));
        server.start();
        return server;
    }

    public static void main(String[] args) throws Exception {
        Server server = createServer(Integer.getInteger("com.github.miltonwright.fly.port", 8080), FLIGHT_LOADER);
        server.join();
    }
}