package com.github.miltonwright.fly;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.eclipse.jetty.server.Server;

import com.github.miltonwright.fly.App.Flight;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheLoader;
import com.google.common.io.CharStreams;

import junit.framework.TestCase;

public class AppTest extends TestCase {

    private static final CacheLoader<String, List<Flight>> FLIGHT_LOADER = new CacheLoader<String, List<Flight>>() {
        @Override
        public List<Flight> load(String arg) throws Exception {
            return App.unXml(AppTest.class.getResource("testdata.xml").openStream());
        }
    };

    public void testApp() throws Exception {
        Server server = App.createServer(0, Clock.fixed(Instant.parse("2018-11-09T21:30:00Z"), ZoneId.of("Europe/Oslo")), FLIGHT_LOADER);
        URLConnection connection = new URL(server.getURI() + "flyavganger").openConnection();
        connection.connect();
        assertEquals("text/plain", connection.getHeaderField("Content-Type"));
        String response = CharStreams.toString(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8));
        assertTrue(response, response.contains("AY2681\tAY\tD\t2018-11-08T21:25:00Z\tHOV\tA26\tDeparted at 22:29:00"));
        server.stop();
    }
}
