package net.automatalib.util.automaton.equivalence.jse;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.ra.GuardedOutputTransition;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.common.util.Pair;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class RMMEquivalenceJSE<LM, LH, I extends ParameterizedSymbol, TM extends GuardedOutputTransition, TH extends GuardedOutputTransition, O extends ParameterizedSymbol>
        extends RegisterEquivalence<LM, LH, I, TM, TH, Void, O> {

    private RMMEquivalenceJSE(RegisterMealyMachine<LM, I, TM, O> model,
                              RegisterMealyMachine<LH, I, TH, O> hyp,
                              Collection<? extends I> inputs,
                              ConstraintSolver solver) {
        super(model, hyp, inputs, solver);
    }

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedOutputTransition, T2 extends GuardedOutputTransition, O extends ParameterizedSymbol> Word<SymbolInstance<I>> findSeparatingWord(
            RegisterMealyMachine<L1, I, T1, O> model,
            RegisterMealyMachine<L2, I, T2, O> hyp,
            Collection<? extends I> inputs,
            ConstraintSolver solver) {
        return new RMMEquivalenceJSE<>(hyp, model, inputs, solver).findCounterExample();
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

            O mo = model.getTransitionProperty(mt);
            O ho = hyp.getTransitionProperty(ht);

            if (!Objects.equals(mo, ho)) {
                gov.nasa.jstateexplorer.transitionSystem.Transition t =
                        new gov.nasa.jstateexplorer.transitionSystem.Transition(guard,
                                                                                effects,
                                                                                getId(false),
                                                                                true,
                                                                                true);
                ret.add(new TransitionTuple(ps, t, "outputs differ: " + mt + " : " + ht));
            } else {
                Map<Parameter<?>, SymbolicDataValue<?>> mMap = new HashMap<>(mt.getAssignment().getOutputAssignment());
                Map<Parameter<?>, SymbolicDataValue<?>> hMap = new HashMap<>(ht.getAssignment().getOutputAssignment());

                Iterator<? extends Entry<Parameter<?>, SymbolicDataValue<?>>> iterator = mMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<Parameter<?>, ? extends SymbolicDataValue<?>> e = iterator.next();
                    SymbolicDataValue<?> mSrc = e.getValue();
                    SymbolicDataValue<?> hSrc = hMap.get(e.getKey());
                    if (mSrc.is(Parameter.class) || hSrc.is(Parameter.class)) {
                        if (!mSrc.equals(hSrc)) {
                            gov.nasa.jstateexplorer.transitionSystem.Transition t =
                                    new gov.nasa.jstateexplorer.transitionSystem.Transition(guard,
                                                                                            effects,
                                                                                            getId(false),
                                                                                            true,
                                                                                            true);
                            ret.add(new TransitionTuple(ps, t, "params differ: " + mt + " : " + ht));
                            return;
                        } else {
                            iterator.remove();
                            hMap.remove(e.getKey());
                        }
                    }
                }

                // ok guard
                // p1 == p1 and p2 == p2 and ...
                Expression<Boolean> okGuard = buildOutputTransitionGuardOK(mt, ht, hMap, effects);
                Expression<Boolean> okTGuard = ExpressionUtil.and(guard, okGuard);

                if (solver.isSatisfiable(okTGuard) == Result.SAT) {
                    LM mSucc = model.getSuccessor(mt);
                    LH hSucc = hyp.getSuccessor(ht);
                    gov.nasa.jstateexplorer.transitionSystem.Transition t =
                            new gov.nasa.jstateexplorer.transitionSystem.Transition(okTGuard,
                                                                                    effects,
                                                                                    getId(true),
                                                                                    true,
                                                                                    false);
                    ret.add(new TransitionTuple(ps, t, "itrans okGuards: " + mt + " : " + ht));
                    Pair<LM, LH> p = Pair.of(mSucc, hSucc);
                    if (visited.add(p)) {
                        queue.add(p);
                    }
                }

                // error guard
                // p1 != p1 or p2 != p2 or ...
                Expression<Boolean> errGuard = buildOutputTransitionGuardError(mt, ht, hMap, effects);
                Expression<Boolean> errTGuard = ExpressionUtil.and(guard, errGuard);

                if (solver.isSatisfiable(errTGuard) == Result.SAT) {
                    gov.nasa.jstateexplorer.transitionSystem.Transition t =
                            new gov.nasa.jstateexplorer.transitionSystem.Transition(errTGuard,
                                                                                    effects,
                                                                                    getId(false),
                                                                                    true,
                                                                                    true);
                    ret.add(new TransitionTuple(ps, t, "itrans errGuards: " + mt + " : " + ht));
                }
            }

        }
    }

    @Override
    protected <L, T extends GuardedTransition> boolean isPartialAutomatonAnError(RegisterStructure<L, ?, T, Void, O> ra,
                                                                                 Collection<T> trans) {
        return true;
    }

    private Expression<Boolean> buildOutputTransitionGuardOK(TM mt,
                                                             TH ht,
                                                             Map<Parameter<?>, ? extends Variable<?>> pmap,
                                                             Map effects) {
        if (pmap.isEmpty()) {
            return ExpressionUtil.TRUE;
        } else {
            List<Expression<Boolean>> result = new ArrayList<>(pmap.size());
            for (Parameter<?> p : pmap.keySet()) {
                result.add(new NumericBooleanExpression(findVariableForSource(mt.getAssignment()
                                                                                .getOutputAssignment()
                                                                                .get(p),
                                                                              modelRegs,
                                                                              modelConsts,
                                                                              effects),
                                                        NumericComparator.EQ,
                                                        findVariableForSource(ht.getAssignment()
                                                                                .getOutputAssignment()
                                                                                .get(p), hypRegs, hypConsts, effects)));
            }
            return ExpressionUtil.and(result);
        }
    }

    private Expression<Boolean> buildOutputTransitionGuardError(TM mt,
                                                                TH ht,
                                                                Map<Parameter<?>, ? extends Variable<?>> pmap,
                                                                Map effects) {
        return new Negation(buildOutputTransitionGuardOK(mt, ht, pmap, effects));
    }

    private Variable<?> findVariableForSource(SymbolicDataValue<?> source,
                                              VariableMapping<?, ?> regs,
                                              VariableMapping<?, ?> consts,
                                              Map effects) {

        if (source instanceof SymbolicDataValue.Register<?>) {
            Variable<?> r = regs.get(source);
            return (Variable<?>) effects.getOrDefault(r, r);
        } else if (source instanceof SymbolicDataValue.Constant<?>) {
            Variable<?> c = consts.get(source);
            return (Variable<?>) effects.getOrDefault(c, c);
        } else if (source instanceof SymbolicDataValue.FreshOutput<?>) {
            throw new IllegalStateException("not implemented yet");
        } else {
            throw new IllegalArgumentException("Unknown source " + source);
        }
    }

}
