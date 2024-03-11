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

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.automaton.concept.MutableRegisterStructure;
import net.automatalib.symbol.data.ParameterizedSymbol;

/**
 * Mutable Register Automaton.
 *
 * @author falk
 */
public interface MutableRegisterAutomaton<L, A extends ParameterizedSymbol, T extends GuardedTransition>
        extends RegisterAutomaton<L, A, T>, MutableRegisterStructure<L, A, T, Boolean, Void> {

    T createTransition(L successor, Expression<Boolean> guard, Assignment assignment);

    default T createTransition(L successor) {
        return createTransition(successor, null);
    }

    default T createTransition(L successor, Void properties) {
        return createTransition(successor, ExpressionUtil.TRUE, new Assignment());
    }

}
