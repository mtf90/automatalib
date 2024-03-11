package net.automatalib.util.automaton.equivalence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.common.util.Pair;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

public abstract class AbstractRAEquivalence<LM, LH, I extends ParameterizedSymbol, TM extends GuardedTransition, TH extends GuardedTransition, SP, TP> {

    protected final RegisterStructure<LH, I, TH, SP, TP> hyp;
    protected final RegisterStructure<LM, I, TM, SP, TP> mod;
    protected final Collection<? extends I> inputs;

    protected final Map<Pair<LH, LM>, List<Expression<Boolean>>> cache;

    protected final ConstraintSolver solver;
    protected int ctr;

    public AbstractRAEquivalence(RegisterStructure<LH, I, TH, SP, TP> hyp,
                                 RegisterStructure<LM, I, TM, SP, TP> mod,
                                 Collection<? extends I> inputs,
                                 ConstraintSolver solver) {
        this.hyp = hyp;
        this.mod = mod;
        this.inputs = inputs;
        this.solver = solver;
        this.cache = new HashMap<>();
        this.ctr = 0;
    }

    protected abstract Word<SymbolInstance<I>> handleState(ExploredState<LH, LM, I> state,
                                                           I input,
                                                           Queue<? super ExploredState<LH, LM, I>> queue);

    protected Word<SymbolInstance<I>> findSeparatingWord() {
        final LH hypInit = hyp.getInitialState();
        final LM modInit = mod.getInitialState();

        if (!Objects.equals(hyp.getStateProperty(hypInit), mod.getStateProperty(modInit))) {
            return Word.epsilon();
        }

        final Deque<ExploredState<LH, LM, I>> queue = new ArrayDeque<>();
        queue.add(new ExploredState<>(hypInit, modInit, initialConstraints()));

        while (!queue.isEmpty()) {
            ExploredState<LH, LM, I> state = queue.poll();

            if (needsExploration(state)) {

                this.cache.computeIfAbsent(state.getKey(), k -> new ArrayList<>()).add(state.expression);

                for (I i : inputs) {

                    final Word<SymbolInstance<I>> sepWord = handleState(state, i, queue);

                    if (sepWord != null) {
                        return sepWord;
                    }
                }
            }

        }

        return null;
    }

    private Expression<Boolean> initialConstraints() {

        final RegisterValuation hypVal = hyp.getInitialRegisters();
        final RegisterValuation modVal = mod.getInitialRegisters();

        final List<Expression<Boolean>> expressions = new ArrayList<>(hypVal.size() + modVal.size());

        for (Entry<Register<?>, DataValue<?>> e : hypVal) {
            Register<?> reg = e.getKey();
            DataValue<?> value = e.getValue();
            expressions.add(NumericBooleanExpression.create(new Variable<>(reg.getType(), regToId(reg, true)),
                                                            NumericComparator.EQ,
                                                            value));
        }

        for (Entry<Register<?>, DataValue<?>> e : modVal) {
            Register<?> reg = e.getKey();
            DataValue<?> value = e.getValue();
            expressions.add(NumericBooleanExpression.create(new Variable<>(reg.getType(), regToId(reg, false)),
                                                            NumericComparator.EQ,
                                                            value));
        }

        return ExpressionUtil.and(expressions);
    }

    private boolean needsExploration(ExploredState<LH, LM, I> state) {

        final List<Expression<Boolean>> constraints = this.cache.getOrDefault(state.getKey(), Collections.emptyList());

        if (constraints.isEmpty()) {
            return true;
        }

        Collection<Register<?>> hRegs = state.hypLoc == null ? Collections.emptyList() : hyp.getRegisters(state.hypLoc);
        Collection<Register<?>> mRegs = state.modLoc == null ? Collections.emptyList() : mod.getRegisters(state.modLoc);

        if (hRegs.isEmpty() && mRegs.isEmpty()) {
            return false;
        } else {
            final Expression<Boolean> cache =
                    quantifyParameters(new Negation(ExpressionUtil.or(constraints)), Quantifier.FORALL);
            final Expression<Boolean> fresh = quantifyParameters(state.expression, Quantifier.EXISTS);
            final PropositionalCompound check = new PropositionalCompound(cache, LogicalOperator.AND, fresh);

            final Valuation val = new Valuation();
            final Result result = solver.solve(check, val);

            return result == Result.SAT;
        }
    }

