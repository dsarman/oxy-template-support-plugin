package ool.idea.plugin.lexer;

import java.io.IOException;
import ool.idea.plugin.psi.OxyTemplateTypes;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * 12/13/14
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class InnerJsTest extends AbstractLexerTest
{
    @Test
    public void blockTest() throws IOException
    {
        String input =
                "<div>\n" +
                "    <ul>\n" +
                "        <% for(var i=0; i<supplies.length; i%10) { %>\n" +
                "            <li class=\"li\">\n" +
                "                <a href=\"neco\" title=\"nevim\">\n" +
                "                    <%= supplies[i]; %>\n" +
                "                </a>\n" +
                "            </li>\n" +
                "        <% } %>\n" +
                "    </ul>\n" +
                "</div>\n";

        lexer.start(input);

        assertEquals(OxyTemplateTypes.T_TEMPLATE_HTML_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_OPEN_BLOCK_MARKER, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_JAVASCRIPT_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_CLOSE_BLOCK_MARKER, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_HTML_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_OPEN_BLOCK_MARKER_PRINT, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_JAVASCRIPT_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_CLOSE_BLOCK_MARKER, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_HTML_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_OPEN_BLOCK_MARKER, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_JAVASCRIPT_CODE, nextToken());
        assertEquals(OxyTemplateTypes.T_CLOSE_BLOCK_MARKER, nextToken());
        assertEquals(OxyTemplateTypes.T_TEMPLATE_HTML_CODE, nextToken());

        assertEquals(null, nextToken());
    }

}