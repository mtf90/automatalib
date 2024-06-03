package net.automatalib.modelchecker.m3c.solver;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.metaframe.gear.game.GameGraph;
import de.metaframe.gear.game.GameGraphNode;
import de.metaframe.gear.game.GameGraphNode.Type;
import de.metaframe.gear.model.Edge;
import de.metaframe.gear.model.Model;
import de.metaframe.gear.model.Model.EdgeType;
import de.metaframe.gear.sexp.SymbolicExpression;
import net.automatalib.common.util.Pair;
import net.automatalib.graph.Graph;
import net.automatalib.graph.ProceduralModalProcessGraph;
import net.automatalib.graph.concept.GraphViewable;
import net.automatalib.modelchecker.m3c.formula.AbstractBinaryFormulaNode;
import net.automatalib.modelchecker.m3c.formula.AbstractModalFormulaNode;
import net.automatalib.modelchecker.m3c.formula.AbstractUnaryFormulaNode;
import net.automatalib.modelchecker.m3c.formula.AtomicNode;
import net.automatalib.modelchecker.m3c.formula.DependencyGraph;
import net.automatalib.modelchecker.m3c.formula.DiamondNode;
import net.automatalib.modelchecker.m3c.formula.FalseNode;
import net.automatalib.modelchecker.m3c.formula.FormulaNode;
import net.automatalib.modelchecker.m3c.formula.OrNode;
import net.automatalib.modelchecker.m3c.formula.modalmu.AbstractFixedPointFormulaNode;
import net.automatalib.modelchecker.m3c.formula.modalmu.GfpNode;
import net.automatalib.modelchecker.m3c.formula.modalmu.LfpNode;
import net.automatalib.modelchecker.m3c.formula.modalmu.VariableNode;
import net.automatalib.ts.modal.transition.ProceduralModalEdgeProperty;
import net.automatalib.visualization.DefaultVisualizationHelper;
import net.automatalib.visualization.VisualizationHelper;
import org.checkerframework.checker.units.qual.N;

public class G4m3Graph<N, L, E, AP> extends GameGraph<N, E> implements GraphViewable {

    private final ProceduralModalProcessGraph<N, L, E, AP, ?> model;

    private final DependencyGraph<L, AP> dg;
    private final AbstractDDSolver<?, L, AP>.WorkUnit<N, E> unit;
    private final Map<L, AbstractDDSolver<?, L, AP>.WorkUnit<?, ?>> units;
    private final BitSet context;

    private final FormulaNode<L, AP> root;
    // a node cache that allows for fast retrieval of game graph nodes for given node/expression
    private final Map<N, Map<FormulaNode<L, AP>, GameGraphNode<N>>> ggnodecache = new HashMap<>();

    // forward- and back-edges
    private final Map<GameGraphNode<N>, Set<GameGraphNode<N>>> succs = new HashMap<>();
    private final Map<GameGraphNode<N>, Set<GameGraphNode<N>>> preds = new HashMap<>();

    // mapping from atomic fix point variable expressions to fix point expression they are bound to
    private final Map<String, FormulaNode<L, AP>> fixvar2fix = new HashMap<>();
    private Map<FormulaNode<L, AP>, Integer> exp2prio = new HashMap<>();
    private Map<Pair<GameGraphNode<N>, GameGraphNode<N>>, Set<E>> labels = new HashMap<>();

