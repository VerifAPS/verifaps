package edu.kit.iti.formal.automation.st.ast;

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2016 - 2017 Alexander Weigl
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

import edu.kit.iti.formal.automation.scope.GlobalScope;
import edu.kit.iti.formal.automation.st.IdentifierPlaceHolder;
import edu.kit.iti.formal.automation.visitors.Visitor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Weigl, Augusto Modanese
 * @version 1 (20.02.17)
 */

@Data
@EqualsAndHashCode(exclude = "methods")
public class ClassDeclaration extends TopLevelScopeElement {
    private String name;
    private boolean final_ = false;
    private boolean abstract_ = false;
    private IdentifierPlaceHolder<ClassDeclaration> parent = new IdentifierPlaceHolder<>();
    private List<IdentifierPlaceHolder<InterfaceDeclaration>> interfaces = new ArrayList<>();
    private List<MethodDeclaration> methods = new ArrayList<>();

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override public String getIdentifier() {
        return name;
    }

    public void setParent(String parent) {
        this.parent.setIdentifier(parent);
    }

    public void setMethods(List<MethodDeclaration> methods) {
        for (MethodDeclaration methodDeclaration : methods)
            methodDeclaration.setParent(this);
        this.methods = methods;
    }

    public void addImplements(String interfaze) {
        interfaces.add(new IdentifierPlaceHolder<>(interfaze));
    }

    public void addImplements(List<String> interfaceList) {
        interfaceList.forEach(i -> addImplements(i));
    }

    @Override
    public void setGlobalScope(GlobalScope global) {
        super.setGlobalScope(global);
        if (parent.getIdentifier() != null)
            parent.setIdentifiedObject(global.resolveClass(parent.getIdentifier()));
        for (IdentifierPlaceHolder<InterfaceDeclaration> interfaceDeclaration : interfaces)
            interfaceDeclaration.setIdentifiedObject(global.resolveInterface(interfaceDeclaration.getIdentifier()));
    }

    /**
     * To be called only after bound to global scope!
     * @return The parent class. Return null if the class has no parent.
     */
    public ClassDeclaration getParentClass() {
        return parent.getIdentifiedObject();
    }

    /**
     * To be called only after bound to global scope!
     * @return The list of classes the class can be an instance of, taking polymorphy into account.
     */
    public List<ClassDeclaration> getExtendedClasses() {
        List<ClassDeclaration> extendedClasses = new ArrayList<>();
        extendedClasses.add(this);
        ClassDeclaration parentClass = getParentClass();
        if (parentClass != null)
            extendedClasses.addAll(parentClass.getExtendedClasses());
        return extendedClasses;
    }

    /**
     * To be called only after bound to global scope!
     * @return Whether the class extends the given other class.
     */
    public boolean extendsClass(ClassDeclaration otherClass) {
        ClassDeclaration parentClass = getParentClass();
        if (parentClass == otherClass)
            return true;
        else if (parentClass == null)
            return false;  // reached top of hierarchy
        return getParentClass().extendsClass(otherClass);
    }

    /**
     * To be called only after bound to global scope!
     * @return The interfaces the class implements. Includes the interfaces of all parent classes.
     */
    public List<InterfaceDeclaration> getImplementedInterfaces() {
        List<InterfaceDeclaration> implementedInterfaces = interfaces.stream()
                .map(IdentifierPlaceHolder::getIdentifiedObject).collect(Collectors.toList());
        // Add interfaces from parent classes
        ClassDeclaration parentClass = getParentClass();
        if (parentClass != null)
            implementedInterfaces.addAll(parentClass.getImplementedInterfaces());
        // Add extended interfaces
        implementedInterfaces.addAll(implementedInterfaces.stream()
                .map(InterfaceDeclaration::getExtendedInterfaces)
                .flatMap(Collection::stream).collect(Collectors.toList()));
        return implementedInterfaces;
    }

    /**
     * To be called only after bound to global scope!
     * @return Whether the class implements the given interface.
     */
    public boolean implementsInterface(InterfaceDeclaration interfaceDeclaration) {
        return getImplementedInterfaces().contains(interfaceDeclaration);
    }

    @Override public ClassDeclaration copy() {
        ClassDeclaration c = new ClassDeclaration();
        c.name = name;
        c.final_ = final_;
        c.abstract_ = abstract_;
        c.parent = parent.copy();
        interfaces.forEach(i -> c.interfaces.add(i.copy()));
        methods.forEach(m -> c.methods.add(m.copy()));
        return c;
    }
}
