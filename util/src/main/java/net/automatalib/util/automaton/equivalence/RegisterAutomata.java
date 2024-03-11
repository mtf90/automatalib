package net.automatalib.util.automaton.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.automaton.concept.RegisterStructure;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class RegisterAutomata {

    public static <L1, L2, I extends ParameterizedSymbol, T1 extends GuardedTransition, T2 extends GuardedTransition> boolean hasDisjunctGuards(
            RegisterAutomaton<L1, I, T1> hyp,
            Collection<? extends I> inputs) {
        return hasDisjunctGuards(hyp, inputs, ConstraintSolverFactory.createSolver("z3"));
    }

    public static <L, I extends ParameterizedSymbol, T extends GuardedTransition, T2 extends GuardedTransition> boolean hasDisjunctGuards(
            RegisterStructure<L, I, T, ?, ?> ra,
            Collection<? extends I> inputs,
            ConstraintSolver solver) {

        Expression<Boolean> consts = ExpressionUtil.TRUE;

        for (Entry<Constant<?>, DataValue<?>> c : ra.getConstants()) {
            consts = new PropositionalCompound(consts,
                                               LogicalOperator.AND,
                                               new NumericBooleanExpression(c.getKey(),
                                                                            NumericComparator.EQ,
                                                                            c.getValue()));
        }

        for (L loc : ra) {
            for (I input : inputs) {
                final Collection<T> transitions = ra.getTransitions(loc, input);

                if (transitions.size() > 1) {
                    final List<T> trans = new ArrayList<>(transitions);
                    for (int i = 0; i < transitions.size() - 1; i++) {
                        for (int j = i + 1; j < transitions.size(); j++) {
                            final Expression<Boolean> g1 = trans.get(i).getGuard();
                            final Expression<Boolean> g2 = trans.get(j).getGuard();

                            final Valuation val = new Valuation();
                            Result result = solver.solve(ExpressionUtil.and(consts, g1, g2), val);

                            if (result == Result.SAT) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public static <L, I extends ParameterizedSymbol, T extends GuardedTransition, T2 extends GuardedTransition> boolean hasConsistentAssignments(
            RegisterStructure<L, I, T, ?, ?> ra,
            Collection<? extends I> inputs) {

        for (L loc : ra) {
            for (I input : inputs) {
                for (T t : ra.getTransitions(loc, input)) {

                    final Set<Register<?>> regs = new HashSet<>(ra.getRegisters(ra.getSuccessor(t)));

                    if (!t.getAssignment().getAssignment().keySet().equals(regs)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