    G4m3Graph(ProceduralModalProcessGraph<N, L, E, AP, ?> model,
              FormulaNode<L, AP> formulaNode,
              DependencyGraph<L, AP> dg,
              AbstractDDSolver<?, L, AP>.WorkUnit<N, E> unit,
              Map<L, AbstractDDSolver<?, L, AP>.WorkUnit<?, ?>> units, BitSet context) {

        this.model = model;
        this.dg = dg;
        this.unit = unit;
        this.units = units;
        this.context = context;
        this.root = formulaNode;

        this.exp2prio = computeExpression2Prio(formulaNode);

//        Collection<FormulaNode<L, AP>> subformulas = new ArrayList<>(dg.getFormulaNodes());
//        subformulas.add(this.root);
        Collection<FormulaNode<L, AP>> subformulas = subformulas(formulaNode);

        for (FormulaNode<L, AP> formula : subformulas) {
            if (formula instanceof AbstractFixedPointFormulaNode) {
                AbstractFixedPointFormulaNode<L, AP> f = (AbstractFixedPointFormulaNode<L, AP>) formula;
                this.fixvar2fix.put(f.getVariable(), f);
            }
        }

        // init nodes
        for (N n : model) {
            final Map<FormulaNode<L, AP>, GameGraphNode<N>> map = new HashMap<>();
            for (FormulaNode<L, AP> f : subformulas) {
                GameGraphNode<N> ggnode = new GameGraphNode<>(new SymbolicExpression(f.toString()), n, this.exp2prio.get(f), getType(n, f));
                map.put(f, ggnode);
                this.succs.put(ggnode, new HashSet<>());
                this.preds.put(ggnode, new HashSet<>());
            }
            ggnodecache.put(n, map);
        }

        // init edges
        for (Entry<N, Map<FormulaNode<L, AP>, GameGraphNode<N>>> e : ggnodecache.entrySet()) {
            N n = e.getKey();
            Map<FormulaNode<L, AP>, GameGraphNode<N>> map = e.getValue();
            for (Entry<FormulaNode<L, AP>, GameGraphNode<N>> e2 : map.entrySet()) {
                FormulaNode<L, AP> exp = e2.getKey();
                GameGraphNode<N> ggn = e2.getValue();
//                GameGraphNode<N> ggn = map.get(exp);

                if (exp instanceof AbstractFixedPointFormulaNode) {
                    // mapping (MIN/MAX X PHI) to subformula PHI
                    AbstractFixedPointFormulaNode<L, AP> f = (AbstractFixedPointFormulaNode<L, AP>) exp;
                    GameGraphNode<N> ggn_succ = map.get(f.getChild());
                    configureEdge(ggn, ggn_succ);
                } else if (exp instanceof AbstractBinaryFormulaNode) {
                    // mapping AND/OR to subformulas at same node
                    AbstractBinaryFormulaNode<L, AP> f = (AbstractBinaryFormulaNode<L, AP>) exp;
                    GameGraphNode<N> succl = map.get(f.getLeftChild());
                    GameGraphNode<N> succr = map.get(f.getRightChild());
                    configureEdge(ggn, succl);
                    configureEdge(ggn, succr);
                } else if (exp instanceof AbstractModalFormulaNode) {
                    // mapping BOX(B)/DIA(B) to successing(predecessing) nodes at subexpression
                    AbstractModalFormulaNode<L, AP> f = (AbstractModalFormulaNode<L, AP>) exp;

                    Collection<E> edges = model.getOutgoingEdges(n);

                    // mapping box/dia to successor nodes in subformula if no branch constraints set
                    for (E edge : edges) {
                        ProceduralModalEdgeProperty property = model.getEdgeProperty(edge);

                        // ignoring MAY edges when at DIA operator
                        if (property.isMayOnly() && exp instanceof DiamondNode) {
                            continue;
                        }

                        N tgt = this.model.getTarget(edge);

                        L label = model.getEdgeLabel(edge);
                        if (property.isInternal()) {
                            // mapping to the only operand and target of edge
                            if (f.getAction() == null || f.getAction().equals(label)) {
                                GameGraphNode<N> succ = this.ggnodecache.get(tgt).get(f.getChild());
                                configureEdge(ggn, succ, edge);
                            }
                        } else {
                            for (FormulaNode<L, AP> sub : subformulas) {

                                if (sub.equals(this.root) || ((f.getChild() instanceof VariableNode) &&
                                                              this.fixvar2fix.get(((VariableNode<L, AP>) f.getChild()).getVariable())
                                                                             .equals(this.root))) {
                                    continue;
                                }

                                boolean[] singleton = new boolean[dg.getNumVariables()];
                                singleton[sub.getVarNumber()] = true;
                                AbstractDDSolver<?, L, AP>.WorkUnit<?, ?> calledUnit = this.units.get(label);
                                BitSet callContext = calledUnit.getInitialTransformer().evaluate(singleton);
                                if (callContext.get(f.getVarNumber())) {
                                    GameGraphNode<N> succ = this.ggnodecache.get(tgt).get(sub);
                                    configureEdge(ggn, succ, edge);
                                }
                            }
//                            BitSet tgtContext = unit.propTransformers.get(tgt).evaluate(dg.toBoolArray(context));
//                            AbstractDDSolver<?, L, AP>.WorkUnit<?, ?> calledUnit = this.units.get(label);
//                            BitSet callContext = calledUnit.getInitialTransformer().evaluate(dg.toBoolArray(tgtContext));
//                            for (FormulaNode<L, AP> subformula : subformulas) {
//                                if (callContext.get(subformula.getVarNumber())) {
//                                    GameGraphNode<N> succ = this.ggnodecache.get(tgt).get(subformula);
//                                    configureEdge(ggn, succ, label);
//                                }
//                            }
                        }
                    }
                } else if (exp instanceof VariableNode) {
                    VariableNode<L, AP> f = (VariableNode<L, AP>) exp;
                    GameGraphNode<N> succ = map.get(this.fixvar2fix.get(f.getVariable()));
                    configureEdge(ggn, succ);
                }
            }
        }

    }

