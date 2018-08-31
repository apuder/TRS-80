# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(. SDL)
add_library( # Sets the name of the library.
             xtrs

             # Sets the library as a shared library.
             SHARED
             native.c
             blit.c
             opensl.c
             trs_chars.c
             trs_printer.c
             trs_disk.c
             trs_mkdisk.c
             trs_rom1.c
             trs_imp_exp.c
             error.c
             trs_hard.c
             trs_rom3.c
             load_cmd.c
             trs_rom4p.c
             load_hex.c
             trs_interrupt.c
             trs_uart.c
             trs_state_save.c
             main.c
             trs_io.c
             trs_sdl_interface.c
             trs_sdl_keyboard.c
             z80.c
             trs_cassette.c
             trs_memory.c
             SDL/SDL.c)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log)
              find_library(OpenSLES-lib OpenSLES)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       xtrs

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       ${OpenSLES-lib})