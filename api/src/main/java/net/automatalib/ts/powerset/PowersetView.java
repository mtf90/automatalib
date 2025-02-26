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
package net.automatalib.ts.powerset;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.automatalib.common.util.HashUtil;
import net.automatalib.ts.PowersetViewTS;
import net.automatalib.ts.TransitionSystem;

/**
 * A (default) {@link PowersetViewTS} implementation that represents states and transitions of the original transition
 * system via {@link Set}s and {@link Collection}s.
 *
 * @param <S>
 *         (original) state type
 * @param <I>
 *         input symbol type
 * @param <T>
 *         (original) transition type
 */
public class PowersetView<S, I, T> implements PowersetViewTS<Set<S>, I, Collection<T>, S, T> {

    private final TransitionSystem<S, I, T> ts;

    public PowersetView(TransitionSystem<S, I, T> ts) {
        this.ts = ts;
    }

    @Override
    public Set<S> getInitialState() {
        return ts.getInitialStates();
    }

    @Override
    public Set<S> getSuccessor(Collection<T> transition) {
        Set<S> result = new HashSet<>(HashUtil.capacity(transition.size()));
        for (T trans : transition) {
            result.add(ts.getSuccessor(trans));
        }
        return result;
    }

    @Override
    public Collection<T> getTransition(Set<S> state, I input) {
        final List<T> result = new LinkedList<>();
        for (S s : state) {
            for (T t : ts.getTransitions(s, input)) {
                // LinkedList's #add is faster than #addAll
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public Collection<S> getOriginalStates(Set<S> state) {
        return state;
    }

    @Override
    public Collection<T> getOriginalTransitions(Collection<T> transition) {
        return transition;
    }
}
