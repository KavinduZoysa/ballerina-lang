/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.ballerinalang.compiler.desugar;

import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.tree.NodeKind;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.SimpleBLangNodeAnalyzer;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerAsyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSendReceiveExpr;
import org.wso2.ballerinalang.compiler.tree.statements.BLangDo;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Names;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.ballerinalang.model.symbols.SymbolOrigin.VIRTUAL;

enum ActionCheck {
    SEND_ONLY,
    RECEIVE_ONLY,
    SEND_RECEIVE
}

// 1. with fork, parallel executions, and CPU bounded executions with sequencial executions
// 2. normal workers
// 3. combined workers

// TODO: check about large number of do stmts
// TODO: check for wait stmts
public class WorkerDesugar {
    private static final CompilerContext.Key<WorkerDesugar> WORKER_DESUGAR_KEY = new CompilerContext.Key<>();
    
    private record Node(BLangFunction worker, List<String> from, List<String> to) {
    }

    private final Map<String, Node> nodes = new HashMap<>();
    
    public static WorkerDesugar getInstance(CompilerContext context) {
        WorkerDesugar workerDesugar = context.get(WORKER_DESUGAR_KEY);
        if (workerDesugar == null) {
            workerDesugar = new WorkerDesugar(context);
        }
        return workerDesugar;
    }
    
    private WorkerDesugar(CompilerContext context) {
        context.put(WORKER_DESUGAR_KEY, this);
    }
    
    // TODO: Multiple connections from/to a single worker is not supported yet
    // TODO: Check eventIndex with Lochana
    public BLangPackage perform(BLangPackage pkgNode) {
        for (BLangFunction function : pkgNode.functions) {
            if (function.getKind() == NodeKind.FUNCTION && function.flagSet.contains(Flag.WORKER)) {
                String name = function.defaultWorkerName.value;
                List<String> from = new ArrayList<>();
                List<String> to = new ArrayList<>();
                for (BLangWorkerSendReceiveExpr.Channel connection : function.sendsToThis) {
                    if (connection.eventIndex != 1) { // TODO: check this
                        continue;
                    }
                    if (connection.receiver.equals(name)) {
                        from.add(connection.sender);
                    } else {
                        to.add(connection.receiver);
                    }
                }
                nodes.put(name, new Node(function, from, to));
            }
        }
        accumulate(pkgNode);
        return pkgNode;
    }
    
