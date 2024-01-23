/* Copyright (C) 2013-2024 TU Dortmund University
 * This file is part of AutomataLib, http://www.automatalib.net/.
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
package net.automatalib.modelchecking.impl.cache;

import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.MutableDFA;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.modelchecking.Lasso.DFALasso;
import net.automatalib.modelchecking.ModelCheckerLassoCache.DFAModelCheckerLassoCache;
import net.automatalib.modelchecking.impl.SizeDFAModelCheckerLassoCache;
import net.automatalib.modelchecking.impl.cache.ModelCheckerMock.DFAModelCheckerMock;
import org.mockito.Mockito;

public class SizeDFAModelCheckLassoCacheTest<I>
        extends AbstractSizeModelCheckLassoCacheTest<I, DFALasso<I>, MutableDFA<?, I>, DFAModelCheckerMock<I>, DFAModelCheckerLassoCache<I, Object>> {

    @Override
    protected DFAModelCheckerMock<I> getModelChecker(MutableDFA<?, I> automaton,
                                                     Object property,
                                                     DFALasso<I> counterexample) {
        return new DFAModelCheckerMock<>(automaton, property, counterexample);
    }

    @Override
    protected DFAModelCheckerLassoCache<I, Object> getCache(DFAModelCheckerMock<I> mockup) {
        return new SizeDFAModelCheckerLassoCache<>(mockup);
    }

    @Override
    protected MutableDFA<?, I> getAutomaton() {
        return new CompactDFA<>(Alphabets.fromArray());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DFALasso<I> getCounterexample() {
        return (DFALasso<I>) Mockito.mock(DFALasso.class);
    }
}
