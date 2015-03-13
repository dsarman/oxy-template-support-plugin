package ool.idea.plugin.action;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ool.idea.plugin.editor.inspection.fix.MissingIncludeDirectiveQuickFix;
import ool.idea.plugin.file.OxyTemplateFile;
import ool.idea.plugin.psi.DirectiveStatement;
import ool.idea.plugin.psi.OxyTemplateHelper;
import ool.web.template.impl.chunk.directive.IncludeDirective;
import ool.web.template.impl.chunk.directive.IncludeOnceDirective;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * 2/10/15
 *
 * @author Petr Mayr <p.mayr@oxyonline.cz>
 */
public class IncludeOptimizer implements ImportOptimizer
{
    @NonNls
    private static final List<String> INCLUDE_DIRECTIVES = Arrays.asList(
            IncludeDirective.NAME, IncludeOnceDirective.NAME
    );

    @Override
    public boolean supports(PsiFile file)
    {
        String currentCommandName = CommandProcessor.getInstance().getCurrentCommandName();

        // don't run with before commit command
        return ! (currentCommandName != null && currentCommandName
                .equals(CodeInsightBundle
                        .message("process.optimize.imports.before.commit"))) && file instanceof OxyTemplateFile;
    }

    @NotNull
    @Override
    public Runnable processFile(final PsiFile file)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                Map<PsiElement, JSElement> usedJsMacros = OxyTemplateHelper.getUsedJsMacros(file);

                for (final DirectiveStatement statement : PsiTreeUtil.getChildrenOfTypeAsList(file, DirectiveStatement.class))
                {
                    if ( ! INCLUDE_DIRECTIVES.contains(statement.getName()))
                    {
                        continue;
                    }

                    statement.delete();
                }

                List<MissingDirectiveDescriptor> missingDirectiveDescriptors = new LinkedList<MissingDirectiveDescriptor>();
                List<VirtualFile> includedFiles = new LinkedList<VirtualFile>();

                for (Map.Entry<PsiElement, JSElement> pair : usedJsMacros.entrySet())
                {
                    final PsiElement macroCall = pair.getKey();
                    final PsiElement reference = pair.getValue();

                    if (!includedFiles.contains(reference.getContainingFile().getVirtualFile())
                            && !reference.getContainingFile().getVirtualFile().equals(file.getVirtualFile()))
                    {
                        missingDirectiveDescriptors.add(new MissingDirectiveDescriptor(macroCall, reference));

                        includedFiles.add(reference.getContainingFile().getVirtualFile());
                    }
                }

                Collections.sort(missingDirectiveDescriptors, new Comparator<MissingDirectiveDescriptor>()
                {
                    @Override
                    public int compare(MissingDirectiveDescriptor e1, MissingDirectiveDescriptor e2)
                    {
                        return String.CASE_INSENSITIVE_ORDER.compare(e1.getReference().getContainingFile().getVirtualFile().getPath(),
                                e2.getReference().getContainingFile().getVirtualFile().getPath());
                    }
                });

                for(MissingDirectiveDescriptor descriptor : missingDirectiveDescriptors)
                {
                    new MissingIncludeDirectiveQuickFix(descriptor.getMacroCall(), descriptor.getReference(), IncludeOnceDirective.NAME)
                            .applyFix(file.getProject());
                }
            }
        };
    }

    private static class MissingDirectiveDescriptor
    {
        private final PsiElement macroCall;
        private final PsiElement reference;

        public MissingDirectiveDescriptor(PsiElement macroCall, PsiElement reference)
        {
            this.macroCall = macroCall;
            this.reference = reference;
        }

        public PsiElement getMacroCall()
        {
            return macroCall;
        }

        public PsiElement getReference()
        {
            return reference;
        }

    }

}