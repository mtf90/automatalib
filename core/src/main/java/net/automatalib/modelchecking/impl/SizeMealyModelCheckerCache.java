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

import net.automatalib.automaton.transducer.MealyMachine;
import net.automatalib.modelchecking.ModelChecker.MealyModelChecker;
import net.automatalib.modelchecking.ModelCheckerCache.MealyModelCheckerCache;
import net.automatalib.modelchecking.impl.InternalModelCheckerDelegator.MealyModelCheckerDelegator;

/**
 * Mealy version of {@link SizeDFAModelCheckerCache}.
 */
public class SizeMealyModelCheckerCache<I, O, P, R> extends SizeModelCheckerCache<I, MealyMachine<?, I, ?, O>, P, R>
        implements MealyModelCheckerCache<I, O, P, R>,
                   MealyModelCheckerDelegator<MealyModelChecker<I, O, P, R>, I, O, P, R> {

    private final MealyModelChecker<I, O, P, R> mealyModelChecker;

    public SizeMealyModelCheckerCache(MealyModelChecker<I, O, P, R> modelChecker) {
        super(modelChecker);
        this.mealyModelChecker = modelChecker;
    }

    @Override
    public MealyModelChecker<I, O, P, R> getModelChecker() {
        return mealyModelChecker;
    }
}
