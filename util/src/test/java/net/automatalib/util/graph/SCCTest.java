/* Copyright (C) 2013-2025 TU Dortmund University
 * This file is part of AutomataLib <https://automatalib.net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.util.graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.graph.impl.CompactSimpleGraph;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SCCTest {

    @Test
    public void testExample1() {

        Integer n0, n1, n2, n3;
        CompactSimpleGraph<Void> graph;
        graph = new CompactSimpleGraph<>();

        n0 = graph.addNode();
        n1 = graph.addNode();
        n2 = graph.addNode();
        n3 = graph.addNode();

        graph.connect(n0, n1, null);
        graph.connect(n1, n0, null);
        graph.connect(n1, n2, null);
        graph.connect(n2, n1, null);
        graph.connect(n2, n3, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(0, 1, 2), Set.of(3));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        Assert.assertEquals(computedSCCs.size(), 2);
        Assert.assertEquals(computedSCCs, expectedSCCs);
    }

    @Test
    public void testExample2() {
        CompactSimpleGraph<Void> graph = new CompactSimpleGraph<>();
        Integer n0, n1, n2, n3, n4;

        n0 = graph.addNode();
        n1 = graph.addNode();
        n2 = graph.addNode();
        n3 = graph.addNode();
        n4 = graph.addNode();

        graph.connect(n0, n1, null);
        graph.connect(n1, n2, null);
        graph.connect(n2, n3, null);
        graph.connect(n3, n1, null);
        graph.connect(n2, n4, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(n0), Set.of(n1, n2, n3), Set.of(n4));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        Assert.assertEquals(computedSCCs.size(), 3);
        Assert.assertEquals(computedSCCs, expectedSCCs);
    }

    /*
     * This example is taken from the following slide deck:
     * https://cs.nyu.edu/courses/spring17/CSCI-UA.0310-001/graphs-scc.pdf
     */
    @Test
    public void testExample3() {
        CompactSimpleGraph<Void> graph = new CompactSimpleGraph<>();
        Integer a, b, c, d, e, f, g, h;
        a = graph.addNode();
        b = graph.addNode();
        c = graph.addNode();
        d = graph.addNode();
        e = graph.addNode();
        f = graph.addNode();
        g = graph.addNode();
        h = graph.addNode();

        graph.connect(a, b, null);
        graph.connect(b, c, null);
        graph.connect(b, f, null);
        graph.connect(b, e, null);
        graph.connect(c, d, null);
        graph.connect(c, g, null);
        graph.connect(d, c, null);
        graph.connect(d, h, null);
        graph.connect(e, a, null);
        graph.connect(e, f, null);
        graph.connect(f, g, null);
        graph.connect(g, f, null);
        graph.connect(g, h, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(a, b, e), Set.of(c, d), Set.of(h), Set.of(f, g));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        Assert.assertEquals(computedSCCs.size(), 4);
        Assert.assertEquals(computedSCCs, expectedSCCs);
    }

    /*
     * This example is taken from the following slide deck:
     * http://www.cse.cuhk.edu.hk/~taoyf/course/comp3506/lec/scc.pdf
     */
    @Test
    public void testExample4() {
        CompactSimpleGraph<Void> graph = new CompactSimpleGraph<>();
        Integer a, b, c, d, e, f, g, h, i, j, k, l;
        a = graph.addNode();
        b = graph.addNode();
        c = graph.addNode();
        d = graph.addNode();
        e = graph.addNode();
        f = graph.addNode();
        g = graph.addNode();
        h = graph.addNode();
        i = graph.addNode();
        j = graph.addNode();
        k = graph.addNode();
        l = graph.addNode();

        graph.connect(a, c, null);
        graph.connect(b, a, null);
        graph.connect(c, b, null);
        graph.connect(d, b, null);
        graph.connect(d, e, null);
        graph.connect(e, a, null);
        graph.connect(e, f, null);
        graph.connect(e, g, null);
        graph.connect(f, d, null);
        graph.connect(f, k, null);
        graph.connect(k, l, null);
        graph.connect(l, f, null);
        graph.connect(g, d, null);
        graph.connect(j, e, null);
        graph.connect(j, g, null);
        graph.connect(j, j, null);
        graph.connect(j, h, null);
        graph.connect(h, i, null);
        graph.connect(i, h, null);
        graph.connect(i, g, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(a, b, c), Set.of(d, e, f, g, l, k), Set.of(i, h), Set.of(j));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        Assert.assertEquals(computedSCCs.size(), 4);
        Assert.assertEquals(computedSCCs, expectedSCCs);
    }

    @Test
    public void testExample5() {
        CompactSimpleGraph<Void> graph = new CompactSimpleGraph<>();
        Integer n0, n1, n2, n3, n4, n5, n6, n7, n8, n9;

        n0 = graph.addNode();
        n1 = graph.addNode();
        n2 = graph.addNode();
        n3 = graph.addNode();
        n4 = graph.addNode();
        n5 = graph.addNode();
        n6 = graph.addNode();
        n7 = graph.addNode();
        n8 = graph.addNode();
        n9 = graph.addNode();

        graph.connect(n5, n7, null);
        graph.connect(n0, n8, null);
        graph.connect(n9, n4, null);
        graph.connect(n9, n7, null);
        graph.connect(n1, n5, null);
        graph.connect(n8, n4, null);
        graph.connect(n8, n2, null);
        graph.connect(n5, n9, null);
        graph.connect(n8, n7, null);
        graph.connect(n5, n6, null);
        graph.connect(n4, n7, null);
        graph.connect(n0, n5, null);
        graph.connect(n6, n8, null);
        graph.connect(n1, n6, null);
        graph.connect(n3, n9, null);
        graph.connect(n4, n6, null);
        graph.connect(n9, n5, null);
        graph.connect(n2, n7, null);
        graph.connect(n1, n0, null);
        graph.connect(n2, n0, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(n3), Set.of(n7), Set.of(n1), Set.of(n0, n2, n4, n5, n6, n8, n9));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        /*
         * An invariant of this algorithm is that every node in the input graph should be maintained in the result set.
         */
        Set<Integer> nodeSet = new HashSet<>(Arrays.asList(n0, n1, n2, n3, n4, n5, n6, n7, n8, n9));
        computedSCCs.forEach(nodeSet::removeAll);
        Assert.assertEquals(nodeSet.size(), 0);

        Assert.assertEquals(computedSCCs, expectedSCCs);
        Assert.assertEquals(computedSCCs.size(), 4);
    }

    /*
     * The following example is taken from Trajans original paper
     * about his SCC-algorithm.
     * TARJAN, Robert. Depth-first search and linear graph algorithms.
     * SIAM journal on computing, 1972, 1. Jg., Nr. 2, S. 146-160.
     */
    @Test
    public void testTarjansSCCPaperExample() {
        CompactSimpleGraph<Void> graph = new CompactSimpleGraph<>();
        Integer n1, n2, n3, n4, n5, n6, n7, n8;

        n1 = graph.addNode();
        n2 = graph.addNode();
        n3 = graph.addNode();
        n4 = graph.addNode();
        n5 = graph.addNode();
        n6 = graph.addNode();
        n7 = graph.addNode();
        n8 = graph.addNode();

        graph.connect(n1, n2, null);
        graph.connect(n2, n3, null);
        graph.connect(n2, n8, null);
        graph.connect(n3, n4, null);
        graph.connect(n3, n7, null);
        graph.connect(n4, n5, null);
        graph.connect(n5, n3, null);
        graph.connect(n5, n6, null);
        graph.connect(n7, n6, null);
        graph.connect(n7, n4, null);
        graph.connect(n8, n1, null);
        graph.connect(n8, n7, null);

        Set<Set<Integer>> expectedSCCs = Set.of(Set.of(n1, n2, n8), Set.of(n6), Set.of(n3, n4, n5, n7));

        Set<Set<Integer>> computedSCCs = computeSCCs(graph);

        Assert.assertEquals(computedSCCs.size(), 3);
        Assert.assertEquals(computedSCCs, expectedSCCs);
    }

    private Set<Set<Integer>> computeSCCs(CompactSimpleGraph<Void> graph) {
        return Graphs.collectSCCs(graph).stream().map(HashSet::new).collect(Collectors.toSet());
    }
}
