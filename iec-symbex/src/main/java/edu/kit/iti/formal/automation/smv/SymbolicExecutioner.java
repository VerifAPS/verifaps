package edu.kit.iti.formal.automation.smv;

/*-
 * #%L
 * iec-symbex
 * %%
 * Copyright (C) 2016 Alexander Weigl
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import edu.kit.iti.formal.automation.datatypes.EnumerateType;
import edu.kit.iti.formal.automation.datatypes.values.Values;
import edu.kit.iti.formal.automation.exceptions.FunctionInvocationArgumentNumberException;
import edu.kit.iti.formal.automation.exceptions.FunctionUndefinedException;
import edu.kit.iti.formal.automation.exceptions.UnknownDatatype;
import edu.kit.iti.formal.automation.exceptions.UnknownVariableException;
import edu.kit.iti.formal.automation.operators.Operators;
import edu.kit.iti.formal.automation.scope.GlobalScope;
import edu.kit.iti.formal.automation.scope.LocalScope;
import edu.kit.iti.formal.automation.smv.translators.*;
import edu.kit.iti.formal.automation.st.ast.*;
import edu.kit.iti.formal.automation.visitors.DefaultVisitor;
import edu.kit.iti.formal.smv.SMVFacade;
import edu.kit.iti.formal.smv.ast.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by weigl on 26.11.16.
 */
public class SymbolicExecutioner extends DefaultVisitor<SMVExpr> {
    @Nullable
    private GlobalScope globalScope = GlobalScope.defaultScope();
    @Nullable
    private LocalScope localScope = new LocalScope(globalScope, null);
    @NotNull
    private Map<String, SVariable> varCache = new HashMap<>();

    @NotNull
    @Getter
    @Setter
    private OperationMap operationMap = new DefaultOperationMap();

    @NotNull
    @Getter
    @Setter
    private TypeTranslator typeTranslator = new DefaultTypeTranslator();

    @NotNull
    @Getter
    @Setter
    private ValueTranslator valueTranslator = new DefaultValueTranslator();

    @NotNull
    @Getter
    @Setter
    private InitValueTranslator initValueTranslator = new DefaultInitValue();

    //region state handling
    @NotNull
    private Stack<SymbolicState> state = new Stack<>();
    private SymbolicState globalState = new SymbolicState();
    private Expression caseExpression;

    public SymbolicExecutioner() {
        push(new SymbolicState());
    }

    public SymbolicExecutioner(@Nullable GlobalScope globalScope) {
        this();
        if (globalScope != null)
            this.globalScope = globalScope;
    }

    //region getter and setters
    @Nullable
    public GlobalScope getGlobalScope() {
        return globalScope;
    }

    public void setGlobalScope(GlobalScope globalScope) {
        this.globalScope = globalScope;
    }
    //endregion

    @Nullable
    public LocalScope getLocalScope() {
        return localScope;
    }

    public void setLocalScope(LocalScope localScope) {
        this.localScope = localScope;
    }

    public SymbolicState peek() {
        return state.peek();
    }

    public SMVExpr get(SVariable var) {
        if (peek().containsKey(var))
            return peek().get(var);
        return globalState.get(var);
    }

    public void put(SVariable var, SMVExpr val) {
        peek().put(var, val);
    }

    public void replace(SVariable var, SMVExpr val) {
        if (peek().containsKey(var))
            peek().replace(var, val);
        else
            globalState.replace(var, val);
    }

    public SymbolicState pop() {
        SymbolicState top = state.pop();
        // Update global variables
        /*
        for (SVariable var : globalState.keySet()) {
            peek().replace(var, top.get(var));
            globalState.replace(var, top.get(var));
        }
        */
        return top;
    }

    public void push() {
        push(new SymbolicState(peek()));
    }
    //endregion

    public <K, V> void push(SymbolicState map) {
        state.push(map);
    }

    public SVariable lift(@NotNull VariableDeclaration vd) {
        try {
            if (vd.getDataType() == null) {
                vd.setDataType(localScope.getGlobalScope()
                        .resolveDataType(vd.getDataTypeName()));
            }
            if (!varCache.containsKey(vd))
                varCache.put(vd.getName(), typeTranslator.translate(vd));
            return varCache.get(vd.getName());
        } catch (NullPointerException e) {
            throw new UnknownDatatype("Datatype not given/inferred for variable "
                    + vd.getName(), e);
        }
    }

