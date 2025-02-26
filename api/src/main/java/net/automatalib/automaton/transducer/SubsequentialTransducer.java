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
package net.automatalib.automaton.transducer;

import java.util.Collection;
import java.util.List;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.concept.DetSuffixOutputAutomaton;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.graph.TransitionEdge.Property;
import net.automatalib.automaton.graph.UniversalAutomatonGraphView;
import net.automatalib.automaton.visualization.SSTVisualizationHelper;
import net.automatalib.graph.UniversalGraph;
import net.automatalib.ts.output.DeterministicOutputTS;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

/**
 * A subsequential transducer (or SST) is an {@link DeterministicOutputTS} whose state and transition properties are
 * output-{@link Word words}. Upon parsing a sequence of input symbols, each transition emits a {@link Word sequence} of
 * output symbols. After all inputs have been parsed, the output of the reached state will be emitted as well.
 * <p>
 * <b>Implementation detail:</b>
 * There exist definitions of SSTs that associate each state with an additional notion of 'acceptance' in order to
 * reject certain transductions. This implementation/interface denotes prefix-closed transductions, i.e. all states are
 * accepting. If you would like to filter out certain transduction you may use a supplementary {@link DFA} for this
 * decision problem.
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
public interface SubsequentialTransducer<S, I, T, O> extends DeterministicOutputTS<S, I, T, O>,
                                                             DetSuffixOutputAutomaton<S, I, T, Word<O>>,
                                                             UniversalDeterministicAutomaton<S, I, T, Word<O>, Word<O>> {

    @Override
    default Word<O> computeStateOutput(S state, Iterable<? extends I> input) {
        // since the outputs are words of unknown length, we can't really pre-compute a sensible builder size
        final WordBuilder<O> result = new WordBuilder<>();

        trace(state, input, result);

        return result.toWord();
    }

    @Override
    default boolean trace(S state, Iterable<? extends I> input, List<? super O> output) {
        S iter = state;

        for (I sym : input) {
            T trans = getTransition(iter, sym);
            if (trans == null) {
                return false;
            }
            final Word<O> out = getTransitionProperty(trans);
            if (out != null) {
                output.addAll(out.asList());
            }
            iter = getSuccessor(trans);
        }

        if (iter == null) {
            return false;
        } else {
            final Word<O> out = getStateProperty(iter);
            if (out != null) {
                output.addAll(out.asList());
            }
        }

        return true;
    }

    @Override
    default UniversalGraph<S, TransitionEdge<I, T>, Word<O>, Property<I, Word<O>>> transitionGraphView(Collection<? extends I> inputs) {
        return new SSTGraphView<>(this, inputs);
    }

    class SSTGraphView<S, I, T, O, A extends SubsequentialTransducer<S, I, T, O>>
            extends UniversalAutomatonGraphView<S, I, T, Word<O>, Word<O>, A> {

        public SSTGraphView(A automaton, Collection<? extends I> inputs) {
            super(automaton, inputs);
        }

        @Override
        public VisualizationHelper<S, TransitionEdge<I, T>> getVisualizationHelper() {
            return new SSTVisualizationHelper<>(automaton);
        }
    }
}
