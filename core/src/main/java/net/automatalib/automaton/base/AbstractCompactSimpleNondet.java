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
package net.automatalib.automaton.base;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.NFA;
import net.automatalib.common.util.collection.PositiveIntSet;
import net.automatalib.ts.PowersetViewTS;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract super class that refines {@link AbstractCompact} for transition-property-less automata. As a result,
 * transitions may be represented as integers (where a transition object effectively <i>is</i> the successor).
 * <p>
 * <b>Implementation note:</b> This class uses {@link BitSet}s to store the successors of each state. This makes the
 * memory consumption of this class depend on the number of states rather than the density of its adjacency matrix
 * (since the maximum bit/index determines the size of each {@link BitSet}). For the majority of cases (tests showed if
 * the average number of outgoing transitions per state is more than 0.2% of the number of states) this still requires
 * less memory than using e.g. a {@link Set} of {@link Integer}s. However, for very large but very sparse {@link NFA}s
 * one may consider using the {@link AbstractFastMutableNondet} class instead.
 *
 * @param <I>
 *         input symbol type
 * @param <SP>
 *         state property type
 */
public abstract class AbstractCompactSimpleNondet<I, SP> extends AbstractCompact<I, Integer, SP, Void> {

    private final BitSet initial;
    private @Nullable BitSet[] transitions;

    public AbstractCompactSimpleNondet(Alphabet<I> alphabet, int stateCapacity, float resizeFactor) {
        super(alphabet, stateCapacity, resizeFactor);

        this.transitions = new BitSet[stateCapacity * numInputs()];
        this.initial = new BitSet();
    }

    protected AbstractCompactSimpleNondet(Alphabet<I> alphabet, AbstractCompactSimpleNondet<?, ?> other) {
        super(alphabet, other);
        this.transitions = other.transitions.clone();
        for (int i = 0; i < transitions.length; i++) {
            final BitSet tgts = transitions[i];
            if (tgts != null) {
                transitions[i] = (BitSet) tgts.clone();
            }
        }

        this.initial = (BitSet) other.initial.clone();
    }

    @Override
    protected void updateTransitionStorage(Payload payload) {
        this.transitions = updateTransitionStorage(this.transitions, BitSet[]::new, null, payload);
    }

    @Override
    public Void getTransitionProperty(Integer transition) {
        return null;
    }

    @Override
    public void setInitial(Integer state, boolean initial) {
        setInitial(state.intValue(), initial);
    }

    public void setInitial(int state, boolean initial) {
        if (initial) {
            this.initial.set(state);
        } else {
            this.initial.clear(state);
        }
    }

    @Override
    public void clear() {
        Arrays.fill(transitions, 0, size() * numInputs(), null);
        this.initial.clear();

        super.clear();
    }

    @Override
    public void setTransitionProperty(Integer transition, Void property) {}

    @Override
    public void removeTransition(Integer state, I input, Integer transition) {
        removeTransition(state.intValue(), input, transition.intValue());
    }

    public void removeTransition(int stateId, I input, int successorId) {
        removeTransition(stateId, getSymbolIndex(input), successorId);
    }

    public void removeTransition(int stateId, int inputIdx, int successorId) {
        final BitSet successors = transitions[toMemoryIndex(stateId, inputIdx)];
        if (successors != null) {
            successors.clear(successorId);
        }
    }

    @Override
    public void removeAllTransitions(Integer state, I input) {
        removeAllTransitions(state.intValue(), input);
    }

    public void removeAllTransitions(int stateId, I input) {
        removeAllTransitions(stateId, getSymbolIndex(input));
    }

    public void removeAllTransitions(int stateId, int inputIdx) {
        transitions[toMemoryIndex(stateId, inputIdx)] = null;
    }

    @Override
    public void removeAllTransitions(Integer state) {
        removeAllTransitions(state.intValue());
    }

    public void removeAllTransitions(int state) {
        final int lower = state * numInputs();
        final int upper = lower + numInputs();

        Arrays.fill(transitions, lower, upper, null);
    }

    @Override
    public void addTransition(Integer state, I input, Integer transition) {
        addTransition(state.intValue(), input, transition.intValue());
    }

    public void addTransition(int stateId, I input, int succId) {
        addTransition(stateId, getSymbolIndex(input), succId);
    }

    public void addTransition(int stateId, int inputIdx, int succId) {
        final int transIdx = toMemoryIndex(stateId, inputIdx);
        BitSet successors = transitions[transIdx];
        if (successors == null) {
            successors = new BitSet();
            transitions[transIdx] = successors;
        }
        successors.set(succId);
    }

    @Override
    public Integer copyTransition(Integer trans, Integer succ) {
        return succ;
    }

    @Override
    public Integer createTransition(Integer successor, Void properties) {
        return successor;
    }

    @Override
    public void setTransitions(Integer state, I input, Collection<? extends Integer> transitions) {
        setTransitions(state.intValue(), input, transitions);
    }

    public void setTransitions(int state, I input, Collection<? extends Integer> successors) {
        setTransitions(state, getSymbolIndex(input), successors);
    }

    public void setTransitions(int state, int inputIdx, Collection<? extends Integer> successors) {
        final int transIdx = toMemoryIndex(state, inputIdx);
        BitSet succs = transitions[transIdx];
        if (succs == null) {
            succs = new BitSet();
            transitions[transIdx] = succs;
        } else {
            succs.clear();
        }
        successors.forEach(succs::set);
    }

    @Override
    public Integer getSuccessor(Integer transition) {
        return transition;
    }

    @Override
    public Collection<Integer> getTransitions(Integer state, I input) {
        return getTransitions(state.intValue(), input);
    }

    public Set<Integer> getTransitions(int state, I input) {
        return getTransitions(state, getSymbolIndex(input));
    }

    public Set<Integer> getTransitions(int state, int inputIdx) {
        final BitSet transition = transitions[toMemoryIndex(state, inputIdx)];
        return transition == null ? Collections.emptySet() : new PositiveIntSet(transition);
    }

    @Override
    public Set<Integer> getInitialStates() {
        return new PositiveIntSet(initial);
    }

    @Override
    public PowersetViewTS<BitSet, I, BitSet, Integer, Integer> powersetView() {
        return new CompactPowersetDTS();
    }

    protected class CompactPowersetDTS implements PowersetViewTS<BitSet, I, BitSet, Integer, Integer> {

        @Override
        public @Nullable BitSet getTransition(BitSet state, I input) {
            final BitSet result = new BitSet(AbstractCompactSimpleNondet.this.size());
            final int inputIdx = getSymbolIndex(input);

            for (int i = state.nextSetBit(0); i >= 0; i = state.nextSetBit(i+1)) {
                final BitSet transitions = AbstractCompactSimpleNondet.this.transitions[toMemoryIndex(i, inputIdx)];
                if (transitions != null) {
                    result.or(transitions);
                }
            }

            return result;
        }

        @Override
        public BitSet getSuccessor(BitSet transition) {
            return transition;
        }

        @Override
        public BitSet getInitialState() {
            return initial;
        }

        @Override
        public Collection<Integer> getOriginalStates(BitSet state) {
            return new PositiveIntSet(state);
        }

        @Override
        public Collection<Integer> getOriginalTransitions(BitSet transition) {
            return new PositiveIntSet(transition);
        }
    }
}
