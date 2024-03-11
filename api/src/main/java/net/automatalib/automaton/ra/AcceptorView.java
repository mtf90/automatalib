package net.automatalib.automaton.ra;

import java.util.Collection;

import net.automatalib.data.Constants;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AcceptorView<L, A extends ParameterizedSymbol, T extends GuardedTransition>
        implements DeterministicAcceptorTS<State<L>, SymbolInstance<A>> {

    private final RegisterAutomaton<L, A, T> ra;

    public AcceptorView(RegisterAutomaton<L, A, T> ra) {
        this.ra = ra;
    }

    @Override
    public @Nullable State<L> getTransition(State<L> state, SymbolInstance<A> input) {
        L location = state.getLocation();
        RegisterValuation regs = state.getValuation();
        ParameterValuation pars = new ParameterValuation(input);
        Constants consts = ra.getConstants();

        Collection<T> candidates = ra.getTransitions(location, input.getBaseSymbol());

        for (T t : candidates) {
            if (t.isEnabled(regs, pars, consts)) {
                regs = t.execute(regs, pars, consts);
                L succ = ra.getSuccessor(t);
                return new State<>(succ, regs);
            }
        }

        return null;
    }

    @Override
    public boolean isAccepting(State<L> state) {
        return ra.getStateProperty(state.getLocation());
    }

    @Override
    public @Nullable State<L> getInitialState() {
        final L initialState = ra.getInitialState();

        if (initialState == null) {
            return null;
        }

        return new State<>(initialState, ra.getInitialRegisters());
    }
}