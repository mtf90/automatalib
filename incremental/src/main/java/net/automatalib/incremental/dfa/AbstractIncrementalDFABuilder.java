/* Copyright (C) 2013-2023 TU Dortmund
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
package net.automatalib.incremental.dfa;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.concept.InputAlphabetHolder;
import net.automatalib.ts.UniversalDTS;
import net.automatalib.visualization.DefaultVisualizationHelper;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.word.Word;

/**
 * Abstract base class for {@link IncrementalDFABuilder}s. This class takes care of holding the input alphabet and its
 * size.
 *
 * @param <I>
 *         input symbol class
 */
public abstract class AbstractIncrementalDFABuilder<I> implements IncrementalDFABuilder<I>, InputAlphabetHolder<I> {

    protected final Alphabet<I> inputAlphabet;
    protected int alphabetSize;

    /**
     * Constructor.
     *
     * @param inputAlphabet
     *         the input alphabet
     */
    public AbstractIncrementalDFABuilder(Alphabet<I> inputAlphabet) {
        this.inputAlphabet = inputAlphabet;
        this.alphabetSize = inputAlphabet.size();
    }

    @Override
    public Alphabet<I> getInputAlphabet() {
        return inputAlphabet;
    }

    @Override
    public boolean hasDefinitiveInformation(Word<? extends I> word) {
        return lookup(word) != Acceptance.DONT_KNOW;
    }

    @Override
    public void insert(Word<? extends I> word) {
        insert(word, true);
    }

    protected abstract static class AbstractGraphView<I, N, E> implements GraphView<I, N, E> {

        @Override
        public VisualizationHelper<N, E> getVisualizationHelper() {
            return new DefaultVisualizationHelper<N, E>() {

                @Override
                public Collection<N> initialNodes() {
                    return Collections.singleton(getInitialNode());
                }

                @Override
                public boolean getNodeProperties(N node, Map<String, String> properties) {
                    super.getNodeProperties(node, properties);

                    switch (getAcceptance(node)) {
                        case TRUE:
                            properties.put(NodeAttrs.SHAPE, NodeShapes.DOUBLECIRCLE);
                            break;
                        case DONT_KNOW:
                            properties.put(NodeAttrs.STYLE, NodeStyles.DASHED);
                            break;
                        default: // case FALSE: default style
                    }

                    return true;
                }

                @Override
                public boolean getEdgeProperties(N src, E edge, N tgt, Map<String, String> properties) {
                    super.getEdgeProperties(src, edge, tgt, properties);

                    properties.put(EdgeAttrs.LABEL, String.valueOf(getInputSymbol(edge)));

                    return true;
                }
            };
        }
    }

    protected abstract static class AbstractTransitionSystemView<S, I, T>
            implements UniversalDTS<S, I, T, Acceptance, Void> {

        @Override
        public Void getTransitionProperty(T transition) {
            return null;
        }
    }

}
