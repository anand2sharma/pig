/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.data.DataType;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.NodeIdGenerator;
import org.apache.pig.impl.plan.VisitorException;

public class LTOrEqualToExpr extends BinaryComparisonOperator {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    transient private final Log log = LogFactory.getLog(getClass());

    public LTOrEqualToExpr(OperatorKey k) {
        this(k, -1);
    }

    public LTOrEqualToExpr(OperatorKey k, int rp) {
        super(k, rp);
        resultType = DataType.BOOLEAN;
    }

    @Override
    public String name() {
        return "Less Than or Equal" + "[" + DataType.findTypeName(resultType) + "]" +" - " + mKey.toString();
    }

    @Override
    public void visit(PhyPlanVisitor v) throws VisitorException {
        v.visitLTOrEqual(this);
    }

    @Override
    public Result getNext(Boolean bool) throws ExecException {
        Result left, right;

        switch (operandType) {
        case DataType.BYTEARRAY:
        case DataType.DOUBLE:
        case DataType.FLOAT:
        case DataType.INTEGER:
        case DataType.LONG:
        case DataType.DATETIME:
        case DataType.CHARARRAY: {
            Object dummy = getDummy(operandType);
            Result r = accumChild(null, dummy, operandType);
            if (r != null) {
                return r;
            }
            left = lhs.getNext(dummy, operandType);
            right = rhs.getNext(dummy, operandType);
            return doComparison(left, right);
        }
        default: {
            int errCode = 2067;
            String msg = this.getClass().getSimpleName() + " does not know how to " +
            "handle type: " + DataType.findTypeName(operandType);
            throw new ExecException(msg, errCode, PigException.BUG);
        }

        }
    }

    @SuppressWarnings("unchecked")
    private Result doComparison(Result left, Result right) {
        if (trueRef == null) {
            initializeRefs();
        }
        if (left.returnStatus != POStatus.STATUS_OK) {
            return left;
        }
        if (right.returnStatus != POStatus.STATUS_OK) {
            return right;
        }
        // if either operand is null, the result should be
        // null
        if(left.result == null || right.result == null) {
            left.result = null;
            left.returnStatus = POStatus.STATUS_NULL;
            return left;
        }
        assert(left.result instanceof Comparable);
        assert(right.result instanceof Comparable);
        if (((Comparable)left.result).compareTo(right.result) <= 0) {
            left.result = trueRef;
        } else {
            left.result = falseRef;
        }
        illustratorMarkup(null, left.result, (Boolean) left.result ? 0 : 1);
        return left;
    }

    @Override
    public LTOrEqualToExpr clone() throws CloneNotSupportedException {
        LTOrEqualToExpr clone = new LTOrEqualToExpr(new OperatorKey(mKey.scope,
            NodeIdGenerator.getGenerator().getNextNodeId(mKey.scope)));
        clone.cloneHelper(this);
        return clone;
    }
}
