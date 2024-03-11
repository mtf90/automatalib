package net.automatalib.util.automaton.equivalence.ra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.util.automaton.equivalence.RegisterAutomata;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class AbstractRAEquivalenceTest {

    private EquivalenceChecker<InputSymbol> checker;
    private ConstraintSolver solver;

    protected abstract EquivalenceChecker<InputSymbol> getChecker();

    @BeforeClass
    public void setUp() {
        this.checker = getChecker();
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @DataProvider
    public static Object[] equivConfigs() {

        final List<CompactRA<InputSymbol>> ra = Arrays.asList(ExampleLogin.buildEqualAutomaton(),
                                                              ExampleLogin.buildBijectiveRegistersAutomaton(),
                                                              ExampleLogin.buildEquivalentGuardsAutomaton());
        final List<CompactRA<InputSymbol>> rio =
                Arrays.asList(ExampleMixedIO.buildAutomaton(), ExampleMixedIO.buildEquivAutomaton());

        final List<Configuration> configs = new ArrayList<>();

        // identity
        configs.add(new Configuration(ExampleLogin.buildLessThanAutomaton()));
        configs.add(new Configuration(ExampleLogin.buildSumAutomaton()));
        configs.add(new Configuration(ExampleLogin.buildResetAutomaton()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildNoopAutomaton()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildLockAutomaton()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildTransitiveConstraintAutomaton()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildDiamondAccept()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildDiamondReject()));
        configs.add(new Configuration(ExampleAsymmetricPaths.buildUpdateNotInGuardsAutomaton1(),
                                      ExampleAsymmetricPaths.buildUpdateNotInGuardsAutomaton2()));

        for (CompactRA<InputSymbol> r : ra) {
            configs.add(new Configuration(r));
        }
        for (CompactRA<InputSymbol> r : rio) {
            configs.add(new Configuration(r));
        }

        // cross-validation
        for (int i = 0; i < ra.size(); i++) {
            for (int j = i + 1; j < ra.size(); j++) {
                configs.add(new Configuration(ra.get(i), ra.get(j)));
            }
        }
        for (int i = 0; i < rio.size(); i++) {
            for (int j = i + 1; j < rio.size(); j++) {
                configs.add(new Configuration(rio.get(i), rio.get(j)));
            }
        }

        return configs.toArray();
    }

    @DataProvider
    public static Object[] inEquivConfigs() {

        final List<CompactRA<InputSymbol>> ra = Arrays.asList(ExampleLogin.buildEqualAutomaton(),
                                                              ExampleLogin.buildTotalAutomaton(),
                                                              ExampleLogin.buildOverridingAutomaton(),
                                                              ExampleLogin.buildLessThanAutomaton(),
                                                              ExampleLogin.buildSinkAutomaton());
        final List<Configuration> configs = new ArrayList<>();

        for (int i = 0; i < ra.size(); i++) {
            for (int j = i + 1; j < ra.size(); j++) {
                configs.add(new Configuration(ra.get(i), ra.get(j)));
            }
        }

        configs.add(new Configuration(ExampleLogin.buildStringAutomaton(),
                                      ExampleLogin.buildStringInclusionAutomaton()));

        configs.add(new Configuration(ExampleLogin.buildEqualAutomaton(), ExampleLogin.buildSumAutomaton()));

        configs.add(new Configuration(ExampleAsymmetricPaths.buildNoopAutomaton(),
                                      ExampleAsymmetricPaths.buildLockAutomaton()));

        configs.add(new Configuration(ExampleAsymmetricPaths.buildDiamondAccept(),
                                      ExampleAsymmetricPaths.buildDiamondReject()));

        configs.add(new Configuration(ExampleAsymmetricPaths.buildNoopAutomaton(),
                                      ExampleAsymmetricPaths.buildTransitiveConstraintAutomaton()));

        return configs.toArray();
    }

    @Test(dataProvider = "equivConfigs")
    public void testEquivalence(Configuration config) {
        Assert.assertNull(checker.findSeparatingWord(config.right, config.left, config.inputs, solver),
                          config.toString());
        Assert.assertNull(checker.findSeparatingWord(config.left, config.right, config.inputs, solver),
                          config.toString());
    }

    @Test(dataProvider = "inEquivConfigs", enabled = false)
    public void testInEquivalence(Configuration config) {
        Word<SymbolInstance<InputSymbol>> ce =
                checker.findSeparatingWord(config.right, config.left, config.inputs, solver);

        Assert.assertNotNull(ce);
        Assert.assertNotEquals(config.right.asAcceptor().accepts(ce),
                               config.left.asAcceptor().accepts(ce),
                               config.toString());

        ce = checker.findSeparatingWord(config.left, config.right, config.inputs, solver);

        Assert.assertNotNull(ce);
        Assert.assertNotEquals(config.right.asAcceptor().accepts(ce),
                               config.left.asAcceptor().accepts(ce),
                               config.toString());
    }

    public interface EquivalenceChecker<I extends ParameterizedSymbol> {

        Word<SymbolInstance<I>> findSeparatingWord(RegisterAutomaton<?, I, ?> hyp,
                                                   RegisterAutomaton<?, I, ?> model,
                                                   Collection<? extends I> inputs,
                                                   ConstraintSolver solver);
    }

    public record Configuration(RegisterAutomaton<?, InputSymbol, ?> left, RegisterAutomaton<?, InputSymbol, ?> right,
                                Collection<? extends InputSymbol> inputs) {

        public Configuration(CompactRA<InputSymbol> ra) {
            this(ra, ra, ra.getInputAlphabet());
        }

        public Configuration(CompactRA<InputSymbol> left, CompactRA<InputSymbol> right) {
            this(left, right, left.getInputAlphabet());
            Assert.assertEquals(left.getInputAlphabet(), right.getInputAlphabet());
        }

        public Configuration {
            Assert.assertTrue(RegisterAutomata.hasDisjunctGuards(left, inputs), left.toString());
            Assert.assertTrue(RegisterAutomata.hasDisjunctGuards(right, inputs), right.toString());
            Assert.assertTrue(RegisterAutomata.hasConsistentAssignments(left, inputs), left.toString());
            Assert.assertTrue(RegisterAutomata.hasConsistentAssignments(right, inputs), right.toString());
        }

        @Override
        public String toString() {
            return left + " vs. " + right;
        }
    }
}
