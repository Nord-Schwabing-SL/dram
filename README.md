[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
# What is Katod

Katod is a converter of TypeScript definition files to Kotlin declarations.
It's a fork of [Dukat](https://github.com/Kotlin/dukat) project, an officially supported by Jetbrains tool for the same purposes.

As of now there now there's absolutely no difference between `dukat` and `katod`, feel free
to use whatever you like more.

Katod requires JRE 1.6+ to run. It generates Kotlin files that are compatible with Kotlin 1.1+ (generated declarations
are tested against latest stable compiler version)

# How to install

The simplest way to use is install the latest version form [npm](https://www.npmjs.com/package/katod):
```shell
npm install -g katod
```

# Usage

```shell
katod [<options>] <d.ts files>
```

where possible options include:
```shell
    -p  <qualifiedPackageName>      package name for the generated file (by default filename.d.ts renamed to filename.d.kt)
    -m  String                      use this value as @file:JsModule annotation value whenever such annotation occurs
    -d  <path>                      destination directory for files with converted declarations (by default declarations are generated in current directory)
    -v, -version                    print version
```

# How to setup and build

1. clone this project
  ```shell
  # on Windows-based platforms set following: `git config core.autocrlf true`   
  git clone <this project url>
  ```
  
2. build
 
 ```shell
 ./gradlew build
 ```
 
3. (optional) Run unit tests

```shell
./gradlew test -Pdukat.test.failure.always
```  

[see CHANGELOG](https://github.com/Kotlin/katod/blob/master/CHANGELOG.md)

# Useful links

- [TypeScript type definitions](https://github.com/DefinitelyTyped/DefinitelyTyped)
 
