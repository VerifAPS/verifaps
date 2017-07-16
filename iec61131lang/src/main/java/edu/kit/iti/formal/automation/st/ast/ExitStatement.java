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


import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by weigla on 09.06.2014.
 *
 * @author weigl
 * @version $Id: $Id
 */
@EqualsAndHashCode
@ToString
public class ExitStatement extends Statement {
    /** Constant <code>EXIT_STATMENT</code> */
    public static ExitStatement EXIT_STATMENT = new ExitStatement();

    /**
     * <p>Constructor for ExitStatement.</p>
     */
    public ExitStatement() {
    }

    /** {@inheritDoc} */
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Statement copy() {
        ExitStatement es = new ExitStatement();
        es.setRuleContext(getRuleContext());
        return es;
    }
}
