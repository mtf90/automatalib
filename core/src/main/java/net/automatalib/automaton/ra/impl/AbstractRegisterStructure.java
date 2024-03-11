package net.automatalib.automaton.ra.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.base.AbstractCompact;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.concept.MutableRegisterStructure;
import net.automatalib.common.util.array.ArrayStorage;
import net.automatalib.data.Constants;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.symbol.data.ParameterizedSymbol;

public abstract class AbstractRegisterStructure<I extends ParameterizedSymbol, T extends GuardedTransition, SP, TP>
        extends AbstractCompact<I, T, SP, TP> implements MutableRegisterStructure<Integer, I, T, SP, TP> {

    private final Alphabet<I> alphabet;
    private final Constants constants;
    private final RegisterValuation initialRegisters;

    private int initial;
    private final ArrayStorage<List<Register<?>>> registers;
    private final ArrayStorage<List<T>> transitions;

    public AbstractRegisterStructure(Alphabet<I> alphabet,
                                     RegisterValuation initialRegisters,
                                     Constants constants,
                                     int stateCapacity,
                                     float resizeFactor) {
        super(alphabet, stateCapacity, resizeFactor);
        this.alphabet = alphabet;
        this.initialRegisters = initialRegisters;
        this.constants = constants;

        this.initial = AbstractCompact.INVALID_STATE;
        this.registers = new ArrayStorage<>(stateCapacity);
        this.transitions = new ArrayStorage<>(stateCapacity * alphabet.size());
    }

    @Override
    public void setInitial(Integer state, boolean initial) {
        if (initial) {
            this.initial = state;
        } else if (this.initial == state) {
            this.initial = AbstractCompact.INVALID_STATE;
        }
    }

    @Override
    public void setTransitions(Integer state, I input, Collection<? extends T> transitions) {
        final int idx = toMemoryIndex(state, alphabet.getSymbolIndex(input));
        List<T> t = this.transitions.get(idx);
        if (t == null) {
            t = new ArrayList<>();
        } else {
            t.clear();
        }

//        for (T trans : transitions) {
//            final Integer succ = getSuccessor(trans);
//            final Set<Register<?>> regs = new HashSet<>(getRegisters(succ));
//            if (!regs.equals(trans.getAssignment().getAssignment().keySet())) {
//                throw new IllegalArgumentException(
//                        "Transition '" + trans + "' does not assign all registers of target location '" + succ + "'");
//            }
//        }

        t.addAll(transitions);
        this.transitions.set(idx, t);
    }

    @Override
    public void removeAllTransitions(Integer state) {
        for (int i = 0; i < alphabet.size(); i++) {
            final int idx = toMemoryIndex(state, i);
            final List<T> t = this.transitions.get(idx);
            if (t != null) {
                t.clear();
            }
        }
    }

    @Override
    public Constants getConstants() {
        return constants;
    }

    @Override
    public Collection<Register<?>> getRegisters(Integer location) {
        final List<Register<?>> regs = registers.get(location);
        return regs == null ? Collections.emptyList() : regs;
    }

    @Override
    public RegisterValuation getInitialRegisters() {
        return initialRegisters;
    }

    @Override
    public Collection<T> getTransitions(Integer state, I input) {
        List<T> t = this.transitions.get(toMemoryIndex(state, alphabet.getSymbolIndex(input)));
        return t == null ? Collections.emptyList() : Collections.unmodifiableCollection(t);
    }

    @Override
    public Set<Integer> getInitialStates() {
        return Collections.singleton(this.initial);
    }

    @Override
    public void clear() {
        for (int i = 0; i < size(); i++) {
            removeAllTransitions(i);
        }
        this.initial = AbstractCompact.INVALID_STATE;
        super.clear();
    }

    @Override
    public Integer addInitialState(Register<?>... regs) {
        Integer id = super.addInitialState();
        return setRegister(id, regs);
    }

    @Override
    public Integer addInitialState(SP property, Register<?>... regs) {
        Integer id = super.addInitialState(property);
        return setRegister(id, regs);
    }

    @Override
    public Integer addState(Register<?>... regs) {
        Integer id = super.addState();
        return setRegister(id, regs);
    }

    @Override
    public Integer addState(SP property, Register<?>... regs) {
        Integer id = super.addState(property);
        return setRegister(id, regs);
    }

    private Integer setRegister(Integer id, Register<?>... regs) {
        this.registers.ensureCapacity(id + 1);
        this.registers.set(id, Arrays.asList(regs));
        return id;
    }
}
