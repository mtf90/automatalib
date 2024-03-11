package net.automatalib.util.automaton.equivalence.ra;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ConstantGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.impl.InputSymbol;

public class ExampleAsymmetricPaths {

    private static final DataType<Integer> T_INT = new DataType<>("int", BuiltinTypes.SINT32);
    private static final DataType<String> T_STR = new DataType<>("str", BuiltinTypes.STRING);
    private static final InputSymbol INPUT = new InputSymbol("input");
    private static final InputSymbol INPUT2 = new InputSymbol("input2");
    private static final InputSymbol INPUT3 = new InputSymbol("input3_int", T_INT);
    private static final InputSymbol INPUT4_INT = new InputSymbol("input4_int", T_INT);
    private static final InputSymbol INPUT4_STR = new InputSymbol("input4_str", T_STR);
    private static final Alphabet<InputSymbol> ALPHABET = Alphabets.singleton(INPUT);
    private static final Alphabet<InputSymbol> ALPHABET2 = Alphabets.fromArray(INPUT, INPUT2);
    private static final Alphabet<InputSymbol> ALPHABET3 = Alphabets.fromArray(INPUT3);
    private static final Alphabet<InputSymbol> ALPHABET4 = Alphabets.fromArray(INPUT3, INPUT4_STR);
    private static final Alphabet<InputSymbol> ALPHABET5 = Alphabets.fromArray(INPUT3, INPUT4_INT);

