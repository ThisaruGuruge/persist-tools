/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.persist.nodegenerator.syntax.sources;

import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.persist.BalException;
import io.ballerina.persist.PersistToolsConstants;
import io.ballerina.persist.components.Client;
import io.ballerina.persist.components.ClientResource;
import io.ballerina.persist.components.Function;
import io.ballerina.persist.components.TypeDescriptor;
import io.ballerina.persist.models.Entity;
import io.ballerina.persist.models.Module;
import io.ballerina.persist.nodegenerator.syntax.clients.ClientSyntax;
import io.ballerina.persist.nodegenerator.syntax.clients.DbClientSyntax;
import io.ballerina.persist.nodegenerator.syntax.clients.DbMockClientSyntax;
import io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants;
import io.ballerina.persist.nodegenerator.syntax.constants.SyntaxTokenConstants;
import io.ballerina.persist.nodegenerator.syntax.utils.BalSyntaxUtils;
import io.ballerina.persist.utils.BalProjectUtils;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.validator.SampleNodeGenerator;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.ballerina.persist.PersistToolsConstants.CUSTOM_SCHEMA_SUPPORTED_DB_PROVIDERS;
import static io.ballerina.persist.PersistToolsConstants.JDBC_CONNECTOR_MODULE_NAME;
import static io.ballerina.persist.PersistToolsConstants.SUPPORTED_VIA_JDBC_CONNECTOR;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.CLIENT_NAME;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.EXECUTE_NATIVE_SQL_QUERY;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.H2_CLIENT_NAME;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.KEYWORD_ISOLATED;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.KEYWORD_PUBLIC;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.MOCK_H2_CLIENT_INIT;
import static io.ballerina.persist.nodegenerator.syntax.constants.BalSyntaxConstants.NEWLINE;

/**
 * This class is used to generate the syntax tree for database.
 *
 * @since 0.3.1
 */
public class DbSyntaxTree implements RDBMSSyntaxTree {

    @Override
    public io.ballerina.compiler.syntax.tree.SyntaxTree getClientSyntax(Module entityModule, String datasource)
            throws BalException {
        DbClientSyntax dbClientSyntax = new DbClientSyntax(entityModule, datasource);
        NodeList<ImportDeclarationNode> imports = dbClientSyntax.getImports();
        NodeList<ModuleMemberDeclarationNode> moduleMembers = dbClientSyntax.getConstantVariables();

        Client clientObject = getClientObject(entityModule, dbClientSyntax, CLIENT_NAME);
        moduleMembers = moduleMembers.add(clientObject.getClassDefinitionNode());
        return BalSyntaxUtils.generateSyntaxTree(imports, moduleMembers);
    }

    public SyntaxTree getTestClientSyntax(Module entityModule) throws BalException {
        DbMockClientSyntax dbClientSyntax = new DbMockClientSyntax(entityModule);
        NodeList<ImportDeclarationNode> imports = dbClientSyntax.getImports();
        NodeList<ModuleMemberDeclarationNode> moduleMembers = dbClientSyntax.getConstantVariables();

        Client clientObject = getClientObject(entityModule, dbClientSyntax, H2_CLIENT_NAME);
        moduleMembers = moduleMembers.add(clientObject.getClassDefinitionNode());
        return BalSyntaxUtils.generateSyntaxTree(imports, moduleMembers);
    }

