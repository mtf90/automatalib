/* Copyright (C) 2013-2023 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
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
package net.automatalib.alphabet.vpa;

import java.util.List;

import net.automatalib.alphabet.impl.GrowingVPAlphabet;
import net.automatalib.alphabet.impl.VPSym;
import net.automatalib.alphabet.util.GrowingVPAlphabetTestUtil;

public class GrowingVPAlphabetTest extends AbstractVPAlphabetTest<VPSym<Character>, GrowingVPAlphabet<Character>> {

    @Override
    protected List<VPSym<Character>> getCallSymbols() {
        return GrowingVPAlphabetTestUtil.CALL_SYMBOLS;
    }

    @Override
    protected List<VPSym<Character>> getInternalSymbols() {
        return GrowingVPAlphabetTestUtil.INTERNAL_SYMBOLS;
    }

    @Override
    protected List<VPSym<Character>> getReturnSymbols() {
        return GrowingVPAlphabetTestUtil.RETURN_SYMBOLS;
    }

    @Override
    protected List<VPSym<Character>> getNonAlphabetSymbols() {
        return GrowingVPAlphabetTestUtil.NON_CONTAINED_SYMBOLS;
    }

    @Override
    protected GrowingVPAlphabet<Character> getAlphabet() {
        return GrowingVPAlphabetTestUtil.ALPHABET;
    }
}

