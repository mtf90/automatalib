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
package net.automatalib.symbol.data;

import java.util.Arrays;
import java.util.Objects;

import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;

/**
 * A concrete data symbol.
 *
 * @author falk
 */
public class SymbolInstance<A extends ParameterizedSymbol> {

    /**
     * action
     */
    private final A baseSymbol;

    /**
     * concrete parameter values
     */
    private final DataValue<?>[] parameterValues;

    public SymbolInstance(A baseSymbol, Object... parameterValues) {

        if (baseSymbol.getArity() == parameterValues.length) {
            this.parameterValues = new DataValue[parameterValues.length];
            final DataType<?>[] types = baseSymbol.getPtypes();
            for (int i = 0; i < parameterValues.length; i++) {
                this.parameterValues[i] = cast(types[i], parameterValues[i]);
            }
        } else {
            throw new IllegalArgumentException("The numbers of parameters and values do not match");
        }

        this.baseSymbol = baseSymbol;
    }

    private static <T> DataValue<T> cast(DataType<T> type, Object obj) {
        Class<T> clazz = type.getType().getCanonicalClass();
//        if (!clazz.isInstance(obj)) {
//            throw new IllegalArgumentException("Type of parameter '" + obj + "' does not match expected type");
//        }
        return new DataValue<>(type, clazz.cast(obj));
    }

    public SymbolInstance(A baseSymbol, DataValue<?>... parameterValues) {

        if (baseSymbol.getArity() == parameterValues.length) {
            final DataType<?>[] types = baseSymbol.getPtypes();
            for (int i = 0; i < parameterValues.length; i++) {
                if (!types[i].equals(parameterValues[i].getDataType())) {
                    throw new IllegalArgumentException(
                            "Type of parameter '" + parameterValues[i] + "' does not match expected type");
                }
            }
        } else {
            throw new IllegalArgumentException("The numbers of parameters and values do not match");
        }

        this.baseSymbol = baseSymbol;
        this.parameterValues = parameterValues;
    }

    public A getBaseSymbol() {
        return baseSymbol;
    }

    public DataValue<?>[] getParameterValues() {
        return parameterValues;
    }

    @Override
    public String toString() {
        return this.baseSymbol.getName() + Arrays.toString(parameterValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SymbolInstance)) {
            return false;
        }
        final SymbolInstance<?> other = (SymbolInstance<?>) obj;

        return Objects.equals(this.baseSymbol, other.baseSymbol) &&
               Arrays.equals(this.parameterValues, other.parameterValues);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + Objects.hashCode(this.baseSymbol);
        hash = 11 * hash + Arrays.hashCode(this.parameterValues);
        return hash;
    }

}
