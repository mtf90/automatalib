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
package net.automatalib.common.util.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A utility class that allows to reuse an {@link Iterator}.
 * <p>
 * Iterators returned by this class' {@link #iterator()} method copy the elements of the given source iterator to a list
 * as they are requested. Subsequent iterators then traverse the cached list (and continue to copy elements, if they
 * haven't been requested before) allowing to arbitrarily re-iterate over the elements of the initial iterator.
 *
 * @param <T>
 *         type of elements to iterate.
 */
public class ReusableIterator<T> implements Iterable<T> {

    private final Iterator<T> iterator;
    private final List<T> cache;
    private int frontier;

    /**
     * Default constructor.
     *
     * @param iterator
     *         the source iterator
     */
    public ReusableIterator(Iterator<T> iterator) {
        this(iterator, new ArrayList<>());
    }

    /**
     * Constructor that allows to explicitly specify that cache to be used. Useful if e.g. the size is known
     * beforehand.
     *
     * @param iterator
     *         the source iterator
     * @param cache
     *         the cache to use
     */
    public ReusableIterator(Iterator<T> iterator, List<T> cache) {
        this.iterator = iterator;
        this.cache = cache;
    }

    @Override
    public Iterator<T> iterator() {
        return new CopyOnReadIterator(this.iterator);
    }

    private class CopyOnReadIterator implements Iterator<T> {

        private final Iterator<T> source;
        private int pos;

        CopyOnReadIterator(Iterator<T> source) {
            this.source = source;
        }

        @Override
        public boolean hasNext() {
            return pos < frontier || source.hasNext();
        }

        @Override
        public T next() {
            if (pos < frontier) {
                return cache.get(pos++);
            }

            final T next = source.next();
            cache.add(next);
            pos++;
            frontier++;

            return next;
        }
    }
}
