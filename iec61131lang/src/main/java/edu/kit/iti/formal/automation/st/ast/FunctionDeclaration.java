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

import edu.kit.iti.formal.automation.datatypes.AnyDt;
import edu.kit.iti.formal.automation.parser.IEC61131Parser;
import edu.kit.iti.formal.automation.st.Identifiable;
import edu.kit.iti.formal.automation.st.IdentifierPlaceHolder;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.*;

/**
 * Created by weigl on 13.06.14.
 *
 * @author weigl, Augusto Modanese
 * @version $Id: $Id
 */
@EqualsAndHashCode(exclude = "stBody", callSuper = true)
@ToString(exclude = "stBody")
@NoArgsConstructor
@Getter
@Setter
public class FunctionDeclaration extends TopLevelScopeElement<IEC61131Parser.Function_declarationContext>
        implements Invocable, Identifiable {
    protected IdentifierPlaceHolder<AnyDt> returnType = new IdentifierPlaceHolder<>();
    protected String name;
    protected StatementList stBody = new StatementList();

    public FunctionDeclaration(String name) {
        this.name = name;
    }

    public String getReturnTypeName() {
        if (returnType.getIdentifier() == null)
            return "VOID";
        return returnType.getIdentifier();
    }

    public void setReturnTypeName(String rt) {
        returnType.setIdentifier(rt);
    }

    public AnyDt getReturnType() {
        return returnType.getIdentifiedObject();
    }

    public void setReturnType(AnyDt rt) {
        returnType.setIdentifiedObject(rt);
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getIdentifier() {
        return getName();
    }

    @Override
    public FunctionDeclaration copy() {
        FunctionDeclaration fd = new FunctionDeclaration();
        fd.name = name;
        fd.returnType = returnType.copy();
        fd.stBody= stBody.copy();
        return fd;
    }
}
