package com.shinbokuow.labyrinthvr;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

class Edge implements Comparable<Edge> {
    public int a, b, c;
    public Edge(int _a, int _b, int _c) {
        a = _a;
        b = _b;
        c = _c;
    }

    @Override
    public int compareTo(Edge e) {
        return Integer.compare(c, e.c);
    }
}

class WallPosition {
    public int x, z;
    // left-up corner position
    WallPosition(int _x, int _z) {
        x = _x;
        z = _z;
    }
}

public class LabGenerator {
    public LabGenerator() {}

    private ArrayList<Edge> edges;
    private int[] parent;
    private Random random;
    private static final int EDGE_COST_BOUND = 10000;
    private static final double SKIP_PERCENT = 0.2;
    private int find(int x) {
        if (x == parent[x])
            return x;
        parent[x] = find(parent[x]);
        return parent[x];
    }
    public int[][] generate(int n) {
        // generate a n * n labyrinth
        // this is a 2 * 2 lab
        // 00000
        // 0 1 0
        // 02020
        // 0 2 0
        // 00000
        //
        // 0 -> walls cannot be destroyed
        // 1 -> walls not destroyed
        // 2 -> walls destroyed from 1
        edges = new ArrayList<>();
        random = new Random();
        int p = 0;
        // left-right edges row = i, column = j
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n - 1; ++j) {
                edges.add(new Edge(
                        i * n + j,
                        i * n + j + 1,
                        random.nextInt(EDGE_COST_BOUND)
                ));
            }
        }
        // up-down edges
        for (int j = 0; j < n; ++j) {
            for (int i = 0; i < n - 1; ++i) {
                edges.add(new Edge(
                        i * n + j,
                        (i + 1) * n + j,
                        random.nextInt(EDGE_COST_BOUND)
                ));
            }
        }
        Collections.sort(edges);
        parent = new int[n * n];
        for (int i = 0; i < n * n; ++i)
            parent[i] = i;
        ArrayList<WallPosition> result = new ArrayList<>();
        for (Edge edge: edges) {
            Log.i("LabGenerator", "a="+String.valueOf(edge.a)+"b="+String.valueOf(edge.b)+"c="+String.valueOf(edge.c));
            int pa = find(edge.a);
            int pb = find(edge.b);
            boolean merge = false;
            if (pa != pb) {
                parent[pa] = pb;
                merge = true;
                Log.i("LabGenerator", "not build");
            }
            if (!merge && random.nextInt(EDGE_COST_BOUND) >= EDGE_COST_BOUND * SKIP_PERCENT) {
                // build a wall
                if (edge.a + 1 == edge.b) {
                    // left-right wall
                    result.add(
                            new WallPosition(
                                    2 * (edge.a % n + 1),
                                    2 * (edge.a / n + 1) - 1
                            )
                    );
                }
                else {
                    assert(edge.a + n == edge.b);
                    // up-down wall
                    result.add(
                            new WallPosition(
                                    2 * (edge.a % n + 1) - 1,
                                    2 * (edge.a / n + 1)
                            )
                    );
                    Log.i("LabGenerator", "build,z="+String.valueOf(2*(edge.a%n+1))+"x="+String.valueOf(2*(edge.a/n+1)-1));
                }
            }
        }
        // add boundary wall
        for (int x = 0; x < 2 * n + 1; ++x) {
            result.add(
                    new WallPosition(x, 0)
            );
            result.add(
                    new WallPosition(x, 2 * n)
            );
        }
        for (int z = 1; z < 2 * n; ++z) {
            result.add(
                    new WallPosition(0, z)
            );
            result.add(
                    new WallPosition(2 * n, z)
            );
        }
        // add inner wall
        for (int x = 1; x < n; ++x) {
            for (int z = 1; z < n; ++z) {
                result.add(
                        new WallPosition(2 * x, 2 * z)
                );
            }
        }
        int[][] map = new int[2 * n + 1][2 * n + 1];
        for (WallPosition pos: result) {
            map[pos.z][pos.x] = 1;
        }
        for (int z = 0; z < 2 * n + 1; ++z) {
            String s = "";
            for (int x = 0; x < 2 * n + 1; ++x) {
                if (map[z][x] == 1)
                    s += "1";
                else
                    s += " ";
            }
            Log.i("LabGenerator", s);
        }
        return map;

    }
}
