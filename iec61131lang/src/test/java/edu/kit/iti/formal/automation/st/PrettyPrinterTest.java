package edu.kit.iti.formal.automation.st;

/*-
 * #%L
 * iec61131lang
 * %%
 * Copyright (C) 2018 Alexander Weigl
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

import edu.kit.iti.formal.automation.IEC61131Facade;
import edu.kit.iti.formal.automation.parser.ErrorReporter;
import edu.kit.iti.formal.automation.st.ast.Top;
import edu.kit.iti.formal.automation.st.ast.TopLevelElements;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

/**
 * @author Alexander Weigl
 * @version 1 (18.02.18)
 */
public class PrettyPrinterTest {
    public static void testPrettyPrintByString(Top astNode, File file) throws IOException {
        String content = FileUtils.readFileToString(file, "utf-8");
        String printed = StructuredTextPrinter.print(astNode);
        Assert.assertEquals(content, printed);
    }

    public static void testPrettyPrintByEquals(TopLevelElements tle) throws IOException {
        String printed = IEC61131Facade.print(tle);
        try {
            TopLevelElements newTle = IEC61131Facade.file(CharStreams.fromString(printed));
            Assert.assertEquals(tle, newTle);
        } catch (ErrorReporter.IEC61131ParserException e) {
            System.err.println(
                    e.print(printed.split("\n"), "\n---\n")
            );
        }finally {
            System.err.println(printed);
        }
    }



    /*public static Stream<Object[]> files() {
        Collection<File> files = FileUtils.listFiles(new File("src/test/resources"),
                new String[]{"sfc", "st"}, true
        );
        return files.stream().map(f->new Object[]{f});
    }

    @Parameterized.Parameter
    public File toRead;
    */

}