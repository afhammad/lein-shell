# lein-shell

A Leiningen plugin for calling shell commands.

## Usage

Use this for user-level plugins:

Put `[lein-shell "0.1.0"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
lein-shell 0.1.0`.

Use this for project-level plugins:

Put `[lein-shell "0.1.0"]` into the `:plugins` vector of your project.clj.

It is very straightforward to use lein-shell: lein-shell will call the shell
command with eventual parameters you include. For instance, if you want your
favourite cow to say hello to you from Leiningen, the following will be printed
within your shell:

    $ lein shell cowsay 'Hello from Leiningen!'
	 _______________________
    < Hello from Leiningen! >
     -----------------------
            \   ^__^
             \  (oo)\_______
                (__)\       )\/\
                    ||----w |
                    ||     ||

Now, this may look rather useless as you can just omit `lein shell` and get the
exact same result in less time. However, it may be of value if you're using
make or ANTLR to generate files for you, needed by either javac or the
Clojure compilation phase:

```clj
:prep-tasks ["shell make" "javac" "compile"]
```

## License

Copyright © 2013 Jean Niklas L'orange

Distributed under the Eclipse Public License, the same as Clojure.
