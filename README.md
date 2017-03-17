# dex-method-counts

Simple tool to output per-package method counts in an Android DEX executable grouped by package, to aid in getting under the 65,536 referenced method limit. More details are [in this blog post](http://blog.persistent.info/2014/05/per-package-method-counts-for-androids.html).

To run it with Ant:

    $ ant jar
    $ ./dex-method-counts path/to/App.apk # or .zip or .dex or directory

or with Gradle:

    $ ./gradlew assemble
    $ ./dex-method-counts path/to/App.apk # or .zip or .dex or directory

on Windows:

    $ gradlew assemble
    $ dex-method-counts.bat path\to\App.apk

You'll see output of the form:

    Read in 65490 method IDs.
    <root>: 65490
        : 3
        android: 6837
            accessibilityservice: 6
            bluetooth: 2
            content: 248
                pm: 22
                res: 45
            ...
        com: 53881
            adjust: 283
                sdk: 283
            codebutler: 65
                android_websockets: 65
            ...
        Overall method count: 65490

Supported options are:

* `--count-fields`: Provide the field count instead of the method count.
* `--include-classes`: Treat classes as packages and provide per-class method counts. One use-case is for protocol buffers where all generated code in a package ends up in a single class.
* `--package-filter=...`: Only consider methods whose fully qualified name starts with this prefix.
* `--max-depth=...`: Limit how far into package paths (or inner classes, with `--include-classes`) counts should be reported for.
* `--filter=[all|defined_only|referenced_only]`: Whether to count all methods (the default), just those defined in the input file, or just those that are referenced in it. Note that referenced methods count against the 64K method limit too.
* `--compare`: Prints a comparison counts between the second and first APK. Note that the comparison will not be accurate if either of the APKs has been obfuscated using ProGuard.
* `--output-style=[flat|tree]`: Print the output as a list or as an indented tree.

The DEX file parsing is based on the `dexdeps` tool from
[the Android source tree](https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/).
