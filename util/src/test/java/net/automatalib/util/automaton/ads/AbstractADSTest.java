/* Copyright (C) 2013-2025 TU Dortmund University
 * This file is part of AutomataLib <https://automatalib.net>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.util.automaton.ads;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automaton.transducer.impl.CompactMealy;
import net.automatalib.common.util.HashUtil;
import net.automatalib.common.util.Pair;
import net.automatalib.graph.ads.ADSNode;
import net.automatalib.word.Word;
import org.testng.Assert;

public abstract class AbstractADSTest {

    protected <I, O> void verifySuccess(CompactMealy<I, O> mealy) {
        this.verifySuccess(mealy, mealy.getStates());
    }

    protected <I, O> void verifySuccess(CompactMealy<I, O> mealy, Collection<Integer> targets) {
        this.verifySuccess(mealy, new HashSet<>(targets));
    }

    protected <I, O> void verifySuccess(CompactMealy<I, O> mealy, Set<Integer> targets) {
        final Optional<ADSNode<Integer, I, O>> defaultADS = ADS.compute(mealy, mealy.getInputAlphabet(), targets);
        final Optional<ADSNode<Integer, I, O>> bestEffortADS =
                BacktrackingSearch.compute(mealy, mealy.getInputAlphabet(), targets);
        final Optional<ADSNode<Integer, I, O>> bfsMinLengthADS = BacktrackingSearch.computeOptimal(mealy,
                                                                                                   mealy.getInputAlphabet(),
                                                                                                   targets,
                                                                                                   BacktrackingSearch.CostAggregator.MIN_LENGTH);
        final Optional<ADSNode<Integer, I, O>> bfsMinSizeADS = BacktrackingSearch.computeOptimal(mealy,
                                                                                                 mealy.getInputAlphabet(),
                                                                                                 targets,
                                                                                                 BacktrackingSearch.CostAggregator.MIN_SIZE);

        Assert.assertTrue(defaultADS.isPresent());
        Assert.assertTrue(bestEffortADS.isPresent());
        Assert.assertTrue(bfsMinLengthADS.isPresent());
        Assert.assertTrue(bfsMinSizeADS.isPresent());

        final ADSNode<Integer, I, O> def = defaultADS.get();
        final ADSNode<Integer, I, O> bestEffort = bestEffortADS.get();
        final ADSNode<Integer, I, O> bfsMinLength = bestEffortADS.get();
        final ADSNode<Integer, I, O> bfsMinSize = bestEffortADS.get();

        this.verifySuccess(mealy, targets, def);
        this.verifySuccess(mealy, targets, bestEffort);
        this.verifySuccess(mealy, targets, bfsMinLength);
        this.verifySuccess(mealy, targets, bfsMinSize);

        final int defaultLength = ADSUtil.computeLength(def);
        final int bestEffortLength = ADSUtil.computeLength(bestEffort);
        final int bfsMinLengthLength = ADSUtil.computeLength(bfsMinLength);
        final int bfsMinSizeLength = ADSUtil.computeLength(bfsMinSize);

        Assert.assertTrue(bfsMinLengthLength <= defaultLength);
        Assert.assertTrue(bfsMinLengthLength <= bestEffortLength);
        Assert.assertTrue(bfsMinLengthLength <= bfsMinSizeLength);

        final int defaultSize = ADSUtil.countSymbolNodes(def);
        final int bestEffortSize = ADSUtil.countSymbolNodes(bestEffort);
        final int bfsMinLengthSize = ADSUtil.countSymbolNodes(bfsMinLength);
        final int bfsMinSizeSize = ADSUtil.countSymbolNodes(bfsMinSize);

        Assert.assertTrue(bfsMinSizeSize <= defaultSize);
        Assert.assertTrue(bfsMinSizeSize <= bestEffortSize);
        Assert.assertTrue(bfsMinSizeSize <= bfsMinLengthSize);
    }

    protected <I, O> void verifySuccess(CompactMealy<I, O> mealy, Set<Integer> targets, ADSNode<Integer, I, O> ads) {

        final Set<ADSNode<Integer, I, O>> leaves = ADSUtil.collectLeaves(ads);

        Assert.assertEquals(targets, leaves.stream().map(ADSNode::getState).collect(Collectors.toSet()));

        final Map<ADSNode<Integer, I, O>, Pair<Word<I>, Word<O>>> traces =
                new HashMap<>(HashUtil.capacity(leaves.size()));
        for (ADSNode<Integer, I, O> leaf : leaves) {
            traces.put(leaf, ADSUtil.buildTraceForNode(leaf));
        }

        // check matching outputs
        for (Map.Entry<ADSNode<Integer, I, O>, Pair<Word<I>, Word<O>>> entry : traces.entrySet()) {
            final Integer state = entry.getKey().getState();
            final Word<I> input = entry.getValue().getFirst();
            final Word<O> output = entry.getValue().getSecond();
            Assert.assertEquals(mealy.computeStateOutput(state, input), output);
        }

        // check uniqueness of ADSs
        final Set<Word<O>> outputSet = traces.values().stream().map(Pair::getSecond).collect(Collectors.toSet());
        Assert.assertEquals(traces.size(), outputSet.size());
    }

    protected <I, O> void verifyFailure(CompactMealy<I, O> mealy) {
        this.verifyFailure(mealy, mealy.getStates());
    }

    protected <I, O> void verifyFailure(CompactMealy<I, O> mealy, Collection<Integer> targets) {
        this.verifyFailure(mealy, new HashSet<>(targets));
    }

    protected <I, O> void verifyFailure(CompactMealy<I, O> mealy, Set<Integer> targets) {
        final Optional<ADSNode<Integer, I, O>> defaultADS = ADS.compute(mealy, mealy.getInputAlphabet(), targets);
        final Optional<ADSNode<Integer, I, O>> bestEffortADS =
                BacktrackingSearch.compute(mealy, mealy.getInputAlphabet(), targets);
        final Optional<ADSNode<Integer, I, O>> bfsMinLengthADS = BacktrackingSearch.computeOptimal(mealy,
                                                                                                   mealy.getInputAlphabet(),
                                                                                                   targets,
                                                                                                   BacktrackingSearch.CostAggregator.MIN_LENGTH);
        final Optional<ADSNode<Integer, I, O>> bfsMinSizeADS = BacktrackingSearch.computeOptimal(mealy,
                                                                                                 mealy.getInputAlphabet(),
                                                                                                 targets,
                                                                                                 BacktrackingSearch.CostAggregator.MIN_SIZE);

        Assert.assertFalse(defaultADS.isPresent());
        Assert.assertFalse(bestEffortADS.isPresent());
        Assert.assertFalse(bfsMinLengthADS.isPresent());
        Assert.assertFalse(bfsMinSizeADS.isPresent());
    }
}
