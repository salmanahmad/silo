
TODO List

*- `return 5` is parsed differently from `return(5)` and could be really confusing to many people. I should add error reporting for this case. The same goes for things like `import()` --- THIS IS REALLY IMPORTANT. I HAVE MADE THIS MISTAKE MULTIPLE TIMES...

- flexible pattern matching is really important in the language. perhaps even go beyond what erlang / scala provide and provide a full blow parser combinator DSL.

- multiline operators would be really awesome especially when lining things up for the pipe operator.

- Reorganize the codebase to pull Compiler and Parser out of the "compiler" package.
- Reorganize the test cases to pull the ParserTest out of the "parser" directory

- How do I allow for custom operator overloading? Would it be possible to allow operators to be parsed as symbols instead of operators? That would be useful for DSLs that may want to either overload the operators or do things like alter the order of operations. Even without changing order of operations, would it be possible to override the operators to do something else instead of the built in? That should be easy to do with a macro, right?

- Can I handle whitespace around ()?
- Make symbol also encode all the operators so that I can use operators as literal symbol values... this makes it possible to do: +(5,5)...i think...
- Support newlines with operator expressions so that I can do things like "foo\n.bar\n.baz\n.zap" and also with pipe operators "foo\n| bar\n| baz"

- Should the form `(foo).(bar).(baz)` be left associative or right associative?

- Should macro have access to "CompilationContext"? In that case, I could implement things like `import` using macro instead of a special form.

- If I have a class that does not have any output types, it by default will to forcing a null return. Instead, it should probably return a var (or an object) and wrap / box as necessary.

- The standard library from the runtime will emit anonymous functions with names like: `__function__1.class`. This could cause collisions with other runtime objects created in user-programs. How can I differentiate them? Embedding a timestamp could prevent collisions, but it could also make it difficult to deterministically re-compile portions of code, especially across different machines. WAIT! This is not actually a problem. You should use packages to scope out the different functions and identifiers. Perhaps, instead, I could initialize the Runtime with a package name to avoid this issue all together?

- Read up on the `visitLdcInsn` method in ASM. In particular this bit about method handles: `cst - the constant to be loaded on the stack. This parameter must be a non null Integer, a Float, a Long, a Double, a String, a Type of OBJECT or ARRAY sort for .class constants, for classes whose version is 49.0, a Type of METHOD sort or a Handle for MethodType and MethodHandle constants, for classes whose version is 51.0.`

- `vararg : silo.lang.Symbol = null` this does not work. I need to figure out a way to allow null assignments to objects that are not strictly "Object.class"

- How do I pass in an array to a varargs method. Like the splat operator in ruby. http://stackoverflow.com/questions/5227290/pass-array-into-vararg-in-ruby

- How do I create a subclass of a certain interface or class? In particular, how would I create a class that implements `runnable` or `actionlistener` so that it could be used with swing or threads? http://tech.puredanger.com/2011/08/12/subclassing-in-clojure/

- Do I need (public / private / protected) and inheritance. Is the actor model good enough?

- Consider augmenting the compilation pipeline. Right now it is "parse -> scaffold (macro-expand + symbol-generation) -> code-generation". I may want to add a separate "macro-extraction" phase before "scaffold". Macro extraction will attempt to discover "forward declaration" of macros that are used in the code block but appear after the point where they are used. The way that this works is that macro-extraction proceeds similar to scaffold in which is macro-expands and walks the tree BUT it does so conservatively --- it only looks at a node's children if it can tell for SURE that the node represent a function call. So, if it sees, "foo(...)" and it cannot statically determine that "foo" is a function call, it will NOT attempts to process the children. This is called only evaluating the "reachable" nodes. This process allows more macros to be "discovered" by the compiler than the current approach which only accepts macros that are lexically defined before use. The new phase looks like this: "parse -> macro-extract -> scaffold (macro-expand + symbol-generation) -> code-generation".

- As an addition or an alternative to the the "macro-extraction" build phase, consider adding a special form for forward declaring macros. Something like "proto" (short for prototype) or "declare" (as opposed to the current "define"). This can be used to tell the compile that this is a macro and it should not process nodes that call the macro during scaffolding. This change will likely need a change to SymbolEntry in ComplicationContext to include "prototype" in addition to "compiled".