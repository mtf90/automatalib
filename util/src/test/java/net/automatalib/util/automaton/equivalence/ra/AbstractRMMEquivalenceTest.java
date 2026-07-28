package net.automatalib.util.automaton.equivalence.ra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.automaton.ra.impl.CompactRMM;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.FreshValueGenerator;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.data.VarMapping.GeneratorMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class AbstractRMMEquivalenceTest {

    private EquivalenceChecker<InputSymbol, OutputSymbol> checker;
    private ConstraintSolver solver;

    protected abstract EquivalenceChecker<InputSymbol, OutputSymbol> getChecker();

    @BeforeClass
    public void setUp() {
        this.checker = getChecker();
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @DataProvider
    public static Object[] equivConfigs() {

        final List<CompactRMM<InputSymbol, OutputSymbol>> rmm = Arrays.asList(ExampleLogin.buildEqualTransducer(),
                                                                              ExampleLogin.buildIsomorphicRegisterTransducer(),
                                                                              ExampleLogin.buildDifferentOutputParameterTransducer(),
                                                                              ExampleLogin.buildDifferentOutputSymbolTransducer(),
                                                                              ExampleLogin.buildInitialValuationTransducer());

        final List<Configuration> configs = new ArrayList<>();

        for (CompactRMM<InputSymbol, OutputSymbol> r : rmm) {
            configs.add(new Configuration(r));
        }

        configs.add(new Configuration(ExampleLogin.buildEqualTransducer(),
                                      ExampleLogin.buildIsomorphicRegisterTransducer()));

        return configs.toArray();
    }

    @DataProvider
    public static Object[] inEquivConfigs() {

        final List<CompactRMM<InputSymbol, OutputSymbol>> rmm = Arrays.asList(ExampleLogin.buildEqualTransducer(),
                                                                              ExampleLogin.buildDifferentOutputParameterTransducer(),
                                                                              ExampleLogin.buildDifferentOutputSymbolTransducer(),
                                                                              ExampleLogin.buildInitialValuationTransducer());

        final List<Configuration> configs = new ArrayList<>();

        for (int i = 0; i < rmm.size(); i++) {
            for (int j = i + 1; j < rmm.size(); j++) {
                configs.add(new Configuration(rmm.get(i), rmm.get(j)));
            }
        }

        return configs.toArray();
    }

    @Test(dataProvider = "equivConfigs")
    public void testEquivalence(Configuration config) {
        Assert.assertNull(checker.findSeparatingWord(config.right, config.left, config.inputs, solver),
                          config.toString());
        Assert.assertNull(checker.findSeparatingWord(config.left, config.right, config.inputs, solver),
                          config.toString());
    }

    @Test(dataProvider = "inEquivConfigs")
    public void testInEquivalence(Configuration config) {
        Word<SymbolInstance<InputSymbol>> ce =
                checker.findSeparatingWord(config.right, config.left, config.inputs, solver);

        FreshValueGenerator<String> cookieGenerator = new CookieGenerator();
        GeneratorMapping generators = new GeneratorMapping();
        generators.put(ExampleLogin.T_MSG, cookieGenerator);

        Assert.assertNotNull(ce);
        Assert.assertNotEquals(config.right.getSemantics(generators).computeOutput(ce),
                               config.left.getSemantics(generators).computeOutput(ce),
                               config.toString());

        ce = checker.findSeparatingWord(config.left, config.right, config.inputs, solver);

        Assert.assertNotNull(ce);
        Assert.assertNotEquals(config.right.getSemantics(generators).computeOutput(ce),
                               config.left.getSemantics(generators).computeOutput(ce),
                               config.toString());
    }

    @Test
    public void testFlippedOutputParameters() {

        final DataType<String> type = new DataType<>("text", BuiltinTypes.STRING);
        final InputSymbol in = new InputSymbol("in", type, type);
        final OutputSymbol out = new OutputSymbol("out", type, type);
        final Alphabet<InputSymbol> alphabet = Alphabets.singleton(in);

        // registers and parameters
        ParameterGenerator inGen = new ParameterGenerator();
        Parameter<String> i1 = inGen.next(type);
        Parameter<String> i2 = inGen.next(type);
        ParameterGenerator outGen = new ParameterGenerator();
        Parameter<String> o1 = outGen.next(type);
        Parameter<String> o2 = outGen.next(type);

        CompactRMM<InputSymbol, OutputSymbol> rmm1 = new CompactRMM<>(alphabet);
        CompactRMM<InputSymbol, OutputSymbol> rmm2 = new CompactRMM<>(alphabet);

        // guards
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        {
            // locations
            Integer l0 = rmm1.addInitialState();

            // assignments
            VarMapping<Register<?>, SymbolicDataValue<?>> emptyMapping = new VarMapping<>();
            VarMapping<Parameter<?>, SymbolicDataValue<?>> outputMapping = new VarMapping<>(o1, i1, o2, i2);

            OutputAssignment passThroughAssign = new OutputAssignment(emptyMapping, outputMapping);

            // initial location
            rmm1.addTransition(l0, in, rmm1.createTransition(l0, trueGuard, passThroughAssign, out));
        }
        {
            // locations
            Integer l0 = rmm2.addInitialState();

            // assignments
            VarMapping<Register<?>, SymbolicDataValue<?>> emptyMapping = new VarMapping<>();
            VarMapping<Parameter<?>, SymbolicDataValue<?>> outputMapping = new VarMapping<>(o1, i2, o2, i1);

            OutputAssignment passThroughAssign = new OutputAssignment(emptyMapping, outputMapping);

            // initial location
            rmm2.addTransition(l0, in, rmm2.createTransition(l0, trueGuard, passThroughAssign, out));
        }

        Word<SymbolInstance<InputSymbol>> w = Word.fromLetter(in.instantiate("t1", "t2"));
        Assert.assertNotEquals(rmm1.getSemantics().computeOutput(w), rmm2.getSemantics().computeOutput(w));

        Word<SymbolInstance<InputSymbol>> ce = checker.findSeparatingWord(rmm1, rmm2, alphabet, solver);
        Assert.assertNotNull(ce);
        Assert.assertNotEquals(rmm1.getSemantics().computeOutput(ce), rmm2.getSemantics().computeOutput(ce));
    }

    private static class CookieGenerator implements FreshValueGenerator<String> {

        @Override
        public DataValue<String> getFreshValue(Collection<DataValue<String>> vals) {
            return new DataValue<>(ExampleLogin.T_MSG, "JSESSIONID=" + vals.size());
        }

        @Override
        public DataType<String> getDataType() {
            return ExampleLogin.T_MSG;
        }
    }

    public interface EquivalenceChecker<I extends ParameterizedSymbol, O extends ParameterizedSymbol> {

        Word<SymbolInstance<I>> findSeparatingWord(RegisterMealyMachine<?, I, ?, O> hyp,
                                                   RegisterMealyMachine<?, I, ?, O> model,
                                                   Collection<? extends I> inputs,
                                                   ConstraintSolver solver);
    }

    public record Configuration(RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol> left,
                                RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol> right,
                                Collection<? extends InputSymbol> inputs) {

        public Configuration(CompactRMM<InputSymbol, OutputSymbol> rmm) {
            this(rmm, rmm, rmm.getInputAlphabet());
        }

        public Configuration(CompactRMM<InputSymbol, OutputSymbol> left, CompactRMM<InputSymbol, OutputSymbol> right) {
            this(left, right, left.getInputAlphabet());
            Assert.assertEquals(left.getInputAlphabet(), right.getInputAlphabet());
        }

        @Override
        public String toString() {
            return left + " vs. " + right;
        }
    }
}
