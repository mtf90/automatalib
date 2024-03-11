package net.automatalib.automaton.ra.impl;

import gov.nasa.jpf.constraints.api.Expression;
import java.util.BitSet;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.MutableRegisterAutomaton;
import net.automatalib.common.util.WrapperUtil;
import net.automatalib.data.Constants;
import net.automatalib.data.RegisterValuation;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class CompactRA<I extends ParameterizedSymbol>
        extends AbstractRegisterStructure<I, CompactRATransition, Boolean, Void>
        implements MutableRegisterAutomaton<Integer, I, CompactRATransition> {

    private final BitSet acceptance;

    public CompactRA(Alphabet<I> alphabet) {
        this(alphabet, new RegisterValuation());
    }

    public CompactRA(Alphabet<I> alphabet, RegisterValuation initialRegisters) {
        this(alphabet, initialRegisters, new Constants(), DEFAULT_INIT_CAPACITY, DEFAULT_RESIZE_FACTOR);
    }

    public CompactRA(Alphabet<I> alphabet, RegisterValuation initialRegisters, Constants constants) {
        this(alphabet, initialRegisters, constants, DEFAULT_INIT_CAPACITY, DEFAULT_RESIZE_FACTOR);
    }

    public CompactRA(Alphabet<I> alphabet,
                     RegisterValuation initialRegisters,
                     Constants constants,
                     int stateCapacity,
                     float resizeFactor) {
        super(alphabet, initialRegisters, constants, stateCapacity, resizeFactor);
        this.acceptance = new BitSet();
    }

    @Override
    public void setStateProperty(int state, Boolean property) {
        acceptance.set(state, WrapperUtil.booleanValue(property));
    }

    @Override
    public Boolean getStateProperty(int state) {
        return acceptance.get(state);
    }

    @Override
    public void setTransitionProperty(CompactRATransition transition, Void property) {}

    @Override
    public CompactRATransition createTransition(Integer successor, Expression<Boolean> guard, Assignment assignment) {
        return new CompactRATransition(guard, assignment, successor);
    }

    @Override
    public Void getTransitionProperty(CompactRATransition transition) {
        return null;
    }

    @Override
    public Integer getSuccessor(CompactRATransition transition) {
        return transition.getTarget();
    }

    @Override
    public void clear() {
        this.acceptance.clear();
        super.clear();
    }

}
