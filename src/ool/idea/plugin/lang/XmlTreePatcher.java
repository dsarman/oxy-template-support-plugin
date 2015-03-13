package ool.idea.plugin.lang;

import com.intellij.lang.xml.XmlTemplateTreePatcher;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import ool.idea.plugin.psi.OxyTemplateHelper;
import ool.idea.plugin.psi.OxyTemplateTypes;

/**
 * 3/2/15
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class XmlTreePatcher extends XmlTemplateTreePatcher
{
    @Override
    public void insert(CompositeElement parent, TreeElement anchorBefore, OuterLanguageElement toInsert)
    {
        if(toInsert.getNode().getElementType() != OxyTemplateTypes.T_OUTER_TEMPLATE_ELEMENT)
        {
            super.insert(parent, anchorBefore, toInsert);
        }
        else if( ! OxyTemplateHelper.insertOuterElementToAST(parent, anchorBefore, toInsert))
        {
            super.insert(parent, anchorBefore, toInsert);
        }
    }

}
