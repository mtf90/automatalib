package net.automatalib.util.automaton.equivalence.jse;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jstateexplorer.SymbolicSearchEngine;
import gov.nasa.jstateexplorer.datastructures.searchImage.SymbolicImage;
import gov.nasa.jstateexplorer.transitionSystem.SynchronisedTransitionHelper;
import gov.nasa.jstateexplorer.transitionSystem.TransitionSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.concept.StateIDs;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.common.util.Pair;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public abstract class RegisterEquivalence<LM, LH, I extends ParameterizedSymbol, TM extends GuardedTransition, TH extends GuardedTransition, SP, TP> {

    private final static Type<Integer> TYPE = BuiltinTypes.SINT32;
    private final static Variable<Integer> MQ = new Variable<>(TYPE, "__mq");
    private final static Variable<Integer> HQ = new Variable<>(TYPE, "__hq");

    protected final Set<Pair<LM, LH>> visited = new HashSet<>();
    protected final Queue<Pair<LM, LH>> queue = new LinkedList<>();

    private final Map<String, TransitionTuple> tuples = new HashMap<>();

    protected final RegisterStructure<LM, I, TM, SP, TP> model;
    protected final RegisterStructure<LH, I, TH, SP, TP> hyp;
    protected final Collection<? extends I> inputs;

    private final StateIDs<LM> mIds;
    private final StateIDs<LH> hIds;

    protected final VariableMapping<Register<?>, Variable<?>> hypRegs = new VariableMapping<>();
    protected final VariableMapping<SymbolicDataValue.Constant<?>, Variable<?>> hypConsts = new VariableMapping<>();
    protected final VariableMapping<Register<?>, Variable<?>> modelRegs = new VariableMapping<>();
    protected final VariableMapping<SymbolicDataValue.Constant<?>, Variable<?>> modelConsts = new VariableMapping<>();

    protected final ConstraintSolver solver;

    private int tId = 0;

    protected RegisterEquivalence(RegisterStructure<LM, I, TM, SP, TP> model,
                                  RegisterStructure<LH, I, TH, SP, TP> hyp,
                                  Collection<? extends I> inputs,
                                  ConstraintSolver solver) {
        this.model = model;
        this.hyp = hyp;
        this.inputs = inputs;
        this.solver = solver;

        this.mIds = model.stateIDs();
        this.hIds = hyp.stateIDs();
    }

    public Word<SymbolInstance<I>> findCounterExample() {

        buildVarMaps();
        tuples.clear();

        TransitionSystem<?> ts = buildTransitionSystem();
        SymbolicImage image = SymbolicSearchEngine.symbolicBreadthFirstSearch(ts, solver, Integer.MIN_VALUE);

        List ce = image.getHistoryForCE();
        if (ce != null && !ce.isEmpty()) {
            return buildCE(ce);
        }
        return null;
    }

    private TransitionSystem<?> buildTransitionSystem() {

        Valuation initVal = computeInitialState();

        List<TransitionTuple> trans = new ArrayList<>();

        visited.clear();
        queue.clear();
        Pair<LM, LH> i = Pair.of(model.getInitialState(), hyp.getInitialState());
        queue.add(i);
        visited.add(i);

        while (!queue.isEmpty()) {
            Pair<LM, LH> p = queue.poll();
            LM ml = p.getFirst();
            LH hl = p.getSecond();

            boolean output = isOutputLoc(ml);
            if (isOutputLoc(ml) ^ isOutputLoc(hl)) {
                continue;
            }

            if (output) {
                //                                oloc(ml, hl, trans);
            } else {
                iloc(ml, hl, trans);
            }
        }

        List<gov.nasa.jstateexplorer.transitionSystem.Transition> tt = new ArrayList<>();
        trans.forEach((t) -> {
            tt.add(t.t);
            tuples.put(t.t.getId(), t);
        });
        return new TransitionSystem<>(initVal, tt, new SynchronisedTransitionHelper());
    }

    //    private void oloc(LM ml, LH hl, List<TransitionTuple> ret) {
    //
    //        for (I ps : inputs) {
    //            for (TM mt : model.getTransitions(ml, ps)) {
    //                for (TH ht : hyp.getTransitions(hl, ps)) {
    //                    //assert mt.getLabel().equals(ht.getLabel());
    //                    Map<Parameter<?>, Variable<?>> pmap = buildParMap(ps);
    //                    otrans(ml, hl, (OutputTransition) mt, (OutputTransition) ht, pmap, ret);
    //                }
    //            }
    //        }
    //    }

    private void iloc(LM ml, LH hl, List<TransitionTuple> ret) {

        for (I ps : inputs) {
            Map<Parameter<?>, Variable<?>> pmap = buildParMap(ps);

            Collection<TM> mTrans = model.getTransitions(ml, ps);
            Collection<TH> hTrans = hyp.getTransitions(hl, ps);

            if (mTrans.isEmpty() && hTrans.isEmpty()) {
                continue;
            } else if (mTrans.isEmpty() != hTrans.isEmpty()) {

                Expression<Boolean> guard =
                        (mTrans.isEmpty()) ? or(hTrans, pmap, hypRegs) : or(mTrans, pmap, modelRegs);

                guard = ExpressionUtil.and(locGuard(ml, hl), guard);

                if (solver.isSatisfiable(guard) == Result.SAT) {
                    boolean isError = (mTrans.isEmpty()) ?
                            isPartialAutomatonAnError(hyp, hTrans) :
                            isPartialAutomatonAnError(model, mTrans);
                    gov.nasa.jstateexplorer.transitionSystem.Transition t =
                            new gov.nasa.jstateexplorer.transitionSystem.Transition(guard,
                                                                                    new HashMap<>(),
                                                                                    getId(!isError),
                                                                                    true,
                                                                                    isError);

                    ret.add(new TransitionTuple(ps, t, "iloc one is null: " + ps));
                }
            } else {
                for (TM mt : mTrans) {
                    for (TH ht : hTrans) {
                        itrans(ps, ml, hl, mt, ht, pmap, ret);
                    }
                }

                Expression<Boolean> mAll = or(mTrans, pmap, modelRegs);
                Expression<Boolean> hAll = or(hTrans, pmap, hypRegs);

                assert mAll != null;
                assert hAll != null;

                Expression<Boolean> guard = new PropositionalCompound(mAll, LogicalOperator.XOR, hAll);

                guard = ExpressionUtil.and(locGuard(ml, hl), guard);

                // error catch all
                if (solver.isSatisfiable(guard) == Result.SAT) {
                    gov.nasa.jstateexplorer.transitionSystem.Transition t =
                            new gov.nasa.jstateexplorer.transitionSystem.Transition(guard,
                                                                                    new HashMap<>(),
                                                                                    getId(false),
                                                                                    true,
                                                                                    true);
                    ret.add(new TransitionTuple(ps, t, "iloc error catch all: " + ps));
                }
            }
        }
    }

    protected abstract void itrans(I ps,
                                   LM ml,
                                   LH hl,
                                   TM mt,
                                   TH ht,
                                   Map<Parameter<?>, Variable<?>> pmap,
                                   List<TransitionTuple> ret);

    protected abstract <L, T extends GuardedTransition> boolean isPartialAutomatonAnError(RegisterStructure<L, ?, T, SP, TP> ra,
                                                                                          Collection<T> trans);

    protected Expression<Boolean> buildInputTransitionGuard(LM ml,
                                                            LH hl,
                                                            TM mt,
                                                            TH ht,
                                                            Map<Parameter<?>, Variable<?>> pmap) {

        Expression<Boolean> mGuard = translateGuardCondition(mt.getGuard(), modelRegs, pmap);
        Expression<Boolean> hGuard = translateGuardCondition(ht.getGuard(), hypRegs, pmap);
        Expression<Boolean> locs = locGuard(ml, hl);

        return ExpressionUtil.and(mGuard, hGuard, locs);
    }

    protected Expression<Boolean> locGuard(LM ml, LH hl) {
        return ExpressionUtil.and(new NumericBooleanExpression(MQ,
                                                               NumericComparator.EQ,
                                                               new Constant<>(TYPE, mIds.getStateId(ml))),
                                  new NumericBooleanExpression(HQ,
                                                               NumericComparator.EQ,
                                                               new Constant<>(TYPE, hIds.getStateId(hl))));
    }

    protected void computeEffects(TM mt,
                                  TH ht,
                                  Map<Variable, Expression<Boolean>> effects,
                                  Map<Parameter<?>, Variable<?>> pmap) {

        computeEffectsTrans(mt, effects, modelConsts, pmap, modelRegs);
        computeEffectsTrans(ht, effects, hypConsts, pmap, hypRegs);
        computeEffectsLoc(model.getSuccessor(mt), hyp.getSuccessor(ht), effects);
    }

    private void computeEffectsLoc(LM ml, LH hl, Map effects) {
        effects.put(MQ, new Constant<>(TYPE, mIds.getStateId(ml)));
        effects.put(HQ, new Constant<>(TYPE, hIds.getStateId(hl)));
    }

    private void computeEffectsTrans(GuardedTransition t,
                                     Map effects,
                                     Map<SymbolicDataValue.Constant<?>, Variable<?>> cmap,
                                     Map<Parameter<?>, Variable<?>> pmap,
                                     Map<Register<?>, Variable<?>> rmap) {

        for (Entry<Register<?>, ? extends SymbolicDataValue<?>> e : t.getAssignment().getAssignment()) {

            // TODO: what about constants?
            Variable<?> v = rmap.get(e.getKey());
            SymbolicDataValue<?> value = e.getValue();
            Expression<?> expr;
            if (value instanceof Register<?>) {
                expr = rmap.get(value);
            } else if (value instanceof Parameter<?>) {
                expr = pmap.get(value);
            } else if (value instanceof SymbolicDataValue.Constant<?>) {
                expr = cmap.get(value);
            } else {
                throw new IllegalArgumentException("Unknown value " + value);
            }

            effects.put(v, expr);
        }
    }

    protected void computeEffectsOutput(TM mt,
                                        TH ht,
                                        Map<Variable, Expression<Boolean>> effects,
                                        Map<Parameter<?>, Variable<?>> pmap) {

        computeEffectsTrans(mt, effects, modelConsts, pmap, modelRegs);
        computeEffectsTrans(ht, effects, hypConsts, pmap, hypRegs);
        computeEffectsLoc(model.getSuccessor(mt), hyp.getSuccessor(ht), effects);
    }

    private Expression<Boolean> translateGuardCondition(Expression<Boolean> condition,
                                                        Map<Register<?>, Variable<?>> regs,
                                                        Map<Parameter<?>, Variable<?>> pmap) {

        Map<SymbolicDataValue<?>, Variable<?>> atoms = new HashMap<>();
        atoms.putAll(pmap);
        atoms.putAll(regs);

        return ExpressionUtil.transformVars(condition, v -> atoms.getOrDefault(v, v));
    }

    private boolean isOutputLoc(Object loc) {
        return false;
        //        return !loc.getOut().isEmpty() && loc.getOut().iterator().next() instanceof OutputTransition;
    }

    private Valuation computeInitialState() {
        Valuation val = new Valuation();

        val.setValue(MQ, mIds.getStateId(model.getInitialState()));
        val.setValue(HQ, hIds.getStateId(hyp.getInitialState()));

        for (Register<?> r : model.getRegisters()) {
            setInitialValues(val, r, model.getInitialRegisters(), modelRegs);
        }
        for (SymbolicDataValue.Constant<?> c : model.getConstants().keySet()) {
            setInitialConstants(val, c, model.getConstants(), modelConsts);
        }
        for (Register<?> r : hyp.getRegisters()) {
            setInitialValues(val, r, hyp.getInitialRegisters(), hypRegs);
        }
        for (SymbolicDataValue.Constant<?> c : hyp.getConstants().keySet()) {
            setInitialConstants(val, c, hyp.getConstants(), hypConsts);
        }

        return val;
    }

    private <T> void setInitialValues(Valuation val,
                                      Register<T> r,
                                      RegisterValuation initial,
                                      VariableMapping<Register<?>, Variable<?>> mapping) {
        final DataValue<T> dataValue = initial.get(r);
        final T value = (initial.containsKey(r)) ? dataValue.getValue() : r.getType().getDefaultValue();
        val.setValue(mapping.get(r), value);
    }

    private <T> void setInitialConstants(Valuation val,
                                         SymbolicDataValue.Constant<T> c,
                                         Constants initial,
                                         VariableMapping<SymbolicDataValue.Constant<?>, Variable<?>> mapping) {
        final DataValue<T> dataValue = initial.get(c);
        final T value = (initial.containsKey(c)) ? dataValue.getValue() : c.getType().getDefaultValue();
        val.setValue(mapping.get(c), value);
    }

    private void buildVarMaps() {
        modelRegs.clear();
        model.getRegisters().forEach(r -> modelRegs.put(r, new Variable<>(r.getType(), "m_" + r)));
        model.getConstants().keySet().forEach(c -> modelConsts.put(c, new Variable<>(c.getType(), "m_" + c)));

        hypRegs.clear();
        hyp.getRegisters().forEach(r -> hypRegs.put(r, new Variable<>(r.getType(), "h_" + r)));
        hyp.getConstants().keySet().forEach(c -> hypConsts.put(c, new Variable<>(c.getType(), "h_" + c)));
    }

    protected Map<Parameter<?>, Variable<?>> buildParMap(ParameterizedSymbol ps) {
        VariableMapping<Parameter<?>, Variable<?>> pmap = new VariableMapping<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (int i = 0; i < ps.getArity(); i++) {
            DataType<?> ptype = ps.getPtypes()[i];
            pmap.put(pgen.next(ptype), new Variable<>(ptype.getType(), ps.getName() + "_" + i));
        }
        return pmap;
    }

    protected String getId(boolean ok) {
        return "t_" + (tId++) + (ok ? "OK" : "ERR");
    }

    private <T extends GuardedTransition> Expression<Boolean> or(Collection<T> trans,
                                                                 Map<Parameter<?>, Variable<?>> pmap,
                                                                 Map<Register<?>, Variable<?>> regs) {

        Expression<Boolean> ret = null;
        for (T t : trans) {
            Expression<Boolean> temp = translateGuardCondition(t.getGuard(), regs, pmap);

            ret = (ret == null) ? temp : ExpressionUtil.or(ret, temp);
        }
        return ret;
    }

    private Word<SymbolInstance<I>> buildCE(List<gov.nasa.jstateexplorer.transitionSystem.Transition> ceTrace) {

        List<TransitionTuple> tupleList = new ArrayList<>();
        for (gov.nasa.jstateexplorer.transitionSystem.Transition t : ceTrace) {
            tupleList.add(tuples.get(t.getId()));
        }

        Expression<Boolean> instCheck = stitchTogether(tupleList);

        Valuation val = new Valuation();
        Result res = solver.solve(instCheck, val);
        assert Result.SAT == res;

        //System.out.println(instCheck);
        //System.out.println(val);

        @SuppressWarnings("unchecked")
        SymbolInstance<I>[] psi = new SymbolInstance[ceTrace.size()];
        int i = 0;
        for (TransitionTuple t : tupleList) {
            I ps = t.a;
            //System.out.println(t.info);

            String prefix = "__" + i + "_";

            DataValue<?>[] dvs = new DataValue[ps.getArity()];
            for (int j = 0; j < ps.getArity(); j++) {
                String varname = prefix + ps.getName() + "_" + j;
                dvs[j] = new DataValue(ps.getPtypes()[j], val.getValue(varname));
                //System.out.println(varname + " : " + val.getValue(varname));
            }
            psi[i] = new SymbolInstance<>(ps, dvs);
            i++;
        }
        return Word.fromSymbols(psi);
    }

    private Expression<Boolean> stitchTogether(List<TransitionTuple> tList) {
        int oldId = 0;
        @SuppressWarnings("unchecked")
        Expression<Boolean>[] temp = new Expression[tList.size() + 1];

        // initial state
        Valuation initial = computeInitialState();
        temp[0] = ExpressionUtil.addPrefix(ExpressionUtil.and(initialStateAsExpression(modelRegs, initial),
                                                              initialStateAsExpression(hypRegs, initial)), "__0_");

        // transitions
        for (TransitionTuple t : tList) {
            temp[oldId + 1] = ExpressionUtil.and(ExpressionUtil.addPrefix(t.t.getGuard(), "__" + oldId + "_"),
                                                 asExpression("__" + oldId + "_",
                                                              "__" + (oldId + 1) + "_",
                                                              t.t.getEffects()));

            for (int j = 0; j < t.a.getArity(); j++) {
                Variable<?> v = new Variable<>(t.a.getPtype(j).getType(), "__" + oldId + "_" + t.a.getName() + "_" + j);
                temp[oldId + 1] =
                        ExpressionUtil.and(temp[oldId + 1], new NumericBooleanExpression(v, NumericComparator.EQ, v));
            }
            oldId++;
        }

        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> asExpression(String oldPrefix,
                                             String newPrefix,
                                             Map<Variable, Expression<Boolean>> effects) {

        @SuppressWarnings("unchecked")
        Expression<Boolean>[] temp = new Expression[modelRegs.size() + hypRegs.size()];
        int i = 0;
        for (Variable<?> v : modelRegs.values()) {
            temp[i++] = effectAsExpression(v, oldPrefix, newPrefix, effects);
        }
        for (Variable<?> v : hypRegs.values()) {
            temp[i++] = effectAsExpression(v, oldPrefix, newPrefix, effects);
        }
        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> initialStateAsExpression(Map<Register<?>, Variable<?>> regs, Valuation vals) {

        @SuppressWarnings("unchecked")
        Expression<Boolean>[] temp = new Expression[regs.size()];
        int idx = 0;
        for (Entry<Register<?>, Variable<?>> e : regs.entrySet()) {
            Object d = vals.getValue(e.getValue());
            temp[idx++] = new NumericBooleanExpression(e.getValue(),
                                                       NumericComparator.EQ,
                                                       new Constant(e.getKey().getType(), d));
        }
        return ExpressionUtil.and(temp);
    }

    private Expression<Boolean> effectAsExpression(Variable<?> v,
                                                   String oldPrefix,
                                                   String newPrefix,
                                                   Map<Variable, Expression<Boolean>> effects) {

        Expression<?> expr = effects.get(v);
        if (expr == null) {
            expr = v;
        }

        return new NumericBooleanExpression(ExpressionUtil.addPrefix(v, newPrefix),
                                            NumericComparator.EQ,
                                            ExpressionUtil.addPrefix(expr, oldPrefix));
    }

    protected class TransitionTuple {

        private final I a;
        private final gov.nasa.jstateexplorer.transitionSystem.Transition t;
        private String info;

        TransitionTuple(I a, gov.nasa.jstateexplorer.transitionSystem.Transition t, String info) {
            this.a = a;
            this.t = t;
            this.info = info;
        }

    }

    protected static class VariableMapping<K extends SymbolicDataValue<?>, V extends Variable<?>>
            extends HashMap<K, V> {

        @Override
        public V put(K key, V value) {
            if (!key.getType().equals(value.getType())) {
                throw new IllegalArgumentException("Types of key and value do not match");
            }
            return super.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> Variable<T> get(SymbolicDataValue<T> key) {
            return (Variable<T>) super.get(key);
        }
    }

}
