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

import java.util.Collection;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.FreshValueGenerator;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.FreshOutput;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import net.automatalib.data.VarMapping.GeneratorMapping;

/**
 * A parallel assignment for registers.
 *
 * @author falk
 */
public class OutputAssignment extends Assignment {

    private final VarMapping<Parameter<?>, ? extends SymbolicDataValue<?>> outputAssignment;
    private final VarMapping<Register<?>, ? extends SymbolicDataValue<?>> registerAssignment;

    public OutputAssignment() {
        this(new VarMapping<>());
    }

    public OutputAssignment(VarMapping<Register<?>, ? extends SymbolicDataValue<?>> registerAssignment) {
        this(registerAssignment, new VarMapping<>());
    }

    public OutputAssignment(VarMapping<Register<?>, ? extends SymbolicDataValue<?>> registerAssignment,
                            VarMapping<Parameter<?>, ? extends SymbolicDataValue<?>> outputAssignment) {
        this.outputAssignment = outputAssignment;
        this.registerAssignment = registerAssignment;

        assert registerAssignment.values()
                                 .stream()
                                 .filter(o -> o instanceof FreshOutput)
                                 .map(FreshOutput.class::cast)
                                 .collect(Collectors.toSet())
                                 .containsAll(outputAssignment.values()
                                                              .stream()
                                                              .filter(o -> o instanceof FreshOutput)
                                                              .map(FreshOutput.class::cast)
                                                              .collect(Collectors.toSet()));
    }

    public ParameterValuation computeOutputValuation(RegisterValuation registers,
                                                     ParameterValuation parameters,
                                                     Constants consts,
                                                     GeneratorMapping generators) {
        ParameterValuation val = new ParameterValuation();
        for (Entry<Parameter<?>, ? extends SymbolicDataValue<?>> e : outputAssignment) {
            SymbolicDataValue<?> valp = e.getValue();
            if (valp.is(Register.class)) {
                val.put(e.getKey(), registers.get((Register<?>) valp));
            } else if (valp.is(Parameter.class)) {
                val.put(e.getKey(), parameters.get((Parameter<?>) valp));
            }
            //TODO: check if we want to copy constant values into vars
            else if (valp.is(Constant.class)) {
                val.put(e.getKey(), consts.get((Constant<?>) valp));
            } else if (valp.is(FreshOutput.class)) {
                val.put(e.getKey(), getFreshOutput((FreshOutput<?>) valp, registers, generators));
            } else {
                throw new IllegalStateException("Illegal assignment: " + e.getKey() + " := " + valp);
            }
        }
        return val;
    }

    public RegisterValuation computeRegisterValuation(RegisterValuation registers,
                                                      ParameterValuation parameters,
                                                      Constants consts,
                                                      GeneratorMapping generators) {
        RegisterValuation val = new RegisterValuation(registers);
        for (Entry<Register<?>, ? extends SymbolicDataValue<?>> e : registerAssignment) {
            SymbolicDataValue<?> valp = e.getValue();
            if (valp.is(Register.class)) {
                val.put(e.getKey(), registers.get((Register<?>) valp));
            } else if (valp.is(Parameter.class)) {
                val.put(e.getKey(), parameters.get((Parameter<?>) valp));
            }
            //TODO: check if we want to copy constant values into vars
            else if (valp.is(Constant.class)) {
                val.put(e.getKey(), consts.get((Constant<?>) valp));
            } else if (valp.is(FreshOutput.class)) {
                val.put(e.getKey(), getFreshOutput((FreshOutput<?>) valp, registers, generators));
            } else {
                throw new IllegalStateException("Illegal assignment: " + e.getKey() + " := " + valp);
            }
        }
        return val;
    }

    private <T> DataValue<T> getFreshOutput(FreshOutput<T> output,
                                            RegisterValuation registers,
                                            GeneratorMapping generators) {
        DataType<T> dataType = output.getDataType();
        Collection<DataValue<T>> values = registers.values(dataType);
        FreshValueGenerator<T> generator = generators.get(dataType);

        return generator.getFreshValue(values);
    }

    @Override
    public String toString() {
        return outputAssignment.toString(":=");
    }

    public VarMapping<Parameter<?>, ? extends SymbolicDataValue<?>> getOutputAssignment() {
        return outputAssignment;
    }

    public VarMapping<Register<?>, ? extends SymbolicDataValue<?>> getAssignment() {
        return registerAssignment;
    }
}