    private Collection<FormulaNode<L, AP>> subformulas(FormulaNode<L, AP> root) {
        final List<FormulaNode<L, AP>> cache = new ArrayList<>();
        subformulas(root, cache);
        return cache;
    }

    private void configureEdge(GameGraphNode<N> src, GameGraphNode<N> tgt) {
        configureEdge(src, tgt, null);
    }

    private void configureEdge(GameGraphNode<N> src, GameGraphNode<N> tgt, E edge) {
        this.succs.get(src).add(tgt);
        this.preds.get(tgt).add(src);
        if (edge != null) {
            this.labels.computeIfAbsent(Pair.of(src, tgt), k -> new HashSet<>()).add(edge);
        }
    }

    public String getLabelBetween(GameGraphNode<N> src, GameGraphNode<N> tgt) {
        Set<E> edges = this.labels.get(Pair.of(src, tgt));
        if (edges != null && !edges.isEmpty()) {
            return edges.stream().map(model::getEdgeLabel).map(Objects::toString).collect(Collectors.joining(", ", "{", "}"));
        }

        return null;
    }

    public Set<E> getEdgesBetween(GameGraphNode<N> src, GameGraphNode<N> tgt) {
        return this.labels.get(Pair.of(src, tgt));
    }

    public FormulaNode<L, AP> getFormula(GameGraphNode<N> ggNode) {
        final Map<FormulaNode<L, AP>, GameGraphNode<N>> map = this.ggnodecache.get(ggNode.getModelNode());
        for (Entry<FormulaNode<L, AP>, GameGraphNode<N>> e : map.entrySet()) {
            if (Objects.equals(ggNode, e.getValue())) {
                return e.getKey();
            }
        }

        return null;
    }

    private void subformulas(FormulaNode<L, AP> root, List<FormulaNode<L, AP>> cache) {

        cache.add(root);

        if (root instanceof AbstractUnaryFormulaNode) {
            AbstractUnaryFormulaNode<L, AP> f = (AbstractUnaryFormulaNode<L, AP>) root;
            subformulas(f.getChild(), cache);
            if (root instanceof AbstractFixedPointFormulaNode) {
                AbstractFixedPointFormulaNode<L, AP> f2 = (AbstractFixedPointFormulaNode<L, AP>) root;
                this.fixvar2fix.put(f2.getVariable(), f2);
            }
        } else if (root instanceof AbstractBinaryFormulaNode) {
            AbstractBinaryFormulaNode<L, AP> f = (AbstractBinaryFormulaNode<L, AP>) root;
            subformulas(f.getLeftChild(), cache);
            subformulas(f.getRightChild(), cache);
        }
    }

