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
package net.automatalib.incremental.dfa;

import net.automatalib.alphabet.SupportsGrowingAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.graph.Graph;
import net.automatalib.incremental.ConflictException;
import net.automatalib.incremental.IncrementalConstruction;
import net.automatalib.ts.UniversalDTS;
import net.automatalib.word.Word;

/**
 * General interface for incremental DFA builders.
 *
 * @param <I>
 *         input symbol type
 */
public interface IncrementalDFABuilder<I> extends IncrementalConstruction<DFA<?, I>, I>, SupportsGrowingAlphabet<I> {

    /**
     * Looks up the tri-state acceptance value for a given word.
     *
     * @param inputWord
     *         the word
     *
     * @return the tri-state acceptance value for this word.
     */
    Acceptance lookup(Word<? extends I> inputWord);

    /**
     * Inserts a new word into the automaton, with a given acceptance value.
     *
     * @param word
     *         the word to insert
     * @param accepting
     *         whether this word should be marked as accepting
     *
     * @throws ConflictException
     *         if the newly provided information conflicts with existing information
     */
    void insert(Word<? extends I> word, boolean accepting);

    /**
     * Inserts a new word into the automaton. This is a convenience method equivalent to invoking {@code insert(word,
     * true)}.
     *
     * @param word
     *         the word to insert
     *
     * @throws ConflictException
     *         if the newly provided information conflicts with existing information
     * @see #insert(Word, boolean)
     */
    default void insert(Word<? extends I> word) {
        insert(word, true);
    }

    @Override
    default boolean hasDefinitiveInformation(Word<? extends I> word) {
        return lookup(word) != Acceptance.DONT_KNOW;
    }

    @Override
    Graph<?, ?> asGraph();

    @Override
    UniversalDTS<?, I, ?, Acceptance, Void> asTransitionSystem();

}
