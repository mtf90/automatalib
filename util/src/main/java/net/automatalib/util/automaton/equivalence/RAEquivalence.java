package net.automatalib.util.automaton.equivalence;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Queue;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class RAEquivalence<LM, LH, I extends ParameterizedSymbol, TM extends GuardedTransition, TH extends GuardedTransition>
        extends AbstractRAEquivalence<LM, LH, I, TM, TH, Boolean, Void> {

    RAEquivalence(RegisterAutomaton<LH, I, TH> hyp,
                  RegisterAutomaton<LM, I, TM> mod,
                  Collection<? extends I> inputs,
                  ConstraintSolver solver) {
        super(hyp, mod, inputs, solver);
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedTransition, T2 extends GuardedTransition> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterAutomaton<L1, I, T1> hyp,
            RegisterAutomaton<L2, I, T2> model,
            Collection<? extends I> inputs) {
        return findSeparatingWord(hyp, model, inputs, ConstraintSolverFactory.createSolver("z3"));
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedTransition, T2 extends GuardedTransition> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterAutomaton<L1, I, T1> hyp,
            RegisterAutomaton<L2, I, T2> model,
            Collection<? extends I> inputs,
            ConstraintSolver solver) {
        return new RAEquivalence<>(hyp, model, inputs, solver).findSeparatingWord();
    }

    @Override
    protected Word<SymbolInstance<I>> handleState(ExploredState<LH, LM, I> state,
                                                  I i,
                                                  Queue<? super ExploredState<LH, LM, I>> queue) {

        final LH hLoc = state.hypLoc;
        final LM mLoc = state.modLoc;

        final Collection<TH> hTrans = hLoc == null ? Collections.emptyList() : hyp.getTransitions(hLoc, i);
        final Collection<TM> mTrans = mLoc == null ? Collections.emptyList() : mod.getTransitions(mLoc, i);

        if (hTrans.isEmpty() && !mTrans.isEmpty()) {

            for (TM modTrans : mTrans) {
                final Expression<Boolean> transitionTest = buildTransitionExpression(state, modTrans, false);
                final Valuation valuation = new Valuation();
                final Result result = this.solver.solve(transitionTest, valuation);

                if (result == Result.SAT) {

                    LM mSucc = mod.getSuccessor(modTrans);

                    if (mod.getStateProperty(mSucc)) {
                        return extractSeparatingWord(valuation,
                                                     new ExploredState<>(null, mSucc, transitionTest, state, i));
                    }

                    Expression<Boolean> expression = buildUpdateExpression(transitionTest,
                                                                           new Assignment(),
                                                                           modTrans.getAssignment(),
                                                                           state.depth);

                    queue.add(new ExploredState<>(null, mSucc, expression, state, i));
                }
            }
        } else if (mTrans.isEmpty() && !hTrans.isEmpty()) {

            for (TH hypTrans : hTrans) {

                final Expression<Boolean> transitionTest = buildTransitionExpression(state, hypTrans, true);
                final Valuation valuation = new Valuation();
                final Result result = this.solver.solve(transitionTest, valuation);

                if (result == Result.SAT) {

                    LH hSucc = hyp.getSuccessor(hypTrans);

                    if (hyp.getStateProperty(hSucc)) {
                        return extractSeparatingWord(valuation,
                                                     new ExploredState<>(hSucc, null, transitionTest, state, i));
                    }

                    Expression<Boolean> expression = buildUpdateExpression(transitionTest,
                                                                           hypTrans.getAssignment(),
                                                                           new Assignment(),
                                                                           state.depth);

                    queue.add(new ExploredState<>(hSucc, null, expression, state, i));
                }
            }
        } else {
            for (TH hypTrans : hTrans) {
                for (TM modTrans : mTrans) {

                    final Expression<Boolean> transitionTest = buildTransitionExpression(state, hypTrans, modTrans);
                    final Valuation valuation = new Valuation();
                    final Result result = this.solver.solve(transitionTest, valuation);

                    if (result == Result.SAT) {

                        LH hSucc = hyp.getSuccessor(hypTrans);
                        LM mSucc = mod.getSuccessor(modTrans);

                        if (!Objects.equals(hyp.getStateProperty(hSucc), mod.getStateProperty(mSucc))) {
                            return extractSeparatingWord(valuation,
                                                         new ExploredState<>(hSucc, mSucc, transitionTest, state, i));
                        }

                        Expression<Boolean> expression = buildUpdateExpression(transitionTest,
                                                                               hypTrans.getAssignment(),
                                                                               modTrans.getAssignment(),
                                                                               state.depth);

                        queue.add(new ExploredState<>(hSucc, mSucc, expression, state, i));
                    }
                }
            }
        }

        return null;
    }
}
