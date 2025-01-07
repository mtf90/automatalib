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
package net.automatalib.serialization.fsm.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.common.util.io.UnclosableInputStream;
import net.automatalib.exception.FormatException;
import net.automatalib.serialization.ModelDeserializer;
import net.automatalib.util.automaton.Automata;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for FSM2DFAParser.
 */
public class FSM2DFAParserTest extends AbstractFSM2ParserTest {

    private ModelDeserializer<CompactDFA<Character>> parser;

    @BeforeClass
    public void setUp() {
        parser = FSM2DFAParser.getParser(s -> s.charAt(0), "label", "accept");
    }

    @Test
    public void testParse1() throws Exception {
        try (InputStream is = FSM2DFAParserTest.class.getResourceAsStream("/DFA1.fsm")) {
            final CompactDFA<Character> actualDFA = parser.readModel(is);

            final Alphabet<Character> alphabet = Alphabets.characters('a', 'b');

            CompactDFA<Character> expectedDFA = AutomatonBuilders.newDFA(alphabet)
                                                                 .from("q0").on('a').to("q1")
                                                                 .from("q1").on('b').to("q0")
                                                                 .withAccepting("q1").withInitial("q0")
                                                                 .create();

            Assert.assertTrue(Automata.testEquivalence(actualDFA, expectedDFA, alphabet));
        }
    }

    @Test
    public void testParse2() throws Exception {
        try (InputStream is = FSM2DFAParserTest.class.getResourceAsStream("/DFA2.fsm")) {
            final CompactDFA<Character> actualDFA = parser.readModel(is);

            final Alphabet<Character> alphabet = Alphabets.characters('a', 'a');

            CompactDFA<Character> expectedDFA = AutomatonBuilders.newDFA(alphabet)
                                                                 .from("q0").on('a').loop()
                                                                 .withAccepting("q0").withInitial("q0")
                                                                 .create();

            Assert.assertTrue(Automata.testEquivalence(actualDFA, expectedDFA, alphabet));
        }
    }

    @Override
    protected CompactDFA<Character> getParsedAutomaton(@Nullable Collection<Character> requiredInputs)
            throws IOException, FormatException {
        try (InputStream is = FSM2DFAParserTest.class.getResourceAsStream("/DFA2.fsm")) {
            final Function<String, Character> ip = s -> s.charAt(0);
            return FSM2DFAParser.getParser(requiredInputs, ip, "label", "accept").readModel(is);
        }
    }

    @Test
    public void doNotCloseInputStreamTest() throws IOException, FormatException {
        try (InputStream is = FSM2DFAParserTest.class.getResourceAsStream("/DFA1.fsm")) {
            parser.readModel(new UnclosableInputStream(is));
        }
    }
}
