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
import net.automatalib.graph.impl.CompactPMPG;
import net.automatalib.graph.impl.DefaultCFMPS;
import net.automatalib.modelchecker.m3c.formula.FormulaNode;
import net.automatalib.modelchecker.m3c.formula.parser.M3CParser;
import net.automatalib.modelchecker.m3c.solver.BDDSolver;
import net.automatalib.ts.modal.transition.ModalEdgeProperty.ModalType;
import net.automatalib.ts.modal.transition.ProceduralModalEdgeProperty.ProceduralType;
import net.automatalib.ts.modal.transition.impl.ProceduralModalEdgePropertyImpl;
import net.automatalib.util.automaton.fsa.MutableDFAs;
import net.automatalib.util.automaton.procedural.SBAs;
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
//        final ContextFreeModalProcessSystem<Character, Void> cfmps = SBAs.toCFMPS(sba);
        final ContextFreeModalProcessSystem<Character, Void> cfmps = createCFMPS();
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
        List<?> gearCE = solver.findGEARWitness(cfmps, alphabet, formula);
        after = System.currentTimeMillis();

        System.out.println("Duration: " + (after - before) / 1000f);

        if (gearCE != null) {
            System.out.println(gearCE);
        }

    }

    private static FormulaNode<Character, Void> createFormula(int length) throws FormatException {
        final StringBuilder sb = new StringBuilder();

        sb.append("EF");
        sb.append("<P><P>");
        for (int i = 0; i < length; i++) {
            sb.append("<R>");
        }
        sb.append("true");

        // use negated formula because witness <-> counterexample to negation
        return M3CParser.parse(sb.toString(), l -> l.charAt(0), ap -> null);
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

    private static ContextFreeModalProcessSystem<Character, Void> createCFMPS() {

        final CompactPMPG<Character, Void> pmpg = new CompactPMPG<>('-');

        Integer init = pmpg.addNode();
        Integer n0 = pmpg.addNode();
        Integer end = pmpg.addNode();

        pmpg.setEdgeLabel(pmpg.connect(init,
                                       n0,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.INTERNAL, ModalType.MUST)),
                          'P');
        pmpg.setEdgeLabel(pmpg.connect(n0,
                                       n0,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.INTERNAL, ModalType.MUST)),
                          'a');
        pmpg.setEdgeLabel(pmpg.connect(n0,
                                       n0,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.INTERNAL, ModalType.MUST)),
                          'b');
        pmpg.setEdgeLabel(pmpg.connect(n0,
                                       n0,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.INTERNAL, ModalType.MUST)),
                          'c');
        pmpg.setEdgeLabel(pmpg.connect(n0,
                                       n0,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.PROCESS, ModalType.MUST)),
                          'Q');
        pmpg.setEdgeLabel(pmpg.connect(n0,
                                       end,
                                       new ProceduralModalEdgePropertyImpl(ProceduralType.INTERNAL, ModalType.MUST)),
                          'R');

        pmpg.setInitialNode(init);
        pmpg.setFinalNode(end);

        return new DefaultCFMPS<>('Q', Collections.singletonMap('Q', pmpg));

    }

}
