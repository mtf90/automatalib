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

import java.util.HashSet;
import java.util.Set;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.MutableAutomaton;
import net.automatalib.automaton.concept.StateLocalInput;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.fsa.impl.CompactNFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastNFA;
import net.automatalib.automaton.transducer.impl.CompactMealy;
import net.automatalib.automaton.transducer.impl.CompactMoore;
import net.automatalib.automaton.transducer.impl.CompactSST;
import net.automatalib.automaton.transducer.impl.FastMealy;
import net.automatalib.automaton.transducer.impl.FastMoore;
import net.automatalib.automaton.transducer.probabilistic.impl.FastProbMealy;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StateLocalInputTest {

    private static final Alphabet<Integer> ALPHABET = Alphabets.integers(1, 4);

    @Test
    public void testCompactDFA() {
        this.testAutomaton(new CompactDFA<>(ALPHABET), false);
    }

    @Test
    public void testCompactNFA() {
        this.testAutomaton(new CompactNFA<>(ALPHABET), false);
    }

    @Test
    public void testFastDFA() {
        this.testAutomaton(new FastDFA<>(ALPHABET), false);
    }

    @Test
    public void testFastNFA() {
        this.testAutomaton(new FastNFA<>(ALPHABET), false);
    }

    @Test
    public void testCompactMealy() {
        this.testAutomaton(new CompactMealy<>(ALPHABET), null);
    }

    @Test
    public void testFastMealy() {
        this.testAutomaton(new FastMealy<>(ALPHABET), null);
    }

    @Test
    public void testFastProbMealy() {
        this.testAutomaton(new FastProbMealy<>(ALPHABET), null);
    }

    @Test
    public void testCompactMoore() {
        this.testAutomaton(new CompactMoore<>(ALPHABET), null);
    }

    @Test
    public void testFastMoore() {
        this.testAutomaton(new FastMoore<>(ALPHABET), null);
    }

    @Test
    public void testCompactSST() {
        this.testAutomaton(new CompactSST<>(ALPHABET), Word.epsilon());
    }

    @Test
    public void testCompactSimpleAutomaton() {
        this.testAutomaton(new CompactSimpleAutomaton<>(ALPHABET), null);
    }

    private <M extends MutableAutomaton<S, Integer, ?, SP, TP> & StateLocalInput<S, Integer>, S, SP, @Nullable TP> void testAutomaton(
            M automaton,
            SP property) {

        // construct cyclic automaton: symbols increase clock-wise and decrease counter-clock-wise
        final S s1 = automaton.addInitialState(property);
        final S s2 = automaton.addState(property);
        final S s3 = automaton.addState(property);
        final S s4 = automaton.addState(property);

        automaton.addTransition(s1, 1, s2, null);
        automaton.addTransition(s2, 2, s3, null);
        automaton.addTransition(s3, 3, s4, null);
        automaton.addTransition(s4, 4, s1, null);

        automaton.addTransition(s1, 4, s4, null);
        automaton.addTransition(s4, 3, s3, null);
        automaton.addTransition(s3, 2, s2, null);
        automaton.addTransition(s2, 1, s1, null);

        // check defined inputs
        Assert.assertEquals(new HashSet<>(automaton.getLocalInputs(s1)), Set.of(1, 4));
        Assert.assertEquals(new HashSet<>(automaton.getLocalInputs(s2)), Set.of(1, 2));
        Assert.assertEquals(new HashSet<>(automaton.getLocalInputs(s3)), Set.of(2, 3));
        Assert.assertEquals(new HashSet<>(automaton.getLocalInputs(s4)), Set.of(3, 4));

    }

}
