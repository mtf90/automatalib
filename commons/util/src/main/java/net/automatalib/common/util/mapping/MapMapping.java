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
package net.automatalib.common.util.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class that wraps a {@link Mapping} around a {@link java.util.Map}.
 *
 * @param <D>
 *         domain type.
 * @param <R>
 *         range type.
 */
public class MapMapping<D, @Nullable R> implements MutableMapping<D, R> {

    private final Map<D, R> map;

    /**
     * Constructor.
     */
    public MapMapping() {
        this(new HashMap<>());
    }

    /**
     * Constructor.
     *
     * @param map
     *         the underlying {@link java.util.Map} object.
     */
    public MapMapping(Map<D, R> map) {
        this(map, false);
    }

    /**
     * Constructor.
     *
     * @param map
     *         the underlying {@link java.util.Map} object.
     * @param copy
     *         whether the given map should be copied or stored by reference.
     */
    public MapMapping(Map<D, R> map, boolean copy) {
        if (copy) {
            this.map = new HashMap<>(map);
        } else {
            this.map = map;
        }
    }

    public static <D, @Nullable R> MapMapping<D, R> create(Map<D, R> map) {
        return new MapMapping<>(map);
    }

    @Override
    public R get(D elem) {
        return map.get(elem);
    }

    /**
     * Delegates to the underlying {@link java.util.Map}.
     *
     * @see java.util.Map#put(Object, Object)
     */
    @Override
    public R put(D key, R value) {
        return map.put(key, value);
    }

    /**
     * Returns the {@link Map#entrySet()} of the underlying map.
     *
     * @return the {@link Map#entrySet()} of the underlying map
     */
    public Set<Map.Entry<@KeyFor("this.map") D, R>> entrySet() {
        return map.entrySet();
    }
}
