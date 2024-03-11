package net.automatalib.automaton.ra;

import java.util.Collection;

import net.automatalib.common.util.Triple;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.VarMapping.GeneratorMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.output.MealyTransitionSystem;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TransducerView<L, A extends ParameterizedSymbol, T extends GuardedOutputTransition, O extends ParameterizedSymbol>
        implements MealyTransitionSystem<State<L>, SymbolInstance<A>, Triple<State<L>, SymbolInstance<A>, T>, SymbolInstance<O>> {

    private final RegisterMealyMachine<L, A, T, O> rmm;
    private final GeneratorMapping generators;

    public TransducerView(RegisterMealyMachine<L, A, T, O> rmm, GeneratorMapping generators) {
        this.rmm = rmm;
        this.generators = generators;
    }

    @Override
    public SymbolInstance<O> getTransitionOutput(Triple<State<L>, SymbolInstance<A>, T> triple) {
        State<L> state = triple.getFirst();
        SymbolInstance<A> input = triple.getSecond();
        RegisterValuation regs = state.getValuation();
        ParameterValuation pars = new ParameterValuation(input);
        Constants consts = rmm.getConstants();

        ParameterValuation outputMapping = triple.getThird().output(regs, pars, consts, generators);
        O output = this.rmm.getTransitionProperty(triple.getThird());

        ParameterGenerator pgen = new ParameterGenerator();
        DataValue<?>[] values = new DataValue[output.getArity()];

        for (int i = 0; i < values.length; i++) {
            values[i] = outputMapping.get(pgen.next(output.getPtypes()[i]));
        }

        return new SymbolInstance<>(output, values);
    }

    @Override
    public @Nullable Triple<State<L>, SymbolInstance<A>, T> getTransition(State<L> state, SymbolInstance<A> input) {
        L location = state.getLocation();
        RegisterValuation regs = state.getValuation();
        ParameterValuation pars = new ParameterValuation(input);
        Constants consts = rmm.getConstants();

        Collection<T> candidates = rmm.getTransitions(location, input.getBaseSymbol());

        for (T t : candidates) {
            if (t.isEnabled(regs, pars, consts)) {
                return Triple.of(state, input, t);
            }
        }

        return null;
    }

    @Override
    public State<L> getSuccessor(Triple<State<L>, SymbolInstance<A>, T> triple) {
        RegisterValuation regs = triple.getFirst().getValuation();
        ParameterValuation pars = new ParameterValuation(triple.getSecond());
        Constants consts = rmm.getConstants();

        regs = triple.getThird().execute(regs, pars, consts, generators);
        L succ = rmm.getSuccessor(triple.getThird());
        return new State<>(succ, regs);
    }

    @Override
    public @Nullable State<L> getInitialState() {
        final L initialState = rmm.getInitialState();

        if (initialState == null) {
            return null;
        }

        return new State<>(initialState, rmm.getInitialRegisters());
    }
}