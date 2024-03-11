package net.automatalib.util.automaton.equivalence.ra;

import java.util.Collection;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.util.automaton.equivalence.RMMEquivalence;
import net.automatalib.word.Word;

public class RMMEquivalenceTest extends AbstractRMMEquivalenceTest {

    @Override
    protected EquivalenceChecker<InputSymbol, OutputSymbol> getChecker() {
        return new RMMChecker2<>();
    }

    private static class RMMChecker2<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
            implements EquivalenceChecker<I, O> {

        @Override
        public Word<SymbolInstance<I>> findSeparatingWord(RegisterMealyMachine<?, I, ?, O> hyp,
                                                          RegisterMealyMachine<?, I, ?, O> model,
                                                          Collection<? extends I> inputs,
                                                          ConstraintSolver solver) {
            return RMMEquivalence.findSeparatingWord(hyp, model, inputs, solver);
        }

        @Override
        public String toString() {
            return "new checker";
        }
    }
}
