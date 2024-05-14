package net.automatalib.modelchecker.m3c.solver;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.metaframe.gear.game.GameGraphNode;
import de.metaframe.gear.game.strategies.Strategy;
import de.metaframe.gear.game.strategies.WinningStrategies;
import net.automatalib.graph.ContextFreeModalProcessSystem;
import net.automatalib.modelchecker.m3c.formula.DependencyGraph;
import net.automatalib.modelchecker.m3c.formula.FormulaNode;

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

    public List<?> findCounterexample(FormulaNode<L, AP> formula) {

        L mainLabel = model.getMainProcess();
        AbstractDDSolver<?, L, AP>.WorkUnit<?, ?> unit = units.get(mainLabel);

        return findCounterexample(unit, formula);
    }

    private <N, E> List<N> findCounterexample(AbstractDDSolver<?, L, AP>.WorkUnit<N, E> unit,
                                              FormulaNode<L, AP> formula) {

        G4m3Graph<N, L, E, AP> graph = new G4m3Graph<>(unit.pmpg, formula, dg, unit, this.units, this.initialContext);
        WinningStrategies<N, E> winningStrategies = new WinningStrategies<>(graph);

        List<N> result = findErrorPath(graph, winningStrategies, graph.getInitialNodes().iterator().next().getModelNode());

//        Visualization.visualize(graph);

        return result;
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
