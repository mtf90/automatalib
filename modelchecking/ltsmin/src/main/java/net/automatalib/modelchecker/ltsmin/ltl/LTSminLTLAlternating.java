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
package net.automatalib.modelchecker.ltsmin.ltl;

import java.util.Collection;
import java.util.function.Function;

import de.learnlib.tooling.annotation.builder.GenerateBuilder;
import net.automatalib.exception.FormatException;
import net.automatalib.modelchecker.ltsmin.LTSminAlternating;
import net.automatalib.modelchecker.ltsmin.LTSminLTLParser;
import net.automatalib.modelchecking.Lasso.MealyLasso;

/**
 * An LTL model checker using LTSmin for Mealy machines using alternating edge semantics.
 *
 * @param <I>
 *         the input type
 * @param <O>
 *         the output type
 */
public class LTSminLTLAlternating<I, O> extends AbstractLTSminLTLMealy<I, O>
        implements LTSminAlternating<I, O, MealyLasso<I, O>> {

    @GenerateBuilder(defaults = BuilderDefaults.class)
    public LTSminLTLAlternating(boolean keepFiles,
                                Function<String, I> string2Input,
                                Function<String, O> string2Output,
                                int minimumUnfolds,
                                double multiplier,
                                Collection<? super O> skipOutputs) {
        super(keepFiles, string2Input, string2Output, minimumUnfolds, multiplier, skipOutputs);
    }

    @Override
    public boolean requiresOriginalAutomaton() {
        return false;
    }

    @Override
    protected void verifyFormula(String formula) throws FormatException {
        LTSminLTLParser.requireValidLetterFormula(formula);
    }
}