    private Expression<Boolean> quantifyParameters(Expression<Boolean> expression, Quantifier quantifier) {

        final List<Variable<?>> inputs = new ArrayList<>();

        for (Variable<?> v : ExpressionUtil.freeVariables(expression)) {
            if (v.getName().startsWith("par_")) {
                inputs.add(v);
            }
        }

        return inputs.isEmpty() ? expression : new QuantifierExpression(quantifier, inputs, expression);
    }

    protected Expression<Boolean> buildTransitionExpression(ExploredState<LH, LM, I> state, TH hypTrans, TM modTrans) {

        final int depth = state.depth;
        final Expression<Boolean> hGuard =
                ExpressionUtil.transformVars(hypTrans.getGuard(), v -> replaceVariable(v, true, depth));
        final Expression<Boolean> mGuard =
                ExpressionUtil.transformVars(modTrans.getGuard(), v -> replaceVariable(v, false, depth));

        return ExpressionUtil.and(state.expression, hGuard, mGuard);
    }

    protected Expression<Boolean> buildTransitionExpression(ExploredState<LH, LM, I> state,
                                                            GuardedTransition trans,
                                                            boolean hyp) {
        final int depth = state.depth;
        final Expression<Boolean> guard =
                ExpressionUtil.transformVars(trans.getGuard(), v -> replaceVariable(v, hyp, depth));

        return ExpressionUtil.and(state.expression, guard);
    }

    protected Expression<Boolean> buildUpdateExpression(Expression<Boolean> expression,
                                                        Assignment hypAss,
                                                        Assignment modAss,
                                                        int depth) {

        final Map<String, Variable<?>> cache = new HashMap<>();

        Expression<Boolean> prev = ExpressionUtil.transformVars(expression, var -> {
            final String name = var.getName();
            if (name.startsWith("m_reg") || name.startsWith("h_reg")) {
                return cache.computeIfAbsent(name, n -> new Variable<>(var.getType(), "tmp_" + ctr++));
            } else {
                return var;
            }
        });

        final Expression<Boolean> hypUpdates = buildUpdateExpression(hypAss, cache, true, depth);
        final Expression<Boolean> modUpdates = buildUpdateExpression(modAss, cache, false, depth);

        return ExpressionUtil.and(prev, hypUpdates, modUpdates);
    }

    private Expression<Boolean> buildUpdateExpression(Assignment ass,
                                                      Map<String, Variable<?>> cache,
                                                      boolean hyp,
                                                      int depth) {

        final List<Expression<Boolean>> updates = new ArrayList<>(ass.getAssignment().size());

        for (Entry<Register<?>, ? extends SymbolicDataValue<?>> e : ass.getAssignment()) {
            final Register<?> reg = e.getKey();
            final SymbolicDataValue<?> val = e.getValue();
            final Expression<?> src;

            if (val instanceof SymbolicDataValue.Register<?> r) {
                String name = (hyp ? "h_reg_" : "m_reg_") + r.getId();
                if (cache.containsKey(name)) {
                    src = cache.get(name);
                } else {
                    src = replaceVariable(val, hyp, depth);
                }
            } else {
                src = replaceVariable(val, hyp, depth);
            }

            updates.add(new NumericBooleanExpression(new Variable<>(reg.getType(), regToId(reg, hyp)),
                                                     NumericComparator.EQ,
                                                     src));
        }

        return ExpressionUtil.and(updates);
    }

