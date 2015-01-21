package ool.idea.plugin.lang;

import com.intellij.lexer.FlexAdapter;

/**
 * 12/12/14
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class OxyTemplateLexerAdapter extends FlexAdapter
{
    public OxyTemplateLexerAdapter()
    {
        super(new OxyTemplateLexer());
    }

}