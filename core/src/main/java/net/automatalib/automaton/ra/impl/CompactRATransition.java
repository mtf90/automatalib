package net.automatalib.automaton.ra.impl;

import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.GuardedTransition;

public class CompactRATransition implements GuardedTransition {

    private final Expression<Boolean> guard;
    private final Assignment assignment;
    private final int target;

    public CompactRATransition(Expression<Boolean> guard, Assignment assignment, int target) {
        this.guard = guard;
        this.assignment = assignment;
        this.target = target;
    }

    @Override
    public Assignment getAssignment() {
        return assignment;
    }

    @Override
    public Expression<Boolean> getGuard() {
        return guard;
    }

    public int getTarget() {
        return target;
    }
}
