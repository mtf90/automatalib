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
package net.automatalib.automaton.visualization;

import java.util.Map;

import net.automatalib.automaton.transducer.MooreMachine;

public class MooreVisualizationHelper<S, I, T, O>
        extends AutomatonVisualizationHelper<S, I, T, MooreMachine<S, I, T, O>> {

    public MooreVisualizationHelper(MooreMachine<S, I, T, O> automaton) {
        super(automaton);
    }

    @Override
    public boolean getNodeProperties(S node, Map<String, String> properties) {
        super.getNodeProperties(node, properties);

        final StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append(node).append(" / ");
        O output = automaton.getStateOutput(node);
        if (output != null) {
            labelBuilder.append(output);
        }
        properties.put(NodeAttrs.LABEL, labelBuilder.toString());

        return true;
    }

}
