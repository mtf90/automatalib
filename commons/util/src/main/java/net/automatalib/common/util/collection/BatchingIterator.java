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
import java.util.NoSuchElementException;

class BatchingIterator<T> implements Iterator<List<T>> {

    private final int batchSize;
    private final Iterator<T> source;
    private final List<T> batch;

    BatchingIterator(Iterator<T> source, int batchSize) {
        this.batchSize = batchSize;
        this.source = source;
        this.batch = new ArrayList<>(batchSize);
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public List<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        this.batch.clear();

        while (batch.size() < batchSize && source.hasNext()) {
            batch.add(source.next());
        }

        return batch;
    }
}
