package net.automatalib.automaton.ra.impl;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.StringBooleanExpression;
import gov.nasa.jpf.constraints.expressions.StringBooleanOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.GuardedTransition;
import net.automatalib.automaton.ra.MutableRegisterAutomaton;
import net.automatalib.automaton.ra.State;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ConstantGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class RATest<L, T extends GuardedTransition> {

    public static final DataType<Integer> T_UID = new DataType<>("T_uid", BuiltinTypes.SINT32);
    public static final DataType<Integer> T_PWD = new DataType<>("T_pwd", BuiltinTypes.SINT32);
    public static final DataType<String> T_UID_S = new DataType<>("T_uid_s", BuiltinTypes.STRING);
    public static final DataType<String> T_PWD_S = new DataType<>("T_pwd_s", BuiltinTypes.STRING);

    public static final InputSymbol REGISTER = new InputSymbol("register", T_UID, T_PWD);
    public static final InputSymbol LOGIN = new InputSymbol("login", T_UID, T_PWD);
    public static final InputSymbol LOGOUT = new InputSymbol("logout");
    public static final InputSymbol REGISTER_S = new InputSymbol("register", T_UID_S, T_PWD_S);
    public static final InputSymbol LOGIN_S = new InputSymbol("login", T_UID_S, T_PWD_S);

    private final RACreator<L, InputSymbol, T> creator;

    @Factory(dataProvider = "creators")
    public RATest(RACreator<L, InputSymbol, T> creator) {
        this.creator = creator;
    }

    @DataProvider(name = "creators")
    public static Object[] creators() {
        final RACreator<Integer, ? extends ParameterizedSymbol, CompactRATransition> compact = CompactRA::new;
        return new RACreator<?, ?, ?>[] {compact};
    }

    @Test
    public void testAcceptor() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        MutableRegisterAutomaton<L, InputSymbol, T> ra =
                creator.create(alphabet, new RegisterValuation(), new Constants());

        // locations
        L l0 = ra.addInitialState(false);
        L l1 = ra.addState(false);
        L l2 = ra.addState(true);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        DeterministicAcceptorTS<State<L>, SymbolInstance<InputSymbol>> acceptor = ra.asAcceptor();

        Word<SymbolInstance<InputSymbol>> input =
                Word.fromSymbols(new SymbolInstance<>(REGISTER, 0, 1), new SymbolInstance<>(LOGIN, 0, 1));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(REGISTER, 0, 1),
                                 new SymbolInstance<>(LOGIN, 0, 1),
                                 new SymbolInstance<>(LOGOUT));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertFalse(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(REGISTER, 0, 1), new SymbolInstance<>(LOGIN, 0, 0));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertFalse(acceptor.accepts(input));

        input = Word.fromLetter(new SymbolInstance<>(LOGIN, 0, 1));
        Assert.assertNull(acceptor.getState(input));
        Assert.assertFalse(acceptor.accepts(input));
    }

    @Test
    public void testInitialValues() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        RegisterValuation initialValues = new RegisterValuation();
        initialValues.put(rUid, new DataValue<>(T_UID, 4));
        initialValues.put(rPwd, new DataValue<>(T_PWD, 2));

        MutableRegisterAutomaton<L, InputSymbol, T> ra = creator.create(alphabet, initialValues, new Constants());

        // locations
        L l0 = ra.addInitialState(false);
        L l1 = ra.addState(false);
        L l2 = ra.addState(true);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));
        ra.addTransition(l0, LOGIN, ra.createTransition(l2, condition, copyAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        DeterministicAcceptorTS<State<L>, SymbolInstance<InputSymbol>> acceptor = ra.asAcceptor();

        Word<SymbolInstance<InputSymbol>> input = Word.fromLetter(new SymbolInstance<>(LOGIN, 4, 2));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromLetter(new SymbolInstance<>(LOGIN, 2, 4));
        Assert.assertNull(acceptor.getState(input));
        Assert.assertFalse(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(REGISTER, 0, 1), new SymbolInstance<>(LOGIN, 0, 1));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(REGISTER, 0, 1),
                                 new SymbolInstance<>(LOGIN, 0, 1),
                                 new SymbolInstance<>(LOGOUT),
                                 new SymbolInstance<>(LOGIN, 0, 1));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));
    }

    @Test
    public void testConstants() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER_S, LOGIN_S, LOGOUT);

        // registers, parameters, and constants
        RegisterGenerator rgen = new RegisterGenerator();
        Register<String> rUid = rgen.next(T_UID_S);
        Register<String> currUid = rgen.next(T_UID_S);
        Register<String> rPwd = rgen.next(T_PWD_S);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<String> pUid = pgen.next(T_UID_S);
        Parameter<String> pPwd = pgen.next(T_PWD_S);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cUid = cgen.next(T_UID_S);
        Constant<String> cPwd = cgen.next(T_PWD_S);

        Constants constants = new Constants();
        constants.put(cUid, new DataValue<>(T_UID_S, "root"));
        constants.put(cPwd, new DataValue<>(T_PWD_S, "admin"));
        MutableRegisterAutomaton<L, InputSymbol, T> ra = creator.create(alphabet, new RegisterValuation(), constants);

        // locations
        L l0 = ra.addInitialState(false);
        L l1 = ra.addState(false);
        L l2 = ra.addState(true);

        // guards
        Expression<Boolean> adminLoginCondition =
                ExpressionUtil.and(new StringBooleanExpression(cUid, StringBooleanOperator.EQUALS, pUid),
                                   new StringBooleanExpression(pPwd, StringBooleanOperator.PREFIXOF, cPwd));
        Expression<Boolean> userLoginCondition =
                ExpressionUtil.and(new StringBooleanExpression(rUid, StringBooleanOperator.EQUALS, pUid),
                                   new StringBooleanExpression(rPwd, StringBooleanOperator.EQUALS, pPwd));
        Expression<Boolean> loginCondition = ExpressionUtil.or(userLoginCondition, adminLoginCondition);
        Expression<Boolean> unregisteredLogoutCondition =
                new StringBooleanExpression(cUid, StringBooleanOperator.EQUALS, currUid);
        Expression<Boolean> registeredLogoutCondition = new Negation(unregisteredLogoutCondition);
        Expression<Boolean> elseCond = new Negation(loginCondition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> registerMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> userLoginMapping =
                new VarMapping<>(rUid, pUid, rPwd, pPwd, currUid, pUid);
        VarMapping<Register<?>, SymbolicDataValue<?>> adminLoginMapping =
                new VarMapping<>(rUid, pUid, rPwd, pPwd, currUid, cUid);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment registerAssign = new Assignment(registerMapping);
        Assignment userLoginAssign = new Assignment(userLoginMapping);
        Assignment adminLoginAssign = new Assignment(adminLoginMapping);

        // initial location
        ra.addTransition(l0, REGISTER_S, ra.createTransition(l1, trueGuard, registerAssign));
        ra.addTransition(l0, LOGIN_S, ra.createTransition(l2, adminLoginCondition, adminLoginAssign));

        // reg. location
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l2, adminLoginCondition, adminLoginAssign));
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l2, userLoginCondition, userLoginAssign));
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, registeredLogoutCondition, copyAssign));
        ra.addTransition(l2, LOGOUT, ra.createTransition(l0, unregisteredLogoutCondition, new Assignment()));

        DeterministicAcceptorTS<State<L>, SymbolInstance<InputSymbol>> acceptor = ra.asAcceptor();

        Word<SymbolInstance<InputSymbol>> input = Word.fromSymbols(new SymbolInstance<>(REGISTER_S, "user", "pwd"),
                                                                   new SymbolInstance<>(LOGIN_S, "user", "pwd"));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(REGISTER_S, "user", "pwd"),
                                 new SymbolInstance<>(LOGIN_S, "user", "pdw"));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertFalse(acceptor.accepts(input));

        input = Word.fromLetter(new SymbolInstance<>(LOGIN_S, "root", "admin123"));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(LOGIN_S, "root", "admin"),
                                 new SymbolInstance<>(LOGOUT),
                                 new SymbolInstance<>(LOGIN_S, "root", "admin"));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));

        input = Word.fromSymbols(new SymbolInstance<>(LOGIN_S, "root", "admin"),
                                 new SymbolInstance<>(LOGOUT),
                                 new SymbolInstance<>(REGISTER_S, "user", "pwd"),
                                 new SymbolInstance<>(LOGIN_S, "user", "pwd"),
                                 new SymbolInstance<>(LOGOUT),
                                 new SymbolInstance<>(LOGIN_S, "user", "pdw"),
                                 new SymbolInstance<>(LOGIN_S, "user", "pwd"));
        Assert.assertNotNull(acceptor.getState(input));
        Assert.assertTrue(acceptor.accepts(input));
    }

    public interface RACreator<L, I extends ParameterizedSymbol, T extends GuardedTransition> {

        MutableRegisterAutomaton<L, I, T> create(Alphabet<I> alphabet,
                                                 RegisterValuation initialRegisters,
                                                 Constants constants);
    }

}
