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
package net.automatalib.graph.base;

import net.automatalib.graph.impl.CompactSimpleGraph;

/**
 * An edge in a {@link CompactSimpleGraph}.
 *
 * @param <EP>
 *         edge property class.
 */
public class CompactEdge<EP> {

    private final int target;
    private EP property;

    public CompactEdge(int target, EP property) {
        this.target = target;
        this.property = property;
    }

    public EP getProperty() {
        return property;
    }

    public void setProperty(EP property) {
        this.property = property;
    }

    public int getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return String.valueOf(property);
    }

}