    private static Client getClientObject(Module entityModule, ClientSyntax dbClientSyntax, String clientName)
            throws BalException {
        Client clientObject = dbClientSyntax.getClientObject(entityModule, clientName);
        Collection<Entity> entityArray = entityModule.getEntityMap().values();
        if (entityArray.isEmpty()) {
            throw new BalException("data definition file() does not contain any entities.");
        }
        clientObject.addMember(dbClientSyntax.getInitFunction(entityModule), true);
        List<ClientResource> resourceList = new ArrayList<>();
        for (Entity entity : entityArray) {
            if (entity.containsUnsupportedTypes()) {
                continue;
            }
            ClientResource resource = new ClientResource();
            resource.addFunction(dbClientSyntax.getGetFunction(entity), true);
            resource.addFunction(dbClientSyntax.getGetByKeyFunction(entity), true);
            resource.addFunction(dbClientSyntax.getPostFunction(entity), true);
            resource.addFunction(dbClientSyntax.getPutFunction(entity), true);
            resource.addFunction(dbClientSyntax.getDeleteFunction(entity), true);
            resourceList.add(resource);
        }
        resourceList.forEach(resource -> {
            resource.getFunctions().forEach(function -> {
                clientObject.addMember(function, false);
            });
        });
        clientObject.addMember(dbClientSyntax.getQueryNativeSQLFunction(), true);
        clientObject.addMember(dbClientSyntax.getExecuteNativeSQLFunction(), true);
        clientObject.addMember(dbClientSyntax.getCloseFunction(), true);
        return clientObject;
    }

    @Override
    public io.ballerina.compiler.syntax.tree.SyntaxTree getDataTypesSyntax(Module entityModule) {
        Collection<Entity> entityArray = entityModule.getEntityMap().values();
        if (!entityArray.isEmpty()) {
            return BalSyntaxUtils.generateTypeSyntaxTree(entityModule, "");
        }
        return null;
    }

