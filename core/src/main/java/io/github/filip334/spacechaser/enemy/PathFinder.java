package io.github.filip334.spacechaser.enemy;

import com.badlogic.gdx.utils.Array;

public class PathFinder {

    private final GridNode[][] grid;

    private final int width;
    private final int height;

    // ---------------- KONSTRUKTOR ----------------

    public PathFinder(boolean[][] walkable) {

        height = walkable.length;
        width = walkable[0].length;

        grid = new GridNode[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                grid[x][y] = new GridNode(
                        x,
                        y,
                        walkable[y][x]
                );
            }
        }
    }

    // ---------------- PATHFINDING (A*) ----------------

    public Array<GridNode> findPath(int startX,int startY,int targetX,int targetY){

        Array<GridNode> open = new Array<>();
        Array<GridNode> closed = new Array<>();

        resetSearchState();

        GridNode start = getWalkableNode(startX, startY);
        GridNode target = getWalkableNode(targetX, targetY);

        if (start == null || target == null) {
            return new Array<>();
        }

        open.add(start);

        start.gCost = 0;
        start.hCost = distance(start, target);

        while (open.size > 0) {

            GridNode current = open.get(0);

            for (GridNode node : open) {

                if (node.getFCost() < current.getFCost()
                        || node.getFCost() == current.getFCost()
                        && node.hCost < current.hCost) {

                    current = node;
                }
            }

            open.removeValue(current, true);
            closed.add(current);

            if (current == target) {
                return reconstructPath(start, target);
            }

            for (GridNode neighbour : getNeighbours(current)) {

                if (!neighbour.walkable || closed.contains(neighbour, true)) {
                    continue;
                }

                float newCost =
                        current.gCost + distance(current, neighbour);

                if (!open.contains(neighbour, true)
                        || newCost < neighbour.gCost) {

                    neighbour.gCost = newCost;
                    neighbour.hCost = distance(neighbour, target);
                    neighbour.parent = current;

                    if (!open.contains(neighbour, true)) {
                        open.add(neighbour);
                    }
                }
            }
        }

        return new Array<>();
    }

    // ---------------- HELPERS ----------------

    private void resetSearchState() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                GridNode node = grid[x][y];
                node.gCost = Float.MAX_VALUE;
                node.hCost = 0f;
                node.parent = null;
            }
        }
    }

    private Array<GridNode> reconstructPath(
            GridNode start,
            GridNode target) {

        Array<GridNode> path = new Array<>();

        GridNode current = target;

        while (current != start) {

            path.add(current);
            current = current.parent;
        }

        path.add(start);

        path.reverse();

        return path;
    }

    private Array<GridNode> getNeighbours(GridNode node) {

        Array<GridNode> neighbours = new Array<>();

        addNeighbour(neighbours, node.x + 1, node.y);
        addNeighbour(neighbours, node.x - 1, node.y);
        addNeighbour(neighbours, node.x, node.y + 1);
        addNeighbour(neighbours, node.x, node.y - 1);

        return neighbours;
    }

    private void addNeighbour(
            Array<GridNode> array,
            int x,
            int y) {

        GridNode node = getNode(x, y);

        if (node != null) {
            array.add(node);
        }
    }

    /**
     * Vraca cvor na (x,y) ako je gazljiv; ako nije - npr. entitet se
     * trenutno nalazi u "tampon zoni" oko zida koju navigaciona mreza
     * tretira kao blokiranu - trazi se najblizi gazljivi cvor u sve vecem
     * radijusu.
     *
     * FIX: findPath() je ranije odmah vracao praznu putanju cim start ILI
     * cilj padnu u takvu tampon celiju, bez obzira da li put realno
     * postoji. Na gusto zidanoj mapi je raketa vrlo cesto stajala bas u
     * takvoj celiji (pored bilo kog zida), pa je A* stalno "odustajao" -
     * to je izgledalo kao da se raketa zbuni i zamrzne.
     */
    private GridNode getWalkableNode(int x, int y) {
        GridNode direct = getNode(x, y);
        if (direct != null && direct.walkable) {
            return direct;
        }

        for (int radius = 1; radius <= 6; radius++) {
            GridNode nearest = null;
            int nearestDist = Integer.MAX_VALUE;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue; // samo obod trenutnog radijusa - vec pretrazeno iznutra
                    }

                    GridNode candidate = getNode(x + dx, y + dy);
                    if (candidate == null || !candidate.walkable) {
                        continue;
                    }

                    int dist = dx * dx + dy * dy;
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = candidate;
                    }
                }
            }

            if (nearest != null) {
                return nearest;
            }
        }

        return null;
    }

    private GridNode getNode(int x, int y) {

        if (x < 0 || x >= width
                || y < 0 || y >= height) {

            return null;
        }

        return grid[x][y];
    }

    private float distance(
            GridNode a,
            GridNode b) {

        return Math.abs(a.x - b.x)
                + Math.abs(a.y - b.y);
    }
}
