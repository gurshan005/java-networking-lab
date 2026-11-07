import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

// Routing algorithm adapted from Gurshan Bhogal's Java implementation.
public final class RoutingDemo {
    private static final long INF = Long.MAX_VALUE / 4;

    private static final class Edge {
        final int to;
        final int weight;

        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    private static final class State {
        final int node;
        final long distance;

        State(int node, long distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    private static final class DijkstraResult {
        final long[] distance;
        final TreeSet<Integer>[] parents;

        DijkstraResult(long[] distance, TreeSet<Integer>[] parents) {
            this.distance = distance;
            this.parents = parents;
        }
    }

    // Directed links with positive costs. Routers 4 and 6 are exits.
    public static void main(String[] args) {
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i <= 7; i++) graph.add(new ArrayList<>());
        graph.get(1).add(new Edge(2, 1));
        graph.get(1).add(new Edge(3, 1));
        graph.get(2).add(new Edge(4, 1));
        graph.get(3).add(new Edge(4, 1));
        graph.get(4).add(new Edge(5, 7));
        graph.get(2).add(new Edge(5, 10));
        graph.get(5).add(new Edge(6, 2));
        boolean[] exits = new boolean[graph.size()];
        exits[4] = true;
        exits[6] = true;
        DijkstraResult result = runDijkstra(1, graph, exits);
        TreeSet<Integer>[] hops = computeFirstHops(1, result.distance, result.parents);
        checkRoute(result, hops, 4, 2L, List.of(2, 3));
        checkRoute(result, hops, 6, 13L, List.of(2));
        checkRoute(result, hops, 7, INF, List.of());
    }

    private static void checkRoute(DijkstraResult result, TreeSet<Integer>[] hops,
                                   int target, long cost, List<Integer> expectedHops) {
        if (result.distance[target] != cost || !new ArrayList<>(hops[target]).equals(expectedHops)) {
            throw new AssertionError("Unexpected route to " + target);
        }
        String weight = cost == INF ? "unreachable" : Long.toString(cost);
        System.out.printf("1 -> %d: cost=%s, first hops=%s%n", target, weight, hops[target]);
    }

    private static DijkstraResult runDijkstra(int source, List<List<Edge>> graph, boolean[] isGateway) {
        int n = graph.size() - 1;
        long[] distance = new long[n + 1];
        Arrays.fill(distance, INF);

        @SuppressWarnings("unchecked")
        TreeSet<Integer>[] parents = new TreeSet[n + 1];
        for (int router = 1; router <= n; router++) {
            parents[router] = new TreeSet<>();
        }

        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingLong(state -> state.distance));
        distance[source] = 0L;
        pq.add(new State(source, 0L));

        while (!pq.isEmpty()) {
            State current = pq.poll();
            if (current.distance != distance[current.node]) {
                continue;
            }

            // Once a packet reaches a gateway, that gateway must be the exit point.
            if (isGateway[current.node] && current.node != source) {
                continue;
            }

            for (Edge edge : graph.get(current.node)) {
                long candidate = current.distance + edge.weight;
                if (candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    parents[edge.to].clear();
                    parents[edge.to].add(current.node);
                    pq.add(new State(edge.to, candidate));
                } else if (candidate == distance[edge.to]) {
                    parents[edge.to].add(current.node);
                }
            }
        }

        return new DijkstraResult(distance, parents);
    }

    private static TreeSet<Integer>[] computeFirstHops(int source, long[] distance, TreeSet<Integer>[] parents) {
        int n = distance.length - 1;

        @SuppressWarnings("unchecked")
        TreeSet<Integer>[] firstHops = new TreeSet[n + 1];
        for (int router = 1; router <= n; router++) {
            firstHops[router] = new TreeSet<>();
        }

        for (int router = 1; router <= n; router++) {
            if (router == source || distance[router] >= INF) {
                continue;
            }

            if (parents[router].contains(source)) {
                firstHops[router].add(router);
            }
        }

        // A small fixpoint pass keeps alternative next hops correct even when zero-weight links
        // place shortest-path parents on the same distance layer.
        boolean changed;
        do {
            changed = false;

            for (int router = 1; router <= n; router++) {
                if (router == source || distance[router] >= INF) {
                    continue;
                }

                for (int parent : parents[router]) {
                    if (parent == source) {
                        continue;
                    }

                    if (firstHops[router].addAll(firstHops[parent])) {
                        changed = true;
                    }
                }
            }
        } while (changed);

        return firstHops;
    }

}
