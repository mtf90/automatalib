package net.automatalib.automaton.ra.impl;

import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.automaton.ra.GuardedOutputTransition;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class CompactRMMTransition<O extends ParameterizedSymbol> implements GuardedOutputTransition {

    private final Expression<Boolean> guard;
    private final OutputAssignment outputAssignment;
    private final int target;

    private O output;

    public CompactRMMTransition(Expression<Boolean> guard, OutputAssignment outputAssignment, O output, int target) {
        this.outputAssignment = outputAssignment;
        this.output = output;
        this.guard = guard;
        this.target = target;
    }

    @Override
    public OutputAssignment getAssignment() {
        return outputAssignment;
    }

    @Override
    public Expression<Boolean> getGuard() {
        return guard;
    }

    public int getTarget() {
        return target;
    }

    void setOutput(O output) {
        this.output = output;
    }

    public O getOutput() {
        return output;
    }
}
