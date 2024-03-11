package net.automatalib.util.automaton.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.automaton.ra.GuardedOutputTransition;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class RMMEquivalence<LM, LH, I extends ParameterizedSymbol, TM extends GuardedOutputTransition, TH extends GuardedOutputTransition, O extends ParameterizedSymbol>
        extends AbstractRAEquivalence<LM, LH, I, TM, TH, Void, O> {

    RMMEquivalence(RegisterMealyMachine<LH, I, TH, O> hyp,
                   RegisterMealyMachine<LM, I, TM, O> mod,
                   Collection<? extends I> inputs,
                   ConstraintSolver solver) {
        super(hyp, mod, inputs, solver);
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedOutputTransition, T2 extends GuardedOutputTransition, O extends ParameterizedSymbol> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterMealyMachine<L1, I, T1, O> hyp,
            RegisterMealyMachine<L2, I, T2, O> model,
            Collection<? extends I> inputs) {
        return findSeparatingWord(hyp, model, inputs, ConstraintSolverFactory.createSolver("z3"));
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedOutputTransition, T2 extends GuardedOutputTransition, O extends ParameterizedSymbol> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterMealyMachine<L1, I, T1, O> hyp,
            RegisterMealyMachine<L2, I, T2, O> model,
            Collection<? extends I> inputs,
            ConstraintSolver solver) {
        return new RMMEquivalence<>(hyp, model, inputs, solver).findSeparatingWord();
    }

    @Override
    protected Word<SymbolInstance<I>> handleState(ExploredState<LH, LM, I> state,
                                                  I i,
                                                  Queue<? super ExploredState<LH, LM, I>> queue) {
        final LH hLoc = state.hypLoc;
        final LM mLoc = state.modLoc;

        final Collection<TH> hTrans = hLoc == null ? Collections.emptyList() : hyp.getTransitions(hLoc, i);
        final Collection<TM> mTrans = mLoc == null ? Collections.emptyList() : mod.getTransitions(mLoc, i);

        if (hTrans.isEmpty() ^ mTrans.isEmpty()) {

            final boolean hyp = mTrans.isEmpty();
            final Collection<GuardedOutputTransition> trans = Collections.unmodifiableCollection(hyp ? hTrans : mTrans);

            for (GuardedOutputTransition t : trans) {
                final Expression<Boolean> transitionTest = buildTransitionExpression(state, t, hyp);
                final Valuation valuation = new Valuation();
                final Result result = this.solver.solve(transitionTest, valuation);

                if (result == Result.SAT) {
                    return extractSeparatingWord(valuation, new ExploredState<>(null, null, transitionTest, state, i));
                }
            }
        } else {
            for (TH hypTrans : hTrans) {
                for (TM modTrans : mTrans) {

                    final Expression<Boolean> transitionTest = buildTransitionExpression(state, hypTrans, modTrans);
                    final Valuation valuation = new Valuation();
                    final Result result = this.solver.solve(transitionTest, valuation);

                    if (result == Result.SAT) {

                        final LH hSucc = hyp.getSuccessor(hypTrans);
                        final LM mSucc = mod.getSuccessor(modTrans);

                        final O hOut = hyp.getTransitionProperty(hypTrans);
                        final O mOut = mod.getTransitionProperty(modTrans);

                        if (Objects.equals(hOut, mOut)) {

                            if (hOut.getArity() > 0) {
                                final Expression<Boolean> outputExpression =
                                        buildOutputExpression(state, hypTrans, modTrans, hOut);
                                final Expression<Boolean> outputTest =
                                        ExpressionUtil.and(transitionTest, outputExpression);

                                final Valuation outputValuation = new Valuation();
                                final Result outputResult = this.solver.solve(outputTest, outputValuation);

                                if (outputResult == Result.SAT) {
                                    return extractSeparatingWord(outputValuation,
                                                                 new ExploredState<>(hSucc,
                                                                                     mSucc,
                                                                                     outputTest,
                                                                                     state,
                                                                                     i));
                                }
                            }
                        } else {
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

    private Expression<Boolean> buildOutputExpression(ExploredState<LH, LM, I> state,
                                                      TH hypTrans,
                                                      TM modTrans,
                                                      O output) {
        final int depth = state.depth;
        final VarMapping<Parameter<?>, ? extends SymbolicDataValue<?>> hypAss =
                hypTrans.getAssignment().getOutputAssignment();
        final VarMapping<Parameter<?>, ? extends SymbolicDataValue<?>> modAss =
                modTrans.getAssignment().getOutputAssignment();
        final List<Expression<Boolean>> constraints = new ArrayList<>(hypAss.size() + modAss.size());

        for (Entry<Parameter<?>, ? extends SymbolicDataValue<?>> e : hypAss) {
            Parameter<?> par = e.getKey();
            constraints.add(new NumericBooleanExpression(new Variable<>(par.getType(), "h_out_" + par.getId()),
                                                         NumericComparator.EQ,
                                                         replaceVariable(e.getValue(), true, depth)));
        }
        for (Entry<Parameter<?>, ? extends SymbolicDataValue<?>> e : modAss) {
            Parameter<?> par = e.getKey();
            constraints.add(new NumericBooleanExpression(new Variable<>(par.getType(), "m_out_" + par.getId()),
                                                         NumericComparator.EQ,
                                                         replaceVariable(e.getValue(), false, depth)));
        }

        final List<Expression<Boolean>> conflicts = new ArrayList<>(output.getArity());

        for (int i = 0; i < output.getArity(); i++) {
            DataType<?> par = output.getPtype(i);
            conflicts.add(new NumericBooleanExpression(new Variable<>(par.getType(), "m_out_" + (i + 1)),
                                                       NumericComparator.NE,
                                                       new Variable<>(par.getType(), "h_out_" + (i + 1))));

        }

        return ExpressionUtil.and(ExpressionUtil.and(constraints), ExpressionUtil.or(conflicts));
    }

}
