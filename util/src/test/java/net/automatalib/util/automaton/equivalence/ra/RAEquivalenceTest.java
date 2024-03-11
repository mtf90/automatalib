package net.automatalib.util.automaton.equivalence.ra;

import java.util.Collection;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.util.automaton.equivalence.RAEquivalence;
import net.automatalib.word.Word;

public class RAEquivalenceTest extends AbstractRAEquivalenceTest {

    @Override
    protected EquivalenceChecker<InputSymbol> getChecker() {
        return new RAChecker2<>();
    }

    private static class RAChecker2<I extends ParameterizedSymbol> implements EquivalenceChecker<I> {

        @Override
        public Word<SymbolInstance<I>> findSeparatingWord(RegisterAutomaton<?, I, ?> hyp,
                                                          RegisterAutomaton<?, I, ?> model,
                                                          Collection<? extends I> inputs,
                                                          ConstraintSolver solver) {
            return RAEquivalence.findSeparatingWord(hyp, model, inputs, solver);
        }

        @Override
        public String toString() {
            return "new checker";
        }
    }
}
