package net.automatalib.util.automaton.equivalence.jse;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.common.util.Pair;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class RAEquivalenceJSE<LM, LH, I extends ParameterizedSymbol, TM extends GuardedTransition, TH extends GuardedTransition>
        extends RegisterEquivalence<LM, LH, I, TM, TH, Boolean, Void> {

    private RAEquivalenceJSE(RegisterAutomaton<LM, I, TM> model,
                             RegisterAutomaton<LH, I, TH> hyp,
                             Collection<? extends I> inputs,
                             ConstraintSolver solver) {
        super(model, hyp, inputs, solver);
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedTransition, T2 extends GuardedTransition> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterAutomaton<L1, I, T1> hyp,
            RegisterAutomaton<L2, I, T2> model,
            Collection<? extends I> inputs,
            ConstraintSolver solver) {
        return new RAEquivalenceJSE<>(hyp, model, inputs, solver).findCounterExample();
    }

    @Override
    protected void itrans(I ps,
                          LM ml,
                          LH hl,
                          TM mt,
                          TH ht,
                          Map<Parameter<?>, Variable<?>> pmap,
                          List<TransitionTuple> ret) {

        Expression<Boolean> guard = buildInputTransitionGuard(ml, hl, mt, ht, pmap);

        if (solver.isSatisfiable(guard) == Result.SAT) {
            Map<Variable, Expression<Boolean>> effects = new HashMap<>();
            computeEffects(mt, ht, effects, pmap);

            LM mSucc = model.getSuccessor(mt);
            LH hSucc = hyp.getSuccessor(ht);

            boolean eq = Objects.equals(model.getStateProperty(mSucc), hyp.getStateProperty(hSucc));

            gov.nasa.jstateexplorer.transitionSystem.Transition t =
                    new gov.nasa.jstateexplorer.transitionSystem.Transition(guard, effects, getId(eq), true, !eq);

            ret.add(new TransitionTuple(ps, t, "itrans: " + mt + " : " + ht));
            Pair<LM, LH> p = Pair.of(mSucc, hSucc);
            if (eq && visited.add(p)) {
                queue.add(p);
            }
        }
    }

    @Override
    protected <L, T extends GuardedTransition> boolean isPartialAutomatonAnError(RegisterStructure<L, ?, T, Boolean, Void> ra,
                                                                                 Collection<T> trans) {
        for (T t : trans) {
            if (ra.getStateProperty(ra.getSuccessor(t))) {
                return true;
            }
        }
        return false;
    }

}
