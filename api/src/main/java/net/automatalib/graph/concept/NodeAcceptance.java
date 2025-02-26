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
package net.automatalib.graph.concept;

import net.automatalib.graph.Graph;

/**
 * Node acceptance concept, for {@link Graph}s that represent a structure for deciding acceptance or rejection.
 *
 * @param <N>
 *         node class
 */
public interface NodeAcceptance<N> {

    /**
     * Checks whether a node is an accepting node.
     *
     * @param node
     *         the node
     *
     * @return {@code true} if the given node is an accepting node, {@code false} otherwise.
     */
    boolean isAcceptingNode(N node);
}
