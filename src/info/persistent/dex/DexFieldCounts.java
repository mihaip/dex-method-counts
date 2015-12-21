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

import com.android.dexdeps.*;

import java.util.*;

public class DexFieldCounts extends DexCount {

    DexFieldCounts(OutputStyle outputStyle) {
        super(outputStyle);
    }

    @Override
    public void generate(DexData dexData, boolean includeClasses, String packageFilter, int maxDepth, Filter filter) {
        FieldRef[] fieldRefs = getFieldRefs(dexData, filter);

        for (FieldRef fieldRef : fieldRefs) {
            String classDescriptor = fieldRef.getDeclClassName();
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
                    } else {
                        Node childPackageNode = new Node();
                        if (name.length() == 0) {
                            // This field is declared in a class that is part of the default package.
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

    private static FieldRef[] getFieldRefs(DexData dexData, Filter filter) {
        FieldRef[] fieldRefs = dexData.getFieldRefs();
        out.println("Read in " + fieldRefs.length + " field IDs.");
        if (filter == Filter.ALL) {
            return fieldRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        out.println("Read in " + externalClassRefs.length + " external class references.");
        Set<FieldRef> externalFieldRefs = new HashSet<FieldRef>();
        for (ClassRef classRef : externalClassRefs) {
            Collections.addAll(externalFieldRefs, classRef.getFieldArray());
        }
        out.println("Read in " + externalFieldRefs.size() + " external field references.");
        List<FieldRef> filteredFieldRefs = new ArrayList<FieldRef>();
        for (FieldRef FieldRef : fieldRefs) {
            boolean isExternal = externalFieldRefs.contains(FieldRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal)
                    || (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredFieldRefs.add(FieldRef);
            }
        }
        out.println("Filtered to " + filteredFieldRefs.size() + " " +
                (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") + " field IDs.");
        return filteredFieldRefs.toArray(new FieldRef[filteredFieldRefs.size()]);
    }


}
