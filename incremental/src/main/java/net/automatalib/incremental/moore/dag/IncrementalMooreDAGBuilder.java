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
package net.automatalib.incremental.moore.dag;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.concept.InputAlphabetHolder;
import net.automatalib.automaton.concept.StateIDs;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.transducer.MooreMachine;
import net.automatalib.automaton.transducer.MooreMachine.MooreGraphView;
import net.automatalib.automaton.visualization.MooreVisualizationHelper;
import net.automatalib.common.util.IntDisjointSets;
import net.automatalib.common.util.UnionFind;
import net.automatalib.graph.Graph;
import net.automatalib.incremental.ConflictException;
import net.automatalib.incremental.moore.IncrementalMooreBuilder;
import net.automatalib.ts.output.MooreTransitionSystem;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Incrementally builds an (acyclic) Moore machine from a set of input and corresponding output words.
 *
 * @param <I>
 *         input symbol class
 * @param <O>
 *         output symbol class
 */
public class IncrementalMooreDAGBuilder<I, O> implements IncrementalMooreBuilder<I, O>, InputAlphabetHolder<I> {

    private final Map<@Nullable StateSignature<O>, State<O>> register;
    private final Alphabet<I> inputAlphabet;
    private int alphabetSize;
    private @MonotonicNonNull State<O> init;

    /**
     * Constructor.
     *
     * @param inputAlphabet
     *         the input alphabet to use
     */
    public IncrementalMooreDAGBuilder(Alphabet<I> inputAlphabet) {
        this.register = new LinkedHashMap<>();
        this.inputAlphabet = inputAlphabet;
        this.alphabetSize = inputAlphabet.size();
    }

    @Override
    public void addAlphabetSymbol(I symbol) {
        if (!this.inputAlphabet.containsSymbol(symbol)) {
            this.inputAlphabet.asGrowingAlphabetOrThrowException().addSymbol(symbol);
        }

        final int newAlphabetSize = this.inputAlphabet.size();
        // even if the symbol was already in the alphabet, we need to make sure to be able to store the new symbol
        if (alphabetSize < newAlphabetSize) {
            register.values().forEach(n -> n.ensureInputCapacity(newAlphabetSize));
            alphabetSize = newAlphabetSize;
        }
    }

    @Override
    public boolean hasDefinitiveInformation(Word<? extends I> word) {
        return getState(word) != null;
    }

    /**
     * Retrieves the (internal) state reached by the given input word, or {@code null} if no information about the input
     * word is present.
     *
     * @param word
     *         the input word
     *
     * @return the corresponding state
     */
    private @Nullable State<O> getState(Word<? extends I> word) {
        State<O> s = init;

        if (s == null) {
            return null;
        }

        for (I sym : word) {
            int idx = inputAlphabet.getSymbolIndex(sym);
            s = s.getSuccessor(idx);
            if (s == null) {
                break;
            }
        }
        return s;
    }

    @Override
    public boolean lookup(Word<? extends I> word, List<? super O> output) {
        State<O> curr = init;

        if (curr == null) {
            return false;
        }

        output.add(curr.getOutput());

        for (I sym : word) {
            int idx = inputAlphabet.getSymbolIndex(sym);
            State<O> succ = curr.getSuccessor(idx);
            if (succ == null) {
                return false;
            }
            output.add(succ.getOutput());
            curr = succ;
        }

        return true;
    }

