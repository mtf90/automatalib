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
package net.automatalib.ts.modal.transition;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ProceduralModalEdgePropertyImpl extends ModalEdgePropertyImpl
        implements MutableProceduralModalEdgeProperty {

    private ProceduralType proceduralType;

    public ProceduralModalEdgePropertyImpl(ProceduralType proceduralType, ModalType modalType) {
        super(modalType);
        this.proceduralType = proceduralType;
    }

    @Override
    public ProceduralType getProceduralType() {
        return this.proceduralType;
    }

    @Override
    public void setProceduralType(ProceduralType type) {
        this.proceduralType = type;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!super.equals(o)) {
            return false;
        }

        final ProceduralModalEdgePropertyImpl that = (ProceduralModalEdgePropertyImpl) o;

        return this.proceduralType == that.proceduralType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(this.proceduralType);
        return result;
    }
}
