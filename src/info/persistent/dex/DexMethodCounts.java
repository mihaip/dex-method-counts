/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.persistent.dex;

import com.android.dexdeps.ClassRef;
import com.android.dexdeps.DexData;
import com.android.dexdeps.MethodRef;
import com.android.dexdeps.Output;

import java.util.*;

public class DexMethodCounts extends DexCount {

    DexMethodCounts(OutputStyle outputStyle) {
        super(outputStyle);
    }

    @Override
    public void generate(DexData dexData, boolean includeClasses, String packageFilter, int maxDepth, Filter filter) {
        MethodRef[] methodRefs = getMethodRefs(dexData, filter);

        for (MethodRef methodRef : methodRefs) {
            String classDescriptor = methodRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            overallCount++;
            if (outputStyle == OutputStyle.TREE) {
                String packageNamePieces[] = packageName.split("\\.");
                Node packageNode = packageTree;
                for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                    packageNode.count++;
                    String name = packageNamePieces[i];
                    if (packageNode.children.containsKey(name)) {
                        packageNode = packageNode.children.get(name);
                    } else if (packageNode.children.containsKey("<default>") && "".equals(name)) {
                        packageNode = packageNode.children.get("<default>");
                    } else {
                        Node childPackageNode = new Node();
                        if (name.length() == 0) {
                            // This method is declared in a class that is part of the default package.
                            // Typical examples are methods that operate on arrays of primitive data types.
                            name = "<default>";
                        }
                        packageNode.children.put(name, childPackageNode);
                        packageNode = childPackageNode;
                    }
                }
                packageNode.count++;
            } else if (outputStyle == OutputStyle.FLAT) {
                IntHolder count = packageCount.get(packageName);
                if (count == null) {
                    count = new IntHolder();
                    packageCount.put(packageName, count);
                }
                count.value++;
            }
        }
    }

    private static MethodRef[] getMethodRefs(DexData dexData, Filter filter) {
        MethodRef[] methodRefs = dexData.getMethodRefs();
        out.println("Read in " + methodRefs.length + " method IDs.");
        if (filter == Filter.ALL) {
            return methodRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        out.println("Read in " + externalClassRefs.length +
                " external class references.");
        Set<MethodRef> externalMethodRefs = new HashSet<MethodRef>();
        for (ClassRef classRef : externalClassRefs) {
            Collections.addAll(externalMethodRefs, classRef.getMethodArray());
        }
        out.println("Read in " + externalMethodRefs.size() +
                " external method references.");
        List<MethodRef> filteredMethodRefs = new ArrayList<MethodRef>();
        for (MethodRef methodRef : methodRefs) {
            boolean isExternal = externalMethodRefs.contains(methodRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal) ||
                    (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredMethodRefs.add(methodRef);
            }
        }
        out.println("Filtered to " + filteredMethodRefs.size() + " " +
                (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") +
                " method IDs.");
        return filteredMethodRefs.toArray(
                new MethodRef[filteredMethodRefs.size()]);
    }
}