    @Override
    public void insert(Word<? extends I> word, Word<? extends O> outputWord) {
        assert word.size() + 1 == outputWord.size();

        Iterator<? extends O> outWordIterator = outputWord.iterator();
        final O rootOut = outWordIterator.next();

        if (init == null) {
            StateSignature<O> initSig = new StateSignature<>(alphabetSize, rootOut);
            this.init = new State<>(initSig);
            register.put(null, init);
        }

        State<O> curr = init;
        State<O> conf = null;

        Deque<Transition<O>> path = new ArrayDeque<>();

        // Find the internal state in the automaton that can be reached by a
        // maximal prefix of the word (i.e., a path of secured information)
        for (I sym : word) {
            // During this, store the *first* confluence state (i.e., state with multiple incoming edges).
            if (conf == null && curr.isConfluence()) {
                conf = curr;
            }

            int idx = inputAlphabet.getSymbolIndex(sym);
            State<O> succ = curr.getSuccessor(idx);
            if (succ == null) {
                break;
            }

            // If a transition exists for the input symbol, it also has an output symbol.
            // Check if this matches the provided one, otherwise there is a conflict
            O outSym = outWordIterator.next();
            if (!Objects.equals(outSym, succ.getOutput())) {
                throw new ConflictException(
                        "Error inserting " + word.prefix(path.size() + 1) + " / " + outputWord.prefix(path.size() + 1) +
                        ": Incompatible output symbols: " + outSym + " vs " + curr.getOutput());
            }
            path.push(new Transition<>(curr, idx));
            curr = succ;
        }

        int len = word.length();
        int prefixLen = path.size();

        // The information was already present - we do not need to continue
        if (prefixLen == len) {
            return;
        }

        State<O> last = curr;

        if (conf != null) {
            if (conf == last) {
                conf = null;
            }
            last = hiddenClone(last);
            if (conf == null) {
                Transition<O> peek = path.peek();
                assert peek != null;
                State<O> prev = peek.state;
                if (prev == init) {
                    updateInitSignature(peek.transIdx, last);
                } else {
                    updateSignature(prev, peek.transIdx, last);
                }
            }
        } else if (last != init) {
            hide(last);
        }

        // We then create a suffix path, i.e., a linear sequence of states corresponding to
        // the suffix (more precisely: the suffix minus the first symbol, since this is the
        // transition which is used for gluing the suffix path to the existing automaton).
        Word<? extends I> suffix = word.subWord(prefixLen);
        Word<? extends O> suffixOut = outputWord.subWord(prefixLen);

        // Here we prepare the "gluing" transition
        I sym = suffix.firstSymbol();
        int suffTransIdx = inputAlphabet.getSymbolIndex(sym);
        O suffTransOut = suffixOut.firstSymbol();

        State<O> suffixState = createSuffix(suffix.subWord(1), suffixOut.subWord(1));

        if (last == init) {
            updateInitSignature(suffTransIdx, suffixState, suffTransOut);
        } else {
            last = unhide(last, suffTransIdx, suffixState);

            if (conf != null) {
                // in case of a cyclic structure, the suffix may make predecessors of 'conf' confluent due to un-hiding
                // update the reference with whatever confluent state comes first
                final Iterator<Transition<O>> iter = path.descendingIterator();
                while (iter.hasNext()) {
                    final State<O> s = iter.next().state;
                    if (s.isConfluence()) {
                        conf = s;
                        break;
                    }
                }
            }
        }

        if (path.isEmpty()) {
            return;
        }

        if (conf != null) {
            // If there was a confluence state, we have to clone all nodes on
            // the prefix path up to this state, in order to separate it from other
            // prefixes reaching the confluence state (we do not know anything about them plus the suffix).
            Transition<O> next;
            do {
                next = path.pop();
                State<O> state = next.state;
                int idx = next.transIdx;
                state = clone(state, idx, last);
                last = state;
            } while (next.state != conf);
        }

        // Finally, we have to refresh all the signatures, iterating backwards until the updating becomes stable.
        while (path.size() > 1) {
            Transition<O> next = path.pop();
            State<O> state = next.state;
            int idx = next.transIdx;

            // when extending the path we previously traversed (i.e. expanding the suffix), it may happen that we end up
            // adding a cyclic transition. If this is the case, simply clone the current state and update the parent in
            // the next iteration
            if (state == last) {
                last = clone(state, idx, last);
                continue;
            }

            State<O> updated = updateSignature(state, idx, last);
            if (state == updated) {
                return;
            }
            last = updated;
        }

        int finalIdx = path.pop().transIdx;

        updateInitSignature(finalIdx, last);
    }

    private State<O> hiddenClone(State<O> other) {
        StateSignature<O> sig = other.getSignature().duplicate();

        for (int i = 0; i < alphabetSize; i++) {
            State<O> succ = sig.successors.array[i];
            if (succ != null) {
                succ.increaseIncoming();
            }
        }
        return new State<>(sig);
    }

    /**
     * Update the signature of a state, changing only the successor state of a single transition index.
     *
     * @param state
     *         the state which's signature to update
     * @param idx
     *         the transition index to modify
     * @param succ
     *         the new successor state
     *
     * @return the resulting state, which can either be the same as the input state (if the new signature is unique), or
     * the result of merging with another state.
     */
    private State<O> updateSignature(State<O> state, int idx, State<O> succ) {
        StateSignature<O> sig = state.getSignature();
        if (sig.successors.array[idx] == succ) {
            return state;
        }

        register.remove(sig);
        if (sig.successors.array[idx] != null) {
            sig.successors.array[idx].decreaseIncoming();
        }
        sig.successors.array[idx] = succ;
        succ.increaseIncoming();
        sig.updateHashCode();
        return replaceOrRegister(state);
    }

