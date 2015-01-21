package ool.idea.plugin.file;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.StdLanguages;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PsiCommentImpl;
import com.intellij.psi.impl.source.tree.RecursiveTreeElementVisitor;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.templateLanguages.ConfigurableTemplateLanguageFileViewProvider;
import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import gnu.trove.THashSet;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import ool.idea.plugin.lang.OxyTemplate;
import ool.idea.plugin.lang.OxyTemplateInnerJs;
import ool.idea.plugin.psi.OxyTemplateTypes;
import org.jetbrains.annotations.NotNull;

/**
 * 7/23/14
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class OxyTemplateFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider
        implements ConfigurableTemplateLanguageFileViewProvider
{
    @NotNull
    private final PsiManager myManager;
    @NotNull
    private final VirtualFile myVirtualFile;

    public OxyTemplateFileViewProvider(PsiManager manager, VirtualFile file, boolean physical)
    {
        super(manager, file, physical);

        myManager = manager;
        myVirtualFile = file;
    }

    private Language getTemplateDataLanguage(PsiManager manager, VirtualFile file)
    {
        // get the main language of the file
        Language dataLang = TemplateDataLanguageMappings.getInstance(manager.getProject()).getMapping(file);
        if (dataLang == null)
        {
            dataLang = OxyTemplate.getDefaultTemplateLang().getLanguage();
        }

        Language substituteLang = LanguageSubstitutors.INSTANCE.substituteLanguage(dataLang, file, manager.getProject());

        // only use a substituted language if it's templateable
        if (TemplateDataLanguageMappings.getTemplateableLanguages().contains(substituteLang))
        {
            dataLang = substituteLang;
        }

        return dataLang;
    }

    @NotNull
    @Override
    public Language getBaseLanguage()
    {
        return OxyTemplate.INSTANCE;
    }

    @NotNull
    @Override
    public Language getTemplateDataLanguage()
    {
        return getTemplateDataLanguage(myManager, myVirtualFile);
    }

    @Override
    protected MultiplePsiFilesPerDocumentFileViewProvider cloneInner(VirtualFile fileCopy)
    {
        return new OxyTemplateFileViewProvider(getManager(), fileCopy, false);
    }

    @NotNull
    @Override
    public Set<Language> getLanguages()
    {
        return new THashSet<Language>(Arrays.asList(new Language[]{
                        OxyTemplate.INSTANCE,
                        OxyTemplateInnerJs.INSTANCE,
                        StdLanguages.HTML}
        ));
    }

    @Override
    protected PsiFile createFile(@NotNull Language lang)
    {
        ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
        if (parserDefinition == null)
        {
            return null;
        }

        Language templateDataLanguage = getTemplateDataLanguage(myManager, myVirtualFile);
        if (lang == templateDataLanguage)
        {
            PsiFileImpl file = (PsiFileImpl) parserDefinition.createFile(this);
            file.setContentElementType(new TemplateDataElementType("TEMPLATE_MARKUP", OxyTemplate.INSTANCE, OxyTemplateTypes.T_TEMPLATE_HTML_CODE, OxyTemplateTypes.T_OUTER_TEMPLATE_ELEMENT));
            return file;
        }
        else if (lang == OxyTemplate.INSTANCE)
        {
            return parserDefinition.createFile(this);
        }
        else if (lang == OxyTemplateInnerJs.INSTANCE)
        {
            PsiFileImpl file = (PsiFileImpl) parserDefinition.createFile(this);
            file.setContentElementType(new TemplateDataElementType("TEMPLATE_JS", OxyTemplateInnerJs.INSTANCE, OxyTemplateTypes.T_TEMPLATE_JAVASCRIPT_CODE, OxyTemplateTypes.T_INNER_TEMPLATE_ELEMENT)
            {
                private final Logger LOG = Logger.getInstance(getClass());

                private final ThreadLocal<LinkedList<Integer>> ourOffsets = new ThreadLocal();

                @Override
                protected CharSequence createTemplateText(CharSequence buf, Lexer lexer)
                {
                    ourOffsets.set(new LinkedList());
                    return super.createTemplateText(buf, lexer);
                }

                @Override
                protected void appendCurrentTemplateToken(StringBuilder result, CharSequence buf, Lexer lexer)
                {
                    super.appendCurrentTemplateToken(result, buf, lexer);
                    ((LinkedList) ourOffsets.get()).add(Integer.valueOf(result.length()));
                    result.append("\n");
                }

                @Override
                protected void prepareParsedTemplateFile(final FileElement root)
                {
                    final LinkedList offsets = (LinkedList) ourOffsets.get();
                    root.acceptTree(new RecursiveTreeElementVisitor()
                    {
                        private int shift = 0;

                        @Override
                        protected boolean visitNode(TreeElement element)
                        {
                            return true;
                        }

                        @Override
                        public void visitLeaf(LeafElement leaf)
                        {
                            if ((offsets.isEmpty()) || (this.shift + leaf.getTextOffset() + leaf.getTextLength() < ((Integer) offsets.peekFirst()).intValue()))
                            {
                                return;
                            }

                            while ((!offsets.isEmpty()) && ((Integer) offsets.peekFirst() < this.shift + leaf.getTextOffset()))
                            {
                                offsets.pollFirst();
                            }

                            String newText = leaf.getText();
                            int localShift = 0;
                            while ((!offsets.isEmpty()) && ((Integer) offsets.peekFirst() < this.shift + leaf.getTextOffset() + leaf.getTextLength()))
                            {
                                int index = (Integer) offsets.pollFirst() - (this.shift + localShift + leaf.getTextOffset());
                                newText = removeChar(newText, index);
                                localShift++;
                            }
                            this.shift += localShift;
                            if (!newText.isEmpty())
                            {
                                TreeElement newAnchor;
                                if ((leaf instanceof PsiComment))
                                {
                                    newAnchor = new PsiCommentImpl(((PsiComment) leaf).getTokenType(), newText);
                                } else
                                {
                                    newAnchor = Factory.createSingleLeafElement(leaf.getElementType(), newText, 0, newText.length(), null, leaf.getManager());
                                }
                                if (leaf.getClass() != newAnchor.getClass())
                                {
                                    LOG.warn("Bad leaf: " + leaf.getText() + " in \n" + root.getText());
                                }
                                leaf.rawInsertBeforeMe(newAnchor);
                            }
                            leaf.rawRemove();
                        }

                        private String removeChar(String text, int index)
                        {
                            if ((index < 0) || (index >= text.length())) return text;
                            if (index == 0) return text.substring(1);
                            if (index == text.length() - 1) return text.substring(0, text.length() - 1);
                            return text.substring(0, index) + text.substring(index + 1);
                        }
                    });

                    ourOffsets.set(null);
                }

                @Override
                protected Language getTemplateFileLanguage(TemplateLanguageFileViewProvider viewProvider)
                {
                    return getLanguage();
                }
            });

            return file;
        }

        return null;
    }

}