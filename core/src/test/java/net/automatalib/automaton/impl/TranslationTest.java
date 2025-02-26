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
package net.automatalib.automaton.impl;

import java.util.Arrays;
import java.util.Collections;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.fsa.impl.CompactNFA;
import net.automatalib.automaton.util.TestUtil;
import net.automatalib.word.Word;
import org.testng.annotations.Test;

public class TranslationTest {

    @Test
    public void testTranslateDFA() {
        final Alphabet<Character> sigma = Alphabets.characters('a', 'c');
        final Alphabet<Integer> sigma2 = Alphabets.integers(1, 3);
        final CompactDFA<Character> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(false);
        final int q2 = dfa.addIntState(false);

        dfa.setTransition(q0, sigma.getSymbolIndex('a'), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex('b'), q2);
        dfa.setTransition(q2, sigma.getSymbolIndex('c'), q0);

        final CompactDFA<Integer> translated = dfa.translate(sigma2);

        TestUtil.checkOutput(translated, Word.fromSymbols(1, 2, 3, 1, 2, 3), true);
        TestUtil.checkOutput(translated, Word.fromSymbols(1, 1, 2, 2), false);
        TestUtil.checkOutput(translated, Word.fromSymbols(2, 1, 2, 1), false);

    }

    @Test
    public void testTranslateNFA() {
        final Alphabet<Character> sigma = Alphabets.characters('a', 'c');
        final Alphabet<Integer> sigma2 = Alphabets.integers(1, 3);
        final CompactNFA<Character> nfa = new CompactNFA<>(sigma);

        final int q0 = nfa.addInitialState(true);
        final int q1 = nfa.addState(false);
        final int q2 = nfa.addState(false);

        nfa.setTransitions(q0, (Character) 'a', Arrays.asList(q0, q1));
        nfa.setTransitions(q1, (Character) 'b', Arrays.asList(q0, q2));
        nfa.setTransitions(q2, (Character) 'c', Collections.singleton(q0));

        final CompactNFA<Integer> translated = nfa.translate(sigma2);

        TestUtil.checkOutput(translated, Word.fromSymbols(1, 2, 3, 1, 2, 3), true);
        TestUtil.checkOutput(translated, Word.fromSymbols(1, 2, 1, 2, 1, 2), true);
        TestUtil.checkOutput(translated, Word.fromSymbols(1, 1, 1), true);
        TestUtil.checkOutput(translated, Word.fromSymbols(1, 1, 2, 2), false);
        TestUtil.checkOutput(translated, Word.fromSymbols(2, 1, 2, 1), false);
    }
}
