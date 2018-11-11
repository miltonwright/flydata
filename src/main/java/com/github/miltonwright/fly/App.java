package com.github.miltonwright.fly;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

public class App {

    private static final ZoneId TIME_ZONE_NORWAY = ZoneId.of("Europe/Oslo");

    private static final CacheLoader<String, List<Flight>> FLIGHT_LOADER = new CacheLoader<String, List<Flight>>() {
        @Override
        public List<Flight> load(String arg) throws Exception {
            /**
             * See documentation on
             * https://avinor.no/konsern/tjenester/flydata/flydata-i-xml-format
             *
             * No matter how I adjust TimeFrom or TimeTo, I have not been able to get
             * any data that is older than a couple of days from this service.
             */
            return unXml(new URL("https://flydata.avinor.no/XmlFeed.asp?TimeFrom=48&TimeTo=7&airport=" + System.getProperty("com.github.miltonwright.fly.aiport", "OSL") + "&direction=D").openStream());
        }
    };

    private static class Handler extends AbstractHandler {
        private final LoadingCache<String, List<Flight>> flightCache;
        private final Clock clock;

        private Handler(Clock clock, CacheLoader<String, List<Flight>> cacheLoader) {
            flightCache =  CacheBuilder.newBuilder().refreshAfterWrite(Duration.ofMinutes(3)).build(cacheLoader);
            this.clock = clock;
        }

        @Override
        public void handle(String path, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {
            if (!"/flyavganger".equals(path) || !"GET".equals(httpRequest.getMethod())) {
                return;
            }

            httpResponse.setContentType("text/plain");
            try (PrintWriter out = new PrintWriter(httpResponse.getOutputStream())) {
                ZonedDateTime midnight = LocalDate.now(clock).atStartOfDay(TIME_ZONE_NORWAY).minusDays(Long.getLong("com.github.miltonwright.fly.daysAgo", 1) - 1);
                for (Flight f : flightCache.getUnchecked("")) {
                    if (!f.scheduleTime.isBefore(midnight.minusDays(1).toInstant()) && f.scheduleTime.isBefore(midnight.toInstant())) {
                        out.println(Joiner.on("\t").join(ImmutableList.of(f.flightId, f.airline, f.domInt, f.scheduleTime, f.airport, MoreObjects.firstNonNull(f.gate, ""), f.getStatusString())));
                    }
                }
            } finally {
                request.setHandled(true);
            }
        }
    }

    public static class Flight {
        final String flightId;
        final String airline;
        final String domInt;
        final Instant scheduleTime;
        final String airport;
        final String gate;
        final String statusCode;
        final Instant statusTime;

        Flight(Map<String, String> params) {
            flightId = params.get("flight_id");
            airline = params.get("airline");
            domInt = params.get("dom_int");
            scheduleTime = Strings.isNullOrEmpty(params.get("schedule_time")) ? null : Instant.parse(params.get("schedule_time"));
            airport = params.get("airport");
            gate = params.get("gate");
            statusCode = params.get("status->code");
            statusTime = Strings.isNullOrEmpty(params.get("status->time")) ? null : Instant.parse(params.get("status->time"));
        }

        String getStatusString() {
            if (statusCode == null) {
                return "";
            }

            // Instead, use https://flydata.avinor.no/flightStatuses.asp?code=XX
            // Also, convert to human friendly time format if this should be read by humans
            switch(statusCode) {
                case "A": return "Arrived at " + statusTime.atZone(TIME_ZONE_NORWAY).format(DateTimeFormatter.ISO_LOCAL_TIME);
                case "C": return "Cancelled";
                case "D": return "Departed at " + statusTime.atZone(TIME_ZONE_NORWAY).format(DateTimeFormatter.ISO_LOCAL_TIME);
                case "E": return "New time";
                case "N": return "New info";
                default: return "";
            }
        }
    }

    /**
     * Quick and dirty way to convert InputStream with xml from
     * https://flydata.avinor.no/XmlFeed.asp to list of flights with attributes.
     */
    public static List<Flight> unXml(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        List<Map<String, String>> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(in);
        NodeList flights = document.getElementsByTagName("flight");
        for (int i = 0; i < flights.getLength(); i++) {
            NodeList list = flights.item(i).getChildNodes();
            Map<String, String> values = new HashMap<>();
            for (int j = 0; j < list.getLength(); j++) {
                Node node = list.item(j);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element elem = (Element) node;
                if ("status".equals(elem.getTagName())) {
                    values.put("status->code", elem.getAttribute("code"));
                    values.put("status->time", elem.getAttribute("time"));
                } else {
                    values.put(elem.getTagName(), elem.getTextContent());
                }
            }
            result.add(values);
        }

        return result.stream().map(s -> new Flight(s)).collect(Collectors.toList());
    }

    public static Server createServer(int port, Clock clock, CacheLoader<String, List<Flight>> cacheLoader) throws Exception {
        Server server = new Server(port);
        server.setHandler(new Handler(clock, cacheLoader));
        server.start();
        return server;
    }

    public static void main(String[] args) throws Exception {
        Server server = createServer(Integer.getInteger("com.github.miltonwright.fly.port", 8080), Clock.systemUTC(), FLIGHT_LOADER);
        server.join();
    }
}