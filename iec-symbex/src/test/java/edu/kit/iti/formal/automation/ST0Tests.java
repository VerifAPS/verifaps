package edu.kit.iti.formal.automation;

/*-
 * #%L
 * iec-symbex
 * %%
 * Copyright (C) 2017 Alexander Weigl
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

import edu.kit.iti.formal.automation.st.ast.TopLevelElements;
import edu.kit.iti.formal.automation.st0.STSimplifier;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.function.Function;

import static java.util.Arrays.asList;

/**
 * @author Alexander Weigl
 * @version 1 (28.06.17)
 */
public class ST0Tests {
    @Test
    public void fbEmbedding1() throws IOException {
        assertResultST0("fbembedding_1");
    }

    private void assertResultST0(String file) throws IOException {
        TopLevelElements st = IEC61131Facade.file(
                CharStreams.fromStream(getClass().getResourceAsStream(file + ".st")));
        TopLevelElements st0exp = IEC61131Facade.file(
                CharStreams.fromStream(getClass().getResourceAsStream(file + ".st0")));

        IEC61131Facade.resolveDataTypes(st);
        IEC61131Facade.resolveDataTypes(st0exp);

        STSimplifier ss = new STSimplifier(st);
        ss.addDefaultPipeline();
        ss.transform();
        TopLevelElements st0 = ss.getProcessed();

        String print = IEC61131Facade.print(st0.get(1));
        System.out.println(print);
        Assert.assertEquals(
                IEC61131Facade.print(st0exp.get(0)),
                print);
        //Assert.assertEquals(st0exp.get(0), st0.get(1));
    }

}
