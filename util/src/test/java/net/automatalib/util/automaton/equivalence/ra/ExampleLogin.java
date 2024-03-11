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
package net.automatalib.util.automaton.equivalence.ra;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.NumericOperator;
import gov.nasa.jpf.constraints.expressions.StringBooleanExpression;
import gov.nasa.jpf.constraints.expressions.StringBooleanOperator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.automaton.ra.impl.CompactRMM;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ConstantGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;

public final class ExampleLogin {

    public static final DataType<Integer> T_UID = new DataType<>("T_uid", BuiltinTypes.SINT32);
    public static final DataType<Integer> T_PWD = new DataType<>("T_pwd", BuiltinTypes.SINT32);
    public static final DataType<String> T_UID_S = new DataType<>("T_uid_s", BuiltinTypes.STRING);
    public static final DataType<String> T_PWD_S = new DataType<>("T_pwd_s", BuiltinTypes.STRING);
    public static final DataType<String> T_MSG = new DataType<>("T_msg", BuiltinTypes.STRING);

    private static final InputSymbol REGISTER = new InputSymbol("register", T_UID, T_PWD);
    private static final InputSymbol LOGIN = new InputSymbol("login", T_UID, T_PWD);
    private static final InputSymbol RESET = new InputSymbol("reset", T_PWD, T_PWD);
    private static final InputSymbol LOGOUT = new InputSymbol("logout");
    private static final InputSymbol REGISTER_S = new InputSymbol("register", T_UID_S, T_PWD_S);
    private static final InputSymbol LOGIN_S = new InputSymbol("login", T_UID_S, T_PWD_S);
    private static final OutputSymbol OK = new OutputSymbol("ok");
    private static final OutputSymbol ERR = new OutputSymbol("err", T_MSG);
    private static final OutputSymbol SUCCESS = new OutputSymbol("success", T_UID);

    private static final Alphabet<InputSymbol> ALPHABET = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

