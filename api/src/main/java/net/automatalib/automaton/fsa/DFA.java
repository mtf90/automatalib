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
package net.automatalib.automaton.fsa;

import java.util.Collection;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.concept.DetSuffixOutputAutomaton;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

/**
 * Deterministic finite state acceptor.
 */
public interface DFA<S, I> extends UniversalDeterministicAutomaton<S, I, S, Boolean, Void>,
                                   DeterministicAcceptorTS<S, I>,
                                   DetSuffixOutputAutomaton<S, I, S, Boolean>,
                                   NFA<S, I> {

    @Override
    default Boolean computeSuffixOutput(Iterable<? extends I> prefix, Iterable<? extends I> suffix) {
        S tgt = getState(prefix);
        return tgt != null && computeStateOutput(tgt, suffix);
    }

    @Override
    default Boolean computeStateOutput(S state, Iterable<? extends I> input) {
        S tgt = getSuccessor(state, input);
        return tgt != null && isAccepting(tgt);
    }

    @Override
    default Boolean computeOutput(Iterable<? extends I> input) {
        return accepts(input);
    }

    @Override
    default boolean isAccepting(Collection<? extends S> states) {
        return DeterministicAcceptorTS.super.isAccepting(states);
    }
}