    /**
     * Update the signature of the initial state. This requires special handling, as the initial state is not stored in
     * the register (since it can never legally act as a predecessor).
     *
     * @param idx
     *         the transition index being changed
     * @param succ
     *         the new successor state
     */
    private void updateInitSignature(int idx, State<O> succ) {
        assert init != null;
        StateSignature<O> sig = init.getSignature();
        State<O> oldSucc = sig.successors.array[idx];
        if (oldSucc == succ) {
            return;
        }
        if (oldSucc != null) {
            oldSucc.decreaseIncoming();
        }
        sig.successors.array[idx] = succ;
        succ.increaseIncoming();
    }

    /**
     * Updates the signature of the initial state, changing both the successor state and the output symbol.
     *
     * @param idx
     *         the transition index to change
     * @param succ
     *         the new successor state
     * @param out
     *         the output symbol
     */
    private void updateInitSignature(int idx, State<O> succ, O out) {
        assert init != null;
        StateSignature<O> sig = init.getSignature();
        State<O> oldSucc = sig.successors.array[idx];
        if (oldSucc == succ && Objects.equals(out, succ.getOutput())) {
            return;
        }
        if (oldSucc != null) {
            oldSucc.decreaseIncoming();
        }
        sig.successors.array[idx] = succ;
        succ.increaseIncoming();
    }

    private void hide(State<O> state) {
        assert state != init;
        StateSignature<O> sig = state.getSignature();

        register.remove(sig);
    }

    private State<O> createSuffix(Word<? extends I> suffix, Word<? extends O> suffixOut) {
        StateSignature<O> sig = new StateSignature<>(alphabetSize, suffixOut.lastSymbol());
        sig.updateHashCode();
        State<O> last = replaceOrRegister(sig);

        int len = suffix.length();
        for (int i = len - 1; i >= 0; i--) {
            sig = new StateSignature<>(alphabetSize, suffixOut.getSymbol(i));
            I sym = suffix.getSymbol(i);
            int idx = inputAlphabet.getSymbolIndex(sym);
            sig.successors.array[idx] = last;
            sig.updateHashCode();
            last = replaceOrRegister(sig);
        }

        return last;
    }

    private State<O> unhide(State<O> state, int idx, State<O> succ) {
        StateSignature<O> sig = state.getSignature();
        State<O> prevSucc = sig.successors.array[idx];
        if (prevSucc != null) {
            prevSucc.decreaseIncoming();
        }
        sig.successors.array[idx] = succ;
        if (succ != null) {
            succ.increaseIncoming();
        }
        sig.updateHashCode();
        return replaceOrRegister(state);
    }

    private State<O> clone(State<O> other, int idx, State<O> succ) {
        StateSignature<O> sig = other.getSignature();
        if (sig.successors.array[idx] == succ) {
            return other;
        }
        sig = sig.duplicate();
        sig.successors.array[idx] = succ;
        sig.updateHashCode();
        return replaceOrRegister(sig);
    }

    private State<O> replaceOrRegister(State<O> state) {
        StateSignature<O> sig = state.getSignature();
        State<O> other = register.get(sig);
        if (other != null) {
            if (state != other) {
                for (State<O> succ : sig.successors.array) {
                    if (succ != null) {
                        succ.decreaseIncoming();
                    }
                }
            }
            return other;
        }

        register.put(sig, state);
        return state;
    }

    private State<O> replaceOrRegister(StateSignature<O> sig) {
        State<O> state = register.get(sig);
        if (state != null) {
            return state;
        }

        state = new State<>(sig);
        register.put(sig, state);
        for (State<O> succ : sig.successors.array) {
            if (succ != null) {
                succ.increaseIncoming();
            }
        }
        return state;
    }

    @Override
    public @Nullable Word<I> findSeparatingWord(MooreMachine<?, I, ?, O> target,
                                                Collection<? extends I> inputs,
                                                boolean omitUndefined) {
        return doFindSeparatingWord(target, inputs, omitUndefined);
    }

