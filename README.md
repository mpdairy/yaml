# yaml

A little yaml decoder I wrote for an exercise that just extracts the
variables of two yaml files and then combines them, almost helpfully.

It takes two yaml files, an old and a new. It looks through the new
file and replaces the values of any variables that are matched in the
old file with the value of that variable from the old file. Also, if
the variable is commented out in the old file, it will be commented
out in the new.

Any vars from the old that are not in the new are commented out and
marked as `# DEPRECATED` and placed at the bottom of the list (or sublist).

## Usage

You can just run it from the command line on two yaml files. Put the
old file first and the newer one second.

```clj
lein run resources/cassandra-1.2.yaml resources/cassandra-2.0.yaml
```

## Note

This really should have been made using a yaml parsing library. You
should really never use this project.

## License

Copyright © 2015 Matt Parker

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
