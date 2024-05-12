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
import java.util.Set;

import de.metaframe.gear.game.GameGraph;
import de.metaframe.gear.game.GameGraphNode;
import de.metaframe.gear.game.GameGraphNode.Type;
import de.metaframe.gear.model.Edge;
import de.metaframe.gear.model.Model;
import de.metaframe.gear.sexp.SymbolicExpression;
import net.automatalib.graph.ProceduralModalProcessGraph;
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
import net.automatalib.modelchecker.m3c.formula.modalmu.LfpNode;
import net.automatalib.modelchecker.m3c.formula.modalmu.VariableNode;
import net.automatalib.ts.modal.transition.ProceduralModalEdgeProperty;

public class G4m3Graph<N, L, E, AP> extends GameGraph<N, E> {

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

    // mapping from atomic fix point variable expressions to fix point expression they are bound to
    private final Map<String, FormulaNode<L, AP>> fixvar2fix = new HashMap<>();

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

        Collection<FormulaNode<L, AP>> subformulas = new ArrayList<>(dg.getFormulaNodes()); //subformulas(formulaNode);
        subformulas.add(this.root);

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
                GameGraphNode<N> ggnode = new GameGraphNode<>(new SymbolicExpression(f.toString()), n, 0, getType(n, f));
                map.put(f, ggnode);
                this.succs.put(ggnode, new HashSet<>());
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
                    FormulaNode<L, AP> subFormula = this.fixvar2fix.get(f.getVariable());
                    this.succs.get(ggn).add(map.get(subFormula));
                } else if (exp instanceof AbstractBinaryFormulaNode) {
                    // mapping AND/OR to subformulas at same node
                    AbstractBinaryFormulaNode<L, AP> f = (AbstractBinaryFormulaNode<L, AP>) exp;
                    FormulaNode<L, AP> right = f.getRightChild();
                    FormulaNode<L, AP> left = f.getLeftChild();
                    this.succs.get(ggn).add(map.get(right));
                    this.succs.get(ggn).add(map.get(left));
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
                                this.succs.get(ggn).add(succ);
                            }
                        } else {
                            // for all teilformeln die dadurch erfüllt werden.
                            BitSet tgtContext = unit.propTransformers.get(tgt).evaluate(dg.toBoolArray(context));
                            AbstractDDSolver<?, L, AP>.WorkUnit<?, ?> calledUnit = this.units.get(label);
                            BitSet callContext = calledUnit.getInitialTransformer().evaluate(dg.toBoolArray(tgtContext));
                            for (FormulaNode<L, AP> subformula : subformulas) {
                                if (callContext.get(subformula.getVarNumber())) {
                                    GameGraphNode<N> succ = this.ggnodecache.get(tgt).get(subformula);
                                    if (succ == null) {
                                        throw new NullPointerException();
                                    }
                                    this.succs.get(ggn).add(succ);
                                }
                            }
                        }
                    }
                } else if (exp instanceof VariableNode) {
                    VariableNode<L, AP> f = (VariableNode<L, AP>) exp;
                    this.succs.get(ggn).add(map.get(this.fixvar2fix.get(f.getVariable())));
                }
            }
        }

    }

    private Collection<FormulaNode<L, AP>> subformulas(FormulaNode<L, AP> root) {
        final List<FormulaNode<L, AP>> cache = new ArrayList<>();
        subformulas(root, cache);
        return cache;
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
        return Collections.emptySet();
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
}


