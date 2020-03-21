package app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import game.HuntState;
import game.Hunter;
import game.Node;
import game.NodeStatus;
import game.ScramState;

/** A solution with huntOrb optimized and scram getting out as fast as possible. */
public class Pollack extends Hunter {

    /** Contains the set of IDs of the nodes that have been visited. */
    private HashSet<Long> visited= new HashSet<>();

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as a
     * failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLocation(), neighbors(), and distanceToOrb() in HuntState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in HuntState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void huntOrb(HuntState state) {
        // TODO 1: Get the orb
        optimizedDfsWalk(state);
    }

    /** The walker is standing on a Node u (say) given by HuntState state. <br>
     * Visit every node reachable along paths of unvisited nodes from node u <br>
     * in order of least distance to the orb. Return if the orb is found <br>
     * (in other words, when the state's distance to the orb is zero. <br>
     * Precondition: u is unvisited. */
    private void optimizedDfsWalk(HuntState state) {
        if (state.distanceToOrb() == 0) return;
        Long currentNodeID= state.currentLocation();
        visited.add(currentNodeID);
        for (NodeStatus neighbor : sortedNeighbors(state.neighbors())) {
            long neighborID= neighbor.getId();
            if (!visited.contains(neighborID)) {
                state.moveTo(neighborID);
                optimizedDfsWalk(state);
                if (state.distanceToOrb() == 0) return;
                state.moveTo(currentNodeID);
            }
        }
    }

    /** Return an ArrayList of NodeStatus objects, sorted by their distance to the orb. */
    private ArrayList<NodeStatus> sortedNeighbors(Collection<NodeStatus> neighbors) {
        ArrayList<NodeStatus> neighborsList= new ArrayList<>(neighbors);
        neighborsList.sort((n1, n2) -> n1.getDistanceToTarget() - n2.getDistanceToTarget());
        return neighborsList;

    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before time runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through ScramState. <br>
     * currentNode() and getExit() will return Node objects of interest, and <br>
     * getNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * getStepsRemaining(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use getStepsRemaining() to get the time still remaining, <br>
     * pickUpGold() to pick up any gold on your current tile <br>
     * (this will fail if no such gold exists), and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before time runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough time to scram using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution */
    @Override
    public void scram(ScramState state) {
        // TODO 2: Get out of the cavern before it collapses, picking up gold along the way
        maxScram(state);
    }

    /** Return the number of steps required to go from the current node, to the <br>
     * target node, and then to the exit. */
    private int stepsToReturn(ScramState state, Node target) {
        return Path.shortestDistance(state.currentNode(), target) +
            Path.shortestDistance(target, state.getExit());
    }

    /** Return a "score" for an end node based on how much gold is on the node <br>
     * and how far away the end node is from the current state <br>
     * (the closer the end node is to the current node and the more gold on the <br>
     * end node, the higher the score). */
    private double calcScore(ScramState state, Node end) {
        double dist= Path.shortestDistance(state.currentNode(), end);
        double gold= end.getTile().gold();
        return gold / dist;
    }

//    private int goldOfNeighbors(Node n, int radius) {
//        int sum= 0;
//        if (radius == 0) return sum;
//        for (Node neighbor : n.getNeighbors()) {
//            sum+= neighbor.getTile().gold();
//            while (Path.shortest(n, neighbor).size() <= radius) {
//                sum+= goldOfNeighbors(neighbor, radius - 1);
//            }
//        }
//        return sum;
//    }

    /** Moves Martha along the shortest path to the specified end node. */
    private void travel(ScramState state, Node end) {
        List<Node> path= Path.shortest(state.currentNode(), end);
        for (int i= 1; i < path.size(); i++ ) {
            state.moveTo(path.get(i));
        }
    }

    /** Travel to the node with the highest score (calculated in the above method) <br>
     * iff traveling to that node and to the exit can be done without running <br>
     * out of steps. Otherwise, continue to run this procedure on the node with <br>
     * the next highest score until the node can be traveled to. If no <br>
     * gold-filled node in the graph can be reached without running out of steps, <br>
     * return to the exit. */
    private void maxScram(ScramState state) {
        // Maintains a max-heap of nodes, with the priority being their score.
        Heap<Node> nodeHeap= new Heap<>(true);
        for (Node n : state.allNodes()) {
            // No need to add nodes with no gold on them
            if (n.getTile().gold() != 0) nodeHeap.add(n, calcScore(state, n));
        }
        if (nodeHeap.size() == 0) {
            travel(state, state.getExit());
            return;
        }
        Node richestNode= nodeHeap.poll();
        int distanceToReturn= stepsToReturn(state, richestNode);
        while (distanceToReturn > state.stepsLeft() && nodeHeap.size() != 0) {
            richestNode= nodeHeap.poll();
            distanceToReturn= stepsToReturn(state, richestNode);
        }
        if (nodeHeap.size() != 0) {
            travel(state, richestNode);
            maxScram(state);
        } else travel(state, state.getExit());

    }
}
