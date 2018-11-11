package com.github.miltonwright.fly;

import java.net.URL;
import java.net.URLConnection;

import org.eclipse.jetty.server.Server;

import junit.framework.TestCase;

public class AppTest extends TestCase {

    public void testApp() throws Exception {
        Server server = App.createServer(0);
        URLConnection connection = new URL(server.getURI() + "flyavganger").openConnection();
        connection.connect();
        assertEquals("text/plain", connection.getHeaderField("Content-Type"));
        server.stop();
    }
}
