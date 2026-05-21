# I'll set all the compiler options myself, since I want to customize them
project('backend', 'cpp', default_options : ['buildtype=plain'])

# By default, `ninja` runs `ninja all`, which builds all the targets
# `ninja test` runs `ninja all`, then runs the test executable

compiler_id = meson.get_compiler('cpp').get_id()

if compiler_id == 'clang'
    add_global_arguments(['-Weverything', '-Werror',
                          '-Wno-padded', '-Wno-comma',
                          '-Wno-c++98-compat', '-Wno-c++98-compat-pedantic',
                          '-std=c++14'], language : 'cpp')
elif compiler_id == 'gcc'
    add_global_arguments(['-Wall', '-Wextra', '-Wpedantic', '-Werror',
                          '-Wno-c++11-compat', '-Wno-c++14-compat',
                          '-std=c++14'], language : 'cpp')
else
    error('Unsupported compiler')
endif

#nonius = include_directories('libs/nonius', is_system : true)

headers = include_directories('src/backend/headers')

backend_src = ['src/backend/cpp/']
backend = shared_library('backend', 'src/backend/cpp/wrapper.cpp',
               include_directories : headers,
               # These options are the same between clang and gcc
               cpp_args : ['-DNDEBUG', '-O3', '-march=native', '-flto', '-ftrapv'],
               link_args : ['-lgmp', '-lmpfr', '-lmpfi', '-lsqlite3', '-ltbb'])

executable('cover', 'src/cover/cpp/cover.cpp',
           include_directories : headers,
           link_with : backend,
           cpp_args : ['-DNDEBUG', '-O3', '-march=native', '-flto', '-ftrapv'],
           # TODO once this works, link jemalloc
           link_args : ['-lgmp', '-lmpfr', '-lmpfi', '-lsqlite3'])

test_backend = executable('test_backend', 'src/test/cpp/main.cpp',
                          include_directories : headers,
                          # TODO on a decent compiler add the sanitize options
                          # Compiling with full optimizations can flesh out bugs
                          cpp_args : ['-g', '-ftrapv',
                                      '-O3', '-march=native', '-flto',
                                      '-Wno-disabled-macro-expansion', '-Wno-global-constructors'],
                          link_args : ['-lgmp', '-lmpfr', '-lmpfi', '-lboost_unit_test_framework'])
test('test backend', test_backend)

#executable('profile', 'profile/backend/profile.cpp',
                      #include_directories : headers,
                      #cpp_args : ['-DNDEBUG', '-O3', '-march=native', '-flto', '-ftrapv', '-g',
                                   #We need these options to get accurate profiling results
                                  #'-fno-omit-frame-pointer', '-fno-inline-functions',
                                  #'-fno-optimize-sibling-calls'],
                      #link_args : ['-lgmp', '-lmpfr', '-lmpfi'])

executable('debug', 'debug/backend/debug.cpp',
                   include_directories : headers,
                   cpp_args : ['-g', '-fsanitize=integer'],
                   link_args : ['-lgmp', '-lmpfr', '-lmpfi', '-lsqlite3', '-ltbb', '-fsanitize=integer'])

# An interesting idea is to run a benchmark for each commit, and see how the performance
# changes over time.
#bench = executable('bench', 'bench/backend/bench.cpp',
                   #include_directories : [headers, nonius],
                   #cpp_args : ['-DNDEBUG', '-O3', '-march=native', '-flto', '-ftrapv'],
                   #link_args : ['-lgmp', '-lmpfr', '-lmpfi'])
#benchmark('benchmark backend', bench)
