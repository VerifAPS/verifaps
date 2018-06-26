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

package edu.kit.iti.formal.automation

import edu.kit.iti.formal.automation.st.ast.*
import edu.kit.iti.formal.automation.st.util.AstVisitor
import edu.kit.iti.formal.smv.ModuleType
import edu.kit.iti.formal.smv.SMVPrinter
import edu.kit.iti.formal.smv.SMVType
import edu.kit.iti.formal.smv.ast.SMVModule
import edu.kit.iti.formal.smv.ast.SVariable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Arrays
import java.util.Objects
import java.util.stream.Collectors

/**
 * @author Augusto Modanese
 * @RunWith(Parameterized.class)
 * public class FacadeSTFileTest {
 * private static final String RESOURCES_PATH = "edu/kit/iti/formal/automation/smv/eval";
 * @Parameterized.Parameter
 * public File file;
 *
 * private Process nuxmv;
 *
 * private static File[] getSTFiles() {
 * URL f = STOOIntegrationTests.class.getClassLoader().getResource(FacadeSTFileTest.RESOURCES_PATH);
 * if (f == null) {
 * System.err.format("Could not find %s%n", FacadeSTFileTest.RESOURCES_PATH);
 * return new File[0];
 * }
 * File file = new File(f.getFile());
 * return Arrays.stream(Objects.requireNonNull(file.listFiles()))
 * .filter(s -> s.getName().contains(".st")).toArray(File[]::new);
 * }
 * @Parameterized.Parameters
 * public static Object[] files() {
 * return getSTFiles();
 * }
 *
 * private static int countStatements(PouElements code) {
 * StatementCounter counter = new StatementCounter();
 * code.accept(counter);
 * return counter.count;
 * }
 *
 * private static void write(SMVModule m, String fileName, boolean append) {
 * SMVPrinter.toFile(m, new File(fileName), append);
 * }
 *
 * private Path getSMVFile() {
 * return Paths.get(getSMVDirectory() + "/" + file.getName() + ".smv");
 * }
 *
 * private Path getSMVDirectory() {
 * return Paths.get(file.getParent() + "/gen/");
 * }
 * @Before
 * public void setUp() throws IOException {
 * if (!Files.exists(getSMVDirectory()))
 * Files.createDirectory(getSMVDirectory());
 * }
 * @Test(timeout = 10 * 1000)  // this may take as much as >=2m; set longer timeout when running complex tests
 * public void testSMVEvaluateProgram() throws IOException, InterruptedException {
 * System.out.println(file.getName());
 * PouElements code = IEC61131Facade.INSTANCE.file(file);
 * System.out.println("Found " + code.stream()
 * .filter(tle -> tle instanceof ClassDeclaration)
 * .collect(Collectors.toList())
 * .size() + " classes");
 * //System.out.print(countStatements(code) + " statements");
 * code = SymbExFacade.INSTANCE.simplifyOO(code, true);
 * PrintWriter pw = new PrintWriter(Paths.get(getSMVDirectory() + "/" + file.getName() + "oo").toString());
 * System.out.println("Wrote STOO file");
 * pw.println(IEC61131Facade.INSTANCE.print(code));
 * pw.close();
 * code = IEC61131Facade.INSTANCE.file(file);
 * code = SymbExFacade.INSTANCE.simplifyOO(code);
 * //System.out.print(countStatements(code) + " statements after simplification");
 * pw = new PrintWriter(Paths.get(getSMVDirectory() + "/" + file.getName() + "0").toString());
 * pw.println(IEC61131Facade.INSTANCE.print(code));
 * pw.close();
 * System.out.println("Wrote ST0 file");
 * SMVModule module = SymbExFacade.INSTANCE.evaluateProgram(code, true);
 * SMVModule mainModule = createMainModule(module);
 * write(mainModule, getSMVFile().toString(), false);
 * write(module, getSMVFile().toString(), true);
 * System.out.println(file.getName());
 * /*
 * ProcessBuilder processBuilder = new ProcessBuilder();
 * processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
 * processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
 * processBuilder.command("nuXmv", getSMVFile().toString());
 * nuxmv = processBuilder.start();
 * Assert.assertEquals(nuxmv.waitFor(), 0);
 *
 * }
 * @After
 * public void tearDown() throws IOException {
 * //Files.delete(getSMVFile());
 * //nuxmv.destroy();
 * }
 *
 * private SMVModule createMainModule(@NotNull SMVModule uut) {
 * SMVModule mainModule = new SMVModule("main");
 * mainModule.setStateVars(new ArrayList<>(uut.getModuleParameters()));
 * SMVType mainModuleType = new ModuleType(uut.getName(), uut.getModuleParameters());
 * mainModule.getStateVars().add(new SVariable("uut", mainModuleType));
 * return mainModule;
 * }
 *
 * private static class StatementCounter extends AstVisitor<Object> {
 * int count = 0;
 * @Override
 * public Object visit(AssignmentStatement assignmentStatement) {
 * count++;
 * return super.visit(assignmentStatement);
 * }
 * @Override
 * public Object visit(RepeatStatement repeatStatement) {
 * count++;
 * return super.visit(repeatStatement);
 * }
 * @Override
 * public Object visit(WhileStatement whileStatement) {
 * count++;
 * return super.visit(whileStatement);
 * }
 * @Override
 * public Object visit(CaseStatement caseStatement) {
 * count++;
 * return super.visit(caseStatement);
 * }
 * @Override
 * public Object visit(ForStatement forStatement) {
 * count++;
 * return super.visit(forStatement);
 * }
 * @Override
 * public Object visit(IfStatement ifStatement) {
 * count++;
 * return super.visit(ifStatement);
 * }
 * @Override
 * public Object visit(ExitStatement exitStatement) {
 * count++;
 * return super.visit(exitStatement);
 * }
 * @Override
 * public Object visit(ReturnStatement returnStatement) {
 * count++;
 * return super.visit(returnStatement);
 * }
 * @Override
 * public Object visit(InvocationStatement fbc) {
 * count++;
 * return super.visit(fbc);
 * }
 * }
 * }
</Object> */