/// <reference path="../node_modules/typescript/lib/typescriptServices.d.ts"/>
/// <reference path="../node_modules/typescript/lib/tsserverlibrary.d.ts"/>
/// <reference path="../node_modules/typescript/lib/typescript.d.ts"/>


if (typeof ts == "undefined") {
    (global as any).ts = require("typescript/lib/tsserverlibrary");
}

declare function print(...arg: any[]): void;

declare function println(arg: String): void;

interface FileResolver {
    resolve(fileName: string): string;
}

declare class FileResolverV8 implements FileResolver {
    resolve(fileName: string): string;
}

if (typeof console == "undefined") {
    (global as any).console = {
        log: (...args: any[]) => {
            if (typeof println == "function") {
                println(args.map(it => String(it)).join(", "));
            }
        }
    }
}

function main(nativeAstFactory: AstFactory, fileResolver: FileResolver, fileName: string) {
    if (fileResolver == null) {
        fileResolver = new FileResolverV8();
    }

    let host = new DukatLanguageServiceHost(fileResolver);
    host.register(fileName);

    let languageService = ts.createLanguageService(host, ts.createDocumentRegistry());

    const program = languageService.getProgram();

    if (program == null) {
        throw new Error(`failed to create languageService ${fileName}`)
    }

    const sourceFile = program.getSourceFile(fileName);

    if (sourceFile == null) {
        throw new Error(`failed to resolve ${fileName}`)
    } else {
        let astConverter: AstConverter = new AstConverter(
            program.getTypeChecker(),
            (fileName) => program.getSourceFile(fileName),
            nativeAstFactory == null ? new AstFactoryV8() : nativeAstFactory
        );

        return astConverter.createSourceSet(fileName);
    }
}


if (typeof module != "undefined") {
    module.exports = main;
}
