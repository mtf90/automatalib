package net.automatalib.automaton.ra;

import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.data.Constants;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;

public interface GuardedTransition {

    Assignment getAssignment();

    Expression<Boolean> getGuard();

    default boolean isEnabled(RegisterValuation registers, ParameterValuation parameters, Constants consts) {
        return getGuard().evaluateSMT(Util.compose(registers, parameters, consts));
    };

    default RegisterValuation execute(RegisterValuation registers, ParameterValuation parameters, Constants consts) {
        return getAssignment().compute(registers, parameters, consts);
    }
}
