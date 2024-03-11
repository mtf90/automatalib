/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
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
package net.automatalib.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 *
 * @author falk
 * @param <K>
 * @param <V>
 */
public class Mapping<K extends TypedValue, V extends TypedValue> extends LinkedHashMap<K, V>
        implements Iterable<Map.Entry<K, V>> {

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return this.entrySet().iterator();
    }


    @Override
    public V put(K key, V value) {
        if (!key.getDataType().equals(value.getDataType())) {
            throw new IllegalArgumentException("Types of key and value do not match");
        }
        return super.put(key, value);
    }

//    /**
//     * returns the contained values of some type.
//     *
//     * @param <T>
//     * @param type the type
//     * @return
//     */
//    public <T, R extends TypedValue<T>> Collection<V> values(DataType<T> type) {
//        List<V> list = new ArrayList<>();
//        for (V v : values()) {
//            if (v.getDataType().equals(type)) {
//                list.add(v);
//            }
//        }
//        return list;
//    }

//    public <T, R extends TypedValue<T>> Collection<V> values(DataType<T> type, Consumer<? super Collection<? super V>> consumer) {
//        List<V> list = new ArrayList<>();
//        for (V v : values()) {
//            if (v.getDataType().equals(type)) {
//                list.add(v);
//            }
//        }
//        consumer.accept(list);
//        return list;
//    }

//    public <T> Collection<V> values(DataType<T> type) {
//        List<V> list = new ArrayList<>();
//        for (V v : values()) {
//            if (v.getDataType().equals(type)) {
//                list.add(v);
//            }
//        }
//        return list;
//    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mapping<?, ?> other = (Mapping<?, ?>) obj;
        return other.entrySet().equals(entrySet());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash * this.entrySet().hashCode();
    }

//    @Override
//    public V get(Object key) {
//        V v = super.get(key);
//        if (v == null) {
//            throw new IllegalStateException();
//        }
//        return v;
//    }

    public String toString(String map) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (Map.Entry<K,V> e : entrySet()) {
            sj.add(e.getKey() + map + e.getValue());
        }
        return sj.toString();
    }

    public Set<K> getAllKeysForValue(V value) {
        Set<K> retKeySet = new LinkedHashSet<>();
        for (Map.Entry<K,V> entry : this.entrySet()) {
            if (entry.getValue().equals(value)){
                retKeySet.add(entry.getKey());
            }
        }
        return retKeySet;
    }

    @Override
    public String toString() {
        return toString(">");
    }
}
