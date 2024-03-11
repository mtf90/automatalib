package net.automatalib.automaton.ra.impl;

import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.MutableRegisterMealyMachine;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.data.Constants;
import net.automatalib.data.RegisterValuation;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class CompactRMM<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
        extends AbstractRegisterStructure<I, CompactRMMTransition<O>, Void, O>
        implements MutableRegisterMealyMachine<Integer, I, CompactRMMTransition<O>, O> {

    public CompactRMM(Alphabet<I> alphabet) {
        this(alphabet, new RegisterValuation());
    }

    public CompactRMM(Alphabet<I> alphabet, RegisterValuation initialRegisters) {
        this(alphabet, initialRegisters, new Constants(), DEFAULT_INIT_CAPACITY, DEFAULT_RESIZE_FACTOR);
    }

    public CompactRMM(Alphabet<I> alphabet, RegisterValuation initialRegisters, Constants constants) {
        this(alphabet, initialRegisters, constants, DEFAULT_INIT_CAPACITY, DEFAULT_RESIZE_FACTOR);
    }

    public CompactRMM(Alphabet<I> alphabet,
                      RegisterValuation initialRegisters,
                      Constants constants,
                      int stateCapacity,
                      float resizeFactor) {
        super(alphabet, initialRegisters, constants, stateCapacity, resizeFactor);
    }

    @Override
    public void setTransitionProperty(CompactRMMTransition<O> transition, O property) {
        transition.setOutput(property);
    }

    @Override
    public Integer getSuccessor(CompactRMMTransition<O> transition) {
        return transition.getTarget();
    }

    @Override
    public void setStateProperty(int state, Void property) {}

    @Override
    public Void getStateProperty(int state) {
        return null;
    }

    @Override
    public CompactRMMTransition<O> createTransition(Integer successor,
                                                    Expression<Boolean> guard,
                                                    OutputAssignment outputAssignment,
                                                    O output) {
        return new CompactRMMTransition<>(guard, outputAssignment, output, successor);
    }

    @Override
    public O getTransitionProperty(CompactRMMTransition<O> transition) {
        return transition.getOutput();
    }
}
