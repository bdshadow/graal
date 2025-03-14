/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.hosted.meta;

import org.graalvm.compiler.word.WordTypes;
import org.graalvm.nativeimage.c.function.RelocatedPointer;
import org.graalvm.word.WordBase;

import com.oracle.svm.core.FrameAccess;
import com.oracle.svm.core.graal.meta.SubstrateSnippetReflectionProvider;
import com.oracle.svm.hosted.ameta.AnalysisConstantReflectionProvider;

import jdk.vm.ci.meta.JavaConstant;

/**
 * The snippet reflection provider that acts as the interface between the Native Image builder and
 * the hosting VM.
 */
public class HostedLookupSnippetReflectionProvider extends SubstrateSnippetReflectionProvider {

    public HostedLookupSnippetReflectionProvider(WordTypes wordTypes) {
        super(wordTypes);
    }

    @Override
    public JavaConstant forObject(Object object) {
        if (object instanceof RelocatedPointer pointer) {
            return new RelocatableConstant(pointer);
        } else if (object instanceof WordBase word) {
            return JavaConstant.forIntegerKind(FrameAccess.getWordKind(), word.rawValue());
        }
        AnalysisConstantReflectionProvider.validateRawObjectConstant(object);
        return super.forObject(object);
    }

    @Override
    public <T> T asObject(Class<T> type, JavaConstant constant) {
        if (constant instanceof RelocatableConstant relocatable) {
            return type.cast(relocatable.getPointer());
        }
        return super.asObject(type, constant);
    }
}
