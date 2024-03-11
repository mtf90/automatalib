package net.automatalib.automaton.ra;

import net.automatalib.data.RegisterValuation;

public class State<L> {

    private final L location;
    private final RegisterValuation valuation;

    public State(L location, RegisterValuation valuation) {
        this.location = location;
        this.valuation = valuation;
    }

    public L getLocation() {
        return location;
    }

    public RegisterValuation getValuation() {
        return valuation;
    }
}
