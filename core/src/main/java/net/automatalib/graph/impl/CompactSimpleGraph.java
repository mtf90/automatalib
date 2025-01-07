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

import net.automatalib.graph.base.AbstractCompactGraph;
import net.automatalib.graph.base.CompactEdge;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CompactSimpleGraph<@Nullable EP> extends AbstractCompactGraph<CompactEdge<EP>, Void, EP> {

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
    protected CompactEdge<EP> createEdge(int source, int target, @Nullable EP property) {
        return new CompactEdge<>(target, property);
    }

}
