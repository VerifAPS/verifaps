package edu.kit.iti.formal.automation.st.ast;

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

import com.google.common.base.Predicates;
import edu.kit.iti.formal.automation.parser.IEC61131Parser;
import edu.kit.iti.formal.automation.st.IdentifierPlaceHolder;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by weigl on 11.06.14.
 *
 * @author weigl
 * @version $Id: $Id
 */
@EqualsAndHashCode
@ToString
public class FunctionBlockCallStatement extends Statement {
    private IdentifierPlaceHolder<FunctionBlockDeclaration> calleeName = new IdentifierPlaceHolder<>();
    private List<Parameter> parameters = new ArrayList<>();

    public FunctionBlockCallStatement(String fnName, Expression... expr) {
        setFunctionBlockName(fnName);
        for (Expression e : expr) {
            parameters.add(new Parameter(e));
        }
    }

    public FunctionBlockCallStatement() {

    }

    /**
     * {@inheritDoc}
     */
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getFunctionBlockName() {
        return calleeName.getIdentifier();
    }

    public void setFunctionBlockName(String functionName) {
        calleeName.setIdentifier(functionName);
    }

    public void addInputParameter(String key, Expression visit) {
        if (visit == null)
            throw new IllegalArgumentException("expression can not be null");
        parameters.add(new Parameter(key, false, visit));
    }

    /**
     * <p>addOutputParameter.</p>
     *
     * @param key   a {@link java.lang.String} object.
     * @param visit a {@link edu.kit.iti.formal.automation.st.ast.Reference} object.
     */
    public void addOutputParameter(String key, Reference visit) {
        assert key != null;
        assert visit != null;
        parameters.add(new Parameter(key, false, visit));
    }

    /**
     * <p>Getter for the field <code>parameters</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * <p>Setter for the field <code>parameters</code>.</p>
     *
     * @param parameters a {@link java.util.List} object.
     */
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public FunctionBlockCallStatement copy() {
        FunctionBlockCallStatement f = new FunctionBlockCallStatement();
        f.setFunctionBlockName(getFunctionBlockName());
        f.parameters = parameters.stream().map(Parameter::copy)
                .collect(Collectors.toList());
        return f;
    }

    public Stream<Parameter> getOutputParameters() {
        return getParameters().stream().filter(Parameter::isOutput);
    }

    public Stream<Parameter> getInputParameters() {
        return getParameters().stream().filter(Predicates.not(Parameter::isOutput));
    }

    public FunctionBlockDeclaration getCallee() {
        return calleeName.getIdentifiedObject();
    }

    public String getCalleeName() {
        return calleeName.getIdentifier();
    }


    @Data
    @EqualsAndHashCode
    @ToString
    public static class Parameter
            extends Top<IEC61131Parser.Param_assignmentContext> {
        private String name;
        private boolean output;
        private Expression expression;

        public Parameter() {
        }

        public Parameter(String name, boolean output, Expression expression) {
            this.name = name;
            this.output = output;
            this.expression = expression;
        }

        public Parameter(Expression expr) {
            this(null, false, expr);
        }

        @Override
        public Parameter copy() {
            return new Parameter(name, output, expression.copy());
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

}
