/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.polyglot.scala;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import org.graalvm.compiler.phases.util.Providers;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.core.ParsingReason;
import com.oracle.svm.core.SubstrateOptions;
import com.oracle.svm.core.feature.InternalFeature;
import com.oracle.svm.core.util.UserError;
import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl;
import com.oracle.svm.util.ModuleSupport;

public class ScalaFeature implements InternalFeature {

    public static final String UNSUPPORTED_SCALA_VERSION = "This is not a supported Scala version. native-image supports Scala 2.11.x and onwards.";

    static class IsEnabled implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            return ImageSingletons.contains(ScalaFeature.class);
        }
    }

    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return access.findClassByName("scala.Predef") != null;
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        initializeScalaEnumerations(access);
        RuntimeClassInitialization.initializeAtBuildTime("scala.Symbol");
        RuntimeClassInitialization.initializeAtBuildTime("scala.Symbol$");
        /* Initialized through an invokedynamic in `scala.Option` */
        RuntimeClassInitialization.initializeAtBuildTime("scala.runtime.LambdaDeserialize");
        RuntimeClassInitialization.initializeAtBuildTime("scala.runtime.StructuralCallSite");
        RuntimeClassInitialization.initializeAtBuildTime("scala.runtime.EmptyMethodCache");
        ModuleSupport.accessPackagesToClass(ModuleSupport.Access.EXPORT, ScalaFeature.class, false, "jdk.internal.vm.compiler", "org.graalvm.compiler.nodes");
    }

    @Override
    public void registerGraphBuilderPlugins(Providers providers, Plugins plugins, ParsingReason reason) {
        ModuleSupport.accessPackagesToClass(ModuleSupport.Access.EXPORT, ScalaFeature.class, false, "jdk.internal.vm.ci", "jdk.vm.ci.meta");
        if (SubstrateOptions.parseOnce() || reason.duringAnalysis()) {
            plugins.appendNodePlugin(new ScalaAnalysisPlugin());
        }
    }

    private static boolean isValDef(Field[] fields, Method m) {
        return Arrays.stream(fields).anyMatch(fd -> fd.getName().equals(m.getName()) && fd.getType().equals(m.getReturnType()));
    }

    /**
     * Not all Scala enumerations can be pre-initialized. For that reason we support the Scala's
     * original mechanism for initializing enumerations reflectively.
     */
    private static void initializeScalaEnumerations(BeforeAnalysisAccess beforeAnalysisAccess) {
        BeforeAnalysisAccessImpl access = (BeforeAnalysisAccessImpl) beforeAnalysisAccess;

        Class<?> scalaEnum = access.findClassByName("scala.Enumeration");
        UserError.guarantee(scalaEnum != null, "%s", UNSUPPORTED_SCALA_VERSION);
        Class<?> scalaEnumVal = access.findClassByName("scala.Enumeration$Val");
        UserError.guarantee(scalaEnumVal != null, "%s", UNSUPPORTED_SCALA_VERSION);
        Class<?> valueClass = access.findClassByName("scala.Enumeration$Value");
        UserError.guarantee(valueClass != null, "%s", UNSUPPORTED_SCALA_VERSION);

        access.findSubclasses(scalaEnum).forEach(enumClass -> {
            /* this is based on implementation of scala.Enumeration.populateNamesMap */
            RuntimeReflection.registerAllDeclaredFields(enumClass);
            // all method relevant for Enums
            Method[] relevantMethods = Arrays.stream(enumClass.getDeclaredMethods())
                            .filter(m -> m.getParameterTypes().length == 0 &&
                                            m.getDeclaringClass() != scalaEnum &&
                                            valueClass.isAssignableFrom(m.getReturnType()) &&
                                            isValDef(enumClass.getDeclaredFields(), m))
                            .toArray(Method[]::new);
            RuntimeReflection.register(relevantMethods);
            try {
                RuntimeReflection.register(scalaEnumVal.getDeclaredMethod("id"));
            } catch (NoSuchMethodException e) {
                throw UserError.abort("%s", UNSUPPORTED_SCALA_VERSION);
            }
        });
    }

}