    private Type getType(N node, FormulaNode<L, AP> formulaNode) {

        if (formulaNode instanceof VariableNode) {
            VariableNode<L, AP> f = (VariableNode<L, AP>) formulaNode;
            return getType(node, this.fixvar2fix.get(f.getVariable()));
        }

        if (formulaNode instanceof AtomicNode) {
            AtomicNode<L, AP> f = (AtomicNode<L, AP>) formulaNode;
            if (this.model.getAtomicPropositions(node).contains(f.getProposition())) {
                return Type.CONJUNCTIVE;
            } else {
                return Type.DISJUNCTIVE;
            }
        }

        if (formulaNode instanceof OrNode || formulaNode instanceof DiamondNode || formulaNode instanceof LfpNode ||
            formulaNode instanceof FalseNode) {
            return Type.DISJUNCTIVE;
        }

        return Type.CONJUNCTIVE;
    }

    /**
     * Compute a mapping that maps each expression to its priority.
     */
    public Map<FormulaNode<L, AP>, Integer> computeExpression2Prio(FormulaNode<L, AP> expression) {
        HashMap<FormulaNode<L, AP>, Integer> exp2prio = new HashMap<>();

        // lets fill the map
        computeExpression2Prio(expression, exp2prio, 0);

        return exp2prio;
    }

    private void computeExpression2Prio(FormulaNode<L, AP> exp, Map<FormulaNode<L, AP>, Integer> exp2prio, int prio) {
        /*
         * Greatest fixpoint is initialized with 0, increased upon change
         * Least fixpoint is initialized with 1, increased upon change
         * So greatest fixpoint alsway have EVEN, least fixpoints have ODD priorities.
         */

        if(exp instanceof AbstractUnaryFormulaNode || exp instanceof AbstractBinaryFormulaNode) {
            int prioNew;

            if(exp instanceof GfpNode && prio == 0)
                // no fixpoint seen so far, so lets init with 0
                prioNew = 0;

            else if(exp instanceof LfpNode && prio == 0)
                // no fixpoint seen so far, so lets init with 1
                prioNew = 1;

            else if(exp instanceof GfpNode && prio % 2 != 0)
                // prio is odd at max => prio change
                prioNew = prio + 1;

            else if(exp instanceof LfpNode && prio % 2 == 0)
                // prio is even at in => prio change
                prioNew = prio + 1;

            else
                prioNew = prio;

            // adding prio for current expression
            exp2prio.put(exp, prioNew);

            // recurse on operands
            if (exp instanceof AbstractUnaryFormulaNode) {
                AbstractUnaryFormulaNode<L, AP> f = (AbstractUnaryFormulaNode<L, AP>) exp;
                computeExpression2Prio(f.getChild(), exp2prio, prioNew);
            } else {
                AbstractBinaryFormulaNode<L, AP> f = (AbstractBinaryFormulaNode<L, AP>) exp;
                computeExpression2Prio(f.getLeftChild(), exp2prio, prioNew);
                computeExpression2Prio(f.getRightChild(), exp2prio, prioNew);
            }

            //  adding prio for bound fixpoint variable and overwriting prio found in recursion
            if(exp instanceof GfpNode) {
                GfpNode<L, AP> f = (GfpNode<L, AP>) exp;
                exp2prio.put(f.getChild(), prioNew);
            } else if (exp instanceof LfpNode) {
                LfpNode<L, AP> f = (LfpNode<L, AP>) exp;
                exp2prio.put(f.getChild(), prioNew);
            }

        } else {
            // expression is atomic - proposition or fix point variable
            exp2prio.put(exp, prio);
        }
    }

