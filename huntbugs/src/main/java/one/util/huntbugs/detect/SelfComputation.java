/*
 * Copyright 2016 HuntBugs contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.util.huntbugs.detect;

import com.strobel.assembler.metadata.JvmType;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Expression;

import one.util.huntbugs.flow.ValuesFlow;
import one.util.huntbugs.registry.MethodContext;
import one.util.huntbugs.registry.anno.AstNodes;
import one.util.huntbugs.registry.anno.AstVisitor;
import one.util.huntbugs.registry.anno.WarningDefinition;
import one.util.huntbugs.util.Methods;
import one.util.huntbugs.util.Nodes;
import one.util.huntbugs.warning.Roles;

/**
 * @author Tagir Valeev
 *
 */
@WarningDefinition(category = "Correctness", name = "SelfComputation", maxScore = 70)
@WarningDefinition(category = "Correctness", name = "SelfComparison", maxScore = 70)
@WarningDefinition(category = "Correctness", name = "SelfEquals", maxScore = 70)
public class SelfComputation {
    @AstVisitor(nodes = AstNodes.EXPRESSIONS)
    public void visit(Expression expr, MethodContext mc) {
        if ((expr.getCode() == AstCode.And || expr.getCode() == AstCode.Or || expr.getCode() == AstCode.Xor
            || expr.getCode() == AstCode.Sub || expr.getCode() == AstCode.Div || expr.getCode() == AstCode.Rem)
            && sameArgs(expr)) {
            mc.report("SelfComputation", 0, expr.getArguments().get(0), Roles.OPERATION.create(expr));
        } else if (expr.getCode().isComparison() && sameArgs(expr)) {
            JvmType type = expr.getArguments().get(0).getInferredType().getSimpleType();
            if ((expr.getCode() != AstCode.CmpEq && expr.getCode() != AstCode.CmpNe)
                || (type != JvmType.Double && type != JvmType.Float))
                mc.report("SelfComparison", 0, expr.getArguments().get(0), Roles.OPERATION.create(expr.getCode()));
        } else if (expr.getCode() == AstCode.InvokeVirtual
            && Methods.isEqualsMethod((MethodReference) expr.getOperand()) && sameArgs(expr) &&
            !ValuesFlow.isAssertion(expr)) {
            mc.report("SelfEquals", 0, expr.getArguments().get(0));
        }
    }

    private boolean sameArgs(Expression expr) {
        return expr.getArguments().size() == 2
            && Nodes.isEquivalent(expr.getArguments().get(0), expr.getArguments().get(1));
    }
}