    public SVariable lift(@NotNull SymbolicReference vd) {
        if (varCache.containsKey(vd.getIdentifier()))
            return varCache.get(vd.getIdentifier());
        else
            throw new UnknownVariableException("Variable access to not declared variable" + vd);
    }

    //region rewriting of expressions using the current state
    @Override
    public SMVExpr visit(@NotNull BinaryExpression binaryExpression) {
        SMVExpr left = binaryExpression.getLeftExpr().accept(this);
        SMVExpr right = binaryExpression.getRightExpr().accept(this);
        return operationMap
                .translateBinaryOperator(left, binaryExpression.getOperator(),
                        right);
    }

    @Override
    public SMVExpr visit(@NotNull UnaryExpression u) {
        SMVExpr left = u.getExpression().accept(this);
        return operationMap.translateUnaryOperator(u.getOperator(), left);
    }

    public SMVExpr visit(@NotNull Invocation invocation) {
        FunctionDeclaration fd = globalScope.resolveFunction(invocation, localScope);
        if (fd == null)
            throw new FunctionUndefinedException(invocation);


        //initialize data structure
        SymbolicState calleeState = new SymbolicState();
        SymbolicState callerState = peek();

        //region register function name as output variable
        if (null == fd.getLocalScope().getVariable(fd.getFunctionName())
                && fd.getReturnType() != null) {
            fd.getLocalScope().builder()
                    .setBaseType(fd.getReturnTypeName())
                    .push(VariableDeclaration.OUTPUT)
                    .identifiers(fd.getFunctionName())
                    .create();
        }
        //endregion

        //region local variables (declaration and initialization)
        for (VariableDeclaration vd : fd.getLocalScope().getLocalVariables().values()) {
            if (!calleeState.containsKey(vd.getName())) {
                TypeDeclaration td = vd.getTypeDeclaration();
                if (td != null && td.getInitialization() != null) {
                    td.getInitialization().accept(this);
                } else {

                    calleeState.put(lift(vd),
                            valueTranslator.translate(
                                    initValueTranslator.getInit(vd.getDataType())));
                }
            }
        }
        //endregion

        //region transfer variables
        List<Invocation.Parameter> parameters = invocation.getParameters();
        List<VariableDeclaration> inputVars = fd.getLocalScope().filterByFlags(
                VariableDeclaration.INPUT | VariableDeclaration.INOUT | VariableDeclaration.OUTPUT);

        if (parameters.size() > inputVars.size()) {
            System.err.println(fd.getFunctionName());
            inputVars.stream().map(VariableDeclaration::getName).forEach(System.err::println);
            throw new FunctionInvocationArgumentNumberException();
        }

        for (int i = 0; i < parameters.size(); i++) {
            Invocation.Parameter parameter = parameters.get(i);
            if (parameter.isOutput())
                continue;
            if (parameter.getName() == null)
                // name from definition, in order of declaration, expression from caller site
                calleeState.put(lift(inputVars.get(i)), parameter.getExpression().accept(this));
            else {
                Optional o = inputVars.stream().filter(iv -> iv.getName().equals(parameter.getName())).findAny();
                if (o.isPresent()) {
                    SMVExpr e = parameter.getExpression().accept(this);
                    assert e != null;
                    calleeState.put(lift((VariableDeclaration) o.get()), e);
                }
            }
        }

        for (VariableDeclaration outputVar : fd.getLocalScope().filterByFlags(VariableDeclaration.OUTPUT))
            calleeState.put(lift(outputVar), valueTranslator.translate(
                    initValueTranslator.getInit(outputVar.getDataType())));

        push(calleeState);
        //endregion

        // execution of body
        fd.getStatements().accept(this);

        SymbolicState returnState = pop();
        // Update output variables
        List<Invocation.Parameter> outputParameters = invocation.getParameters();
        List<VariableDeclaration> outputVars = fd.getLocalScope().filterByFlags(
                VariableDeclaration.OUTPUT | VariableDeclaration.INOUT);
        for (Invocation.Parameter parameter : outputParameters) {
            Optional o = outputVars.stream().filter(iv -> iv.getName().equals(parameter.getName())).findAny();
            if (o.isPresent())
                replace(lift((SymbolicReference) parameter.getExpression()),
                        returnState.get(lift((VariableDeclaration) o.get())));
        }

        return fd.getReturnType() != null
                ? calleeState.get(lift(fd.getLocalScope().getVariable(fd.getFunctionName())))
                : null;
    }

    //endregion

