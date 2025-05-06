package org.openl.rules.project.model.v5_15.converter;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.WildcardPattern;
import org.openl.rules.project.model.v5_14.PublisherType_v5_14;
import org.openl.rules.project.model.v5_15.RulesDeploy_v5_15;
import org.openl.util.CollectionUtils;

public class RulesDeployVersionConverter_v5_15 implements ObjectVersionConverter<RulesDeploy, RulesDeploy_v5_15> {
    @Override
    public RulesDeploy fromOldVersion(RulesDeploy_v5_15 oldVersion) {
        RulesDeploy rulesDeploy = new RulesDeploy();

        rulesDeploy.setConfiguration(oldVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(oldVersion.getInterceptingTemplateClassName());

        if (oldVersion.getLazyModulesForCompilationPatterns() != null) {
            List<WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                    Arrays.asList(oldVersion.getLazyModulesForCompilationPatterns()),
                    e -> e == null ? null : new WildcardPattern(e.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                    .toArray(new WildcardPattern[0]));
        }

        rulesDeploy.setProvideRuntimeContext(oldVersion.isProvideRuntimeContext());

        if (oldVersion.getPublishers() != null) {
            List<RulesDeploy.PublisherType> publishers = CollectionUtils.map(Arrays.asList(oldVersion.getPublishers()),
                    version -> {
                        if (version == null) {
                            return null;
                        }

                        switch (version) {
                            case WEBSERVICE:
                                return RulesDeploy.PublisherType.WEBSERVICE;
                            case RESTFUL:
                                return RulesDeploy.PublisherType.RESTFUL;
                            default:
                                throw new IllegalArgumentException();
                        }
                    });
            rulesDeploy.setPublishers(publishers.toArray(new RulesDeploy.PublisherType[0]));
        }

        rulesDeploy.setServiceClass(oldVersion.getServiceClass());
        rulesDeploy.setServiceName(oldVersion.getServiceName());
        rulesDeploy.setUrl(oldVersion.getUrl());

        return rulesDeploy;
    }

    @Override
    public RulesDeploy_v5_15 toOldVersion(RulesDeploy currentVersion) {
        RulesDeploy_v5_15 rulesDeploy = new RulesDeploy_v5_15();

        rulesDeploy.setConfiguration(currentVersion.getConfiguration());
        rulesDeploy.setInterceptingTemplateClassName(currentVersion.getInterceptingTemplateClassName());

        if (currentVersion.getLazyModulesForCompilationPatterns() != null) {
            List<WildcardPattern> lazyModulesForCompilationPatterns = CollectionUtils.map(
                    Arrays.asList(currentVersion.getLazyModulesForCompilationPatterns()),
                    version -> version == null ? null : new WildcardPattern(version.getValue()));
            rulesDeploy.setLazyModulesForCompilationPatterns(lazyModulesForCompilationPatterns
                    .toArray(new WildcardPattern[0]));
        }

        rulesDeploy.setProvideRuntimeContext(currentVersion.isProvideRuntimeContext());

        if (currentVersion.getPublishers() != null) {
            List<PublisherType_v5_14> publishers = CollectionUtils
                    .map(Arrays.asList(currentVersion.getPublishers()), oldVersion -> {
                        if (oldVersion == null) {
                            return null;
                        }

                        switch (oldVersion) {
                            case WEBSERVICE:
                                return PublisherType_v5_14.WEBSERVICE;
                            case RESTFUL:
                                return PublisherType_v5_14.RESTFUL;
                            case RMI:
                                throw new UnsupportedOperationException("RMI publisher is not supported in old version.");
                            case KAFKA:
                                throw new UnsupportedOperationException("KAFKA publisher is not supported in old version.");
                            default:
                                throw new IllegalArgumentException();
                        }
                    });
            rulesDeploy.setPublishers(publishers.toArray(new PublisherType_v5_14[0]));
        }

        rulesDeploy.setServiceClass(currentVersion.getServiceClass());
        rulesDeploy.setServiceName(currentVersion.getServiceName());
        rulesDeploy.setUrl(currentVersion.getUrl());

        return rulesDeploy;
    }
}
