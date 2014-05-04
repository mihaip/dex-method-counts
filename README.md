# dex-method-counts

Simple tool to output per-package method counts in an Android DEX executable
grouped by package, to aid in getting under the 65536 referenced method limit.

To use it:

    $ ant jar
    $ ./dex-method-counts path/to/App.apk # or .zip or .dex

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

The DEX file parsing is based on the `dexdeps` tool from
[the Android source tree](https://android.googlesource.com/platform/dalvik.git/+/master/tools/dexdeps/).
