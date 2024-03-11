package net.automatalib.data;

import java.util.Collection;

public interface FreshValueGenerator<T> extends TypedValue {

    DataValue<T> getFreshValue(Collection<DataValue<T>> vals);
}
