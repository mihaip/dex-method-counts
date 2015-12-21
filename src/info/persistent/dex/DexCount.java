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

import com.android.dexdeps.DexData;

import java.io.PrintStream;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public abstract class DexCount {

    static final PrintStream out = System.out;
    final OutputStyle outputStyle;
    final Node packageTree;
    final Map<String, IntHolder> packageCount;
    int overallCount = 0;

    DexCount(OutputStyle outputStyle) {
        this.outputStyle = outputStyle;
        packageTree = this.outputStyle == OutputStyle.TREE ? new Node() : null;
        packageCount = this.outputStyle == OutputStyle.FLAT
                ? new TreeMap<String, IntHolder>() : null;
    }

    public abstract void generate(
            DexData dexData, boolean includeClasses, String packageFilter, int maxDepth, Filter filter);

    class IntHolder {

        int value;
    }

    enum Filter {
        ALL,
        DEFINED_ONLY,
        REFERENCED_ONLY
    }

    enum OutputStyle {
        TREE {
            @Override
            void output(DexCount counts) {
                counts.packageTree.output("");
            }
        },
        FLAT {
            @Override
            void output(DexCount counts) {
                for (Map.Entry<String, IntHolder> e : counts.packageCount.entrySet()) {
                    String packageName = e.getKey();
                    if (packageName == "") {
                        packageName = "<no package>";
                    }
                    System.out.printf("%6s %s\n", e.getValue().value, packageName);
                }
            }
        };

        abstract void output(DexCount counts);
    }

    void output() {
        outputStyle.output(this);
    }

    int getOverallCount() {
        return overallCount;
    }

    static class Node {

        int count = 0;
        NavigableMap<String, Node> children = new TreeMap<String, Node>();

        void output(String indent) {
            if (indent.length() == 0) {
                out.println("<root>: " + count);
            }
            indent += "    ";
            for (String name : children.navigableKeySet()) {
                Node child = children.get(name);
                out.println(indent + name + ": " + child.count);
                child.output(indent);
            }
        }
    }

}
