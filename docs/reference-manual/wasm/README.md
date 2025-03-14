---
layout: docs-experimental
toc_group: wasm
link_title: WebAssembly Reference
permalink: /reference-manual/wasm/
---

# GraalVM Implementation of WebAssembly

GraalVM can run programs compiled to WebAssembly.
It can interpret and compile WebAssembly code in the binary format or embed it into other programs.
The support for WebAssembly is in the early stages of its development.

## Getting Started

As of GraalVM for JDK 21, the GraalVM WebAssembly runtime (known as Wasm) is available as a standalone distribution. 
You can download a standalone based on Oracle GraalVM or GraalVM Community Edition. 

1. Download the Wasm standalone for your operating system:
   - Native standalone
      * [Linux x64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB3A93C6E0631818000A2314/content)
      * [Linux AArch64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB3F93C6E0631818000A2314/content)
      * [macOS x64](https://gds.oracle.com/api/20220101/artifacts/04F488A062154081E0631818000A781E/content)
      * [macOS AArch64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB4493C6E0631818000A2314/content)
      * [Windows x64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB4993C6E0631818000A2314/content)
   - JVM standalone
      * [Linux x64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB4E93C6E0631818000A2314/content)
      * [Linux AArch64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB5393C6E0631818000A2314/content)
      * [macOS x64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB5893C6E0631818000A2314/content)
      * [macOS AArch64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB5D93C6E0631818000A2314/content)
      * [Windows x64](https://gds.oracle.com/api/20220101/artifacts/05013E46CB6293C6E0631818000A2314/content)

2. Unzip the archive:

    > Note: If you are using macOS Catalina and later you may need to remove the quarantine attribute:
    ```shell
    sudo xattr -r -d com.apple.quarantine <archive>.tar.gz
    ```

    Extact:
    ```shell
    tar -xzf <archive>.tar.gz
    ```
   
3. A standalone comes with a JVM in addition to its native launcher. Check the version to see GraalVM WebAssembly runtime is active:
    ```bash
    ./path/to/bin/wasm --version
    ```

## Running WebAssembly Programs

You can run a program written in the language that compiles to WebAssembly on GraalVM.
For example, put the following C program in a file named _floyd.c_:
```c
#include <stdio.h>

int main() {
  int number = 1;
  int rows = 10;
  for (int i = 1; i <= rows; i++) {
    for (int j = 1; j <= i; j++) {
      printf("%d ", number);
      ++number;
    }
    printf(".\n");
  }
  return 0;
}
```

Compile it using the most recent [Emscripten compiler frontend](https://emscripten.org/docs/tools_reference/emcc.html) version. It should produce a standalone _floyd.wasm_ file in the current working directory:
```shell
emcc -o floyd.wasm floyd.c
```

Then you can run the compiled WebAssembly binary on GraalVM as follows:
```shell
wasm --Builtins=wasi_snapshot_preview1 floyd.wasm
```

In this example, the flag `--Builtins` specifies builtin modules that the [Emscripten toolchain](https://emscripten.org/index.html) requires.

## Embedding WebAssembly Programs

The compiled WebAssembly binary code can be accessed programmatically with [GraalVM Polyglot API](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/package-summary.html), which allows embedding GraalVM WebAssembly into user programs. Here is a simple example of how to call WebAssembly code from a Java application:

```java
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.ByteSequence;
//Load the WASM contents into a byte array
byte[] binary = readBytes("example.wasm");
Context.Builder contextBuilder = Context.newBuilder("wasm");
Source.Builder sourceBuilder = Source.newBuilder("wasm", ByteSequence.create(binary), "example");
Source source = sourceBuilder.build();
Context context = contextBuilder.build();

context.eval(source);

Value mainFunction = context.getBindings("wasm").getMember("main").getMember("_start");
mainFunction.execute();
```

For more polyglot examples, visit the [Embedding Languages](../embedding/embed-languages.md) guide.