    @Override
    public GameGraphNode<N> getGameGraphNode(N modelNode, SymbolicExpression exp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<GameGraphNode<N>> getNodes() {
        return this.succs.keySet();
    }

    @Override
    public Set<GameGraphNode<N>> getPreds(GameGraphNode<N> node) {
        return this.preds.get(node);
    }

    @Override
    public Set<GameGraphNode<N>> getSuccs(GameGraphNode<N> node) {
        return this.succs.get(node);
    }

    @Override
    public Set<GameGraphNode<N>> getEdgeSources() {
        final Set<GameGraphNode<N>> result = new HashSet<>();
        for (Entry<GameGraphNode<N>, Set<GameGraphNode<N>>> e : this.succs.entrySet()) {
            if (!e.getValue().isEmpty()) {
                result.add(e.getKey());
            }
        }
        return  result;
    }

    @Override
    public int getOutDegree(GameGraphNode<N> ggNode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SymbolicExpression getTopExpression() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GameGraphNode<N> getGameGraphNode(N modelnode) {
        return this.ggnodecache.get(this.model.getInitialNode()).get(this.root);
    }

    @Override
    public Model<N, E> getModel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Edge<GameGraphNode<N>>> getOutgoingEdges(GameGraphNode<N> node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Edge<GameGraphNode<N>>> getIncomingEdges(GameGraphNode<N> node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Edge<GameGraphNode<N>>> getEdges() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GameGraphNode<N> getSource(Edge<GameGraphNode<N>> edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GameGraphNode<N> getTarget(Edge<GameGraphNode<N>> edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getEdgeLabels(Edge<GameGraphNode<N>> edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getAtomicPropositions(GameGraphNode<N> node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getIdentifier(GameGraphNode<N> node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EdgeType getEdgeType(Edge<GameGraphNode<N>> edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<GameGraphNode<N>> getInitialNodes() {
        return Collections.singleton(this.ggnodecache.get(this.model.getInitialNode()).get(this.root));
    }

    @Override
    public List<GameGraphNode<N>> getGameGraphNodes(N node) {
        return new ArrayList<>(this.ggnodecache.get(node).values());
    }

    @Override
    public void removeEdges(Collection<Edge<GameGraphNode<N>>> es) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEdge(Edge<GameGraphNode<N>> e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEdge(GameGraphNode<N> src, GameGraphNode<N> tgt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Graph<?, ?> graphView() {
        return new AsGraph();
    }

    private class AsGraph implements Graph<GNode<N>, GNode<N>> {


        private final Map<GameGraphNode<N>, GNode<N>> nodes;
        private final Map<GNode<N>, Set<GNode<N>>> adjacency;

        public AsGraph() {

            this.nodes = new HashMap<>();
            this.adjacency = new HashMap<>();

            for (Entry<N, Map<FormulaNode<L, AP>, GameGraphNode<N>>> e : ggnodecache.entrySet()) {
                for (Entry<FormulaNode<L, AP>, GameGraphNode<N>> e2 : e.getValue().entrySet()) {
                    FormulaNode<L, AP> formula = e2.getKey();
                    GameGraphNode<N> node = e2.getValue();
                    this.nodes.put(node, new GNode<>(node.getModelNode(), node, formula));
                }
            }

            for (GNode<N> node : this.nodes.values()) {
                final Set<GNode<N>> set = new HashSet<>();
                for (GameGraphNode<N> ggn : succs.get(node.ggnode)) {
                    set.add(this.nodes.get(ggn));
                }
                this.adjacency.put(node, set);
            }

        }

        @Override
        public Collection<GNode<N>> getOutgoingEdges(GNode<N> node) {
            return this.adjacency.get(node);
        }

        @Override
        public GNode<N> getTarget(GNode<N> edge) {
            return edge;
        }

        @Override
        public Collection<GNode<N>> getNodes() {
            return this.nodes.values();
        }

        @Override
        public VisualizationHelper<GNode<N>, GNode<N>> getVisualizationHelper() {
            return new DefaultVisualizationHelper<>() {

                @Override
                public boolean getEdgeProperties(GNode<N> src,
                                                 GNode<N> edge,
                                                 GNode<N> tgt,
                                                 Map<String, String> properties) {
                    String label = getLabelBetween(src.ggnode, edge.ggnode);
                    properties.put(EdgeAttrs.LABEL, label == null ? "" : label);
                    return true;
                }
            };
        }
    }

    private class GNode<N> {

        private final N src;
        private final GameGraphNode<N> ggnode;
        private final FormulaNode<L, AP> formula;

        private GNode(N src, GameGraphNode<N> ggnode, FormulaNode<L, AP> formula) {
            this.src = src;
            this.ggnode = ggnode;
            this.formula = formula;
        }

        @Override
        public String toString() {
            return src + ": " + formula + '\n' + ggnode.getType();
        }
    }
}


