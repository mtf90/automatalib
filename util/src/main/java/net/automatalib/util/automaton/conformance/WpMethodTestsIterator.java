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
package net.automatalib.util.automaton.conformance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.common.util.HashUtil;
import net.automatalib.common.util.collection.AbstractThreeLevelIterator;
import net.automatalib.common.util.collection.IterableUtil;
import net.automatalib.common.util.collection.IteratorUtil;
import net.automatalib.common.util.mapping.MutableMapping;
import net.automatalib.util.automaton.Automata;
import net.automatalib.util.automaton.cover.Covers;
import net.automatalib.util.automaton.equivalence.CharacterizingSets;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Iterator that returns test words generated by the partial W method.
 * <p>
 * See "Test selection based on finite state models" by S. Fujiwara et al.
 *
 * @param <I>
 *         input symbol type
 */
public class WpMethodTestsIterator<I> implements Iterator<Word<I>> {

    private final Iterator<Word<I>> wpIterator;

    /**
     * Convenience-constructor for {@link #WpMethodTestsIterator(UniversalDeterministicAutomaton, Collection, int)} that
     * selects {@code 0} as {@code maxDepth}.
     *
     * @param automaton
     *         the automaton for which the testing sequences should be generated
     * @param inputs
     *         the input symbols that should be considered for test sequence generation
     */
    public WpMethodTestsIterator(UniversalDeterministicAutomaton<?, I, ?, ?, ?> automaton,
                                 Collection<? extends I> inputs) {
        this(automaton, inputs, 0);
    }

    /**
     * Constructor.
     *
     * @param automaton
     *         the automaton for which the testing sequences should be generated
     * @param inputs
     *         the input symbols that should be considered for test sequence generation
     * @param maxDepth
     *         the maximum number of symbols that are appended to the transition-cover part of the test sequences
     */
    public WpMethodTestsIterator(UniversalDeterministicAutomaton<?, I, ?, ?, ?> automaton,
                                 Collection<? extends I> inputs,
                                 int maxDepth) {

        final Set<Word<I>> stateCover = new HashSet<>(HashUtil.capacity(automaton.size()));
        final Set<Word<I>> transitionCover = new HashSet<>(HashUtil.capacity(automaton.size() * inputs.size()));

        Covers.cover(automaton, inputs, stateCover, transitionCover);

        Iterator<Word<I>> characterizingIter = CharacterizingSets.characterizingSetIterator(automaton, inputs);

        // Special case: List of characterizing suffixes may be empty,
        // but in this case we still need to iterate over the prefixes!
        if (!characterizingIter.hasNext()) {
            characterizingIter = IteratorUtil.singleton(Word.epsilon());
        }

        // Phase 1: state cover * middle part * global suffixes
        final Iterator<Word<I>> firstIterator = new FirstPhaseIterator<>(stateCover,
                                                                         IterableUtil.allTuples(inputs, 0, maxDepth),
                                                                         characterizingIter);

        // Phase 2: transitions (not in state cover) * middle part * local suffixes
        transitionCover.removeAll(stateCover);
        final Iterator<Word<I>> secondIterator = new SecondPhaseIterator<>(automaton,
                                                                           inputs,
                                                                           transitionCover,
                                                                           IterableUtil.allTuples(inputs,
                                                                                                  0,
                                                                                                  maxDepth));

        wpIterator = IteratorUtil.concat(firstIterator, secondIterator);
    }

    @Override
    public boolean hasNext() {
        return this.wpIterator.hasNext();
    }

    @Override
    public Word<I> next() {
        return this.wpIterator.next();
    }

    private static class FirstPhaseIterator<I> extends AbstractThreeLevelIterator<Word<I>, List<I>, Word<I>, Word<I>> {

        private final Iterable<Word<I>> prefixes;
        private final Iterable<List<I>> middleParts;

        FirstPhaseIterator(Iterable<Word<I>> prefixes, Iterable<List<I>> middleParts, Iterator<Word<I>> suffixes) {
            super(suffixes);

            this.prefixes = prefixes;
            this.middleParts = middleParts;
        }

        @Override
        protected Iterator<List<I>> l2Iterator(Word<I> suffix) {
            return middleParts.iterator();
        }

        @Override
        protected Iterator<Word<I>> l3Iterator(Word<I> suffix, List<I> middle) {
            return prefixes.iterator();
        }

        @Override
        protected Word<I> combine(Word<I> suffix, List<I> middle, Word<I> prefix) {
            final WordBuilder<I> wb = new WordBuilder<>(prefix.size() + middle.size() + suffix.size());
            return wb.append(prefix).append(middle).append(suffix).toWord();
        }
    }

    private static class SecondPhaseIterator<S, I>
            extends AbstractThreeLevelIterator<Word<I>, List<I>, Word<I>, Word<I>> {

        private final UniversalDeterministicAutomaton<S, I, ?, ?, ?> automaton;
        private final Collection<? extends I> inputs;

        private final MutableMapping<S, @Nullable List<Word<I>>> localSuffixSets;
        private final Iterable<List<I>> middleParts;

        SecondPhaseIterator(UniversalDeterministicAutomaton<S, I, ?, ?, ?> automaton,
                            Collection<? extends I> inputs,
                            Iterable<Word<I>> prefixes,
                            Iterable<List<I>> middleParts) {
            super(prefixes.iterator());

            this.automaton = automaton;
            this.inputs = inputs;
            this.localSuffixSets = automaton.createStaticStateMapping();
            this.middleParts = middleParts;
        }

        @Override
        protected Iterator<List<I>> l2Iterator(Word<I> prefix) {
            return middleParts.iterator();
        }

        @Override
        protected Iterator<Word<I>> l3Iterator(Word<I> prefix, List<I> middle) {

            @SuppressWarnings("nullness") // input sequences have been computed on defined transitions
            final @NonNull S tmp = automaton.getState(prefix);
            @SuppressWarnings("nullness") // input sequences have been computed on defined transitions
            final @NonNull S state = automaton.getSuccessor(tmp, middle);

            @Nullable List<Word<I>> localSuffixes = localSuffixSets.get(state);

            if (localSuffixes == null) {
                localSuffixes = Automata.stateCharacterizingSet(automaton, inputs, state);
                if (localSuffixes.isEmpty()) {
                    localSuffixes = Collections.singletonList(Word.epsilon());
                }
                localSuffixSets.put(state, localSuffixes);
            }

            return localSuffixes.iterator();
        }

        @Override
        protected Word<I> combine(Word<I> prefix, List<I> middle, Word<I> suffix) {
            final WordBuilder<I> wb = new WordBuilder<>(prefix.size() + middle.size() + suffix.size());
            return wb.append(prefix).append(middle).append(suffix).toWord();
        }
    }
}
