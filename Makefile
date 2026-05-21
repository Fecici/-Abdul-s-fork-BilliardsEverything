# `make` builds the target
# `make file.o` creates the `file` object file
# `make clean` will rm all object files and the target

TARGET = ../cover-all

UNAME := $(shell uname -s)

ifeq ($(UNAME),Linux)
	CXX = g++
	WARNINGS = -Wall -Wextra -Wno-c++11-compat -Wno-c++14-compat
endif
ifeq ($(UNAME),Darwin)
	CXX = clang++
	WARNINGS = -Weverything -Wno-padded -Wno-comma -Wno-exit-time-destructors -Wno-global-constructors -Wno-c++98-compat -Wno-c++98-compat-pedantic
endif

STD = -std=c++14
OPTS = -O3 -march=native -flto -ftrapv -DNDEBUG
CXXFLAGS = $(STD) $(WARNINGS) $(OPTS)

# TODO benchmark jemalloc against the standard linux allocator
# It is certainly faster on Mac, but I want to check linux
LDLIBS = -lmpfr -lgmp -ltbb -ljemalloc

SOURCES = src/backend/cpp/basic.cpp src/backend/cpp/bounding_inequalities.cpp src/backend/cpp/bounding_region.cpp src/backend/cpp/code_sequence.cpp src/backend/cpp/common.cpp src/backend/cpp/conversion.cpp src/backend/cpp/evaluator.cpp src/backend/cpp/general.cpp src/backend/cpp/cover/load.cpp src/backend/cpp/cover/save.cpp src/backend/cpp/division.cpp src/backend/cpp/parse.cpp src/backend/cpp/math/symbols.cpp src/backend/cpp/shooting_vectors.cpp src/backend/cpp/trig_identities.cpp src/backend/cpp/unfolding.cpp src/cover/cpp/cover.cpp
#SOURCES += $(shell find src/cover/cpp/ -type f -name '*.cpp')

HEADERS = src/backend/headers/basic.hpp src/backend/headers/bounding_inequalities.hpp src/backend/headers/bounding_region.hpp src/backend/headers/common.hpp src/backend/headers/conversion.hpp src/backend/headers/cover/load.hpp src/backend/headers/cover/save.hpp src/backend/headers/division.hpp src/backend/headers/parse.hpp src/backend/headers/math/symbols.hpp src/backend/headers/shooting_vectors.hpp src/backend/headers/trig_identities.hpp src/backend/headers/unfolding.hpp
#HEADERS += $(shell find src/backend/headers/ -type f -name '*.hpp')

#OBJECTS = $(SOURCES:.cpp=.o)

$(TARGET): $(SOURCES) $(HEADERS)
	$(CXX) $(CXXFLAGS) -DCOMPUTE_CANADA -Isrc/backend/headers/ $(SOURCES) -o $@ $(LDLIBS)

# link the object files to create the target
#$(TARGET): $(OBJECTS)
	#$(CXX) $(CXXFLAGS) $^ -o make/$@ $(LDLIBS)

## compile rule for the object files
#%.o: %.cpp $(HEADERS)
	#$(CXX) $(CXXFLAGS) -fsyntax-only -Isrc/backend/headers/ -c $< -o $@

.PHONY: clean

clean:
	-rm $(OBJECTS) $(TARGET)
