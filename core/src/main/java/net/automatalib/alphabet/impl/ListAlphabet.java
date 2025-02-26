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
package net.automatalib.alphabet.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ListAlphabet<I> extends AbstractAlphabet<I> {

    private final List<I> list;

    public ListAlphabet(List<? extends I> list) {
        this.list = Collections.unmodifiableList(list);
    }

    @Override
    public I getSymbol(int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException ioobe) {
            throw new IllegalArgumentException(ioobe);
        }
    }

    @Override
    public int getSymbolIndex(I symbol) {
        int idx = list.indexOf(symbol);
        if (idx == -1) {
            throw new IllegalArgumentException("Symbol " + symbol + " is not contained in the alphabet");
        }
        return idx;
    }

    @Override
    public boolean containsSymbol(I symbol) {
        return list.contains(symbol);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Iterator<I> iterator() {
        return list.iterator();
    }

    @Override
    public ListIterator<I> listIterator(int index) {
        return list.listIterator();
    }
}
