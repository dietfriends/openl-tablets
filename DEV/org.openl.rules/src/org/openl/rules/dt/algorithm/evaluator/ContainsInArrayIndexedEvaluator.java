package org.openl.rules.dt.algorithm.evaluator;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.DecisionTableRuleNodeBuilder;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.index.EqualsIndex;
import org.openl.rules.helpers.NumberUtils;

/**
 * @author snshor
 *
 */
public class ContainsInArrayIndexedEvaluator extends AContainsInArrayIndexedEvaluator {

    @Override
    public ARuleIndex makeIndex(ICondition condition, IIntIterator iterator) {

        if (iterator.size() < 1) {
            return null;
        }

        Map<Object, DecisionTableRuleNodeBuilder> map = null;
        Map<Object, DecisionTableRuleNode> nodeMap = null;
        DecisionTableRuleNodeBuilder emptyBuilder = new DecisionTableRuleNodeBuilder();
        boolean comparatorBasedMap = false;

        while (iterator.hasNext()) {

            int i = iterator.nextInt();

            if (condition.isEmpty(i)) {

                emptyBuilder.addRule(i);
                if (map != null) {
                    for (DecisionTableRuleNodeBuilder builder : map.values()) {
                        builder.addRule(i);
                    }
                }

                continue;
            }

            Object values = condition.getParamValue(0, i);

            int length = Array.getLength(values);

            for (int j = 0; j < length; j++) {

                Object value = Array.get(values, j);
                if (comparatorBasedMap && !(value instanceof Comparable<?>)) {
                    throw new IllegalArgumentException("Invalid state! Index based on comparable interface!");
                }
                if (map == null) {
                    if (NumberUtils.isFloatPointNumber(value)) {
                        if (value instanceof BigDecimal) {
                            map = new TreeMap<>();
                            nodeMap = new TreeMap<>();
                        } else {
                            map = new TreeMap<>(FloatTypeComparator.getInstance());
                            nodeMap = new TreeMap<>(FloatTypeComparator.getInstance());
                        }
                        comparatorBasedMap = true;
                    } else {
                        map = new HashMap<>();
                        nodeMap = new HashMap<>();
                    }
                }

                DecisionTableRuleNodeBuilder builder = map.computeIfAbsent(value,
                    e -> new DecisionTableRuleNodeBuilder(emptyBuilder));
                builder.addRule(i);
            }
        }
        if (map != null) {
            for (Map.Entry<Object, DecisionTableRuleNodeBuilder> element : map.entrySet()) {
                nodeMap.put(element.getKey(), (element.getValue()).makeNode());
            }
        } else {
            nodeMap = Collections.emptyMap();
        }

        return new EqualsIndex(emptyBuilder.makeNode(), nodeMap);
    }

    @Override
    public int getPriority() {
        return IConditionEvaluator.ARRAY_CONDITION_PRIORITY;
    }
}
