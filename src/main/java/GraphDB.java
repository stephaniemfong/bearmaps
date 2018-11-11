import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    Graph graph;
    Trie trie;

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        this.graph = new Graph();
        this.trie = new Trie();
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Long> toRemove = new ArrayList<>();
        for (long l : this.vertices()) {
            if (!this.adjacent(l).iterator().hasNext()) {
                toRemove.add(l);
            }
        }
        removeIDs(toRemove);
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        return this.graph.nodes.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        Node temp = this.graph.nodes.get(v);
        List<Long> adj = new ArrayList<>();
        for (Node n : temp.neighbors) {
            adj.add(n.id);
        }
        return adj;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double dist = Double.MAX_VALUE;
        long id = -1;
        for (long l : vertices()) {
            Node temp = this.graph.nodes.get(l);
            double newDist = distance(lon, lat, temp.lon, temp.lat);
            if (newDist < dist) {
                dist = newDist;
                id = l;
            }
        }
        return id;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return this.graph.nodes.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return this.graph.nodes.get(v).lat;
    }

    void addNode(Node n) {
        this.graph.nodes.put(n.id, n);
    }

    void removeNode(Node remove) {
        removeEdge(remove);
        for (Node n : remove.neighbors) {
            n.neighbors.remove(remove);
        }
        this.graph.nodes.remove(remove.id);
    }

    void removeIDs(List<Long> ids) {
        for (Long l : ids) {
            removeNode(this.graph.nodes.get(l));
        }
    }

    void addEdge(Edge e) {
        Node a = e.start;
        Node b = e.end;

        if (!this.graph.edges.containsKey(a.id)) {
            List<Edge> temp = new ArrayList<>();
            temp.add(e);
            this.graph.edges.put(a.id, temp);
        } else {
            this.graph.edges.get(a.id).add(e);
        }

        if (!this.graph.edges.containsKey(b.id)) {
            List<Edge> temp = new ArrayList<>();
            temp.add(e);
            this.graph.edges.put(b.id, temp);
        } else {
            this.graph.edges.get(b.id).add(e);
        }
    }

    void removeEdge(Node n) {
        if (this.graph.edges.containsKey(n.id)) {
            List<Edge> toBeRemoved = this.graph.edges.get(n.id);
            Map<Node, Edge> otherNodes = new HashMap<>();

            for (Edge e : toBeRemoved) {
                if (e.start.equals(n)) {
                    otherNodes.put(e.end, e);
                } else {
                    otherNodes.put(e.start, e);
                }
            }
            for (Node node : otherNodes.keySet()) {
                this.graph.edges.get(n.id).remove(otherNodes.get(node));
            }
            this.graph.edges.remove(n.id);
        }
    }

    Edge findEdge(Node start, Node end) {
        for (Edge e : graph.edges.get(start.id)) {
            if (e.start.equals(end) || e.end.equals(end)) {
                return e;
            }
        }
        return null;
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return trie.get(prefix);
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" : Number, The latitude of the node. <br>
     * "lon" : Number, The longitude of the node. <br>
     * "name" : String, The actual name of the node. <br>
     * "id" : Number, The id of the node. <br>
     */
    public List<Map<String, Object>> getLocations(String locationName) {
//        List<Map<String, Object>> locs = new LinkedList<>();
//        for (Long l : this.graph.locations.get(cleanString(locationName))) {
//            Map<String, Object> info = new HashMap<>();
////            System.out.println(l);
////            System.out.println(this.graph.nodes.containsKey(l));
//            info.put("lat", this.graph.nodes.get(l).lat);
//            info.put("lon", this.graph.nodes.get(l).lon);
//            info.put("name", this.graph.nodes.get(l).name);
//            info.put("id", this.graph.nodes.get(l).id);
//            locs.add(info);
//        }
//        return locs;
        return this.graph.locations.get(cleanString(locationName));
    }

    static class Graph {
        Map<Long, Node> nodes;
        Map<Long, List<Edge>> edges;
        Map<String, List<Map<String, Object>>> locations;

        Graph() {
            nodes = new LinkedHashMap<>();
            edges = new LinkedHashMap<>();
            locations = new HashMap<>();
        }
    }

    static class Edge {
        Node start;
        Node end;
        double weight;

        Edge(Node s, Node e, double dist) {
            this.start = s;
            this.end = e;
            this.weight = dist;
        }

        @Override
        public String toString() {
            return String.valueOf(start.id + ", " + end.id);
        }
    }

    static class Node {
        long id;
        double lon;
        double lat;
//        Map<String, String> extraInfo;
        String name;
        List<Node> neighbors;
        long wayID;
        String wayName;

        Node() { }

        Node(String id, String lon, String lat) {
            this.id = Long.parseLong(id);
            this.lon = Double.parseDouble(lon);
            this.lat = Double.parseDouble(lat);
//            this.extraInfo = new HashMap<>();
            this.neighbors = new LinkedList<>();
            this.wayName = "";
        }

        void connect(Node other) {
            this.neighbors.add(other);
        }

        @Override
        public boolean equals(Object o) {
            Node other = (Node) o;
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, lon, lat);
        }

        @Override
        public String toString() {
            return String.valueOf(this.id);
        }
    }

    static class Trie {
        static class TrieNode {
            HashMap<Character, TrieNode> children = new HashMap<>();
            boolean exists;
            ArrayList<String> words = new ArrayList<>();

            TrieNode() { }
        }

        TrieNode root;

        Trie() {
            root = new TrieNode();
        }

        public List<String> get(String key) {
            if (key == null) {
                throw new IllegalArgumentException("argument to get() is null");
            }
            TrieNode x = get(root, key, 0);
            if (x == null) {
                return null;
            }
            List<String> foundWords = new LinkedList<>();
            findWords(x, foundWords);
            return foundWords;
        }

        private TrieNode get(TrieNode x, String key, int d) {
            if (x == null) {
                return null;
            }
            if (d == key.length()) {
                return x;
            }
            char c = key.charAt(d);
            return get(x.children.get(c), key, d + 1);
        }

        private void findWords(TrieNode x, List<String> words) {
            if (x == null) {
                return;
            }
            if (x.exists) {
                for (String w : x.words) {
                    words.add(w);
                }
            }
            for (Character c : x.children.keySet()) {
                findWords(x.children.get(c), words);
            }
        }

        public void put(String word) {
            String cleaned = cleanString(word);
            put(root, word, cleaned, 0);
        }

        public TrieNode put(TrieNode x, String unclean, String key, int d) {
            if (x == null) {
                x = new TrieNode();
            }
            if (d == key.length()) {
                x.exists = true;
                x.words.add(unclean);
                return x;
            }
            char c = key.charAt(d);
            x.children.put(c, put(x.children.get(c), unclean, key, d + 1));
            return x;
        }
    }
}
