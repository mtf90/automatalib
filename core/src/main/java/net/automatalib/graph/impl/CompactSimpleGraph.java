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
package net.automatalib.graph.impl;

import net.automatalib.graph.base.AbstractCompactUniversalGraph;
import net.automatalib.graph.base.CompactEdge;

/**
 * A compact graph representation that supports arbitrary edge properties.
 *
 * @param <EP>
 *         edge property type
 */
public class CompactSimpleGraph<EP> extends AbstractCompactUniversalGraph<CompactEdge<EP>, Void, EP> {

    public CompactSimpleGraph() {
        // default constructor
    }

    public CompactSimpleGraph(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void setNodeProperty(int node, Void property) {}

    @Override
    public Void getNodeProperty(int node) {
        return null;
    }

    @Override
    protected CompactEdge<EP> createEdge(int source, int target, EP property) {
        return new CompactEdge<>(target, property);
    }

    public Integer addNode() {
        return super.addNode(null);
    }

    public int addIntNode() {
        return super.addIntNode(null);
    }

}
