
TODO List

- `return 5` is parsed differently from `return(5)` and could be really confusing to many people. I should add error reporting for this case. The same goes for things like `import()`

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