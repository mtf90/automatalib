/* Copyright (C) 2013-2024 TU Dortmund University
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
package net.automatalib.common.util.collection;

import java.util.ListIterator;

import com.google.common.collect.ForwardingListIterator;

/**
 * Wraps a given {@link ListIterator} so that any mutating operations throw an {@link UnsupportedOperationException}.
 *
 * @param <T>
 *         type of elements in the given iterator
 */
public class UnmodifiableListIterator<T> extends ForwardingListIterator<T> {

    private final ListIterator<? extends T> delegate;

    public UnmodifiableListIterator(ListIterator<? extends T> delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ListIterator<T> delegate() {
        return (ListIterator<T>) this.delegate;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void set(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void add(T t) {
        throw new UnsupportedOperationException();
    }
}
