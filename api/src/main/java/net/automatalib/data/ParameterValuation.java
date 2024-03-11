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
package net.automatalib.data;

import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * A valuation of parameters.
 *
 * @author falk
 */
public class ParameterValuation extends Valuation<Parameter<?>, DataValue<?>> {

    public ParameterValuation() {

    }

    public ParameterValuation(SymbolInstance<?> psi) {
        ParameterGenerator pgen = new ParameterGenerator();
        for (DataValue<?> dv : psi.getParameterValues()) {
            this.put(pgen.next(dv.getDataType()), dv);
        }
    }

    public ParameterValuation(Word<? extends SymbolInstance<?>> dw) {
        ParameterGenerator pgen = new ParameterGenerator();
        for (SymbolInstance<?> psi : dw) {
            for (DataValue<?> dv : psi.getParameterValues()) {
                put(pgen.next(dv.getDataType()), dv);
            }
        }
    }
}
