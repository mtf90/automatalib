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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.automatalib.common.util.mapping.Mapping;
import net.automatalib.common.util.mapping.MutableMapping;
import net.automatalib.graph.BidirectionalGraph;
import net.automatalib.graph.Graph;
import net.automatalib.graph.concept.EdgeWeights;
import net.automatalib.util.graph.apsp.APSPResult;
import net.automatalib.util.graph.apsp.FloydWarshallAPSP;
import net.automatalib.util.graph.scc.SCCListener;
import net.automatalib.util.graph.scc.SCCs;
import net.automatalib.util.graph.scc.TarjanSCCVisitor;
import net.automatalib.util.graph.sssp.DijkstraSSSP;
import net.automatalib.util.graph.sssp.SSSPResult;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Graphs {

    /**
     * Float value to signal that no valid distance is returned (e.g., when attempting to retrieve the length of a path
     * that does not exist).
     */
    public static final float INVALID_DISTANCE = Float.NEGATIVE_INFINITY;

    private Graphs() {}

    public static <N, E> Mapping<N, @Nullable Collection<E>> incomingEdges(Graph<N, E> graph) {
        if (graph instanceof BidirectionalGraph) {
            final BidirectionalGraph<N, E> bdGraph = (BidirectionalGraph<N, E>) graph;
            return bdGraph::getIncomingEdges;
        }

        MutableMapping<N, @Nullable Collection<E>> inEdgesMapping = graph.createStaticNodeMapping();

        for (N node : graph) {
            Collection<E> outEdges = graph.getOutgoingEdges(node);
            for (E e : outEdges) {
                N tgt = graph.getTarget(e);
                Collection<E> inEdges = inEdgesMapping.get(tgt);
                if (inEdges == null) {
                    inEdges = new ArrayList<>();
                    inEdgesMapping.put(tgt, inEdges);
                }
                inEdges.add(e);
            }
        }

        return inEdgesMapping;
    }

    /**
     * Converts a list of edges into a corresponding list of nodes. Note that the list of nodes is always one larger
     * than the respective list of edges.
     *
     * @param edgeList
     *         the list of edges
     * @param graph
     *         the graph
     * @param init
     *         the initial node
     * @param <N>
     *         node type
     * @param <E>
     *         edge type
     *
     * @return the node list corresponding to the given edge list.
     */
    public static <N, E> List<N> toNodeList(List<E> edgeList, Graph<N, E> graph, N init) {
        List<N> result = new ArrayList<>(edgeList.size() + 1);
        result.add(init);

        for (E edge : edgeList) {
            N tgt = graph.getTarget(edge);
            result.add(tgt);
        }

        return result;
    }

    /**
     * Computes the shortest paths between all pairs of nodes in a graph, using the Floyd-Warshall dynamic programming
     * algorithm. Note that the result is only correct if the graph contains no cycles with negative edge weight sums.
     *
     * @param graph
     *         the graph
     * @param edgeWeights
     *         the edge weights
     * @param <N>
     *         node type
     * @param <E>
     *         edge type
     *
     * @return the all pairs shortest paths result
     *
     * @see FloydWarshallAPSP
     */
    public static <N, E> APSPResult<N, E> findAPSP(Graph<N, E> graph, EdgeWeights<E> edgeWeights) {
        return FloydWarshallAPSP.findAPSP(graph, edgeWeights);
    }

    /**
     * Computes the shortest paths between a single source node and all other nodes in a graph, using Dijkstra's
     * algorithm. Note that the result is only correct if the graph contains no edges with negative weights.
     *
     * @param graph
     *         the graph
     * @param init
     *         the source node
     * @param edgeWeights
     *         the edge weights
     * @param <N>
     *         node type
     * @param <E>
     *         edge type
     *
     * @return the single-source shortest paths result
     *
     * @see DijkstraSSSP
     */
    public static <N, E> SSSPResult<N, E> findSSSP(Graph<N, E> graph, N init, EdgeWeights<E> edgeWeights) {
        return DijkstraSSSP.findSSSP(graph, init, edgeWeights);
    }

    /**
     * Collects all strongly-connected components in a graph. The SCCs are returned as a list of lists.
     * <p>
     * Tarjan's algorithm is used for realizing the SCC search.
     *
     * @param graph
     *         the graph
     * @param <N>
     *         node type
     * @param <E>
     *         edge type
     *
     * @return a list of all SCCs, each represented as a list of its nodes
     *
     * @see TarjanSCCVisitor
     * @see SCCs
     */
    public static <N, E> List<List<N>> collectSCCs(Graph<N, E> graph) {
        return SCCs.collectSCCs(graph);
    }

    /**
     * Find all strongly-connected components in a graph. When a new SCC is found, the {@link
     * SCCListener#foundSCC(java.util.Collection)} method is invoked. The listener object may hence not be null.
     * <p>
     * Tarjan's algorithm is used for realizing the SCC search.
     *
     * @param graph
     *         the graph
     * @param sccListener
     *         the SCC listener
     * @param <N>
     *         node type
     * @param <E>
     *         edge type
     *
     * @see TarjanSCCVisitor
     * @see SCCs
     */
    public static <N, E> void findSCCs(Graph<N, E> graph, SCCListener<N> sccListener) {
        SCCs.findSCCs(graph, sccListener);
    }
}
