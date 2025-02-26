# AutomataLib

[![CI](https://github.com/LearnLib/automatalib/actions/workflows/ci.yml/badge.svg)](https://github.com/LearnLib/automatalib/actions/workflows/ci.yml)
[![Coverage](https://coveralls.io/repos/github/LearnLib/automatalib/badge.svg?branch=develop)](https://coveralls.io/github/LearnLib/automatalib?branch=develop)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.automatalib/automata-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.automatalib/automata-parent)

AutomataLib is a free, open source ([Apache License, v2.0][1]) Java library for modeling automata, graphs, and transition systems.


## About

AutomataLib is developed at [TU Dortmund University, Germany][2].
Its original purpose is to serve as the automaton framework for the [LearnLib][3] automata learning library.
However, it is completely independent of LearnLib and can be used for other projects as well.

AutomataLib supports modeling a variety of graph-based structures.
Currently, it covers generic transition systems, Deterministic Finite Automata (DFAs) and Mealy machines as well as more advanced structures such as Modal Transition Systems (MTSs), Subsequential Transducers (SSTs), Visibly Pushdown Automata (VPAs) and Procedural Systems (SPAs, SBAs, SPMMs).

Models of AutomataLib can be (de-)serialized (from) to one of the various supported serialization formats and may be visualized using either the GraphViz or JUNG library.
Furthermore, a plethora of graph-/automata-based algorithms is implemented, covering the following topics:

* graph theory (traversal, shortest paths, strongly-connected components)
* automata theory (equivalence, minimization)
* model-based testing (adaptive distinguishing sequences, W(p)Method, characterizing sets, state/transition covers)
* model verification (LTL checking (via [LTSMin][ltsmin]), CTL & µ-calculus checking (via [M3C][m3c] & [ADDlib][addlib]))

While we strive to deliver code at a high quality, please note that there exist parts of the library that still need thorough testing.
Contributions -- whether it is in the form of new features, better documentation or tests -- are welcome.

## Build Instructions

For simply using AutomataLib you may use the Maven artifacts which are available in the [Maven Central repository][maven-central].
It is also possible to download a bundled [distribution artifact][maven-central-distr] if you want to use AutomataLib without Maven support.
Note that AutomataLib requires Java 11 (or newer) to build but still supports Java 8 at runtime.

#### Building development versions

If you intend to use development versions of AutomataLib, you can either use the deployed SNAPSHOT artifacts from the continuous integration server (see [Using Development Versions](https://github.com/LearnLib/automatalib/wiki/Using-Development-Versions)) or build them yourself.
Simply clone the development branch of the repository

```
git clone -b develop --single-branch https://github.com/LearnLib/automatalib.git
```

and run a single `mvn clean install`.
This will build all the required maven artifacts and will install them in your local Maven repository so that you can reference them in other projects.

If you plan to use a development version of AutomataLib in an environment where no Maven support is available, simply run `mvn clean package -Pbundles`.
The respective JARs are then available under `distribution/target/bundles`.

#### Developing AutomataLib

For developing the code base of AutomataLib it is suggested to use one of the major Java IDEs which come with out-of-the-box Maven support.

* For [IntelliJ IDEA][intellij]:
  1. Select `File` -> `New` -> `Project from existing sources` and select the folder containing the development checkout.
  1. Choose "Import Project from external model", select "Maven" and click `Create`.

* For [Eclipse][eclipse]:
  1. **Note**: AutomataLib uses annotation processing on several occasions throughout the build process.
  This is usually handled correctly by Maven.
  However, for Eclipse, you may need to manually enable annotation processing under `Preferences` -> `Maven` -> `Annotation Processing`.
  1. Select `File` -> `Import...` and select "Existing Maven Projects".
  1. Select the folder containing the development checkout as the root directory and click `Finish`.


## Documentation

* **Maven Project Site:** [latest release](https://learnlib.github.io/automatalib/maven-site/latest/) | [older versions](https://learnlib.github.io/automatalib/maven-site/)
* **API Documentation:** [latest release](https://learnlib.github.io/automatalib/maven-site/latest/apidocs/) | [older versions](https://learnlib.github.io/automatalib/maven-site/)


## Questions?

If you have any questions regarding the usage of AutomataLib or if you want to discuss new and exciting ideas for future contributions, feel free to use the [Discussions](https://github.com/LearnLib/automatalib/discussions) page to get in touch with the AutomataLib community.


## Maintainers

* [Markus Frohme][5] (2017 - )
* [Malte Isberner][4] (2013 - 2015)

[1]: http://www.apache.org/licenses/LICENSE-2.0
[2]: https://cs.tu-dortmund.de
[3]: https://learnlib.de
[4]: https://github.com/misberner
[5]: https://github.com/mtf90

[maven-central]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.automatalib%22
[maven-central-distr]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.automatalib.distribution%22
[intellij]: https://www.jetbrains.com/idea/
[eclipse]: https://www.eclipse.org/
[ltsmin]: https://ltsmin.utwente.nl/
[m3c]: https://doi.org/10.1007/978-3-030-00244-2_15
[addlib]: https://add-lib.scce.info/