    @NotNull
    @Override
    public SLiteral visit(@NotNull Literal literal) {
        return valueTranslator.translate(literal);
    }

    @Nullable
    @Override
    public SCaseExpression visit(@NotNull ProgramDeclaration programDeclaration) {
        localScope = programDeclaration.getLocalScope();
        globalScope = localScope.getGlobalScope();

        push(new SymbolicState(localScope.getLocalVariables().size()));

        // initialize root state
        for (VariableDeclaration vd : localScope) {
            SVariable s = lift(vd);
            peek().put(s, s);
        }

        globalState = new SymbolicState();
        for (VariableDeclaration var : localScope.filterByFlags(VariableDeclaration.GLOBAL))
            globalState.put(lift(var), get(lift(var)));

        programDeclaration.getProgramBody().accept(this);
        return null;
    }

    @Nullable
    @Override
    public SMVExpr visit(@NotNull AssignmentStatement assign) {
        put(lift((SymbolicReference) assign.getLocation()),
                assign.getExpression().accept(this));
        return null;
    }

    @Nullable
    @Override
    public SCaseExpression visit(@NotNull StatementList statements) {
        for (Statement s : statements) {
            if (s instanceof ExitStatement) {
                return null;
            }
            s.accept(this);
        }
        return null;
    }

    @Override
    public SMVExpr visit(@NotNull InvocationStatement fbc) {
        return visit(fbc.getInvocation());
    }

    @NotNull
    @Override
    public SMVExpr visit(@NotNull SymbolicReference symbolicReference) {
        if (symbolicReference.getDataType() instanceof EnumerateType
                && ((EnumerateType) symbolicReference.getDataType())
                .getAllowedValues().contains(symbolicReference.getIdentifier()))
            return valueTranslator.translate(new Values.VAnyEnum(
                    (EnumerateType) symbolicReference.getDataType(),
                    symbolicReference.getIdentifier()));
        else
            return get(lift(symbolicReference));
    }

    @Nullable
    @Override
    public SCaseExpression visit(@NotNull IfStatement statement) {
        SymbolicBranches branchStates = new SymbolicBranches();

        for (GuardedStatement gs : statement.getConditionalBranches()) {
            SMVExpr condition = gs.getCondition().accept(this);
            push();
            gs.getStatements().accept(this);
            branchStates.addBranch(condition, pop());
        }

        push();
        statement.getElseBranch().accept(this);
        branchStates.addBranch(SLiteral.TRUE, pop());

        peek().putAll(branchStates.asCompressed());
        return null;
    }

    @Nullable
    @Override
    public SMVExpr visit(@NotNull CaseStatement caseStatement) {
        SymbolicBranches branchStates = new SymbolicBranches();

        for (CaseStatement.Case gs : caseStatement.getCases()) {
            SMVExpr condition = buildCondition(caseStatement.getExpression(), gs);
            push();
            gs.getStatements().accept(this);
            branchStates.addBranch(condition, pop());
        }

        push();
        caseStatement.getElseCase().accept(this);
        branchStates.addBranch(SLiteral.TRUE, pop());

        peek().putAll(branchStates.asCompressed());
        return null;
    }

    private SMVExpr buildCondition(Expression e, @NotNull CaseStatement.Case c) {
        caseExpression = e;
        return c.getConditions()
                .stream()
                .map(a -> a.accept(this))
                .reduce(SMVFacade.reducer(SBinaryOperator.OR)).get();
    }

    @NotNull
    @Override
    public SMVExpr visit(@NotNull CaseCondition.Range r) {
        BinaryExpression lower = new BinaryExpression(caseExpression, r.getStart(), Operators.GREATER_EQUALS);
        BinaryExpression upper = new BinaryExpression(r.getStop(), caseExpression, Operators.GREATER_EQUALS);
        BinaryExpression and = new BinaryExpression(lower, upper, Operators.AND);
        return and.accept(this);
    }

    @NotNull
    @Override
    public SMVExpr visit(@NotNull CaseCondition.IntegerCondition i) {
        BinaryExpression be = new BinaryExpression(caseExpression, i.getValue(), Operators.EQUALS);
        return be.accept(this);
    }


    @NotNull
    @Override
    public SMVExpr visit(@NotNull CaseCondition.Enumeration e) {
        BinaryExpression be = new BinaryExpression(caseExpression, e.getStart(), Operators.EQUALS);
        return be.accept(this);
        //TODO rework case conditions
    }
}
