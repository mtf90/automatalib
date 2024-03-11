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

import java.util.Map.Entry;
import java.util.Objects;

import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import net.automatalib.data.RegisterValuation;

/**
 * A parallel assignment for registers.
 *
 * @author falk
 */
public class Assignment {

    private final VarMapping<Register<?>, ? extends SymbolicDataValue<?>> assignment;

    public Assignment() {
        this(new VarMapping<>());
    }

    public Assignment(VarMapping<Register<?>, ? extends SymbolicDataValue<?>> assignment) {
        this.assignment = assignment;
    }

    public RegisterValuation compute(RegisterValuation registers, ParameterValuation parameters, Constants consts) {
        // TODO this is required by current RALib tests
        RegisterValuation val = new RegisterValuation(registers);
        assignment.keySet().forEach(val::remove);

        for (Entry<Register<?>, ? extends SymbolicDataValue<?>> e : assignment) {
            SymbolicDataValue<?> value = e.getValue();
            DataValue<?> fresh;
            if (value.is(Register.class)) {
                fresh = registers.get( (Register<?>) value);
            }
            else if (value.is(Parameter.class)) {
                fresh = parameters.get( (Parameter<?>) value);
            }
            //TODO: check if we want to copy constant values into vars
            else if (value.is(Constant.class)) {
                fresh = consts.get( (Constant<?>) value);
            }
            else {
                throw new IllegalStateException("Illegal assignment: " +
                        e.getKey() + " := " + value);
            }

            final DataValue<?> old = val.put(e.getKey(), fresh);

            if (Objects.equals(old, fresh)) {
                throw new IllegalArgumentException("Unique-Valuedness violated. You cannot assign the same data value to different registers");
            }
        }
        return val;
    }

    @Override
    public String toString() {
        return assignment.toString(":=");
    }

    public VarMapping<Register<?>, ? extends SymbolicDataValue<?>> getAssignment() {
        return assignment;
    }

}
