package ool.idea.plugin.lang;

import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.templateLanguages.SimpleTreePatcher;
import ool.idea.plugin.psi.OxyTemplateHelper;

/**
 * 3/2/15
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class InnerJsTreePatcher extends SimpleTreePatcher
{
    @Override
    public void insert(CompositeElement parent, TreeElement anchorBefore, OuterLanguageElement toInsert)
    {
        if( ! OxyTemplateHelper.insertOuterElementToAST(parent, anchorBefore, toInsert))
        {
            super.insert(parent, anchorBefore, toInsert);
        }
    }

}
