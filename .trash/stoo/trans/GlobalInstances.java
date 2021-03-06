/*
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2017 Alexander Weigl
 * %%
 * This program isType free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program isType distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a clone of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package edu.kit.iti.formal.automation.stoo.trans;

import edu.kit.iti.formal.automation.datatypes.*;
import edu.kit.iti.formal.automation.scope.InstanceScope;
import edu.kit.iti.formal.automation.scope.Scope;
import edu.kit.iti.formal.automation.st.ast.*;
import edu.kit.iti.formal.automation.st.util.AstVisitor;
import edu.kit.iti.formal.automation.st.util.Tuple;
import edu.kit.iti.formal.automation.stoo.STOOSimplifier;
import edu.kit.iti.formal.automation.visitors.Utils;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Create arrays in the GVL to hold all instances. Add instance ID to class as attribute.
 *
 * @author Augusto Modanese
 */
public class GlobalInstances extends STOOTransformation {
    @Override
    public void transform(STOOSimplifier.State state) {
        // Add GVL if none
        if (state.getTopLevelElements().stream().noneMatch(tle -> tle instanceof GlobalVariableListDeclaration)) {
            GlobalVariableListDeclaration gvl = new GlobalVariableListDeclaration();
            gvl.setScope(state.getGlobalScope());
            // Add right before program; that's the safest spot to prevent undefined types and other issues
            state.getTopLevelElements().add(
                    state.getTopLevelElements().indexOf(Utils.INSTANCE.findProgram(state.getTopLevelElements())),
                    gvl);
            state.getScope().getTopLevel().addVariables(gvl.getScope());
        }
        // Add instance ID type
        SubRangeTypeDeclaration instanceIDType = new SubRangeTypeDeclaration(
                new Range(NULL_REFERENCE_ID, state.getInstances().size() - 1));
        instanceIDType.setTypeName(INSTANCE_ID_VAR_NAME + INSTANCE_ID_TYPE_SUFFIX);
        instanceIDType.setBaseTypeName(DataTypes.INSTANCE.getINT().getName());
        instanceIDType.setInitialization(
                new Literal(instanceIDType.getDataType(state.getScope()),
                        Integer.toString(NULL_REFERENCE_ID)));
        state.getTopLevelElements().add(new TypeDeclarations(instanceIDType));
        state.getScope().registerType(instanceIDType);
        // Add global instance arrays
        Scope gvl = state.getScope().getTopLevel();
        for (ClassDeclaration classDeclaration : state.getScope().getClasses().values()) {
            if (state.getInstanceScope().getPolymorphInstancesOfClass(classDeclaration).size() == 0)
                continue;  // ignore if no instances present
            // Set array type declaration
            ArrayTypeDeclaration instanceArray = new ArrayTypeDeclaration();
            for (Tuple<Integer, Integer> range : state.getInstanceIDRangesToClass(classDeclaration))
                instanceArray.addSubRange(new Range(range.getA(), range.getB()));
            instanceArray.setBaseType(state.getScope().resolveDataType(classDeclaration.getName()));
            // Set instances initializations in the array's initializations
            ArrayInitialization arrayInitialization = new ArrayInitialization();
            List<InstanceScope.Instance> classInstances = state.getInstanceScope()
                    .getPolymorphInstancesOfClass(classDeclaration);
            classInstances.sort(Comparator.comparingInt(state::getInstanceID));
            for (InstanceScope.Instance instance : classInstances) {
                // Initialization contains only variables in the class' scope (no inheritance)
                StructureInitialization instanceInitialization = new StructureInitialization(
                        instance.getInitialization().getInitValues().entrySet().stream()
                                .filter(entry -> classDeclaration.getScope().hasVariable(entry.getKey()))
                                .collect(Collectors.toList()));
                // Include instance ID in its initialization
                instanceInitialization.addField(INSTANCE_ID_VAR_NAME,
                        new Literal(instanceIDType.getDataType(state.getScope()),
                                Integer.toString(state.getInstanceID(instance))));
                // Initialize child instances too
                for (VariableDeclaration variable : classDeclaration.getScope().stream()
                        .filter(v -> v.getDataType() instanceof ClassDataType
                                || v.getDataType() instanceof ReferenceType
                                || v.getDataType() instanceof InterfaceDataType)
                        .collect(Collectors.toList())) {
                    Optional<InstanceScope.Instance> parentInstance = state.getInstancesOfVariable(variable).stream()
                            .filter(i -> i.getParent().equals(instance)).findAny();
                    parentInstance.ifPresent(i -> instanceInitialization.addField(variable.getName(),
                            new Literal(instanceIDType.getDataType(state.getGlobalScope()),
                                    Integer.toString(state.getInstanceID(i)))));
                }
                arrayInitialization.add(instanceInitialization);
            }
            instanceArray.setInitialization(arrayInitialization);
            // Add the array to the GVL
            VariableDeclaration arrayVar = new VariableDeclaration(
                    INSTANCE_ARRAY_NAME_PREFIX + classDeclaration.getName(), instanceArray);
            arrayVar.setType(VariableDeclaration.Companion.getGLOBAL());
            arrayVar.setTypeDeclaration(instanceArray);
            arrayVar.setDataType(new IECArray(instanceArray));
            gvl.add(arrayVar);
        }
        // Add instance IDs
        state.getTopLevelElements().accept(
                new AddInstanceIDVisitor(instanceIDType.getDataType(state.getScope())));
    }

    @AllArgsConstructor
    private static class AddInstanceIDVisitor extends AstVisitor<Object> {
        private final AnyDt instanceIDType;

        @Override
        public Object visit(ClassDeclaration clazz) {
            // Add instance ID to local scope
            VariableDeclaration instanceID = new VariableDeclaration();
            instanceID.setName(INSTANCE_ID_VAR_NAME);
            //instanceID.setType(VariableDeclaration.PUBLIC | VariableDeclaration.CONSTANT);
            instanceID.setTypeDeclaration(new SimpleTypeDeclaration());
            instanceID.setDataType(instanceIDType);
            instanceID.setInit(new Literal(instanceIDType, Integer.toString(NULL_REFERENCE_ID)));
            instanceID.setParent(clazz);
            clazz.getScope().add(instanceID);
            return super.visit(clazz);
        }

        @Override
        public Object visit(FunctionBlockDeclaration functionBlockDeclaration) {
            return visit((ClassDeclaration) functionBlockDeclaration);
        }
    }
}