    private <S, T> @Nullable Word<I> doFindSeparatingWord(MooreMachine<S, I, T, O> moore,
                                                          Collection<? extends I> inputs,
                                                          boolean omitUndefined) {
        State<O> init1 = init;
        S init2 = moore.getInitialState();

        if (init1 == null && init2 == null) {
            return null;
        } else if (init1 == null || init2 == null) {
            return omitUndefined ? null : Word.epsilon();
        }

        if (!Objects.equals(init1.getOutput(), moore.getStateOutput(init2))) {
            return Word.epsilon();
        }

        Map<State<O>, Integer> ids = new HashMap<>();
        StateIDs<S> mooreIds = moore.stateIDs();

        int thisStates = register.size();
        int id1 = getStateId(init1, ids), id2 = mooreIds.getStateId(init2) + thisStates;

        IntDisjointSets uf = new UnionFind(thisStates + moore.size());
        uf.link(id1, id2);

        Queue<Record<S, I, O>> queue = new ArrayDeque<>();

        queue.offer(new Record<>(init1, init2));

        I lastSym = null;

        Record<S, I, O> current;

        explore:
        while ((current = queue.poll()) != null) {
            State<O> state1 = current.state1;
            S state2 = current.state2;

            for (I sym : inputs) {
                int idx = inputAlphabet.getSymbolIndex(sym);
                State<O> succ1 = state1.getSuccessor(idx);
                if (succ1 == null) {
                    continue;
                }

                S succ2 = moore.getSuccessor(state2, sym);
                if (succ2 == null) {
                    if (omitUndefined) {
                        continue;
                    }
                    lastSym = sym;
                    break explore;
                }

                O out1 = succ1.getOutput();
                O out2 = moore.getStateOutput(succ2);
                if (!Objects.equals(out1, out2)) {
                    lastSym = sym;
                    break explore;
                }

                id1 = getStateId(succ1, ids);
                id2 = mooreIds.getStateId(succ2) + thisStates;

                int r1 = uf.find(id1), r2 = uf.find(id2);

                if (r1 == r2) {
                    continue;
                }

                uf.link(r1, r2);

                queue.offer(new Record<>(succ1, succ2, current, sym));
            }
        }

        if (current == null) {
            return null;
        }

        int ceLength = current.depth;
        if (lastSym != null) {
            ceLength++;
        }

        @SuppressWarnings("nullness") // we make sure to set each index to a value of type I
        WordBuilder<I> wb = new WordBuilder<>(null, ceLength);

        int index = ceLength;

        if (lastSym != null) {
            wb.setSymbol(--index, lastSym);
        }

        while (current.reachedFrom != null) {
            final I reachedVia = current.reachedVia;
            wb.setSymbol(--index, reachedVia);
            current = current.reachedFrom;
        }

        return wb.toWord();
    }

    private static <O> int getStateId(State<O> state, Map<State<O>, Integer> ids) {
        return ids.computeIfAbsent(state, k -> ids.size());
    }

    @Override
    public Alphabet<I> getInputAlphabet() {
        return inputAlphabet;
    }

    @Override
    public MooreTransitionSystem<?, I, ?, O> asTransitionSystem() {
        return new AutomatonView();
    }

    @Override
    public Graph<?, ?> asGraph() {
        return new MooreGraphView<State<O>, I, State<O>, O, AutomatonView>(new AutomatonView(), inputAlphabet) {

            @Override
            public VisualizationHelper<State<O>, TransitionEdge<I, State<O>>> getVisualizationHelper() {
                return new MooreVisualizationHelper<State<O>, I, State<O>, O>(automaton) {

                    @Override
                    public boolean getNodeProperties(State<O> node, Map<String, String> properties) {
                        super.getNodeProperties(node, properties);

                        properties.put(NodeAttrs.LABEL, String.valueOf(node.getOutput()));
                        if (node.isConfluence()) {
                            properties.put(NodeAttrs.SHAPE, NodeShapes.OCTAGON);
                        }

                        return true;
                    }
                };
            }
        };
    }

    // /////////////////////////////////////////////////////////////////////
    // Equivalence test //
    // /////////////////////////////////////////////////////////////////////

    private static final class Record<S, I, O> {

        private final State<O> state1;
        private final S state2;
        private final I reachedVia;
        private final @Nullable Record<S, I, O> reachedFrom;
        private final int depth;

        @SuppressWarnings("nullness") // we will only access reachedVia after checking reachedFrom for null
        Record(State<O> state1, S state2) {
            this.state1 = state1;
            this.state2 = state2;
            this.reachedFrom = null;
            this.reachedVia = null;
            this.depth = 0;
        }

        Record(State<O> state1, S state2, Record<S, I, O> reachedFrom, I reachedVia) {
            this.state1 = state1;
            this.state2 = state2;
            this.reachedFrom = reachedFrom;
            this.reachedVia = reachedVia;
            this.depth = reachedFrom.depth + 1;
        }
    }

    private final class AutomatonView implements MooreMachine<State<O>, I, State<O>, O> {

        @Override
        public O getStateOutput(State<O> state) {
            return state.getOutput();
        }

        @Override
        public @Nullable State<O> getTransition(State<O> state, I input) {
            return state.getSuccessor(inputAlphabet.getSymbolIndex(input));
        }

        @Override
        public State<O> getSuccessor(State<O> transition) {
            return transition;
        }

        @Override
        public @Nullable State<O> getInitialState() {
            return init;
        }

        @Override
        public Collection<State<O>> getStates() {
            return Collections.unmodifiableCollection(register.values());
        }
    }
}
