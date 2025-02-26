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
package net.automatalib.ts.acceptor;

import java.util.Collection;
import java.util.Iterator;

import net.automatalib.automaton.concept.SuffixOutput;
import net.automatalib.common.util.collection.IterableUtil;
import net.automatalib.ts.AcceptorPowersetViewTS;
import net.automatalib.ts.DeterministicTransitionSystem;
import net.automatalib.ts.UniversalDTS;
import net.automatalib.ts.powerset.DeterministicAcceptorPowersetView;

/**
 * A deterministic acceptor transition system.
 *
 * @see AcceptorTS
 * @see DeterministicTransitionSystem
 */
public interface DeterministicAcceptorTS<S, I>
        extends AcceptorTS<S, I>, UniversalDTS<S, I, S, Boolean, Void>, SuffixOutput<I, Boolean> {

    @Override
    default Boolean computeOutput(Iterable<? extends I> input) {
        return accepts(input);
    }

    @Override
    default Boolean computeSuffixOutput(Iterable<? extends I> prefix, Iterable<? extends I> suffix) {
        return computeOutput(IterableUtil.concat(prefix, suffix));
    }

    @Override
    default boolean accepts(Iterable<? extends I> input) {
        S state = getState(input);
        return state != null && isAccepting(state);
    }

    @Override
    default boolean isAccepting(Collection<? extends S> states) {
        if (states.isEmpty()) {
            return false;
        }
        Iterator<? extends S> stateIt = states.iterator();
        assert stateIt.hasNext();

        S firstState = stateIt.next();
        if (stateIt.hasNext()) {
            throw new IllegalArgumentException("Acceptance of state sets is undefined for DFAs");
        }
        return isAccepting(firstState);
    }

    @Override
    default AcceptorPowersetViewTS<?, I, S> powersetView() {
        return new DeterministicAcceptorPowersetView<>(this);
    }
}
