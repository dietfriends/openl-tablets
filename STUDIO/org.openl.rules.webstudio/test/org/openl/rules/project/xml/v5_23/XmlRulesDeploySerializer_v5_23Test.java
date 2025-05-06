package org.openl.rules.project.xml.v5_23;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.project.xml.BaseRulesDeploySerializerTest.generateRulesDeployForTest;

import java.io.FileInputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.project.model.v5_23.RulesDeploy_v5_23;
import org.openl.rules.project.model.v5_23.converter.RulesDeployVersionConverter_v5_23;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

public class XmlRulesDeploySerializer_v5_23Test {

    @Test
    public void testReadRulesDeploy() throws Exception {
        FileInputStream fis = new FileInputStream("test-resources/org.openl.rules.project.xml/rules-deploy.xml");
        BaseRulesDeploySerializer<RulesDeploy_v5_23> serializer =
                new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_23(), RulesDeploy_v5_23.class);
        RulesDeploy rulesDeploy = serializer.deserialize(fis);

        assertNotNull(rulesDeploy);
        assertEquals(Boolean.FALSE, rulesDeploy.isProvideRuntimeContext());
        assertEquals("rulesDeployName", rulesDeploy.getServiceName());
        assertNotNull(rulesDeploy.getPublishers());
        assertEquals(1, rulesDeploy.getPublishers().length);
        assertEquals(PublisherType.RESTFUL, rulesDeploy.getPublishers()[0]);
        assertEquals(String.class.getName(), rulesDeploy.getInterceptingTemplateClassName());
        assertEquals(String.class.getName(), rulesDeploy.getAnnotationTemplateClassName());
        assertEquals(String.class.getName(), rulesDeploy.getRmiServiceClass());
        assertEquals("someURL", rulesDeploy.getUrl());
        assertNull(rulesDeploy.getRmiName());
        assertEquals("v1", rulesDeploy.getVersion());
        assertEquals("group1,group2", rulesDeploy.getGroups());
        assertEquals(String.class.getName(), rulesDeploy.getServiceClass());
        assertNotNull(rulesDeploy.getLazyModulesForCompilationPatterns());
        assertEquals(2, rulesDeploy.getLazyModulesForCompilationPatterns().length);
        assertEquals("some1*", rulesDeploy.getLazyModulesForCompilationPatterns()[0].getValue());
        assertEquals("some2*", rulesDeploy.getLazyModulesForCompilationPatterns()[1].getValue());
        assertEquals(Map.of("rootClassNamesBinding", "com.chartis.premier.rating.result.ChartisCompoundStep,com.chartis.premier.rating.result.ChartisSimpleStep",
                        "some.other.key", "some.other.value"),
                rulesDeploy.getConfiguration());
    }

    @Test
    public void testWriteRulesDeploy() throws Exception {
        RulesDeploy rulesDeploy = generateRulesDeployForTest();
        rulesDeploy.setPublishers(new PublisherType[]{PublisherType.RMI, PublisherType.KAFKA});
        BaseRulesDeploySerializer<RulesDeploy_v5_23> serializer =
                new BaseRulesDeploySerializer<>(new RulesDeployVersionConverter_v5_23(), RulesDeploy_v5_23.class);
        String value = serializer.serialize(rulesDeploy);
        assertEquals(EXPECTED_VALUE, value);
    }

    private static final String EXPECTED_VALUE = "<rules-deploy>\n" +
            "    <isProvideRuntimeContext>false</isProvideRuntimeContext>\n" +
            "    <serviceName>rulesDeployName</serviceName>\n" +
            "    <publishers>\n" +
            "        <publisher>RMI</publisher>\n" +
            "        <publisher>KAFKA</publisher>\n" +
            "    </publishers>\n" +
            "    <interceptingTemplateClassName>java.lang.String</interceptingTemplateClassName>\n" +
            "    <annotationTemplateClassName>java.lang.String</annotationTemplateClassName>\n" +
            "    <serviceClass>java.lang.String</serviceClass>\n" +
            "    <rmiServiceClass>java.lang.String</rmiServiceClass>\n" +
            "    <url>someURL</url>\n" +
            "    <version>v1</version>\n" +
            "    <groups>group1,group2</groups>\n" +
            "    <configuration>\n" +
            "        <entry>\n" +
            "            <string>key2</string>\n" +
            "            <string>value2</string>\n" +
            "        </entry>\n" +
            "        <entry>\n" +
            "            <string>key</string>\n" +
            "            <string>value</string>\n" +
            "        </entry>\n" +
            "    </configuration>\n" +
            "    <lazy-modules-for-compilation>\n" +
            "        <module name=\"some1*\"/>\n" +
            "        <module name=\"some2*\"/>\n" +
            "    </lazy-modules-for-compilation>\n" +
            "</rules-deploy>";
}
