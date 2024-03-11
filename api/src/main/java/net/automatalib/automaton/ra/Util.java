package net.automatalib.automaton.ra;

import gov.nasa.jpf.constraints.api.Valuation;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValue;

public final class Util {

    @SafeVarargs
    public static Valuation compose(net.automatalib.data.Valuation<? extends SymbolicDataValue<?>, DataValue<?>>... varVals) {
        Valuation val = new Valuation();

        for (net.automatalib.data.Valuation<? extends SymbolicDataValue<?>, DataValue<?>> valuation : varVals) {
            fill(val, valuation);
        }

        return val;
    }

    private static void fill(Valuation val,
                             net.automatalib.data.Valuation<? extends SymbolicDataValue<?>, ? extends DataValue<?>> valuation) {
        for (SymbolicDataValue<?> k : valuation.keySet()) {
            fill(val, valuation, k);
        }
    }

    private static <T, K extends SymbolicDataValue<T>> void fill(Valuation val,
                                                                 net.automatalib.data.Valuation<? extends SymbolicDataValue<?>, ? extends DataValue<?>> valuation,
                                                                 K key) {
        val.setValue(key, valuation.get(key).getValue());
    }

}