    public static CompactRA<InputSymbol> buildNoopAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.fromArray(INPUT, INPUT2, INPUT3)) {

            @Override
            public String toString() {
                return "noopRA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false);

        // initial location
        ra.addTransition(l0, INPUT, ra.createTransition(l0, ExpressionUtil.TRUE, new Assignment()));
        ra.addTransition(l0, INPUT2, ra.createTransition(l0, ExpressionUtil.TRUE, new Assignment()));
        ra.addTransition(l0, INPUT3, ra.createTransition(l0, ExpressionUtil.TRUE, new Assignment()));

        return ra;
    }

    public static CompactRA<InputSymbol> buildLockAutomaton() {

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r = rgen.next(T_INT);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c1 = cgen.next(T_INT);
        Constant<Integer> c2 = cgen.next(T_INT);
        Constant<Integer> c3 = cgen.next(T_INT);

        Constants consts = new Constants();
        consts.put(c1, new DataValue<>(T_INT, 1));
        consts.put(c2, new DataValue<>(T_INT, 2));
        consts.put(c3, new DataValue<>(T_INT, 3));

        RegisterValuation initialValues = new RegisterValuation();
        initialValues.put(r, consts.get(c1));

        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET, initialValues, consts) {

            @Override
            public String toString() {
                return "lockRA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false, r);
        Integer l1 = ra.addState(false);
        Integer l2 = ra.addState(false);
        Integer l3 = ra.addState(true);

        // guards
        Expression<Boolean> cond1 = new NumericBooleanExpression(r, NumericComparator.EQ, c1);
        Expression<Boolean> cond2 = new NumericBooleanExpression(r, NumericComparator.EQ, c2);
        Expression<Boolean> cond3 = new NumericBooleanExpression(r, NumericComparator.GE, c3);
        Expression<Boolean> elseCond = new Negation(ExpressionUtil.or(cond1, cond2, cond3));
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        Assignment empty = new Assignment();
        Assignment copy = new Assignment(new VarMapping<>(r, r));
        Assignment store2 = new Assignment(new VarMapping<>(r, c2));
        Assignment store3 = new Assignment(new VarMapping<>(r, c3));

        // initial location
        ra.addTransition(l0, INPUT, ra.createTransition(l1, cond1, empty));
        ra.addTransition(l0, INPUT, ra.createTransition(l2, cond2, empty));
        ra.addTransition(l0, INPUT, ra.createTransition(l3, cond3, empty));
        ra.addTransition(l0, INPUT, ra.createTransition(l0, elseCond, copy));

        ra.addTransition(l1, INPUT, ra.createTransition(l0, trueGuard, store2));

        ra.addTransition(l2, INPUT, ra.createTransition(l0, trueGuard, store3));

        ra.addTransition(l3, INPUT, ra.createTransition(l3, trueGuard, empty));

        return ra;
    }

    public static CompactRA<InputSymbol> buildTransitiveConstraintAutomaton() {

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p = pgen.next(T_INT);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c0 = cgen.next(T_INT);
        Constant<Integer> c1 = cgen.next(T_INT);
        Constant<Integer> c2 = cgen.next(T_INT);
        Constant<Integer> c3 = cgen.next(T_INT);

        Constants consts = new Constants();
        consts.put(c0, new DataValue<>(T_INT, 0));
        consts.put(c1, new DataValue<>(T_INT, 1));
        consts.put(c2, new DataValue<>(T_INT, 2));
        consts.put(c3, new DataValue<>(T_INT, 3));

        RegisterValuation initialValues = new RegisterValuation();
        initialValues.put(r, consts.get(c0));

        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET3, initialValues, consts) {

            @Override
            public String toString() {
                return "comboRA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false, r);
        Integer l1 = ra.addState(false, r);
        Integer l2 = ra.addState(false, r);
        Integer l3 = ra.addState(true);

        // guards
        Expression<Boolean> pCond1 = new NumericBooleanExpression(p, NumericComparator.GT, c1);
        Expression<Boolean> rCond2 = new NumericBooleanExpression(r, NumericComparator.GT, c2);
        Expression<Boolean> pCond3 = new NumericBooleanExpression(p, NumericComparator.GT, c3);

        // assignments
        Assignment empty = new Assignment();
        Assignment store1 = new Assignment(new VarMapping<>(r, p));
        Assignment copyAssign = new Assignment(new VarMapping<>(r, r));

        // initial location
        ra.addTransition(l0, INPUT3, ra.createTransition(l1, new Negation(rCond2), store1));
        ra.addTransition(l0, INPUT3, ra.createTransition(l3, rCond2, empty));

        ra.addTransition(l1, INPUT3, ra.createTransition(l2, pCond1, copyAssign));
        ra.addTransition(l2, INPUT3, ra.createTransition(l0, pCond3, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildUpdateNotInGuardsAutomaton1() {

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p = pgen.next(T_INT);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c1 = cgen.next(T_INT);

        Constants consts = new Constants();
        consts.put(c1, new DataValue<>(T_INT, 5));

        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET4, new RegisterValuation(), consts) {

            @Override
            public String toString() {
                return "guards1RA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, r);
        Integer l2 = ra.addState(true);

        // guards
        Expression<Boolean> cond1 = new NumericBooleanExpression(p, NumericComparator.LT, c1);
        Expression<Boolean> cond2 = new NumericBooleanExpression(r, NumericComparator.GE, c1);

        // assignments
        Assignment empty = new Assignment();
        Assignment store = new Assignment(new VarMapping<>(r, p));
        Assignment copy = new Assignment(new VarMapping<>(r, r));

        // initial location
        ra.addTransition(l0, INPUT3, ra.createTransition(l1, cond1, store));

        ra.addTransition(l1, INPUT3, ra.createTransition(l2, cond2, empty));
        ra.addTransition(l1, INPUT4_STR, ra.createTransition(l1, ExpressionUtil.TRUE, copy));

        ra.addTransition(l2, INPUT3, ra.createTransition(l2, ExpressionUtil.TRUE, empty));
        return ra;
    }

    public static CompactRA<InputSymbol> buildUpdateNotInGuardsAutomaton2() {

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p = pgen.next(T_INT);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c1 = cgen.next(T_INT);

        Constants consts = new Constants();
        consts.put(c1, new DataValue<>(T_INT, 6));

        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET4, new RegisterValuation(), consts) {

            @Override
            public String toString() {
                return "guards2RA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, r);
        Integer l2 = ra.addState(false, r);
        Integer l3 = ra.addState(false);

        // guards
        Expression<Boolean> cond1 = new NumericBooleanExpression(p, NumericComparator.LT, c1);

        // assignments
        Assignment empty = new Assignment();
        Assignment store = new Assignment(new VarMapping<>(r, p));
        Assignment assign = new Assignment(new VarMapping<>(r, r));

        // initial location
        ra.addTransition(l0, INPUT3, ra.createTransition(l1, cond1, store));

        ra.addTransition(l1, INPUT3, ra.createTransition(l2, cond1, assign));
        ra.addTransition(l1, INPUT4_STR, ra.createTransition(l3, cond1, empty));

        ra.addTransition(l2, INPUT3, ra.createTransition(l2, ExpressionUtil.TRUE, assign));

        ra.addTransition(l3, INPUT4_STR, ra.createTransition(l1, ExpressionUtil.TRUE, assign));
        return ra;
    }

    public static CompactRA<InputSymbol> buildDiamondReject() {
        return buildDiamond(false);
    }

    public static CompactRA<InputSymbol> buildDiamondAccept() {
        return buildDiamond(true);
    }

    private static CompactRA<InputSymbol> buildDiamond(boolean acc) {

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r = rgen.next(T_INT);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c0 = cgen.next(T_INT);
        Constant<Integer> c1 = cgen.next(T_INT);
        Constant<Integer> c2 = cgen.next(T_INT);

        Constants consts = new Constants();
        consts.put(c0, new DataValue<>(T_INT, 0));
        consts.put(c1, new DataValue<>(T_INT, 1));
        consts.put(c2, new DataValue<>(T_INT, 2));

        RegisterValuation initialValues = new RegisterValuation();
        initialValues.put(r, consts.get(c0));

        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET2, initialValues, consts) {

            @Override
            public String toString() {
                return "sym" + (acc ? "Acc" : "Rej") + "RA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, r);
        Integer l2 = ra.addState(false, r);
        Integer l3 = ra.addState(false, r);
        Integer l4 = ra.addState(acc);

        // guards
        Expression<Boolean> cond = new NumericBooleanExpression(r, NumericComparator.GT, c0);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        Assignment empty = new Assignment();
        Assignment store1 = new Assignment(new VarMapping<>(r, c1));
        Assignment store2 = new Assignment(new VarMapping<>(r, c2));
        Assignment copy = new Assignment(new VarMapping<>(r, r));

        // initial location
        ra.addTransition(l0, INPUT, ra.createTransition(l1, trueGuard, store1));
        ra.addTransition(l0, INPUT2, ra.createTransition(l2, trueGuard, store2));
        ra.addTransition(l3, INPUT, ra.createTransition(l4, cond, empty));
        ra.addTransition(l3, INPUT2, ra.createTransition(l4, cond, empty));

        ra.addTransition(l1, INPUT, ra.createTransition(l3, trueGuard, copy));
        ra.addTransition(l2, INPUT, ra.createTransition(l3, trueGuard, copy));

        return ra;
    }
}