    protected Word<SymbolInstance<I>> extractSeparatingWord(Valuation valuation, ExploredState<LH, LM, I> state) {
        final WordBuilder<SymbolInstance<I>> wb = new WordBuilder<>(state.depth);

        ExploredState<LH, LM, I> iter = state;

        while (iter.depth > 0) {
            final I sym = iter.sym;
            final int params = sym.getArity();

            final DataValue<?>[] values = new DataValue[params];

            for (int i = 0; i < params; i++) {
                values[i] = extractValue(valuation, sym.getPtype(i), i + 1, iter.depth);
            }

            wb.append(new SymbolInstance<>(sym, values));
            iter = iter.prev;
        }

        return wb.reverse().toWord();
    }

    protected <T> DataValue<T> extractValue(Valuation valuation, DataType<T> type, int id, int depth) {
        final Parameter<T> parameter = new Parameter<>(type, id);
        final String parName = parToId(parameter, depth);
        final Variable<T> var = new Variable<>(type.getType(), parName);
        // if the parameter does not occur in the valuation, its value doesn't matter so we can choose an arbitrary one
        final T value = valuation.containsValueFor(var) ? valuation.getValue(var) : type.getType().getDefaultValue();
        return new DataValue<>(type, value);
    }

    protected Expression<?> replaceVariable(Variable<?> variable, boolean hyp, int depth) {
        if (variable instanceof SymbolicDataValue.Register<?> r) {
            return new Variable<>(r.getType(), regToId(r, hyp));
        } else if (variable instanceof SymbolicDataValue.Parameter<?> p) {
            return new Variable<>(p.getType(), parToId(p, depth + 1));
        } else if (variable instanceof SymbolicDataValue.Constant<?> c) {
            return constToConst(c, hyp);
        } else {
            throw new IllegalArgumentException("Unknown variable type " + variable);
        }
    }

    protected static String regToId(Register<?> register, boolean hyp) {
        if (hyp) {
            return "h_reg_" + register.getId();
        } else {
            return "m_reg_" + register.getId();
        }
    }

    protected static String parToId(Parameter<?> parameter, int depth) {
        return "par_" + depth + "_" + parameter.getId();
    }

    protected <T> gov.nasa.jpf.constraints.expressions.Constant<T> constToConst(SymbolicDataValue.Constant<T> constant,
                                                                                boolean hyp) {
        if (hyp) {
            return new gov.nasa.jpf.constraints.expressions.Constant<>(constant.getType(),
                                                                       this.hyp.getConstants()
                                                                               .get(constant)
                                                                               .getValue());
        } else {
            return new gov.nasa.jpf.constraints.expressions.Constant<>(constant.getType(),
                                                                       this.mod.getConstants()
                                                                               .get(constant)
                                                                               .getValue());
        }
    }

    protected static final class ExploredState<L1, L2, I extends ParameterizedSymbol> {

        protected final L1 hypLoc;
        protected final L2 modLoc;
        protected final Expression<Boolean> expression;
        protected final ExploredState<L1, L2, I> prev;
        protected final I sym;
        protected final int depth;

        ExploredState(L1 hypLoc, L2 modLoc, Expression<Boolean> expression) {
            this(hypLoc, modLoc, expression, null, null, 0);
        }

        ExploredState(L1 hypLoc, L2 modLoc, Expression<Boolean> expression, ExploredState<L1, L2, I> prev, I sym) {
            this(hypLoc, modLoc, expression, prev, sym, prev.depth + 1);
        }

        private ExploredState(L1 hypLoc,
                              L2 modLoc,
                              Expression<Boolean> expression,
                              ExploredState<L1, L2, I> prev,
                              I sym,
                              int depth) {
            this.hypLoc = hypLoc;
            this.modLoc = modLoc;
            this.expression = expression;
            this.prev = prev;
            this.sym = sym;
            this.depth = depth;
        }

        private Pair<L1, L2> getKey() {
            return Pair.of(hypLoc, modLoc);
        }
    }

}
