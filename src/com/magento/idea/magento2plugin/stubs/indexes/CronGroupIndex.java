/**
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */
package com.magento.idea.magento2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.magento.idea.magento2plugin.project.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * CronGroupIndex collects all cron groups from cron_groups.xml files.
 * It's used for autocompleting groups during cronjob generation
 */
public class CronGroupIndex extends FileBasedIndexExtension<String, String> {
    public static final ID<String, String> KEY
            = ID.create("com.magento.idea.magento2plugin.stubs.indexes.cron_tabs");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return fileContent -> {
            Map<String, String> map = new HashMap<>();
            PsiFile psiFile = fileContent.getPsiFile();

            if (!Settings.isEnabled(psiFile.getProject())) {
                return map;
            }

            if (!(psiFile instanceof XmlFile)) {
                return map;
            }

            XmlDocument document = ((XmlFile) psiFile).getDocument();
            if (document == null) {
                return map;
            }

            XmlTag[] xmlTags = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
            if (xmlTags == null) {
                return map;
            }

            // cron_groups file is load, now it's time to collect group IDs
            for (XmlTag xmlTag: xmlTags) {
                if (xmlTag.getName().equals("config")) {
                    for (XmlTag typeNode: xmlTag.findSubTags("group")) {
                        String groupId = typeNode.getAttributeValue("id");

                        if (groupId != null) {
                            map.put(groupId, fileContent.getFile().getPath());
                        }
                    }
                }
            }

            return map;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return new EnumeratorStringDescriptor();
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> (virtualFile.getFileType() == XmlFileType.INSTANCE
                && virtualFile.getNameWithoutExtension().equals("cron_groups"));
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
