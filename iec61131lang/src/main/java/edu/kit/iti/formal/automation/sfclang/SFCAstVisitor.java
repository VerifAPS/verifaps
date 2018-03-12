package edu.kit.iti.formal.automation.sfclang;

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

import edu.kit.iti.formal.automation.sfclang.ast.SFCDeclaration;
import edu.kit.iti.formal.automation.sfclang.ast.StepDeclaration;
import edu.kit.iti.formal.automation.sfclang.ast.TransitionDeclaration;

/**
 * <p>SFCAstVisitor interface.</p>
 *
 * @author weigl
 * @version $Id: $Id
 */
public interface SFCAstVisitor<T> {
    /**
     * <p>accept.</p>
     *
     * @param decl a {@link edu.kit.iti.formal.automation.sfclang.ast.SFCDeclaration} object.
     * @return a T object.
     */
    T visit(SFCDeclaration decl);

    /**
     * <p>accept.</p>
     *
     * @param decl a {@link edu.kit.iti.formal.automation.sfclang.ast.StepDeclaration} object.
     * @return a T object.
     */
    T visit(StepDeclaration decl);

    /**
     * <p>accept.</p>
     *
     * @param decl a {@link edu.kit.iti.formal.automation.sfclang.ast.TransitionDeclaration} object.
     * @return a T object.
     */
    T visit(TransitionDeclaration decl);
}