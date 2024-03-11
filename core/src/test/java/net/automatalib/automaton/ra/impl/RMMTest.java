package net.automatalib.automaton.ra.impl;

import java.util.Collection;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.concept.Output;
import net.automatalib.automaton.ra.GuardedOutputTransition;
import net.automatalib.automaton.ra.MutableRegisterMealyMachine;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.FreshValueGenerator;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.FreshOutput;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ConstantGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.FreshOutputGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.data.VarMapping.GeneratorMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class RMMTest<L, T extends GuardedOutputTransition> {

    public static final DataType<Integer> T_UID = new DataType<>("T_uid", BuiltinTypes.SINT32);
    public static final DataType<Integer> T_PWD = new DataType<>("T_pwd", BuiltinTypes.SINT32);
    public static final DataType<String> T_MSG = new DataType<>("T_msg", BuiltinTypes.STRING);

    public static final InputSymbol I_REGISTER = new InputSymbol("register", T_UID, T_PWD);
    public static final InputSymbol I_LOGIN = new InputSymbol("login", T_UID, T_PWD);
    public static final InputSymbol I_LOGOUT = new InputSymbol("logout");
    public static final OutputSymbol O_OK = new OutputSymbol("ok", T_MSG);
    public static final OutputSymbol O_ERR = new OutputSymbol("err", T_MSG);
    public static final OutputSymbol O_SUCCESS = new OutputSymbol("success", T_UID);
    public static final OutputSymbol O_COOKIE = new OutputSymbol("cookie", T_MSG);

    private final RMMCreator<L, InputSymbol, T, OutputSymbol> creator;

    @Factory(dataProvider = "creators")
    public RMMTest(RMMCreator<L, InputSymbol, T, OutputSymbol> creator) {
        this.creator = creator;
    }

    @DataProvider(name = "creators")
    public static Object[] creators() {
        final RMMCreator<Integer, ? extends ParameterizedSymbol, ? extends GuardedOutputTransition, ? extends ParameterizedSymbol>
                compact =
                (RMMCreator<Integer, ParameterizedSymbol, CompactRMMTransition<ParameterizedSymbol>, ParameterizedSymbol>) CompactRMM::new;
        return new RMMCreator<?, ?, ?, ?>[] {compact};
    }

    @Test
    public void testTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(I_REGISTER, I_LOGIN, I_LOGOUT);

        FreshValueGenerator<String> cookieGenerator = new CookieGenerator();
        GeneratorMapping generators = new GeneratorMapping();
        generators.put(T_MSG, cookieGenerator);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        Register<String> rCookie = rgen.next(T_MSG);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen0 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen0.next(T_UID);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<String> pCookie = pgen2.next(T_MSG);
        ParameterGenerator pgen3 = new ParameterGenerator();
        Parameter<String> pErr = pgen3.next(T_MSG);
        ParameterGenerator pgen4 = new ParameterGenerator();
        Parameter<String> pLogout = pgen4.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);
        FreshOutputGenerator fgen = new FreshOutputGenerator();
        FreshOutput<String> fCookie = fgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));

        MutableRegisterMealyMachine<L, InputSymbol, T, OutputSymbol> rmm =
                creator.create(alphabet, new RegisterValuation(), constants);

        // locations
        L l0 = rmm.addInitialState();
        L l1 = rmm.addState();
        L l2 = rmm.addState();

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyRegisters = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> registerRegisters = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> loginRegisters =
                new VarMapping<>(rUid, pUid, rPwd, pPwd, rCookie, fCookie);

        VarMapping<Parameter<?>, SymbolicDataValue<?>> successOutput = new VarMapping<>(pSuccess, pUid); //###
        VarMapping<Parameter<?>, SymbolicDataValue<?>> cookieOutput = new VarMapping<>(pCookie, fCookie);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> logoutOutput = new VarMapping<>(pLogout, rCookie);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorOutput = new VarMapping<>(pErr, cErr);

        OutputAssignment registerAssign = new OutputAssignment(registerRegisters, successOutput);
        OutputAssignment loginSuccessAssign = new OutputAssignment(loginRegisters, cookieOutput);
        OutputAssignment loginErrorAssign = new OutputAssignment(copyRegisters, errorOutput);
        OutputAssignment logoutAssign = new OutputAssignment(copyRegisters, logoutOutput);

        // initial location
        rmm.addTransition(l0, I_REGISTER, rmm.createTransition(l1, trueGuard, registerAssign, O_SUCCESS));

        // reg. location
        rmm.addTransition(l1, I_LOGIN, rmm.createTransition(l2, condition, loginSuccessAssign, O_COOKIE));
        rmm.addTransition(l1, I_LOGIN, rmm.createTransition(l1, elseCond, loginErrorAssign, O_ERR));

        // login location
        rmm.addTransition(l2, I_LOGOUT, rmm.createTransition(l1, trueGuard, logoutAssign, O_OK));

        Output<SymbolInstance<InputSymbol>, Word<SymbolInstance<OutputSymbol>>> transducer = rmm.asTransducer(generators);

        Word<SymbolInstance<InputSymbol>> input = Word.fromSymbols(new SymbolInstance<>(I_REGISTER, 42, 1),
                                                                   new SymbolInstance<>(I_LOGIN, 42, 1),
                                                                   new SymbolInstance<>(I_LOGOUT),
                                                                   new SymbolInstance<>(I_LOGIN, 42, 1),
                                                                   new SymbolInstance<>(I_LOGOUT));
        Assert.assertEquals(transducer.computeOutput(input),
                            Word.fromSymbols(O_SUCCESS.instantiate(42),
                                             O_COOKIE.instantiate("JSESSIONID=0"),
                                             O_OK.instantiate("JSESSIONID=0"),
                                             O_COOKIE.instantiate("JSESSIONID=1"),
                                             O_OK.instantiate("JSESSIONID=1")));

        input = Word.fromSymbols(new SymbolInstance<>(I_REGISTER, 0, 1), new SymbolInstance<>(I_LOGIN, 0, 0));
        Assert.assertEquals(transducer.computeOutput(input),
                            Word.fromSymbols(new SymbolInstance<>(O_SUCCESS, 0), new SymbolInstance<>(O_ERR, "error")));
    }

    public interface RMMCreator<L, I extends ParameterizedSymbol, T extends GuardedOutputTransition, O extends ParameterizedSymbol> {

        MutableRegisterMealyMachine<L, I, T, O> create(Alphabet<I> alphabet,
                                                       RegisterValuation initialRegisters,
                                                       Constants constants);
    }

    private static class CookieGenerator implements FreshValueGenerator<String> {

        @Override
        public DataValue<String> getFreshValue(Collection<DataValue<String>> vals) {
            return new DataValue<>(T_MSG, "JSESSIONID=" + vals.size());
        }

        @Override
        public DataType<String> getDataType() {
            return T_MSG;
        }
    }

}
