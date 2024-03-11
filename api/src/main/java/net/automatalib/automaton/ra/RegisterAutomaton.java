/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
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
package net.automatalib.automaton.ra;

import java.util.Collection;

import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.graph.TransitionEdge.Property;
import net.automatalib.graph.UniversalGraph;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

/**
 * @author falk
 */
public interface RegisterAutomaton<L, A extends ParameterizedSymbol, T extends GuardedTransition>
        extends RegisterStructure<L, A, T, Boolean, Void> {

    default DeterministicAcceptorTS<State<L>, SymbolInstance<A>> asAcceptor() {
        return new AcceptorView<>(this);
    }

    @Override
    default UniversalGraph<L, TransitionEdge<A, T>, Boolean, Property<A, Void>> transitionGraphView(Collection<? extends A> inputs) {
        return new RegisterAutomatonGraphView<>(this, inputs);
    }

}

