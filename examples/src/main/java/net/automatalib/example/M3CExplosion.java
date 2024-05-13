package net.automatalib.example;

import java.util.Collections;
import java.util.List;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.DefaultProceduralInputAlphabet;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.procedural.SBA;
import net.automatalib.automaton.procedural.impl.StackSBA;
import net.automatalib.exception.FormatException;
import net.automatalib.graph.ContextFreeModalProcessSystem;
import net.automatalib.modelchecker.m3c.formula.FormulaNode;
import net.automatalib.modelchecker.m3c.formula.NotNode;
import net.automatalib.modelchecker.m3c.formula.parser.M3CParser;
import net.automatalib.modelchecker.m3c.solver.BDDSolver;
import net.automatalib.modelchecker.m3c.solver.WitnessTree;
import net.automatalib.util.automaton.fsa.MutableDFAs;
import net.automatalib.util.automaton.procedural.SBAs;
import net.automatalib.visualization.Visualization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class M3CExplosion {

    public static void main(String[] args) throws FormatException {

        final Alphabet<Character> internalAlphabet = Alphabets.characters('a', 'c');
        final Alphabet<Character> callAlphabet = Alphabets.singleton('P');
        final DefaultProceduralInputAlphabet<Character> alphabet =
                new DefaultProceduralInputAlphabet<>(internalAlphabet, callAlphabet, 'R');

        final int formulaLength = 2;

        final FormulaNode<Character, Void> formula = createFormula(formulaLength);
        final SBA<?, Character> sba = createSBA(alphabet);
        final ContextFreeModalProcessSystem<Character, Void> cfmps = SBAs.toCFMPS(sba);
        final BDDSolver<Character, Void> solver = new BDDSolver<>(cfmps);

        System.out.println(formula);
//        Visualization.visualize(cfmps);

        long before, after;

        // BOOM
//        before = System.currentTimeMillis();
//        WitnessTree<Character, Void> ce = solver.findCounterExample(cfmps, alphabet, formula);
//        after = System.currentTimeMillis();
//
//        System.out.println("Duration: " + (after - before) / 1000f);
//
//        if (ce != null) {
//            System.out.println(ce.size());
//            System.out.println(ce.getWitness());
//        }

        before = System.currentTimeMillis();
        List<?> gearCE = solver.findGEARCounterExample(cfmps, alphabet, formula);
        after = System.currentTimeMillis();

        System.out.println("Duration: " + (after - before) / 1000f);

        if (gearCE != null) {
            System.out.println(gearCE);
        }

    }

    private static FormulaNode<Character, Void> createFormula(int length) throws FormatException {
        final StringBuilder sb = new StringBuilder();

        sb.append("EF");
        sb.append("<P>");
        for (int i = 0; i < length; i++) {
            sb.append("<R>");
        }
        sb.append("true");

        // use negated formula because witness <-> counterexample to negation
        return new NotNode<>(M3CParser.parse(sb.toString(), l -> l.charAt(0), ap -> null));
    }

    private static @NonNull SBA<?, Character> createSBA(ProceduralInputAlphabet<Character> alphabet) {
        final CompactDFA<Character> dfa = new CompactDFA<>(alphabet);

        Integer init = dfa.addInitialState(true);
        Integer ret = dfa.addState(true);

        for (int i = 0; i < alphabet.getNumInternals(); i++) {
            dfa.addTransition(init, alphabet.getInternalSymbol(i), init);
        }

        dfa.addTransition(init, 'P', init);
        dfa.addTransition(init, 'R', ret);

        MutableDFAs.complete(dfa, alphabet);

        return new StackSBA<>(alphabet, 'P', Collections.singletonMap('P', dfa));
    }

}
