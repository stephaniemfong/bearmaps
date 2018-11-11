import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    static Map<Long, Double> priority = new HashMap<>();

    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        try {
            GraphDB.Node source = g.graph.nodes.get(g.closest(stlon, stlat));
            GraphDB.Node goal = g.graph.nodes.get(g.closest(destlon, destlat));

            Comparator<GraphDB.Node> comparator = new NodeComparator();
            PriorityQueue<GraphDB.Node> fringe = new PriorityQueue<>(11, comparator);

            Map<Long, Double> heuristic = new HashMap<>();
            Map<Long, Double> distTo = new HashMap<>();
            Map<Long, Long> edgeTo = new HashMap<>();
            Set<GraphDB.Node> seen = new HashSet<>();

            for (long l : g.vertices()) {
                distTo.put(l, Double.MAX_VALUE);
                edgeTo.put(l, l);
                heuristic.put(l, g.distance(l, goal.id));
            }

            distTo.put(source.id, 0.0);
            fringe.add(source);

            while (!fringe.isEmpty()) {
                GraphDB.Node r = fringe.poll();
                if (r.equals(goal)) {
                    break;
                }
                if (!seen.contains(r)) {
                    seen.add(r);
                    for (GraphDB.Node neighbor : r.neighbors) {
                        if (!seen.contains(neighbor)) {
                            if ((distTo.get(r.id) + g.findEdge(r, neighbor).weight)
                                    < distTo.get(neighbor.id)) {
                                distTo.put(neighbor.id, distTo.get(r.id)
                                        + g.findEdge(r, neighbor).weight);
                                edgeTo.put(neighbor.id, r.id);
                                priority.put(neighbor.id, distTo.get(r.id)
                                        + g.findEdge(r, neighbor).weight
                                        + heuristic.get(neighbor.id));
                                fringe.add(neighbor);
                            }
                        }
                    }
                }
            }

            List<Long> path = new LinkedList<>();
            GraphDB.Node curr = goal;
            path.add(0, curr.id);
            while (!curr.equals(source)) {
                path.add(0, edgeTo.get(curr.id));
                curr = g.graph.nodes.get(edgeTo.get(curr.id));
            }
            return path;
        } catch (java.lang.OutOfMemoryError e) {
            return new LinkedList<>();
        }
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> directions = new LinkedList<>();
        boolean start = true;
        long prevNode = route.get(0);
        double distance = 0;

        NavigationDirection temp = new NavigationDirection();
        temp.direction = NavigationDirection.START;
        temp.way = g.graph.nodes.get(route.get(0)).wayName;

        for (Long l : route) {
            if (g.graph.nodes.get(l).wayName.equals(temp.way)) {
                distance += g.distance(prevNode, l);
            } else {
                distance += g.distance(prevNode, l);
                temp.distance = distance;
                directions.add(temp);

                temp = new NavigationDirection();
                temp.direction = getDirectionInt(g.bearing(prevNode, l));
                if (g.graph.nodes.get(l).wayName == null) {
                    temp.way = NavigationDirection.UNKNOWN_ROAD;
                } else {
                    temp.way = g.graph.nodes.get(l).wayName;
                }
                distance = 0;
            }

            prevNode = l;
        }
        return directions;
    }

    private static int getDirectionInt(double bearing) {
        if (bearing > -15 && bearing < 15) {
            return 1;
        } else if (bearing > -30 && bearing < 30) {
            if (bearing < 0) {
                return 2;
            } else {
                return 3;
            }
        } else if (bearing > -100 && bearing < 100) {
            if (bearing < 0) {
                return 4;
            } else {
                return 5;
            }
        } else {
            if (bearing < 0) {
                return 6;
            } else {
                return 7;
            }
        }
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int LEFT = 4;
        public static final int RIGHT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }

    static class NodeComparator implements Comparator<GraphDB.Node> {
        @Override
        public int compare(GraphDB.Node first, GraphDB.Node second) {
            double firstPriority = priority.get(first.id);
            double secondPriority = priority.get(second.id);
            if (firstPriority == secondPriority) {
                return 0;
            } else if (firstPriority < secondPriority) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
