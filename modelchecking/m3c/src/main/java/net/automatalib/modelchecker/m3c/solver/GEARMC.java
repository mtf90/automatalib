package net.automatalib.modelchecker.m3c.solver;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.metaframe.gear.game.GameGraphNode;
import de.metaframe.gear.game.strategies.Strategy;
import de.metaframe.gear.game.strategies.WinningStrategies;
import net.automatalib.graph.ContextFreeModalProcessSystem;
import net.automatalib.graph.ProceduralModalProcessGraph;
import net.automatalib.modelchecker.m3c.formula.DependencyGraph;
import net.automatalib.modelchecker.m3c.formula.FormulaNode;
import net.automatalib.modelchecker.m3c.solver.AbstractDDSolver.WorkUnit;
import net.automatalib.visualization.Visualization;

public class GEARMC<L, AP> {

    private final ContextFreeModalProcessSystem<L, AP> model;
    private final Map<L, AbstractDDSolver<?, L, AP>.WorkUnit<?, ?>> units;
    private final DependencyGraph<L, AP> dg;
    private final BitSet initialContext;

    GEARMC(ContextFreeModalProcessSystem<L, AP> model,
           Map<L, AbstractDDSolver<?, L, AP>.WorkUnit<?, ?>> units,
           DependencyGraph<L, AP> dg,
           BitSet initialContext) {
        this.model = model;
        this.units = units;
        this.dg = dg;
        this.initialContext = initialContext;
    }

    public List<L> findWitness(FormulaNode<L, AP> formula) {
        return findWitness(formula, model.getMainProcess());
    }

    public List<L> findWitness(FormulaNode<L, AP> formula, L label) {
        AbstractDDSolver<?, L, AP>.WorkUnit<?, ?> unit = units.get(label);
        return findWitness(unit, formula);
    }

    private <N, E> List<L> findWitness(AbstractDDSolver<?, L, AP>.WorkUnit<N, E> unit,
                                       FormulaNode<L, AP> formula) {

        G4m3Graph<N, L, E, AP> graph = new G4m3Graph<>(unit.pmpg, formula, dg, unit, this.units, this.initialContext);
        WinningStrategies<N, E> winningStrategies = new WinningStrategies<>(graph);

        List<L> result = findWitnessPath(graph, unit, winningStrategies, unit.pmpg.getInitialNode());

//        Visualization.visualize(graph);

        return result;
    }

