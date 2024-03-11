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

import net.automatalib.automaton.MutableAutomaton;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.symbol.data.ParameterizedSymbol;

/**
 * Mutable Register Automaton.
 *
 * @author falk
 */
public interface MutableRegisterStructure<L, A extends ParameterizedSymbol, T extends GuardedTransition, SP, TP>
        extends RegisterStructure<L, A, T, SP, TP>, MutableAutomaton<L, A, T, SP, TP> {

    L addInitialState(Register<?>... regs);

    L addInitialState(SP property, Register<?>... regs);

    L addState(SP property, Register<?>... regs);

    L addState(Register<?>... regs);
}
