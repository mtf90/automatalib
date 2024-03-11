package net.automatalib.data;

import gov.nasa.jpf.constraints.api.Expression;

public interface GuardElement extends TypedValue {

    Expression<?> asExpression();

}
