package edu.kit.iti.formal.automation.analysis;


/*-
 * #%L
 * iec61131lang
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

import edu.kit.iti.formal.automation.datatypes.AnyDt;
import edu.kit.iti.formal.automation.datatypes.EnumerateType;
import edu.kit.iti.formal.automation.exceptions.DataTypeNotDefinedException;
import edu.kit.iti.formal.automation.scope.Scope;
import edu.kit.iti.formal.automation.st.ast.*;
import edu.kit.iti.formal.automation.st.util.AstVisitor;
import lombok.val;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Search and set the data type attributes based on the given global scope.
 *
 * @author Alexander Weigl, Augusto Modanese
 * @version 1
 * @since 25.11.16
 */
public class ResolveDataTypes extends AstVisitor<Object> {
    private final Scope globalScope;

    public ResolveDataTypes(Scope globalScope) {
        this.globalScope = globalScope;
    }

    @Override
    public Object visit(ProgramDeclaration programDeclaration) {
        super.visit(programDeclaration);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object visit(FunctionDeclaration functionDeclaration) {
        super.visit(functionDeclaration);
        functionDeclaration.setReturnType(
                currentScope.resolveDataType(functionDeclaration.getReturnTypeName()));
        return null;
    }

    @Override
    public Object visit(FunctionBlockDeclaration functionBlockDeclaration) {
        visit((ClassDeclaration) functionBlockDeclaration);
        return super.visit(functionBlockDeclaration);
    }

    @Override
    public Object visit(Scope localScope) {
        currentScope = localScope;
        currentScope.setParent(globalScope);

        localScope.getVariables().values().forEach(vd -> {
            vd.setDataType(currentScope.resolveDataType(vd.getDataTypeName()));
            if (vd.getInit() != null) {
                vd.getInit().accept(this);
            }
        });
        return null;
    }

    @Override
    public Object visit(VariableDeclaration variableDeclaration) {
        variableDeclaration.setDataType(
                variableDeclaration.getTypeDeclaration()
                        .getDataType(globalScope));
        return null;
    }

    @Override
    public Object visit(ClassDeclaration classDeclaration) {
        if (classDeclaration.getParent().getIdentifier() != null) {
            classDeclaration.getParent().setIdentifiedObject(
                    classDeclaration.getScope().resolveClass(classDeclaration.getParent().getIdentifier()));
            assert classDeclaration.getParentClass() != null;
        }
        visitClassifier(classDeclaration);
        return super.visit(classDeclaration);
    }

    @Override
    public Object visit(InterfaceDeclaration interfaceDeclaration) {
        visitClassifier(interfaceDeclaration);
        return super.visit(interfaceDeclaration);
    }

    @Override
    public Object visit(MethodDeclaration methodDeclaration) {
        super.visit(methodDeclaration);
        methodDeclaration.setReturnType(currentScope.resolveDataType(methodDeclaration.getReturnTypeName()));
        return null;
    }

    @Override
    public Object visit(GlobalVariableListDeclaration globalVariableListDeclaration) {
        globalVariableListDeclaration.getScope().setParent(globalScope);
        return super.visit(globalVariableListDeclaration);
    }

    @Override
    public Object visit(ArrayTypeDeclaration arrayTypeDeclaration) {
        arrayTypeDeclaration.setBaseType(globalScope.resolveDataType(arrayTypeDeclaration.getTypeName()));
        return super.visit(arrayTypeDeclaration);
    }

    @Override
    public Object visit(SymbolicReference ref) {
        String first = ref.getIdentifier();
        try {
            AnyDt dataType = currentScope.resolveDataType(first);
            EnumerateType et = (EnumerateType) dataType;
            ref.setDataType(et);
            if (ref.getSub() != null) {
                String second = ((SymbolicReference) ref.getSub()).getIdentifier();
                // TODO...?
            }
        } catch (ClassCastException | DataTypeNotDefinedException e) {

        }
        return null;
    }

    @Override
    public Object visit(Literal literal) {
        try {
            literal.setDataType(currentScope.resolveDataType(literal.getDataTypeName()));
        } catch (ClassCastException | DataTypeNotDefinedException e) {
        }
        return null;
    }

    private <T extends ParserRuleContext> void visitClassifier(Classifier<T> c) {
        val seq = c.getInterfaces();
        seq.forEach(face ->
                face.setIdentifiedObject(c.getScope().resolveInterface(face.getIdentifier())));
    }
}