    @Override
    public io.ballerina.compiler.syntax.tree.SyntaxTree getDataStoreConfigSyntax(String datasource) {
        NodeList<ImportDeclarationNode> imports = AbstractNodeFactory.createEmptyNodeList();
        NodeList<ModuleMemberDeclarationNode> moduleMembers = AbstractNodeFactory.createEmptyNodeList();

        MinutiaeList commentMinutiaeList = createCommentMinutiaeList(String.
                format(BalSyntaxConstants.AUTO_GENERATED_COMMENT));
        imports = imports.add(getImportDeclarationNodeWithAutogeneratedComment(datasource, commentMinutiaeList));

        if (SUPPORTED_VIA_JDBC_CONNECTOR.contains(datasource)) {
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_JDBC_URL));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_USER));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_PASSWORD));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    String.format(BalSyntaxConstants.CONFIGURABLE_OPTIONS,
                            PersistToolsConstants.SupportedDataSources.JDBC)));
        } else {
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_PORT));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_HOST));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_USER));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_DATABASE));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_PASSWORD));
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    String.format(BalSyntaxConstants.CONFIGURABLE_OPTIONS, datasource)));
        }

        if (CUSTOM_SCHEMA_SUPPORTED_DB_PROVIDERS.contains(datasource)) {
            moduleMembers = moduleMembers.add(NodeParser.parseModuleMemberDeclaration(
                    BalSyntaxConstants.CONFIGURABLE_DEFAULT_SCHEMA));
        }

        Token eofToken = AbstractNodeFactory.createIdentifierToken(BalSyntaxConstants.EMPTY_STRING);
        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(imports, moduleMembers, eofToken);
        TextDocument textDocument = TextDocuments.from(BalSyntaxConstants.EMPTY_STRING);
        io.ballerina.compiler.syntax.tree.SyntaxTree balTree =
                io.ballerina.compiler.syntax.tree.SyntaxTree.from(textDocument);
        return balTree.modifyWith(modulePartNode);
    }

    @Override
    public io.ballerina.compiler.syntax.tree.SyntaxTree getConfigTomlSyntax(String moduleName, String datasource)
            throws BalException {
        io.ballerina.toml.syntax.tree.NodeList<DocumentMemberDeclarationNode> moduleMembers =
                io.ballerina.toml.syntax.tree.AbstractNodeFactory.createEmptyNodeList();
        moduleMembers = moduleMembers.add(SampleNodeGenerator.createTable(moduleName, null));
        moduleMembers = populateConfigNodeList(moduleMembers, datasource);
        moduleMembers = BalProjectUtils.addNewLine(moduleMembers, 1);
        io.ballerina.toml.syntax.tree.Token eofToken = io.ballerina.toml.syntax.tree.AbstractNodeFactory.
                createIdentifierToken("");
        DocumentNode documentNode = io.ballerina.toml.syntax.tree.NodeFactory.createDocumentNode(
                moduleMembers, eofToken);
        TextDocument textDocument = TextDocuments.from(documentNode.toSourceCode());
        return io.ballerina.compiler.syntax.tree.SyntaxTree.from(textDocument);
    }

    private static ImportDeclarationNode getImportDeclarationNodeWithAutogeneratedComment(
            String datasource, MinutiaeList commentMinutiaeList) {
        Token orgNameToken = AbstractNodeFactory.createIdentifierToken(BalSyntaxConstants.KEYWORD_BALLERINAX);
        ImportOrgNameNode importOrgNameNode = NodeFactory.createImportOrgNameNode(
                orgNameToken,
                SyntaxTokenConstants.SYNTAX_TREE_SLASH);
        String moduleName = datasource;
        if (SUPPORTED_VIA_JDBC_CONNECTOR.contains(datasource)) {
            moduleName = JDBC_CONNECTOR_MODULE_NAME;
        }
        Token moduleNameToken = AbstractNodeFactory.createIdentifierToken(moduleName);
        SeparatedNodeList<IdentifierToken> moduleNodeList = AbstractNodeFactory
                .createSeparatedNodeList(moduleNameToken);
        Token importToken = NodeFactory.createToken(SyntaxKind.IMPORT_KEYWORD,
                commentMinutiaeList, NodeFactory.createMinutiaeList(AbstractNodeFactory
                        .createWhitespaceMinutiae(BalSyntaxConstants.SPACE)));
        return NodeFactory.createImportDeclarationNode(
                importToken,
                importOrgNameNode,
                moduleNodeList,
                null,
                SyntaxTokenConstants.SYNTAX_TREE_SEMICOLON);
    }

    private static io.ballerina.toml.syntax.tree.NodeList<DocumentMemberDeclarationNode> populateConfigNodeList(
            io.ballerina.toml.syntax.tree.NodeList<DocumentMemberDeclarationNode> moduleMembers, String datasource)
            throws BalException {

        if (SUPPORTED_VIA_JDBC_CONNECTOR.contains(datasource)) {
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_URL,
                    PersistToolsConstants.EMPTY_VALUE, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_USER,
                    PersistToolsConstants.EMPTY_VALUE, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_PASSWORD,
                    PersistToolsConstants.EMPTY_VALUE, null));
        } else {
            String host;
            String port;
            String user;
            String password = PersistToolsConstants.EMPTY_VALUE;
            String database = PersistToolsConstants.EMPTY_VALUE;

            switch (datasource) {
                case PersistToolsConstants.SupportedDataSources.MYSQL_DB -> {
                    host = PersistToolsConstants.DBConfigs.MySQL.DEFAULT_HOST;
                    port = PersistToolsConstants.DBConfigs.MySQL.DEFAULT_PORT;
                    user = PersistToolsConstants.DBConfigs.MySQL.DEFAULT_USER;
                }
                case PersistToolsConstants.SupportedDataSources.MSSQL_DB -> {
                    host = PersistToolsConstants.DBConfigs.MSSQL.DEFAULT_HOST;
                    port = PersistToolsConstants.DBConfigs.MSSQL.DEFAULT_PORT;
                    user = PersistToolsConstants.DBConfigs.MSSQL.DEFAULT_USER;
                }
                case PersistToolsConstants.SupportedDataSources.POSTGRESQL_DB -> {
                    host = PersistToolsConstants.DBConfigs.POSTGRESQL.DEFAULT_HOST;
                    port = PersistToolsConstants.DBConfigs.POSTGRESQL.DEFAULT_PORT;
                    user = PersistToolsConstants.DBConfigs.POSTGRESQL.DEFAULT_USER;
                }
                default -> throw new BalException("Unsupported datasource: " + datasource);
            }

            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_HOST, host, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createNumericKV(
                    PersistToolsConstants.DBConfigs.KEY_PORT, port, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_USER, user, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_PASSWORD, password, null));
            moduleMembers = moduleMembers.add(SampleNodeGenerator.createStringKV(
                    PersistToolsConstants.DBConfigs.KEY_DATABASE, database, null));
        }
        return moduleMembers;
    }

    protected static MinutiaeList createCommentMinutiaeList(String comment) {
        return NodeFactory.createMinutiaeList(
                AbstractNodeFactory.createCommentMinutiae(BalSyntaxConstants.AUTOGENERATED_FILE_COMMENT),
                AbstractNodeFactory.createEndOfLineMinutiae(System.lineSeparator()),
                AbstractNodeFactory.createEndOfLineMinutiae(System.lineSeparator()),
                AbstractNodeFactory.createCommentMinutiae(comment),
                AbstractNodeFactory.createEndOfLineMinutiae(System.lineSeparator()),
                AbstractNodeFactory.createCommentMinutiae(BalSyntaxConstants.COMMENT_SHOULD_NOT_BE_MODIFIED),
                AbstractNodeFactory.createEndOfLineMinutiae(System.lineSeparator()),
                AbstractNodeFactory.createEndOfLineMinutiae(System.lineSeparator()));
    }

    public SyntaxTree getTestInitSyntax(String[] dbScripts) {
        NodeList<ImportDeclarationNode> imports = AbstractNodeFactory.createEmptyNodeList();
        NodeList<ModuleMemberDeclarationNode> moduleMembers = AbstractNodeFactory.createEmptyNodeList();
        MinutiaeList commentMinutiaeList = createCommentMinutiaeList(String.
                format(BalSyntaxConstants.AUTO_GENERATED_COMMENT));
        imports = imports.add(BalSyntaxUtils.getImportDeclarationNodeWithAutogeneratedComment(
                BalSyntaxConstants.PERSIST_MODULE, commentMinutiaeList));

        ModuleMemberDeclarationNode moduleMemberDeclarationNode =
                NodeParser.parseModuleMemberDeclaration(MOCK_H2_CLIENT_INIT);
        moduleMembers = moduleMembers.add(moduleMemberDeclarationNode);
        Function beforeFunction = new Function("setupTestDB", SyntaxKind.METHOD_DECLARATION);
        beforeFunction.addQualifiers(new String[] { KEYWORD_PUBLIC, KEYWORD_ISOLATED});
        beforeFunction.addReturns(TypeDescriptor.getOptionalTypeDescriptorNode(BalSyntaxConstants.EMPTY_STRING,
                BalSyntaxConstants.PERSIST_ERROR));
        for (String dbScript : dbScripts) {
            if (dbScript.equals(NEWLINE)) {
                continue;
            }
            beforeFunction.addStatement(NodeParser.parseStatement(String.format(EXECUTE_NATIVE_SQL_QUERY, dbScript)));
        }
        moduleMembers = moduleMembers.add(beforeFunction.getFunctionDefinitionNode());

        Function afterFunction = new Function("cleanupTestDB", SyntaxKind.METHOD_DECLARATION);
        afterFunction.addQualifiers(new String[] { KEYWORD_PUBLIC, KEYWORD_ISOLATED});
        afterFunction.addReturns(TypeDescriptor.getOptionalTypeDescriptorNode(BalSyntaxConstants.EMPTY_STRING,
                BalSyntaxConstants.PERSIST_ERROR));
        for (String dbScript : dbScripts) {
            if (dbScript.startsWith("DROP")) {
                afterFunction.addStatement(NodeParser.parseStatement(
                        String.format(EXECUTE_NATIVE_SQL_QUERY, dbScript)));
            }
        }
        moduleMembers = moduleMembers.add(afterFunction.getFunctionDefinitionNode());
        return BalSyntaxUtils.generateSyntaxTree(imports, moduleMembers);
    }
}
