package com.github.miltonwright.fly;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.eclipse.jetty.server.Server;

import com.github.miltonwright.fly.App.Flight;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

public class AppTest extends TestCase {

    private static final CacheLoader<String, List<Flight>> FLIGHT_LOADER = new CacheLoader<String, List<Flight>>() {
        @Override
        public List<Flight> load(String arg) throws Exception {
            return ImmutableList.of();
        }
    };

    public void testApp() throws Exception {
        Server server = App.createServer(0, FLIGHT_LOADER);
        URLConnection connection = new URL(server.getURI() + "flyavganger").openConnection();
        connection.connect();
        assertEquals("text/plain", connection.getHeaderField("Content-Type"));
        server.stop();
    }
}
