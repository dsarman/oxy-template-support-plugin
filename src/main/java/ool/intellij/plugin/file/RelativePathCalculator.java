/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ool.intellij.plugin.file;

import java.io.File;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

/**
 * duplicated to {@link com.intellij.openapi.vcs.changes.patch.RelativePathCalculator}
 */
public class RelativePathCalculator
{
    private final int ourNumOfAllowedStepsAbove = 10;
    private static final int ourAllowedStepsDown = 20;

    private final String myShifted;
    private final String myBase;

    private String myResult;
    private boolean myRename;

    public RelativePathCalculator(final String base, final String shifted)
    {
        myShifted = shifted;
        myBase = base;
    }

    private static boolean stringEqual(@NotNull final String s1, @NotNull final String s2)
    {
        if (!SystemInfo.isFileSystemCaseSensitive)
        {
            return s1.equalsIgnoreCase(s2);
        }
        return s1.equals(s2);
    }

    public void execute()
    {
        if (myShifted == null || myBase == null)
        {
            myResult = null;
            return;
        }
        if (stringEqual(myShifted, myBase))
        {
            myResult = ".";
            myRename = false;
            return;
        }
        final String[] baseParts = split(myBase);
        final String[] shiftedParts = split(myShifted);

        myRename = checkRename(baseParts, shiftedParts);

        int cnt = 0;
        while (true)
        {
            if ((baseParts.length <= cnt) || (shiftedParts.length <= cnt))
            {
                // means that directory moved to a file or vise versa -> error
                return;
            }
            if (!stringEqual(baseParts[cnt], shiftedParts[cnt]))
            {
                break;
            }
            ++cnt;
        }

        final int stepsUp = baseParts.length - cnt - 1;
        if ((!myRename) && (stepsUp > ourNumOfAllowedStepsAbove) && ((shiftedParts.length - cnt) <= ourAllowedStepsDown))
        {
            myResult = myShifted;
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stepsUp; i++)
        {
            sb.append("../");
        }

        for (int i = cnt; i < shiftedParts.length; i++)
        {
            final String shiftedPart = shiftedParts[i];
            sb.append(shiftedPart);
            if (i < (shiftedParts.length - 1))
            {
                sb.append('/');
            }
        }

        myResult = sb.toString();
    }

    public boolean isRename()
    {
        return myRename;
    }

    private boolean checkRename(final String[] baseParts, final String[] shiftedParts)
    {
        if (baseParts.length == shiftedParts.length)
        {
            for (int i = 0; i < baseParts.length; i++)
            {
                if (!stringEqual(baseParts[i], shiftedParts[i]))
                {
                    return i == (baseParts.length - 1);
                }
            }
        }
        return false;
    }

    public String getResult()
    {
        return myResult;
    }

    public static String[] split(final String s)
    {
        return s.replace(File.separatorChar, '/').split("/");
    }

}
