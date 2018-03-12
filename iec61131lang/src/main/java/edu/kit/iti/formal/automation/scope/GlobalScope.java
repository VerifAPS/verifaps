package edu.kit.iti.formal.automation.scope;

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

import com.google.common.collect.ImmutableMap;
import edu.kit.iti.formal.automation.analysis.ResolveDataTypes;
import edu.kit.iti.formal.automation.datatypes.*;
import edu.kit.iti.formal.automation.exceptions.DataTypeNotDefinedException;
import edu.kit.iti.formal.automation.st.ast.*;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by weigl on 24.11.16.
 *
 * @author weigl, Augusto Modanese
 * @version $Id: $Id
 */
@Data
public class GlobalScope implements Serializable {
    @NotNull
    private Map<String, ProgramDeclaration> programs = new HashMap<>();
    @NotNull
    private Map<String, FunctionBlockDeclaration> fb = new HashMap<>();
    // TODO expand built-in function list
    @NotNull
    private Map<String, FunctionDeclaration> functions = new HashMap<>(ImmutableMap.of(
            "REF", new FunctionDeclaration("REF")
    ));
    @NotNull
    private Map<String, TypeDeclaration> dataTypes = new HashMap<>();
    @NotNull
    private List<FunctionResolver> functionResolvers = new LinkedList<>();
    @NotNull
    private TypeScope types = TypeScope.builtin();
    @NotNull
    private Map<String, ClassDeclaration> classes = new LinkedHashMap<>();
    @NotNull
    private Map<String, InterfaceDeclaration> interfaces = new LinkedHashMap<>();
    @NotNull
    private LocalScope globalVariableList = new LocalScope();
    @NotNull
    private Map<String, EnumerationTypeDeclaration> enumerateTypeMap = new HashMap<>();

    /**
     * <p>defaultScope.</p>
     *
     * @return a {@link edu.kit.iti.formal.automation.scope.GlobalScope} object.
     */
    @NotNull
    public static GlobalScope defaultScope() {
        GlobalScope g = new GlobalScope();
        g.functionResolvers.add(new DefinedFunctionResolver());
        g.functionResolvers.add(new FunctionResolverMUX());
        return g;
    }

    /**
     * <p>getProgram.</p>
     *
     * @param key a {@link java.lang.Object} object.
     * @return a {@link edu.kit.iti.formal.automation.st.ast.ProgramDeclaration} object.
     */
    public ProgramDeclaration getProgram(@NotNull Object key) {
        return programs.get(key);
    }

    @NotNull
    public List<ProgramDeclaration> getPrograms() {
        return new ArrayList<>(programs.values());
    }

    /**
     * <p>getCalleeName.</p>
     *
     * @param key a {@link java.lang.Object} object.
     * @return a {@link edu.kit.iti.formal.automation.st.ast.FunctionBlockDeclaration} object.
     */
    public FunctionBlockDeclaration getFunctionBlock(@NotNull Object key) {
        return fb.get(key);
    }

    @NotNull
    public List<FunctionBlockDeclaration> getFunctionBlocks() {
        return new ArrayList<>(fb.values());
    }

    /**
     * <p>getInvoked.</p>
     *
     * @param key a {@link java.lang.Object} object.
     * @return a {@link edu.kit.iti.formal.automation.st.ast.FunctionDeclaration} object.
     */
    public FunctionDeclaration getFunction(String key) {
        return functions.get(key);
    }

    /**
     * <p>registerProgram.</p>
     *
     * @param programDeclaration a {@link edu.kit.iti.formal.automation.st.ast.ProgramDeclaration} object.
     */
    public void registerProgram(@NotNull ProgramDeclaration programDeclaration) {
        programs.put(programDeclaration.getIdentifier(), programDeclaration);
    }

    /**
     * <p>registerFunction.</p>
     *
     * @param functionDeclaration a {@link edu.kit.iti.formal.automation.st.ast.FunctionDeclaration} object.
     */
    public void registerFunction(@NotNull FunctionDeclaration functionDeclaration) {
        functions.put(functionDeclaration.getIdentifier(), functionDeclaration);
    }

    /**
     * <p>registerFunctionBlock.</p>
     *
     * @param fblock a {@link edu.kit.iti.formal.automation.st.ast.FunctionBlockDeclaration} object.
     */
    public void registerFunctionBlock(@NotNull FunctionBlockDeclaration fblock) {
        fb.put(fblock.getIdentifier(), fblock);
        classes.put(fblock.getIdentifier(), fblock);
    }

    /**
     * <p>registerType.</p>
     *
     * @param dt a {@link edu.kit.iti.formal.automation.st.ast.TypeDeclaration} object.
     */
    public void registerType(@NotNull TypeDeclaration dt) {
        dataTypes.put(dt.getTypeName(), dt);
    }

