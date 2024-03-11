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

import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A user-defined type of data values.
 *
 * @author falk
 */
// todo: make a record?
public final class DataType<T> implements TypedValue {

    /**
     * name of type (defining member)
     */
    private final String name;

    private final Type<T> type;

    public DataType(String name, Type<T> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataType<?> dataType = (DataType<?>) o;
        return Objects.equals(name, dataType.name); // TODO: && Objects.equals(type, dataType.type);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.name);
        // TODO include type as well?
        return hash;
    }

    public String getName() {
        return name;
    }

    public Type<T> getType() {
        return type;
    }

    public static Type<?> valueOf(Class<?> cls) {
        if (Integer.class.isAssignableFrom(cls) || int.class.isAssignableFrom(cls)) {
            return BuiltinTypes.SINT32;
        } else if (Double.class.isAssignableFrom(cls) || double.class.isAssignableFrom(cls)) {
            return BuiltinTypes.DOUBLE;
        } else if (Long.class.isAssignableFrom(cls) || long.class.isAssignableFrom(cls)) {
            return BuiltinTypes.SINT64;
        } else if (BigDecimal.class.isAssignableFrom(cls)) {
            return BuiltinTypes.DECIMAL;
        }
        throw new RuntimeException("Cannot deserialize values of the class " + cls);
    }

    @Override
    public DataType<?> getDataType() {
        return this;
    }
}
