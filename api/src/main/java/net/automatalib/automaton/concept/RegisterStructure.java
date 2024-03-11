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
package net.automatalib.automaton.concept;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.automatalib.automaton.UniversalAutomaton;
import net.automatalib.automaton.graph.TransitionEdge;
import net.automatalib.automaton.graph.TransitionEdge.Property;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.RegisterAutomatonGraphView;
import net.automatalib.data.Constants;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.graph.UniversalGraph;
import net.automatalib.symbol.data.ParameterizedSymbol;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Mutable Register Automaton.
 *
 * @author falk
 */
public interface RegisterStructure<L, A extends ParameterizedSymbol, T extends GuardedTransition, SP, TP>
        extends UniversalAutomaton<L, A, T, SP, TP> {

    Constants getConstants();

    default Collection<Register<?>> getRegisters() {
        Set<Register<?>> registers = new LinkedHashSet<>();
        for (L s : getStates()) {
            registers.addAll(getRegisters(s));
        }

        return registers;
    }

    Collection<Register<?>> getRegisters(L location);

    RegisterValuation getInitialRegisters();

    default @Nullable L getInitialState() {
        final Set<L> initialStates = getInitialStates();
        if (initialStates.isEmpty()) {
            return null;
        } else if (initialStates.size() == 1) {
            return initialStates.iterator().next();
        } else {
            throw new IllegalStateException("Register automata must not have multiple initial states");
        }
    }

}
