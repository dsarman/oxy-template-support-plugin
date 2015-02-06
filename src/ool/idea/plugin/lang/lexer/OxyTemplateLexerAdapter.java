package ool.idea.plugin.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import java.io.Reader;

/**
 * 12/12/14
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class OxyTemplateLexerAdapter extends FlexAdapter
{
    public OxyTemplateLexerAdapter()
    {
        super(new OxyTemplateLexer((Reader)null));
    }

}
