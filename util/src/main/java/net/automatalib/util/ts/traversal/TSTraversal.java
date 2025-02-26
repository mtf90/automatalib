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
package net.automatalib.util.ts.traversal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import net.automatalib.common.util.Holder;
import net.automatalib.ts.TransitionSystem;
import net.automatalib.util.traversal.TraversalOrder;
import net.automatalib.util.ts.traversal.DFRecord.LastTransition;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class TSTraversal {

    public static final int NO_LIMIT = -1;

    private TSTraversal() {
        // prevent instantiation
    }

    /**
     * Traverses the given transition system in a breadth-first fashion. The traversal is steered by the specified
     * visitor.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     */
    public static <S, I, T, D> void breadthFirst(TransitionSystem<S, ? super I, T> ts,
                                                 Collection<? extends I> inputs,
                                                 TSTraversalVisitor<S, I, T, D> visitor) {
        breadthFirst(ts, NO_LIMIT, inputs, visitor);
    }

    /**
     * Traverses the given transition system in a breadth-first fashion. The traversal is steered by the specified
     * visitor.
     *
     * @param ts
     *         the transition system
     * @param limit
     *         the upper bound on the number of states to be visited
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     *
     * @return {@code false} if the number of explored states reached {@code limit}, {@code true} otherwise
     */
    public static <S, I, T, D> boolean breadthFirst(TransitionSystem<S, ? super I, T> ts,
                                                    int limit,
                                                    Collection<? extends I> inputs,
                                                    TSTraversalVisitor<S, I, T, D> visitor) {
        Deque<BFSRecord<S, D>> bfsQueue = new ArrayDeque<>();

        // setting the following to false means that the traversal had to be aborted due to reaching the limit
        boolean complete = true;
        int stateCount = 0;

        Holder<D> dataHolder = new Holder<>();

        for (S initS : ts.getInitialStates()) {
            dataHolder.value = null;
            TSTraversalAction act = visitor.processInitial(initS, dataHolder);
            switch (act) {
                case ABORT_INPUT:
                case ABORT_STATE:
                case IGNORE:
                    continue;
                case ABORT_TRAVERSAL:
                    return complete;
                case EXPLORE:
                    if (stateCount == limit) {
                        complete = false;
                    } else {
                        bfsQueue.offer(new BFSRecord<>(initS, dataHolder.value));
                        stateCount++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown action " + act);
            }
        }

        while (!bfsQueue.isEmpty()) {
            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
            @NonNull BFSRecord<S, D> current = bfsQueue.poll();

            S state = current.state;
            D data = current.data;

            if (!visitor.startExploration(state, data)) {
                continue;
            }

            inputs_loop:
            for (I input : inputs) {
                Collection<T> transitions = ts.getTransitions(state, input);

                for (T trans : transitions) {
                    S succ = ts.getSuccessor(trans);

                    dataHolder.value = null;
                    TSTraversalAction act = visitor.processTransition(state, data, input, trans, succ, dataHolder);

                    switch (act) {
                        case IGNORE:
                            continue;
                        case ABORT_INPUT:
                            continue inputs_loop;
                        case ABORT_STATE:
                            break inputs_loop;
                        case ABORT_TRAVERSAL:
                            return complete;
                        case EXPLORE:
                            if (stateCount == limit) {
                                complete = false;
                            } else {
                                bfsQueue.offer(new BFSRecord<>(succ, dataHolder.value));
                                stateCount++;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unknown action " + act);
                    }
                }
            }

            visitor.finishExploration(state, data);
        }

        return complete;
    }

    /**
     * Returns an {@link Iterable} for the (reachable) states of the given transition system in breadth-first order.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the inputs which should be considered for the traversal
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     *
     * @return an {@link Iterable} for the (reachable) states of the given transition system in breadth-first order
     */
    public static <S, I> Iterable<S> breadthFirstOrder(TransitionSystem<S, I, ?> ts, Collection<? extends I> inputs) {
        return () -> breadthFirstIterator(ts, inputs);
    }

    /**
     * Returns an {@link Iterator} for the (reachable) states of the given transition system in breadth-first order.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the inputs which should be considered for the traversal
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     *
     * @return an {@link Iterator} for the (reachable) states of the given transition system in breadth-first order
     */
    public static <S, I> Iterator<S> breadthFirstIterator(TransitionSystem<S, I, ?> ts, Collection<? extends I> inputs) {
        return new BreadthFirstIterator<>(ts, inputs);
    }

    /**
     * Traverses the given transition system in a depth-first fashion. The traversal is steered by the specified
     * visitor.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     */
    public static <S, I, T, D> void depthFirst(TransitionSystem<S, I, T> ts,
                                               Collection<? extends I> inputs,
                                               TSTraversalVisitor<S, I, T, D> visitor) {
        depthFirst(ts, NO_LIMIT, inputs, visitor);
    }

    /**
     * Traverses the given transition system in a depth-first fashion. The traversal is steered by the specified
     * visitor.
     *
     * @param ts
     *         the transition system
     * @param limit
     *         the upper bound on the number of states to be visited
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     *
     * @return {@code false} if the number of explored states reached {@code limit}, {@code true} otherwise
     */
    public static <S, I, T, D> boolean depthFirst(TransitionSystem<S, ? super I, T> ts,
                                                  int limit,
                                                  Collection<? extends I> inputs,
                                                  TSTraversalVisitor<S, I, T, D> visitor) {
        Deque<DFRecord<S, I, T, D>> dfsStack = new ArrayDeque<>();
        Holder<D> dataHolder = new Holder<>();

        // setting the following to false means that the traversal had to be aborted due to reaching the limit
        boolean complete = true;
        int stateCount = 0;

        for (S initS : ts.getInitialStates()) {
            dataHolder.value = null;
            TSTraversalAction act = visitor.processInitial(initS, dataHolder);

            switch (act) {
                case ABORT_INPUT:
                case ABORT_STATE:
                case IGNORE:
                    continue;
                case ABORT_TRAVERSAL:
                    return complete;
                case EXPLORE:
                    if (stateCount == limit) {
                        complete = false;
                    } else {
                        dfsStack.push(new DFRecord<>(initS, inputs, dataHolder.value));
                        stateCount++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown action " + act);
            }
        }

        while (!dfsStack.isEmpty()) {
            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
            @NonNull DFRecord<S, I, T, D> current = dfsStack.peek();

            S currState = current.state;
            D currData = current.data;

            if (current.start(ts) && !visitor.startExploration(currState, currData)) {
                dfsStack.pop();
                continue;
            }

            LastTransition<S, I, T, D> lastTransition = current.getLastTransition();
            if (lastTransition != null) {
                visitor.backtrackTransition(currState,
                                        currData,
                                        lastTransition.input,
                                        lastTransition.transition,
                                        lastTransition.state,
                                        lastTransition.data);
            }

            if (!current.hasNextTransition(ts)) {
                dfsStack.pop();
                visitor.finishExploration(currState, currData);
                continue;
            }

            I input = current.input();
            T trans = current.transition();

            S succ = ts.getSuccessor(trans);
            dataHolder.value = null;
            TSTraversalAction act = visitor.processTransition(currState, currData, input, trans, succ, dataHolder);

            switch (act) {
                case ABORT_INPUT:
                    current.advanceInput(ts);
                    break;
                case ABORT_STATE:
                    dfsStack.pop();
                    break;
                case ABORT_TRAVERSAL:
                    return complete;
                case IGNORE:
                    current.advance(ts);
                    break;
                case EXPLORE:
                    if (stateCount == limit) {
                        complete = false;
                    } else {
                        D data = dataHolder.value;
                        current.setLastTransition(input, trans, succ, data);
                        dfsStack.push(new DFRecord<>(succ, inputs, data));
                        stateCount++;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unknown action " + act);
            }
        }

        return complete;
    }

    /**
     * Returns an {@link Iterable} for the (reachable) states of the given transition system in depth-first order.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the inputs which should be considered for the traversal
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     *
     * @return an {@link Iterable} for the (reachable) states of the given transition system in depth-first order
     */
    public static <S, I> Iterable<S> depthFirstOrder(TransitionSystem<S, I, ?> ts, Collection<? extends I> inputs) {
        return () -> depthFirstIterator(ts, inputs);
    }

    /**
     * Returns an {@link Iterator} for the (reachable) states of the given transition system in depth-first order.
     *
     * @param ts
     *         the transition system
     * @param inputs
     *         the inputs which should be considered for the traversal
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     *
     * @return an {@link Iterator} for the (reachable) states of the given transition system in depth-first order
     */
    public static <S, I> Iterator<S> depthFirstIterator(TransitionSystem<S, I, ?> ts, Collection<? extends I> inputs) {
        return new DepthFirstIterator<>(ts, inputs);
    }

    /**
     * Traverses the given transition system in a given order. The traversal is steered by the specified visitor.
     *
     * @param order
     *         the order in which the states should be traversed
     * @param ts
     *         the transition system
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     */
    public static <S, I, T, D> void traverse(TraversalOrder order,
                                             TransitionSystem<S, ? super I, T> ts,
                                             Collection<? extends I> inputs,
                                             TSTraversalVisitor<S, I, T, D> visitor) {
        traverse(order, ts, NO_LIMIT, inputs, visitor);
    }

    /**
     * Traverses the given transition system in a given order. The traversal is steered by the specified visitor.
     *
     * @param order
     *         the order in which the states should be traversed
     * @param ts
     *         the transition system
     * @param limit
     *         the upper bound on the number of states to be visited
     * @param inputs
     *         the input alphabet
     * @param visitor
     *         the visitor
     * @param <S>
     *         state type
     * @param <I>
     *         input symbol type
     * @param <T>
     *         transition type
     * @param <D>
     *         (user) data type
     *
     * @return {@code false} if the number of explored states reached {@code limit}, {@code true} otherwise
     */
    public static <S, I, T, D> boolean traverse(TraversalOrder order,
                                                TransitionSystem<S, ? super I, T> ts,
                                                int limit,
                                                Collection<? extends I> inputs,
                                                TSTraversalVisitor<S, I, T, D> visitor) {
        switch (order) {
            case BREADTH_FIRST:
                return breadthFirst(ts, limit, inputs, visitor);
            case DEPTH_FIRST:
                return depthFirst(ts, limit, inputs, visitor);
            default:
                throw new IllegalArgumentException("Unknown traversal order: " + order);
        }
    }

}
