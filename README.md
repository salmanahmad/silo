The Silo Programming Language.

---

Developed by Salman Ahmad <salman@salmanahmad.com>.

Licensed under the Apache License, Version 2.0.
See COPYRIGHT.txt and LICENSE.txt for more details.

---

To build: `mvn package -DskipTests`.

To run: `target/lib/silo-<version>/bin/silo`.

All build artifacts will appear in `target/lib/silo-<version>/`.

When copying Silo to another location for installation (for example, `~/bin` or `/usr/local/bin`) you must copy the entire `target/lib/silo-<version>` directory. A nice strategy is moving the entire directory to the location of your choice and then creating a symlink to make the `silo` command line tool accessible. For example, copy the build directory so that it reside at `~/bin/silo-<version>` and then symlink `~/bin/silo -> ~/bin/silo-<version>/bin/silo`.

As a convenience the `target/lib/silo-<version>` directory is bundled together as zip and tgz archives so Silo can be easily copied onto another machine.
