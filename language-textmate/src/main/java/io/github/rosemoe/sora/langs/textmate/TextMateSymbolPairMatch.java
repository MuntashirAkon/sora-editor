/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.textmate;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.EncodedTokenAttributes;
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType;
import org.eclipse.tm4e.languageconfiguration.model.AutoClosingPair;
import org.eclipse.tm4e.languageconfiguration.model.AutoClosingPairConditional;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class TextMateSymbolPairMatch extends SymbolPairMatch {

    private static final String surroundingPairFlag = "surroundingPair";

    private static final List<String> surroundingPairFlagWithList = List.of(surroundingPairFlag);

    private final TextMateLanguage language;

    private boolean enabled;

    public TextMateSymbolPairMatch(TextMateLanguage language) {
        super(new SymbolPairMatch.DefaultSymbolPairs());
        this.language = language;

        updatePair();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            removeAllPairs();
        } else {
            updatePair();
        }
    }


    public void updatePair() {

        if (!enabled) {
            return;
        }

        LanguageConfiguration languageConfiguration = language.languageConfiguration;

        if (languageConfiguration == null) {
            return;
        }

        removeAllPairs();


        @Nullable List<AutoClosingPair> surroundingPairs = languageConfiguration.getSurroundingPairs();

        @Nullable List<AutoClosingPairConditional> autoClosingPairs = languageConfiguration.getAutoClosingPairs();

        ArrayList<AutoClosingPairConditional> mergePairs = new ArrayList<AutoClosingPairConditional>();

        if (autoClosingPairs != null) {
            mergePairs.addAll(autoClosingPairs);
        }


        if (surroundingPairs != null) {

            for (AutoClosingPair surroundingPair : surroundingPairs) {

                AutoClosingPairConditional newPair = new AutoClosingPairConditional(surroundingPair.open, surroundingPair.close,
                        surroundingPairFlagWithList);

                int mergePairIndex = mergePairs.indexOf(newPair);

                if (mergePairIndex >= 0) {
                    AutoClosingPairConditional mergePair = mergePairs.get(mergePairIndex);

                    if (mergePair.notIn == null || mergePair.notIn.isEmpty()) {
                        mergePairs.add(newPair);
                        continue;
                    }

                    mergePair.notIn.add(surroundingPairFlag);

                }
                mergePairs.add(newPair);

            }
        }

        for (AutoClosingPairConditional pair : mergePairs) {
            putPair(pair.open, new SymbolPair(pair.open, pair.close, new SymbolPairEx(pair)));
        }

    }

    static class SymbolPairEx implements SymbolPair.SymbolPairEx {

        int[] notInTokenTypeArray;

        boolean isSurroundingPair = false;

        public SymbolPairEx(AutoClosingPairConditional pair) {

            List<String> notInList = pair.notIn;

            if (notInList == null || notInList.isEmpty()) {
                notInTokenTypeArray = null;
                return;
            }

            if (notInList.contains(surroundingPairFlag)) {
                //
                isSurroundingPair = true;
                if (notInList == surroundingPairFlagWithList) {
                    return;
                } else {
                    notInList.remove(surroundingPairFlag);
                }
            }

            notInTokenTypeArray = new int[notInList.size()];

            for (int i = 0; i < notInTokenTypeArray.length; i++) {
                String notInValue = notInList.get(i).toLowerCase();

                int notInTokenType = StandardTokenType.String;

                switch (notInValue) {
                    case "string":
                        break;
                    case "comment":
                        notInTokenType = StandardTokenType.Comment;
                        break;
                    case "regex":
                        notInTokenType = StandardTokenType.RegEx;
                        break;
                }

                notInTokenTypeArray[i] = notInTokenType;
            }

            Arrays.sort(notInTokenTypeArray);

        }

        @Override
        public boolean shouldDoReplace(CodeEditor editor, ContentLine contentLine, int leftColumn) {

            if (editor.getCursor().isSelected()) {
                return true;
            }

            if (notInTokenTypeArray == null) {
                return true;
            }

            Cursor cursor = editor.getCursor();

            int currentLine = cursor.getLeftLine();
            int currentColumn = cursor.getLeftColumn();

            List<Span> spansOnCurrentLine = editor.getSpansForLine(currentLine);

            Span currentSpan = binarySearchSpan(spansOnCurrentLine, currentColumn);


            Object extra = currentSpan.extra;


            if (extra instanceof Integer) {
                int index = Arrays.binarySearch(notInTokenTypeArray, (Integer) extra);
                return index < 0;
            }

            return true;
        }

        private int checkIndex(int index, int max) {
            return Math.max(Math.min(index, max), 0);
        }

        private Span binarySearchSpan(List<Span> spanList, int column) {
            int start = 0, end = spanList.size() - 1, middle, size = spanList.size() - 1;

            Span currentSpan = null;

            while (start <= end) {
                middle = (start + end) / 2;

                currentSpan = spanList.get(middle);
                if (currentSpan.column == column) {
                    break;
                }

                if (currentSpan.column < column) {
                    Span nextSpan = spanList.get(checkIndex(middle + 1, size));

                    if (nextSpan.column > column) {
                        return currentSpan;
                    }

                    start++;

                    continue;

                }

                // if (currentSpan.column > column)
                Span previousSpan = spanList.get(checkIndex(middle - 1, size));

                if (previousSpan.column < column) {
                    return currentSpan;
                }

                end--;

            }

            return currentSpan;

        }

        @Override
        public boolean shouldDoAutoSurround(Content content) {
            return isSurroundingPair && content.getCursor().isSelected();
        }
    }
}
