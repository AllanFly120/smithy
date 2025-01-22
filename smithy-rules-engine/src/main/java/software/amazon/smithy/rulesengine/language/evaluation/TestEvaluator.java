/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestExpectation;
import software.amazon.smithy.rulesengine.traits.ExpectedEndpoint;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides facilities for evaluating an endpoint rule-set and tests.
 */
@SmithyUnstableApi
public final class TestEvaluator {
    private TestEvaluator() {}

    /**
     * Evaluate the given rule-set and test case. Throws an exception in the event
     * the test case does not pass.
     *
     * @param ruleset  The rule-set to be tested.
     * @param testCase The test case.
     */
    public static void evaluate(EndpointRuleSet ruleset, EndpointTestCase testCase) {
        Map<Identifier, Value> parameters = new LinkedHashMap<>();
        for (Map.Entry<StringNode, Node> entry : testCase.getParams().getMembers().entrySet()) {
            parameters.put(Identifier.of(entry.getKey()), Value.fromNode(entry.getValue()));
        }
        Value result = RuleEvaluator.evaluate(ruleset, parameters);

        StringBuilder messageBuilder = new StringBuilder("while executing test case");
        if (testCase.getDocumentation().isPresent()) {
            messageBuilder.append(" ").append(testCase.getDocumentation().get());
        }
        RuleError.context(messageBuilder.toString(), testCase, () -> evaluateExpectation(testCase.getExpect(), result));
    }

    private static void evaluateExpectation(EndpointTestExpectation expected, Value actual) {
        if (!(expected.getEndpoint().isPresent() || expected.getError().isPresent())) {
            throw new RuntimeException("Unhandled endpoint test case expectation.");
        }

        if (expected.getEndpoint().isPresent()) {
            ExpectedEndpoint expectedEndpoint = expected.getEndpoint().get();
            EndpointValue.Builder builder = EndpointValue.builder()
                    .url(expectedEndpoint.getUrl())
                    .headers(expectedEndpoint.getHeaders());
            for (Map.Entry<String, Node> entry : expectedEndpoint.getProperties().entrySet()) {
                builder.putProperty(entry.getKey(), Value.fromNode(entry.getValue()));
            }

            if (!actual.expectEndpointValue().equals(builder.build())) {
                throw new AssertionError(String.format("Expected endpoint:%n%s but got:%n%s (generated by %s)",
                        StringUtils.indent(expectedEndpoint.toString(), 2),
                        StringUtils.indent(actual.toString(), 2),
                        expectedEndpoint.getSourceLocation()));
            }
        } else {
            String wantError = expected.getError().get();
            RuleError.context("While checking endpoint test (expecting an error)", () -> {
                if (!actual.expectStringValue().getValue().equals(wantError)) {
                    throw new AssertionError(String.format("Expected error `%s` but got `%s`", wantError, actual));
                }
            });
        }
    }
}
