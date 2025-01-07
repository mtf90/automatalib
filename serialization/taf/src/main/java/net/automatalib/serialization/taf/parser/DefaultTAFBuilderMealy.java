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
package net.automatalib.serialization.taf.parser;

import java.util.Collection;
import java.util.Set;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.AutomatonCreator;
import net.automatalib.automaton.transducer.MutableMealyMachine;

final class DefaultTAFBuilderMealy<A extends MutableMealyMachine<S, String, T, String>, S, T>
        extends AbstractTAFBuilder<S, T, Void, String, A> implements TAFBuilderMealy {

    private final AutomatonCreator<A, String> creator;

    DefaultTAFBuilderMealy(InternalTAFParser parser, AutomatonCreator<A, String> creator) {
        super(parser);
        this.creator = creator;
    }

    @Override
    public void addTransitions(String source, Collection<String> symbols, String output, String targetId) {
        doAddTransitions(source, symbols, targetId, output);
    }

    @Override
    public void addWildcardTransitions(String source, String output, String targetId) {
        doAddWildcardTransitions(source, targetId, output);
    }

    @Override
    protected A createAutomaton(Alphabet<String> stringAlphabet) {
        return creator.createAutomaton(stringAlphabet);
    }

    @Override
    protected Void getStateProperty(Set<String> options) {
        return null;
    }

}
