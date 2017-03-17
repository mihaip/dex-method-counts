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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

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

            @Override
            void output(DexCount counts, DexCount comparedTo) {
                counts.packageTree.output("", comparedTo.packageTree);
            }
        },
        FLAT {
            @Override
            void output(DexCount counts) {
                for (Map.Entry<String, IntHolder> e : counts.packageCount.entrySet()) {
                    String packageName = e.getKey();
                    if (packageName.equals("")) {
                        packageName = "<no package>";
                    }
                    System.out.printf("%6s %s\n", e.getValue().value, packageName);
                }
            }

            @Override
            void output(DexCount counts, DexCount comparedTo) {
                for (Map.Entry<String, IntHolder> e : counts.packageCount.entrySet()) {
                    String packageName = e.getKey();
                    final IntHolder comparedToValue;

                    if (packageName.equals("")) {
                        packageName = "<no package>";
                        comparedToValue = null;
                    } else {
                        comparedToValue = comparedTo.packageCount.get(packageName);
                    }

                    final int diff = (comparedToValue == null)
                            ? e.getValue().value
                            : e.getValue().value - comparedToValue.value;
                    System.out.printf(
                            "%6d (%+6d) %s\n",
                            e.getValue().value,
                            diff,
                            packageName);
                }

                final Set<String> removed = new LinkedHashSet<>(comparedTo.packageCount.keySet());
                removed.removeAll(counts.packageCount.keySet());
                removed.forEach(packageName -> System.out.printf(
                        "%6d (%+6d) %s\n",
                        0,
                        -comparedTo.packageCount.get(packageName).value,
                        packageName));
            }
        };

        abstract void output(DexCount counts);
        abstract void output(DexCount counts, DexCount comparedTo);
    }

    void output() {
        outputStyle.output(this);
    }

    void output(DexCount comparedTo) {
        outputStyle.output(this, comparedTo);
    }

    int getOverallCount() {
        return overallCount;
    }

    static class Node {

        private static final NumberFormat FORMAT = new DecimalFormat("+#;-#");

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

        void output(String indent, Node comparedTo) {
            if (indent.length() == 0) {
                out.println("<root>: " + count);
            }
            indent += "    ";

            for (String name : children.navigableKeySet()) {
                Node child = children.get(name);

                final Node comparedToChild = (comparedTo == null)
                        ? null
                        :comparedTo.children.get(name);
                final int diff = (comparedToChild == null)
                        ? child.count
                        : child.count - comparedToChild.count;

                out.println(indent + name + ": " + child.count + " (" + FORMAT.format(diff) + ")");
                child.output(indent, comparedToChild);
            }
        }
    }
}
