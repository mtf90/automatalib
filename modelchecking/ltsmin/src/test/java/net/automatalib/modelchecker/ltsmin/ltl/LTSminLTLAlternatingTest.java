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
package net.automatalib.modelchecker.ltsmin.ltl;

import net.automatalib.modelchecking.Lasso.MealyLasso;
import net.automatalib.modelchecking.MealyLassoImpl;

/**
 * Tests whether LTSminLTLAlternating actually uses alternating edge semantics.
 *
 * @see LTSminLTLIOTest
 */
public class LTSminLTLAlternatingTest extends AbstractLTSminLTLMealyTest {

    private LTSminLTLAlternating<String, String> modelChecker;

    @Override
    public LTSminLTLAlternating<String, String> getModelChecker() {
        return modelChecker;
    }

    @Override
    public String getSkipFormula() {
        return "!(letter == \"b\")";
    }

    @Override
    public void newModelChecker() {
        modelChecker = new LTSminLTLAlternatingBuilder<String, String>().withString2Input(s -> s).
                withString2Output(s -> s).create();
    }

    @Override
    protected MealyLasso<String, String> createCounterExample() {
        return new MealyLassoImpl<>(createAutomaton(), getAlphabet(), 4);
    }

    @Override
    protected String createFalseProperty() {
        return "X letter == \"a\"";
    }
}

