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
package net.automatalib.modelchecking.impl;

import net.automatalib.automaton.fsa.DFA;
import net.automatalib.modelchecking.Lasso.DFALasso;
import net.automatalib.modelchecking.ModelCheckerLasso.DFAModelCheckerLasso;
import net.automatalib.modelchecking.ModelCheckerLassoCache.DFAModelCheckerLassoCache;
import net.automatalib.modelchecking.impl.InternalModelCheckerDelegator.ModelCheckerLassoDelegator;

/**
 * An extension of {@link SizeDFAModelCheckerCache} that additionally implements {@link DFAModelCheckerLassoCache}.
 */
public class SizeDFAModelCheckerLassoCache<I, P> extends SizeModelCheckerCache<I, DFA<?, I>, P, DFALasso<I>> implements
                                                                                                             DFAModelCheckerLassoCache<I, P>,
                                                                                                             ModelCheckerLassoDelegator<DFAModelCheckerLasso<I, P>, I, DFA<?, I>, P, DFALasso<I>> {

    private final DFAModelCheckerLasso<I, P> modelChecker;

    public SizeDFAModelCheckerLassoCache(DFAModelCheckerLasso<I, P> modelChecker) {
        super(modelChecker);
        this.modelChecker = modelChecker;
    }

    @Override
    public DFAModelCheckerLasso<I, P> getModelChecker() {
        return modelChecker;
    }
}