    public static CompactRA<InputSymbol> buildEqualAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "eqRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false, rUid, rPwd);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildTotalAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "totRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment emptyAssign = new Assignment();
        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));
        ra.addTransition(l0, LOGIN, ra.createTransition(l0, trueGuard, emptyAssign));
        ra.addTransition(l0, LOGOUT, ra.createTransition(l0, trueGuard, emptyAssign));

        // reg. location
        ra.addTransition(l1, REGISTER, ra.createTransition(l0, trueGuard, emptyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGOUT, ra.createTransition(l0, trueGuard, emptyAssign));

        // login location
        ra.addTransition(l2, REGISTER, ra.createTransition(l0, trueGuard, emptyAssign));
        ra.addTransition(l2, LOGIN, ra.createTransition(l0, trueGuard, emptyAssign));
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }
    public static CompactRA<InputSymbol> buildBijectiveRegistersAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "bijRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rPwd = rgen.next(T_PWD);
        Register<Integer> rUid = rgen.next(T_UID);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildEquivalentGuardsAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "equivRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.LE, pUid),
                                   new NumericBooleanExpression(rUid, NumericComparator.GE, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildOverridingAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "ovRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));
        ra.addTransition(l1, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildLessThanAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "ltRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.LE, pPwd));
        Expression<Boolean> elseCond = ExpressionUtil.or(new NumericBooleanExpression(rUid, NumericComparator.NE, pUid),
                                                         new NumericBooleanExpression(rPwd,
                                                                                      NumericComparator.GT,
                                                                                      pPwd));
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildSumAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(ALPHABET) {

            @Override
            public String toString() {
                return "sumRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(new NumericCompound<>(rPwd, NumericOperator.PLUS, rUid),
                                                                NumericComparator.EQ,
                                                                pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildResetAutomaton() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT, RESET);
        CompactRA<InputSymbol> ra = new CompactRA<>(alphabet) {

            @Override
            public String toString() {
                return "resetRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<Integer> oldPwd = pgen2.next(T_PWD);
        Parameter<Integer> newPwd = pgen2.next(T_PWD);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = ExpressionUtil.or(new NumericBooleanExpression(rUid, NumericComparator.NE, pUid),
                                                         new NumericBooleanExpression(rPwd,
                                                                                      NumericComparator.NE,
                                                                                      pPwd));
        Expression<Boolean> pwGuard =
                ExpressionUtil.or(new NumericBooleanExpression(rPwd, NumericComparator.EQ, oldPwd));
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> resetMapping = new VarMapping<>(rPwd, newPwd, rUid, rUid);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
        Assignment resetAssign = new Assignment(resetMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));
        ra.addTransition(l2, RESET, ra.createTransition(l1, pwGuard, resetAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildSinkAutomaton() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);
        CompactRA<InputSymbol> ra = new CompactRA<>(alphabet) {

            @Override
            public String toString() {
                return "sinkRA";
            }
        };

        // locations
        Integer l0 = ra.addInitialState(false);

        // assignments
        Assignment emptyAssign = new Assignment();

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l0, ExpressionUtil.TRUE, emptyAssign));
        ra.addTransition(l0, LOGIN, ra.createTransition(l0, ExpressionUtil.TRUE, emptyAssign));
        ra.addTransition(l0, LOGOUT, ra.createTransition(l0, ExpressionUtil.TRUE, emptyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildStringAutomaton() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER_S, LOGIN_S, LOGOUT);
        CompactRA<InputSymbol> ra = new CompactRA<>(alphabet) {

            @Override
            public String toString() {
                return "strRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<String> rUid = rgen.next(T_UID_S);
        Register<String> rPwd = rgen.next(T_PWD_S);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<String> pUid = pgen.next(T_UID_S);
        Parameter<String> pPwd = pgen.next(T_PWD_S);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new StringBooleanExpression(rUid, StringBooleanOperator.EQUALS, pUid),
                                   new StringBooleanExpression(rPwd, StringBooleanOperator.EQUALS, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER_S, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRA<InputSymbol> buildStringInclusionAutomaton() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER_S, LOGIN_S, LOGOUT);
        CompactRA<InputSymbol> ra = new CompactRA<>(alphabet) {

            @Override
            public String toString() {
                return "incRA";
            }
        };

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<String> rUid = rgen.next(T_UID_S);
        Register<String> rPwd = rgen.next(T_PWD_S);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<String> pUid = pgen.next(T_UID_S);
        Parameter<String> pPwd = pgen.next(T_PWD_S);

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false, rUid, rPwd);
        Integer l2 = ra.addState(true, rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new StringBooleanExpression(rUid, StringBooleanOperator.EQUALS, pUid),
                                   new StringBooleanExpression(rPwd, StringBooleanOperator.CONTAINS, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER_S, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN_S, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    public static CompactRMM<InputSymbol, OutputSymbol> buildEqualTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen1 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen1.next(T_UID);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<String> pErr = pgen2.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(alphabet, new RegisterValuation(), constants) {

            @Override
            public String toString() {
                return "eqRMM";
            }
        };

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState(rUid, rPwd);
        Integer l2 = rmm.addState(rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> successMapping = new VarMapping<>(pSuccess, pUid);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorMapping = new VarMapping<>(pErr, cErr);

        OutputAssignment emptyAssign = new OutputAssignment(copyMapping);
        OutputAssignment successAssign = new OutputAssignment(storeMapping, successMapping);
        OutputAssignment errorAssign = new OutputAssignment(copyMapping, errorMapping);

        // initial location
        rmm.addTransition(l0, REGISTER, rmm.createTransition(l1, trueGuard, successAssign, SUCCESS));

        // reg. location
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l2, condition, emptyAssign, OK));
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l1, elseCond, errorAssign, ERR));

        // login location
        rmm.addTransition(l2, LOGOUT, rmm.createTransition(l1, trueGuard, emptyAssign, OK));

        return rmm;
    }

    public static CompactRMM<InputSymbol, OutputSymbol> buildIsomorphicRegisterTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rPwd = rgen.next(T_PWD);
        Register<Integer> rUid = rgen.next(T_UID);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen1 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen1.next(T_UID);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<String> pErr = pgen2.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(alphabet, new RegisterValuation(), constants) {

            @Override
            public String toString() {
                return "isoRMM";
            }
        };

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState(rUid, rPwd);
        Integer l2 = rmm.addState(rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> successMapping = new VarMapping<>(pSuccess, pUid);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorMapping = new VarMapping<>(pErr, cErr);

        OutputAssignment emptyAssign = new OutputAssignment(copyMapping);
        OutputAssignment successAssign = new OutputAssignment(storeMapping, successMapping);
        OutputAssignment errorAssign = new OutputAssignment(copyMapping, errorMapping);

        // initial location
        rmm.addTransition(l0, REGISTER, rmm.createTransition(l1, trueGuard, successAssign, SUCCESS));

        // reg. location
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l2, condition,  emptyAssign, OK));
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l1, elseCond, errorAssign, ERR));

        // login location
        rmm.addTransition(l2, LOGOUT, rmm.createTransition(l1, trueGuard, emptyAssign, OK));

        return rmm;
    }

    public static CompactRMM<InputSymbol, OutputSymbol> buildDifferentOutputParameterTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rUid2 = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen2.next(T_UID);
        ParameterGenerator pgen3 = new ParameterGenerator();
        Parameter<String> pErr = pgen3.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));

        RegisterValuation initial = new RegisterValuation();
        initial.put(rUid2, new DataValue<>(T_UID, -2));

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(alphabet, initial, constants) {

            @Override
            public String toString() {
                return "paraRMM";
            }
        };

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState(rUid, rPwd);
        Integer l2 = rmm.addState(rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> successMapping = new VarMapping<>(pSuccess, rUid2);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorMapping = new VarMapping<>(pErr, cErr);

        OutputAssignment emptyAssign = new OutputAssignment(copyMapping);
        OutputAssignment successAssign = new OutputAssignment(storeMapping, successMapping);
        OutputAssignment errorAssign = new OutputAssignment(copyMapping, errorMapping);

        // initial location
        rmm.addTransition(l0, REGISTER, rmm.createTransition(l1, trueGuard, successAssign, SUCCESS));

        // reg. location
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l2, condition, emptyAssign, OK));
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l1, elseCond, errorAssign, ERR));

        // login location
        rmm.addTransition(l2, LOGOUT, rmm.createTransition(l1, trueGuard, emptyAssign, OK));

        return rmm;
    }

    public static CompactRMM<InputSymbol, OutputSymbol> buildDifferentOutputSymbolTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen1 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen1.next(T_UID);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<String> pErr = pgen2.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);
        Constant<String> cFail = cgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));
        constants.put(cFail, new DataValue<>(T_MSG, "fail"));

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(alphabet, new RegisterValuation(), constants) {

            @Override
            public String toString() {
                return "symRMM";
            }
        };

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState(rUid, rPwd);
        Integer l2 = rmm.addState(rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> successMapping = new VarMapping<>(pSuccess, pUid);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorMapping = new VarMapping<>(pErr, cFail);

        OutputAssignment emptyAssign = new OutputAssignment(copyMapping);
        OutputAssignment successAssign = new OutputAssignment(storeMapping, successMapping);
        OutputAssignment errorAssign = new OutputAssignment(copyMapping, errorMapping);

        // initial location
        rmm.addTransition(l0, REGISTER, rmm.createTransition(l1, trueGuard, successAssign, SUCCESS));

        // reg. location
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l2, condition, emptyAssign, OK));
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l1, elseCond, errorAssign, ERR));

        // login location
        rmm.addTransition(l2, LOGOUT, rmm.createTransition(l1, trueGuard, emptyAssign, OK));

        return rmm;
    }

    public static CompactRMM<InputSymbol, OutputSymbol> buildInitialValuationTransducer() {
        Alphabet<InputSymbol> alphabet = Alphabets.fromArray(REGISTER, LOGIN, LOGOUT);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);
        ParameterGenerator pgen2 = new ParameterGenerator();
        Parameter<Integer> pSuccess = pgen2.next(T_UID);
        ParameterGenerator pgen3 = new ParameterGenerator();
        Parameter<String> pErr = pgen3.next(T_MSG);
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<String> cErr = cgen.next(T_MSG);

        Constants constants = new Constants();
        constants.put(cErr, new DataValue<>(T_MSG, "error"));

        RegisterValuation initial = new RegisterValuation();
        initial.put(rUid, new DataValue<>(T_UID, -2));
        initial.put(rPwd, new DataValue<>(T_PWD, -2));

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(alphabet, initial, constants) {

            @Override
            public String toString() {
                return "initRMM";
            }
        };

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState(rUid, rPwd);
        Integer l2 = rmm.addState(rUid, rPwd);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> successMapping = new VarMapping<>(pSuccess, pUid);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> errorMapping = new VarMapping<>(pErr, cErr);

        OutputAssignment emptyAssign = new OutputAssignment(copyMapping);
        OutputAssignment successAssign = new OutputAssignment(storeMapping, successMapping);
        OutputAssignment errorAssign = new OutputAssignment(copyMapping, errorMapping);

        // initial location
        rmm.addTransition(l0, REGISTER, rmm.createTransition(l1, trueGuard, successAssign, SUCCESS));
        rmm.addTransition(l0, LOGIN, rmm.createTransition(l1, condition, emptyAssign, OK));

        // reg. location
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l2, condition, emptyAssign, OK));
        rmm.addTransition(l1, LOGIN, rmm.createTransition(l1, elseCond, errorAssign, ERR));

        // login location
        rmm.addTransition(l2, LOGOUT, rmm.createTransition(l1, trueGuard, emptyAssign, OK));

        return rmm;
    }

}