    public void registerType(@NotNull EnumerationTypeDeclaration enumerationTypeDeclaration) {
        registerType((TypeDeclaration) enumerationTypeDeclaration);
        enumerationTypeDeclaration.getAllowedValues().forEach(
                v -> enumerateTypeMap.put(v.getText(), enumerationTypeDeclaration));
    }

    public Any resolveDataType(@NotNull ClassDeclaration classDeclaration) {
        return resolveDataType(classDeclaration.getName());
    }

    public Any resolveDataType(@NotNull EnumerateType enumerateType) {
        return resolveDataType(enumerateType.getName());
    }

    /**
     * <p>resolveDataType.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link edu.kit.iti.formal.automation.datatypes.Any} object.
     */
    public Any resolveDataType(@NotNull String name) {
        if (types.containsKey(name))
            return types.get(name);

        boolean a = fb.containsKey(name);
        boolean b = dataTypes.containsKey(name);
        boolean c = classes.containsKey(name);
        boolean d = interfaces.containsKey(name);

        if (a && b || a && c || b && c) {
            System.out.println("ambguity fb vs. type");
        }

        Any q;
        if (a) {
            q = new FunctionBlockDataType(fb.get(name));
            types.put(name, q);
            return q;
        }

        if (c) {
            q = new ClassDataType(classes.get(name));
            types.put(name, q);
            return q;
        }

        if (d) {
            q = new InterfaceDataType(interfaces.get(name));
            types.put(name, q);
            return q;
        }

        if (b) {
            q = dataTypes.get(name).getDataType(this);
            types.put(name, q);
            return q;
        }

        // Reference
        if (name.length() > 7 && name.substring(0, 7).equals("REF_TO "))
            return new ReferenceType(resolveDataType(name.substring(7)));

        throw new DataTypeNotDefinedException("Could not find: " + name);
    }

    /**
     * <p>resolveFunction.</p>
     *
     * @param invocation a {@link Invocation} object.
     * @param local        a {@link edu.kit.iti.formal.automation.scope.LocalScope} object.
     * @return a {@link edu.kit.iti.formal.automation.st.ast.FunctionDeclaration} object.
     */
    @Nullable
    public FunctionDeclaration resolveFunction(Invocation invocation,
                                               LocalScope local) {
        for (FunctionResolver fr : functionResolvers) {
            FunctionDeclaration decl = fr.resolve(invocation, local);
            if (decl != null)
                return decl;
        }
        return null;
    }

    /**
     * Used to make a class or interface to be known.
     *
     * @param clazz
     * @see ResolveDataTypes
     */
    public void registerClass(@NotNull ClassDeclaration clazz) {
        classes.put(clazz.getIdentifier(), clazz);
    }

    public ClassDeclaration resolveClass(@NotNull String key) {
        ClassDeclaration classDeclaration = classes.get(key);
        if (classDeclaration == null)
            classDeclaration = getFunctionBlock(key);
        return classDeclaration;
    }

    @NotNull
    public List<ClassDeclaration> getClasses() {
        return new ArrayList<>(classes.values());
    }

    public void registerInterface(@NotNull InterfaceDeclaration interfaceDeclaration) {
        interfaces.put(interfaceDeclaration.getName(), interfaceDeclaration);
    }

    public InterfaceDeclaration resolveInterface(@NotNull String key) {
        return interfaces.get(key);
    }

    @NotNull
    public List<InterfaceDeclaration> getInterfaces() {
        return new ArrayList<>(interfaces.values());
    }

    @NotNull
    public GlobalScope clone() {
        GlobalScope gs = new GlobalScope();
        gs.classes = new HashMap<>(classes);
        gs.interfaces = new HashMap<>(interfaces);
        gs.dataTypes = new HashMap<>(dataTypes);
        gs.fb = new HashMap<>(fb);
        gs.functionResolvers = new ArrayList<>(functionResolvers);
        gs.functions = new HashMap<>(functions);
        gs.programs = new HashMap<>(programs);
        gs.types = types.clone();
        // Needed to resolve circular dependency
        globalVariableList.setGlobalScope(null);
        gs.globalVariableList = globalVariableList.copy();
        gs.globalVariableList.setGlobalScope(this);
        globalVariableList.setGlobalScope(this);
        return gs;
    }

    public List<EnumerateType> getEnumerateTypes() {
        return dataTypes.values().stream()
                .map(t -> t.getBaseType())
                .filter(t -> t instanceof EnumerateType)
                .map(t -> (EnumerateType) t)
                .collect(Collectors.toList());
    }

    public List<String> getAllowedTypeValues() {
        return getEnumerateTypes().stream()
                .flatMap(t -> t.getAllowedValues().stream())
                .collect(Collectors.toList());
    }

    @NotNull
    public EnumerateType getTypeOfValue(String value) {
        return getEnumerateTypes().stream()
                .filter(t -> t.isAllowedValue(value))
                .findAny().get();
    }
}