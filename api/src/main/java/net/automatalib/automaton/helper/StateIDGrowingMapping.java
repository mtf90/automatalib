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
package net.automatalib.automaton.helper;

import net.automatalib.automaton.concept.StateIDs;
import net.automatalib.common.util.array.ResizingArrayStorage;
import net.automatalib.common.util.mapping.MutableMapping;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StateIDGrowingMapping<S, @Nullable V> implements MutableMapping<S, V> {

    private final StateIDs<S> stateIds;
    private final ResizingArrayStorage<V> storage;

    public StateIDGrowingMapping(StateIDs<S> stateIds, int size) {
        this.stateIds = stateIds;
        this.storage = new ResizingArrayStorage<>(Object.class, size);
    }

    @Override
    public V get(S elem) {
        int id = stateIds.getStateId(elem);
        if (id >= 0 && id < storage.array.length) {
            return storage.array[id];
        }
        return null;
    }

    @Override
    public V put(S key, V value) {
        int id = stateIds.getStateId(key);
        if (id >= storage.array.length) {
            storage.ensureCapacity(id + 1);
        }
        V old = storage.array[id];
        storage.array[id] = value;
        return old;
    }

}
