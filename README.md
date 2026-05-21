Accurate as of 2024-05-01

This project has essentially two components: the computational backend that calculates the equations, and the graphical front end that displays the results.

The computational side of this project is written in C++, which is a language perfectly suited for high performance mathematical computation. The graphical interface is written in Java, which has the very nice JavaFX GUI framework.

I toyed with the idea of rewriting the GUI part of the project in Qt5 to have a unified C++ codebase, but that would take a long time. If we ever run into computational limits with Java, we might consider this.

# Operating Systems

The code has been developed with a Unix operating system and environment in mind. I personally used a Mac, but any Linux distro should work too (note that George uses Macs though, so you still need to be familiar with them). It might be possible to get this code to work on Windows, but I don't even want to think about the mess that would entail. If you ever want to run this on a Windows machine, it's probably easier just to install Linux side-by-side (perhaps on a separate partition or in a VM, like VirtualBox) and run it on that (check out [Fedora](https://getfedora.org) for a nice modern Linux distro). The rest of this tutorial shows how to setup the work environment from scratch on a Mac, Ubuntu, or Fedora.

# Basic Setup

This readme assumes a basic familiarity with the command line. If you are unfamiliar, please read [this](https://www.learnenough.com/command-line-tutorial) or another tutorial. (Pleeeeeez read all of it. I know you just want to dive in and start working on things, but your life will be sooooo much easier and things will make sooooo much more sense if you finish the tutorial. Take things slowly. You will thank yourself.) On a Mac, I recommend using [iTerm2](https://www.iterm2.com) for your terminal emulator instead of the default Terminal.

# Package Manager

A package manager is a command line tool used for installing and managing programming packages. Linux systems come with a package manager builtin (`apt` on Ubuntu and `dnf` on Fedora), so there is nothing else to do here for those. Sadly, Macs do not come with a package manager, so we will need to install one. The best one out there is [Homebrew](http://brew.sh), and there are several steps we need to do to install it.

First, install the Command Line Developer Tools. Simply run

```
$ xcode-select --install
```

and then click `Install` on the pop-up window. This installs the Clang compiler and various other tools which are needed for the second step.

Next, go to the Homebrew webpage, and follow the installation instructions there (if you read the command line tutorial above, this will all be a cinch). Next, familiarize yourself with basic Homebrew commands (`man brew` is a good place to start, but again, if you read the command line tutorial you will already know this). Don't forget to run `brew upgrade` from time-to-time to update your software.

# C++

This project uses C++ and so requires a C++ compiler. There are two major compilers one can use: Clang and GCC. Clang is the default on Mac, and GCC the default on Linux.

On a Mac, the Command Line Developer Tools come bundled with the Clang compiler, so if you followed the above step, there isn't anything to do here.

For Linux,

```
$ sudo apt install gcc
```

# Java

The GUI component of this project is written in Java. This project uses Java 8. There are two versions of Java, the JRE and the JDK. The former is for consumers who only want to run Java code, and the latter is for developers (us) who want to compile it. Getting these two mixed up has caused a lot of trouble for me (eg. some questions on Stack Overflow deal with one and not the other), so be aware of the difference. There are also different "flavors" of Java that come packaged in different ways. 

Regardless of your operating system, I recommend installing the Oracle 8u66 jdk from the [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) website (the link may change in the future, so poke around if the webpage doesn't work), accept the license agreement, and install the version for your system (the demos and samples are optional). Follow all the instructions, and there you go (you may also want to go to the Java Control Pane and disable Java in the browser for security reasons). Oracle jdks come prepackaged with the correct javafx version, so this will save you a lot of trouble compared to installing the jdk and javafx seperately.

# Version Control

This project uses Git for its version control. It is also hosted on github, and you should message austin7@ualberta.ca for access
```
$ brew install git
$ sudo apt install git
$ sudo dnf install git
```
# C++ Libraries

The application depends on the following libraries.

- [GMP](https://gmplib.org): Arbitrary precision arithmetic library for integers and rational numbers.
- [MPFR](http://www.mpfr.org): Multiprecision library for floating point numbers.
- [MPFI](https://perso.ens-lyon.fr/nathalie.revol/software.html): Multiprecision library for interval arithmetic.
- [Boost](http://www.boost.org): General utilities for C++. If you are looking for any C++ libraries or utilities, look here first. One of the most important libraries for us is the Multiprecision collection, which provides C++ wrappers around the previous three numerical libraries (which are written in C).
- [Eigen](http://eigen.tuxfamily.org/index.php?title=Main_Page): Template-based linear algebra.
- [Thread Building Blocks](https://www.threadingbuildingblocks.org/): Parallelism library.
- [Jemalloc](http://jemalloc.net/): High performance memory allocator. Our program uses a decent amount of memory for the multiprecision arithmetic calculations, and using this allocator improves the performance of the application by about 15% over the system allocator (at least, it does on Mac).

Let's install these using the package manager. (Run the command matching your package manager)

```
$ brew install gmp mpfr mpfi boost eigen tbb jemalloc
$ sudo apt install libgmp-dev libmpfr-dev libmpfi-dev libboost-all-dev libeigen3-dev libtbb-dev libjemalloc-dev
$ sudo dnf install gmp-devel mpfr-devel mpfi-devel boost-devel eigen3-devel tbb-devel jemalloc-devel
```

# Setting Up the Database

This program uses SQLite to store geometric information about the code sequences. Macs come with a version of SQLite bundled with the Command Line Developer Tools, so there is nothing to install for them. For Linux,

```
$ sudo apt install libsqlite3-dev
$ sudo dnf install sqlite-devel
```

# Build Systems

## Gradle


The project comes with gradlew and gradlew.bat files, so you do not need to install gradle yourself. Instead, use the gradlew file to build and run the project. Currently the wrapper uses Gradle 4.9, but you are welcome to update it to a newer version if you can get it working. 

As an example, the following command will run the project using the gradle wrapper.
```
$ ./gradlew run

```


## Meson

To setup meson

```
$ mkdir meson
$ cd meson
$ meson.py ..
```

# having a static analysis build would be nice
# as well as profile guided optimization the code

# Eclipse

In practice, you can use whatever editor or IDE you like to develop the code. I for example use neovim. One option is to use Eclipse, which is what George uses. Here are the instructions for setting up Eclipse for our project. Note that we do not use the default Eclipse installer, since that installs a bunch of junk we don't need and doesn't install a bunch of junk we do need. So, we do a custom setup.

Google Eclipse Project Downloads, and go to Latest Downloads. Click on the latest release, it should be a number like 4.8 or something. Then, scroll down the page to Platform Runtime Binary, and download the one for your platform.

Next, launch Eclipse and go to: Help > Install New Software
Under Work With, select All Available Sites
Install the following:

- Collaboration > Git integration for eclipse
- General Purpose Tools > Buildship (for dumb ol' gradle)
- Eclipse Java Development Tools > Eclipse Java Development Tools
- Programming Languages > C/C++ Development Tools

# Design Choices

- Boost: a collection of some of the best libraries for C++. Whenever you are looking for a library for C++, look here first. Of particular interest are the Multiprecision and Interval Arithmetic collections. If you are looking for C++ wrappers of GMP, MPFR, and MPFI, here is a good place.
- Ginac: this is a symbolic mathematics library. These sorts of libraries are difficult to make, and Ginac seems to be the best (and perhaps only) one in C++ land. I ended up writing my own symbolic math code instead, which drastically increased the performance and simplified the code, so using Ginac is unnecessary.
- CLN: an arbitrary precision arithmetic library built into Ginac.
- CGAL: the most advanced and de facto computational geometry library for pretty well anything, not just C++.
- Arb:
- Flint:

The graphical front end on the other hand could be written in a variety of languages. For example,

- Python. Python has no native GUI library (one written in Python), but has bindings to essentially every GUI library out there. However,
- Java. Java has a long history of graphical libraries, going back to AWT and Swing. The modern one is JavaFX, which is very easy to use and also quite powerful.
- C++. C++ has the most powerful and advanced GUI libraries out there, most notably GTK and Qt. However, they are difficult to use and get right (as pretty well all of C++ is).

Java, on the other hand, is not meant for mathematical and numerical computation. It is a fast language, but its JIT compiler nature will never have the performance of AOT languages, and the intermediate compilation into bytecode makes optimizatin difficult. As such, it has has comparatively fewer mathematical libraries. Here are the better ones.

- Apache Commons Math: your one stop shop for numerical computation. Any time you are trying to do something numerically, look here first.
- Java Algebra System: library for polynomial algebra. Count your lucky stars if a problem ever deals with polynomials (espectially ones with rational coefficients), because they are extremely well behaved and understood.

Other useful libraries

- Guava: not a mathematical library, but the immutable collections are a welcome relief from the mutability of the built in Java collections

You will notice there is no general symbolic math library in this list. That's because there are no decent ones for Java. I briefly tried one called Symja, but it was terrible: terribly designed with terrible documentation and terrible performance. The lack of such a library is the single largest disadvantage of using Java. I made my own that is specially suited for our problems.

I also tried a library called Apfloat, which has arbitrary precision integers and floating point numbers. However, in some Java Microbenchmark Harness (JMH) benchmarks it was consistently beaten by the built in BigInteger and BigDecimal. However, this may be becasue BigInteger and BigDecimal to have an upper bound to how large they can be (though it is very large), while Apint is truly without an upper bound.

Another omission is interval arithmetic. Interval arithmetic allows you to deal rigourously with floating point calculations.

However, Java does have several advantages:

- It is much easier to use and safer than C++. C++ is probably the most complicated programming language on Earth, and has very few safety features. Java will catch you when you make a mistake; by default, C++ will not. This is the terrible scourge of undefined behaviour, and you must be vigilant to avoid it at all costs.
- Distributing Java applications is much easier. In C++, you have to worry about compilers, libraries, linking, Windows (ugh), etc. Distributing Java code on the other hand is effortless.

# In the Future

The build system should be revamped. Right now we are using Gradle for Java and Meson for C++. I'd like to have one build system that handles both. Gradle support for C++ isn't the greatest now, but if it improves (say, in Gradle 5), feel free to switch back to that. Also, maybe check out Bazel once it hits 1.0. In the meantime, I believe there are significant changes coming to Gradle in version 5 (a new components thing, and Kotlin-script too), so feel free to update that if there are no better build systems by that time.

# Books

Oracle has some online tutorials about Java [here](https://docs.oracle.com/javase/tutorial/). They're very comprehensive, but could be better explained in some places. There is also a list of books [here](https://www.quora.com/What-are-the-best-books-to-learn-Java). Don't forget you can access a lot of these books online through the university library website.

There aren't a whole lot of JavaFX resources out there. The best book I found is *Learn JavaFX 8: Building User Experience and Interfaces with Java 8* by Kishori Sharan. Other interesting JavaFX libraries are ReactFX and RxJava. Make sure you look for JavaFX 8 tutorials (not JavaFX 2 or 2.2, since much has changed since then).

For C++, Stackoverflow has a comprehensive list [here](http://stackoverflow.com/questions/388242/the-definitive-c-book-guide-and-list).

SQLite has good documentation on its [website](https://www.sqlite.org/docs.html).

# Libraries

Look at awesome-java and awesome-cpp. Also most things in Boost.

Very important article
https://en.wikipedia.org/wiki/Polynomial_greatest_common_divisor#Euclidean_division

Mostly Surfaces - Schwartz
http://www.maths.dur.ac.uk/users/anna.felikson/Projects/billiards/billiards-res.html
http://reu.dimacs.rutgers.edu/~eskrj/research.html
http://sites.millersville.edu/rumble/seminar.html
http://slideplayer.com/slide/4598283/
https://www.quantamagazine.org/new-shapes-solve-infinite-pool-table-problem-20170808/