    private void accumulate(BLangPackage pkgNode) {
        Map<String, List<String>> accumulatedWorkers = new HashMap<>();
        Map<String, String> workerStatus = new HashMap<>();
        
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            String worker = entry.getKey();
            Node node = entry.getValue();
            if (workerStatus.containsKey(worker)) {
                continue;
            }
            if (isSeqStart(node)) {
                detectSeq(worker, accumulatedWorkers, workerStatus);
            }
        }
        for (Map.Entry<String, List<String>> entry : accumulatedWorkers.entrySet()) {
            // analyse for send action
            List<BLangStatement> stmts = new ArrayList<>();
            ActionsAccumulator actionAccumulator = new ActionsAccumulator(stmts);
            
            // [w1] -> [w1, w2, w3, w4]
            // [w2, w3, w4] will be combined with w1
            List<String> workers = entry.getValue();
            String firstWorkerInSeq = workers.get(0);
            BLangFunction firstWorkerNode = nodes.get(firstWorkerInSeq).worker;
            actionAccumulator.analyze(firstWorkerNode, ActionCheck.SEND_ONLY, firstWorkerNode.pos); // change `analyze` to `accumulate`

            for (int i = 1; i < workers.size() - 1; i++) {
                BLangFunction workerNode = nodes.get(workers.get(i)).worker;
                actionAccumulator.analyze(workerNode, ActionCheck.SEND_RECEIVE, workerNode.pos);
            }
            String lastWorkerInSeq = workers.get(workers.size() - 1);
            BLangFunction lastWorkerNode = nodes.get(lastWorkerInSeq).worker;
            actionAccumulator.analyze(lastWorkerNode, ActionCheck.RECEIVE_ONLY, lastWorkerNode.pos);
            ((BLangBlockFunctionBody) firstWorkerNode.body).stmts = stmts;
            
            WorkerReceiverReplacer receiverReplacer = new WorkerReceiverReplacer(firstWorkerInSeq);
            receiverReplacer.analyze(lastWorkerNode);
            
            deleteAccumulatedWorkers(pkgNode, workers);
            updateChannels(firstWorkerNode, firstWorkerInSeq, lastWorkerInSeq);
        }
    }
    
    // Here seq start is considered as a worker which has only one connection 
    // to another worker(or multiple connections to a single worker).
    // Among the workers that satisfy the above condition, 
    // for the worker that has only one incoming connection, the previous worker should be considered
    // That worker should not be a part of the sequence.
    private boolean isSeqStart(Node node) {
        List<String> from = node.from;
        List<String> to = node.to;
        
        if (to.size() != 1) {
            return false;
        }
        if (from.size() == 1) {
            String prevWorker = from.get(0);
            Node prevNode = nodes.get(prevWorker);
            return prevNode.to.size() > 1;
        }
        return true;
    }
    
    private boolean isSeqMiddle(Node node) {
        List<String> from = node.from;
        List<String> to = node.to;
        // Do we need to check "from" size?
        return from.size() == 1 && to.size() == 1;
    }
    
    private void detectSeq(String startWorker, Map<String, List<String>> accumulatedWorkers, Map<String, 
            String> workerStatus) {
        String worker = nodes.get(startWorker).to.get(0);
        List<String> seq = new ArrayList<>();
        seq.add(startWorker);
        while (true) {
            Node node = nodes.get(worker);
            if (node == null) {
                // Should properly handle this, this comes when the worker is `function`
                break;
            }
            if (isSeqMiddle(node)) {
                workerStatus.put(worker, "seq");
                seq.add(worker);
                worker = node.to.get(0);
                continue;
            }
            // This is the end of the sequence
            if (node.to.isEmpty()) {
                seq.add(worker);
            }
            break;
        }
        if (seq.size() > 1) {
            accumulatedWorkers.put(startWorker, seq);
            workerStatus.put(startWorker, "seq");
        }
    }
    
    private void deleteAccumulatedWorkers(BLangPackage pkgNode, List<String> workers) {
        for (int i = 1; i < workers.size(); i++) {
            BLangFunction function = nodes.get(workers.get(i)).worker;
            BLangNode parent = function.parent.parent.parent;
            if (parent.getKind() == NodeKind.VARIABLE_DEF) {
                BLangSimpleVariableDef varDef = (BLangSimpleVariableDef) parent;
                BLangNode functionBlock = parent.parent;
                if (functionBlock.getKind() == NodeKind.BLOCK_FUNCTION_BODY) {
                    List<BLangStatement> stmts = ((BLangBlockFunctionBody) functionBlock).stmts;
                    int ii = stmts.indexOf(varDef);
                    if (ii != -1) {
                        stmts.remove(ii);
                        stmts.remove(ii); // remove var-def for function call
                    }
                }
            }
            int ii = pkgNode.functions.indexOf(function);
            if (ii != -1) {
                pkgNode.functions.remove(ii);
            }
        }
    }
    
    private void updateChannels(BLangFunction combinedWorker, String firstWorker, String lastWorker) {
        LinkedHashSet<BLangWorkerSendReceiveExpr.Channel> sendsToThis = new LinkedHashSet<>();
        if (nodes.get(lastWorker).to.isEmpty()) {
            return;
        }
        String nextToSeqEnd = nodes.get(lastWorker).to.get(0);
        for (BLangWorkerSendReceiveExpr.Channel channel : combinedWorker.sendsToThis) {
            if (channel.receiver.equals(firstWorker)) {
                sendsToThis.add(channel);
            } else {
                sendsToThis.add(new BLangWorkerSendReceiveExpr.Channel(firstWorker, nextToSeqEnd, channel.eventIndex));
            }
        }
        combinedWorker.sendsToThis = sendsToThis;

        if (nextToSeqEnd.equals("function")) {
            return;
        }
        BLangFunction nodeNextToSeqEnd = nodes.get(nextToSeqEnd).worker;
        LinkedHashSet<BLangWorkerSendReceiveExpr.Channel> sendsToThis1 = new LinkedHashSet<>();
        for (BLangWorkerSendReceiveExpr.Channel channel : nodeNextToSeqEnd.sendsToThis) {
            if (channel.sender.equals(lastWorker)) {
                sendsToThis1.add(new BLangWorkerSendReceiveExpr.Channel(firstWorker, channel.receiver, channel.eventIndex));
            } else {
                sendsToThis1.add(channel);
            }
        }
        nodeNextToSeqEnd.sendsToThis = sendsToThis1;
    }
    
    private static class ActionsAccumulator extends SimpleBLangNodeAnalyzer<ActionsAccumulator.AnalyzerData> {
        private final List<BLangStatement> stmts;
        BLangExpression commonVarRef;
        public ActionsAccumulator(List<BLangStatement> stmts) {
            this.stmts = stmts;
        }
        
        public void analyze(BLangFunction worker, ActionCheck actionCheck, Location pos) {
            List<BLangStatement> workerStmts = ((BLangBlockFunctionBody) worker.body).stmts;
            BLangDo blangDo = (BLangDo) TreeBuilder.createDoNode();
            blangDo.body = ASTBuilderUtil.createBlockStmt(pos, new ArrayList<>());
            for (BLangStatement stmt : workerStmts) {
                AnalyzerData data = new AnalyzerData(blangDo.body.stmts, actionCheck);
                data.doStmts.add(stmt);
                visitNode(stmt, data);
                // set env
            }
            this.stmts.add(blangDo);
        }

        @Override
        public void analyzeNode(BLangNode node, AnalyzerData data) {
            
        }

        @Override
        public void visit(BLangPackage node, AnalyzerData data) {

        }
        
        private BLangExpression analyzeExpr(BLangExpression expr, AnalyzerData data) {
            if (isActionFound(data)) {
                return expr;
            }
            data.receiveExpr = null;
            visitNode(expr, data);
            if (data.receiveExpr == null) {
                return expr;
            }
            return data.receiveExpr;
        }
        
        // visit statements
        @Override
        public void visit(BLangSimpleVariableDef varDef, AnalyzerData data) {
            visitNode(varDef.var, data);
        }
        
        @Override
        public void visit(BLangSimpleVariable var, AnalyzerData data) {
            analyzeExpr(var.expr, data);
        }
        
        @Override
        public void visit(BLangExpressionStmt exprStmt, AnalyzerData data) {
            analyzeExpr(exprStmt.expr, data);
        }
        
        // visit expressions
        @Override
        public void visit(BLangWorkerAsyncSendExpr asyncSendExpr, AnalyzerData data) {
            data.foundSendAction = true;
            if (data.actionCheck != ActionCheck.RECEIVE_ONLY) {
                Location pos = asyncSendExpr.pos;
                BLangSimpleVariableDef variableDef = createVarDef(asyncSendExpr, pos);
                this.stmts.add(variableDef);
                this.commonVarRef = ASTBuilderUtil.createVariableRef(pos, variableDef.var.symbol);
                data.doStmts.remove(data.doStmts.size() - 1);
                data.doStmts.add(ASTBuilderUtil.createAssignmentStmt(pos, this.commonVarRef, asyncSendExpr.expr));
            }
        }

        @Override
        public void visit(BLangWorkerReceive receiveExpr, AnalyzerData data) {
            data.foundReceiveAction = true;
            if (data.actionCheck != ActionCheck.SEND_ONLY) {
                data.receiveExpr = this.commonVarRef;
            }
        }
        
        @Override
        public void visit(BLangCheckedExpr checkedExpr, AnalyzerData data) {
            checkedExpr.expr = analyzeExpr(checkedExpr.expr, data);
        }
        
        private BLangSimpleVariableDef createVarDef(BLangWorkerAsyncSendExpr asyncSendExpr, Location pos) {
            String name = asyncSendExpr.workerIdentifier.value;
            BType type = asyncSendExpr.sendType;
            BSymbol owner = asyncSendExpr.env.scope.owner;
            BVarSymbol symbol = new BVarSymbol(0, Names.fromString("$_" + name), owner.pkgID, type, owner, pos, VIRTUAL);
            BLangSimpleVariable variable = ASTBuilderUtil.createVariable(pos, name, type, null, symbol);
            return ASTBuilderUtil.createVariableDef(pos, variable);
        }
        
        private boolean isActionFound(AnalyzerData data) {
            return switch (data.actionCheck) {
                case SEND_ONLY -> data.foundSendAction;
                case RECEIVE_ONLY -> data.foundReceiveAction;
                default -> data.foundSendAction && data.foundReceiveAction;
            };
        }

        public static class AnalyzerData {
            private AnalyzerData(List<BLangStatement> doStmts, ActionCheck actionCheck) {
                this.doStmts = doStmts;
                this.actionCheck = actionCheck;
            }
            List<BLangStatement> doStmts;
            ActionCheck actionCheck;
            boolean foundSendAction = false;
            boolean foundReceiveAction = false;
            BLangExpression receiveExpr;
        }
    }
    
    private static class WorkerReceiverReplacer extends SimpleBLangNodeAnalyzer<WorkerReceiverReplacer.AnalyzerData> {
        private final String replace;

        public WorkerReceiverReplacer(String replace) {
            this.replace = replace;
        }

        @Override
        public void analyzeNode(BLangNode node, AnalyzerData data) {

        }

        private void analyze(BLangFunction worker) {
            AnalyzerData data = new AnalyzerData();
            List<BLangStatement> workerStmts = ((BLangBlockFunctionBody) worker.body).stmts;
            for (BLangStatement stmt : workerStmts) {
                visitNode(stmt, data);
            }
        }

        @Override
        public void visit(BLangPackage node, AnalyzerData data) {

        }
        
        @Override
        public void visit(BLangWorkerAsyncSendExpr sendExpr, AnalyzerData data) {
            BLangWorkerReceive receiveExpr = sendExpr.receive;
            BLangWorkerSendReceiveExpr.Channel channel = receiveExpr.getChannel();
            receiveExpr.workerIdentifier = createIdentifier(replace);
            BLangWorkerSendReceiveExpr.Channel newChannel = new BLangWorkerSendReceiveExpr.Channel(replace, channel.receiver, channel.eventIndex);
            receiveExpr.setChannel(newChannel);
            sendExpr.setChannel(newChannel);
        }

        private BLangIdentifier createIdentifier(String name) {
            BLangIdentifier identifier = new BLangIdentifier();
            identifier.setValue(name);
            identifier.setOriginalValue(name);
            return identifier;
        }

        public static class AnalyzerData {

        }
    }
}


