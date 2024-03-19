/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver.command.executors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.ballerinalang.langserver.command.docs.DocAttachmentInfo;
import org.ballerinalang.langserver.command.docs.DocumentationGenerator;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.PathUtil;
import org.ballerinalang.langserver.commons.ExecuteCommandContext;
import org.ballerinalang.langserver.commons.command.CommandArgument;
import org.ballerinalang.langserver.commons.command.LSCommandExecutorException;
import org.ballerinalang.langserver.commons.command.spi.LSCommandExecutor;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.ballerinalang.langserver.command.CommandUtil.applyMultipleTextEdits;
import static org.ballerinalang.langserver.command.CommandUtil.applySingleTextEdit;
import static org.ballerinalang.langserver.command.docs.DocumentationGenerator.getDocumentableSymbol;
import static org.ballerinalang.langserver.command.docs.DocumentationGenerator.getDocumentationEditForNode;

/**
 * An abstract class to perform add/update documentation operations to all the nodes. This will update the documentation
 * if already exists, add otherwise.
 *
 * @since 2.0.0
 */
public abstract class AbstractDocumentationExecutor implements LSCommandExecutor {

    /**
     * {@inheritDoc}
     *
     * @param ctx
     */
    @Override
    public Object execute(ExecuteCommandContext ctx) throws LSCommandExecutorException {
        String documentUri = "";
        Range nodeRange = null;
        VersionedTextDocumentIdentifier textDocumentIdentifier = new VersionedTextDocumentIdentifier();
        for (CommandArgument arg : ctx.getArguments()) {
            String argKey = arg.key();
            switch (argKey) {
                case CommandConstants.ARG_KEY_DOC_URI:
                    documentUri = arg.valueAs(String.class);
                    textDocumentIdentifier.setUri(documentUri);
                    break;
                case CommandConstants.ARG_KEY_NODE_RANGE:
                    nodeRange = arg.valueAs(Range.class);
                    break;
                default:
                    break;
            }
        }

        Optional<Path> filePath = PathUtil.getPathFromURI(documentUri);
        if (filePath.isEmpty() || nodeRange == null) {
            return Collections.emptyList();
        }

        SyntaxTree syntaxTree = ctx.workspace().syntaxTree(filePath.get()).orElseThrow();
        NonTerminalNode node = CommonUtil.findNode(nodeRange, syntaxTree);
        if (node.kind() == SyntaxKind.MODULE_PART) {
            node = ((ModulePartNode) node).members().get(0);
        }

        Optional<DocAttachmentInfo> docAttachmentInfo = getDocumentationEditForNode(node, syntaxTree);
        if (docAttachmentInfo.isEmpty()) {
            return Collections.emptyList();
        }

        SemanticModel semanticModel = ctx.workspace().semanticModel(filePath.get()).orElseThrow();
        if (node.kind() == SyntaxKind.TYPE_DEFINITION) {
            return createDocForTypeDef((TypeDefinitionNode) node, docAttachmentInfo.get(), semanticModel,
                    textDocumentIdentifier, ctx.getLanguageClient());
        } else {
            return createDocs(node, docAttachmentInfo.get(), semanticModel, textDocumentIdentifier,
                    ctx.getLanguageClient());
        }
    }
    
//        if (documentableSymbol.isPresent()) {
//            Symbol symbol = documentableSymbol.get();
//            if (symbol.kind() == SymbolKind.TYPE_DEFINITION && ((TypeDefinitionSymbol) symbol).typeDescriptor().typeKind() == TypeDescKind.RECORD) {
//                List<TextEdit> textEdits = new ArrayList<>();
//                Map<String, RecordFieldSymbol> fields = ((RecordTypeSymbol) ((TypeDefinitionSymbol) symbol).typeDescriptor()).fieldDescriptors();
//                for (Map.Entry<String, RecordFieldSymbol> field : fields.entrySet()) {
//                    String fName = field.getKey();
//                    String doc = docs.parameterMap().get(fName);
//                    RecordFieldSymbol fieldSymbol = field.getValue();
//                    fieldSymbol.getLocation().ifPresent(location -> {
////                        Position p = new Position(location.lineRange().startLine().line(), location.lineRange().startLine().offset());
//                        Position p = new Position(location.lineRange().startLine().line(), 4);
//                        Range r = new Range(p, p);
//                        textEdits.add(new TextEdit(r,  "# " + doc + "\n\t"));
//                    });
//                }
//                return applyMultipleTextEdits(textEdits, textDocumentIdentifier, lsClient);
//            }
//        }

    private Object createDocForTypeDef(TypeDefinitionNode typeDefNode, DocAttachmentInfo docAttachmentInfo, 
                                       SemanticModel semanticModel, 
                                       VersionedTextDocumentIdentifier textDocumentIdentifier, 
                                       LanguageClient lsClient) {
        Node typeDescriptor = typeDefNode.typeDescriptor();
        if (typeDescriptor.kind() != SyntaxKind.RECORD_TYPE_DESC) {
            return createDocs(typeDefNode, docAttachmentInfo, semanticModel, textDocumentIdentifier, lsClient);
        }
        List<TextEdit> textEdits = getTextEdits((RecordTypeDescriptorNode) typeDescriptor);
//        return applyMultipleTextEdits(textEdits, textDocumentIdentifier, lsClient);
        return createDocs(typeDefNode, docAttachmentInfo, semanticModel, textDocumentIdentifier, lsClient);
    }

    private List<TextEdit> getTextEdits(RecordTypeDescriptorNode recordTypeDescriptor) {
        List<TextEdit> textEdits = new ArrayList<>();
        Position posOfDescription = new Position(recordTypeDescriptor.lineRange().startLine().line(), 0);
        textEdits.add(new TextEdit(new Range(posOfDescription, posOfDescription), "# - \n"));
        NodeList<Node> fields = recordTypeDescriptor.fields();
        for (Node field : fields) {
            Position pos = new Position(field.lineRange().startLine().line(), 0);
            textEdits.add(new TextEdit(new Range(pos, pos), "\t# - \n"));
        }
        return textEdits;
    }

    private Object createDocs(NonTerminalNode node, DocAttachmentInfo docs, SemanticModel semanticModel, 
                              VersionedTextDocumentIdentifier textDocumentIdentifier, LanguageClient lsClient) {
        DocAttachmentInfo mergedDocs = mergeDocs(node, docs, semanticModel);

        Optional<Range> docsRange = DocumentationGenerator.getDocsRange(node);
        Range range;
        boolean isUpdate = false;
        if (docsRange.isPresent()) {
            isUpdate = true;
            range = docsRange.get();
        } else {
            range = new Range(mergedDocs.getDocStartPos(), mergedDocs.getDocStartPos());
        }

        return applySingleTextEdit(mergedDocs.getDocumentationString(!isUpdate), range, textDocumentIdentifier, lsClient);
    }
    
    private DocAttachmentInfo mergeDocs(NonTerminalNode node, DocAttachmentInfo docs, SemanticModel semanticModel) {
        Optional<Symbol> documentableSymbol = getDocumentableSymbol(node, semanticModel);
        if (documentableSymbol.isPresent()) {
            Optional<Documentation> documentation = ((Documentable) documentableSymbol.get()).documentation();
            if (documentation.isPresent()) {
                return docs.mergeDocAttachment(documentation.get());
            }
        }
        return docs;
    }
}
