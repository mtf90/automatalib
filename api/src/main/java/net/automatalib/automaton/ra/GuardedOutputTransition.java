package net.automatalib.automaton.ra;

import net.automatalib.data.Constants;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.VarMapping.GeneratorMapping;

public interface GuardedOutputTransition extends GuardedTransition {

    OutputAssignment getAssignment();

    default RegisterValuation execute(RegisterValuation registers, ParameterValuation parameters, Constants consts) {
        return execute(registers, parameters, consts, new GeneratorMapping());
    }

    default RegisterValuation execute(RegisterValuation registers, ParameterValuation parameters, Constants consts, GeneratorMapping generators) {
        return getAssignment().computeRegisterValuation(registers, parameters, consts, generators);
    }

    default ParameterValuation output(RegisterValuation registers, ParameterValuation parameters, Constants consts, GeneratorMapping generators) {
        return getAssignment().computeOutputValuation(registers, parameters, consts, generators);
    }

}
