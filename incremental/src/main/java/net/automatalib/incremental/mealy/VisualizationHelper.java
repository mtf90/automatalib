/* Copyright (C) 2013-2024 TU Dortmund University
 * This file is part of AutomataLib, http://www.automatalib.net/.
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
package net.automatalib.incremental.mealy;

import java.util.Map;

import net.automatalib.automaton.transducer.MealyMachine;
import net.automatalib.automaton.visualization.MealyVisualizationHelper;

/**
 * A utility class for rendering {@link IncrementalMealyBuilder}s.
 *
 * @param <S>
 *         state type
 * @param <I>
 *         input symbol type
 * @param <T>
 *         transition type
 * @param <O>
 *         output symbol type
 */
public class VisualizationHelper<S, I, T, O> extends MealyVisualizationHelper<S, I, T, O> {

    private int idx;

    public VisualizationHelper(MealyMachine<S, I, T, O> mealy) {
        super(mealy);
    }

    @Override
    public boolean getNodeProperties(S node, Map<String, String> properties) {
        super.getNodeProperties(node, properties);

        properties.put(NodeAttrs.LABEL, "n" + (idx++));

        return true;
    }
}
