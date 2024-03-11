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
package net.automatalib.automaton.ra;

import java.util.Collection;
import java.util.Map;
import net.automatalib.automaton.UniversalAutomaton;
import net.automatalib.automaton.graph.AutomatonGraphView;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.graph.TransitionEdge.Property;
import net.automatalib.automaton.graph.UniversalAutomatonGraphView;
import net.automatalib.automaton.visualization.AutomatonVisualizationHelper;
import net.automatalib.graph.UniversalGraph;
import net.automatalib.visualization.DefaultVisualizationHelper;
import net.automatalib.visualization.VisualizationHelper;

public class RegisterAutomatonGraphView<S, I, T extends GuardedTransition, SP, TP, A extends UniversalAutomaton<S, I, T, SP, TP>>
        extends UniversalAutomatonGraphView<S, I, T, SP, TP, A> {

    public RegisterAutomatonGraphView(A automaton, Collection<? extends I> inputs) {
        super(automaton, inputs);
    }

    @Override
    public VisualizationHelper<S, TransitionEdge<I, T>> getVisualizationHelper() {
        return new AutomatonVisualizationHelper<>(automaton) {

            @Override
            public boolean getNodeProperties(S node, Map<String, String> properties) {
                super.getNodeProperties(node, properties);

                if (Boolean.TRUE.equals(automaton.getStateProperty(node))) {
                    properties.put(NodeAttrs.SHAPE, NodeShapes.DOUBLECIRCLE);
                }

                return true;
            }

            @Override
            public boolean getEdgeProperties(S src, TransitionEdge<I, T> edge, S tgt, Map<String, String> properties) {
                final I input = edge.input();
                final T transition = edge.transition();

                super.getEdgeProperties(src, edge, tgt, properties);

                properties.put(EdgeAttrs.LABEL,
                               input + ", " + transition.getGuard() + ", " + transition.getAssignment());

                return true;
            }
        };
    }
}