    private <N, E> List<L> findWitnessPath(G4m3Graph<N, L, E, AP> gamegraph,
                                           AbstractDDSolver<?, L, AP>.WorkUnit<N, E> unit,
                                           WinningStrategies<N, E> winningStrategies,
                                           N modelnode) {

        List<L> path = new ArrayList<>();

        if (winningStrategies == null) {
            throw new IllegalStateException("There is no strategy present. You need to perform model checking first.");
        }
        if (modelnode == null) {throw new IllegalArgumentException("I need a model node to start from.");}

        // return an empy list if the clicked node is not part of an error path
        if (!winningStrategies.getOrPlayerStrategy().getDef().contains(gamegraph.getGameGraphNode(modelnode))) {
            return new ArrayList<>();
        }

        // creating result list
        List<GameGraphNode<N>> ggNodes = new ArrayList<GameGraphNode<N>>();

        // init the node we are starting from
        GameGraphNode<N> ggNode = gamegraph.getGameGraphNode(modelnode);
        if (ggNode.isAndPlayer()) {return null;}

        ggNodes.add(ggNode);

        // fetch strategy we are walking, i.e. a losing strategy for error paths
        Strategy<N> losingStrategy = winningStrategies.getOrPlayerStrategy();

        // iterating nodes if they are present or if we havent seen them yet
        int c = 0;
        List<N> result = new ArrayList<N>();
        result.add(ggNode.getModelNode());
        while (!ggNode.isAndPlayer() && losingStrategy.get(ggNode) != null &&
               !ggNodes.contains(losingStrategy.get(ggNode))) {

            ggNode = losingStrategy.get(ggNode);
            ggNodes.add(ggNode);

            // putting into list if model node is not null and not a double entry
            if (ggNode.getModelNode() != null && ggNode.getModelNode() != result.get(result.size() - 1)) {
                result.add(ggNode.getModelNode());
            }

            if (ggNodes.size() > 1) {
//                String label = gamegraph.getLabelBetween(ggNodes.get(ggNodes.size() - 2), ggNode);
//                if (label != null) {
//                    path.add(label);
//                }
                GameGraphNode<N> ggPrev = ggNodes.get(ggNodes.size() - 2);
                Set<E> edges = gamegraph.getEdgesBetween(ggPrev, ggNode);
                if (edges != null && !edges.isEmpty()) {
                    E edge = edges.iterator().next();
                    if (unit.pmpg.getEdgeProperty(edge).isProcess()) {
                        BitSet tgtContext = unit.propTransformers.get(result.get(result.size() - 1)).evaluate(dg.toBoolArray(initialContext));
                        GEARMC<L, AP> mc = new GEARMC<>(model, units, dg, tgtContext);
                        List<L> sub = mc.findWitness(gamegraph.getFormula(ggPrev), unit.pmpg.getEdgeLabel(edge));
                        path.addAll(sub);
                    } else {
                        path.add(unit.pmpg.getEdgeLabel(edge));
                    }
                }
            }

            if (c++ > 10000) {
                System.err.println("Cycling too long through strategies");
                break;
            }
        }

        System.out.println();
        System.out.println(ggNodes.stream().map(Object::toString).collect(Collectors.joining("\n")));
        System.out.println();
        System.out.println(path);
        return path;
    }

    public static <N, L, E, AP> List<N> findErrorPath(G4m3Graph<N, L, E, AP> gamegraph,
                                               WinningStrategies<N, E> winningStrategies,
                                               N modelnode) {

        List<String> path = new ArrayList<>();

        if (winningStrategies == null) {
            throw new IllegalStateException("There is no strategy present. You need to perform model checking first.");
        }
        if (modelnode == null) {throw new IllegalArgumentException("I need a model node to start from.");}

        // return an empy list if the clicked node is not part of an error path
        if (!winningStrategies.getAndPlayerStrategy().getDef().contains(gamegraph.getGameGraphNode(modelnode))) {
            return new ArrayList<N>();
        }

        // creating result list
        List<GameGraphNode<N>> ggNodes = new ArrayList<GameGraphNode<N>>();

        // init the node we are starting from
        GameGraphNode<N> ggNode = gamegraph.getGameGraphNode(modelnode);
        if (!ggNode.isAndPlayer()) {return null;}

        ggNodes.add(ggNode);

        // fetch strategy we are walking, i.e. a losing strategy for error paths
        Strategy<N> losingStrategy = winningStrategies.getAndPlayerStrategy();

        // iterating nodes if they are present or if we havent seen them yet
        int c = 0;
        List<N> result = new ArrayList<N>();
        result.add(ggNode.getModelNode());
        while (ggNode.isAndPlayer() && losingStrategy.get(ggNode) != null &&
               !ggNodes.contains(losingStrategy.get(ggNode))) {

            ggNode = losingStrategy.get(ggNode);
            ggNodes.add(ggNode);

            // putting into list if model node is not null and not a double entry
            if (ggNode.getModelNode() != null && ggNode.getModelNode() != result.get(result.size() - 1)) {
                result.add(ggNode.getModelNode());
            }

            if (ggNodes.size() > 1) {
                String label = gamegraph.getLabelBetween(ggNodes.get(ggNodes.size() - 2), ggNode);
                if (label != null) {
                    path.add(label);
                }
            }

            if (c++ > 10000) {
                System.err.println("Cycling too long through strategies");
                break;
            }
        }

        System.out.println();
        System.out.println(ggNodes.stream().map(Object::toString).collect(Collectors.joining("\n")));
        System.out.println();
        System.out.println(path);
        return result;
    }
}
