# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.0)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


include_directories(. SDL retrostore/include trs-io/include tcpip/include proto)
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
             trsio-wrapper.cpp
             SDL/SDL.c
             trs-io/core.cpp
             trs-io/trs-io.cpp
             retrostore/ApiProtos.pb.c
             retrostore/pb_common.cpp
             retrostore/pb_decode.cpp
             retrostore/retrostore.cpp
             retrostore/esp_mock.cpp
             retrostore/loader_basic.c
             retrostore/loader_cmd.c
             retrostore/backend.cpp
             retrostore/cJSON.cpp
             retrostore/rsclient.c
             retrostore/boot.c
             retrostore/utils.cpp
             tcpip/tcpip.cpp
             google/protobuf/any_lite.cc
             google/protobuf/arena.cc
             google/protobuf/arenastring.cc
             google/protobuf/arenaz_sampler.cc
             google/protobuf/extension_set.cc
             google/protobuf/generated_enum_util.cc
             google/protobuf/generated_message_tctable_lite.cc
             google/protobuf/generated_message_util.cc
             google/protobuf/implicit_weak_message.cc
             google/protobuf/inlined_string_field.cc
             google/protobuf/io/coded_stream.cc
             google/protobuf/io/io_win32.cc
             google/protobuf/io/strtod.cc
             google/protobuf/io/zero_copy_stream.cc
             google/protobuf/io/zero_copy_stream_impl.cc
             google/protobuf/io/zero_copy_stream_impl_lite.cc
             google/protobuf/map.cc
             google/protobuf/message_lite.cc
             google/protobuf/parse_context.cc
             google/protobuf/repeated_field.cc
             google/protobuf/repeated_ptr_field.cc
             google/protobuf/stubs/bytestream.cc
             google/protobuf/stubs/common.cc
             google/protobuf/stubs/int128.cc
             google/protobuf/stubs/status.cc
             google/protobuf/stubs/statusor.cc
             google/protobuf/stubs/stringpiece.cc
             google/protobuf/stubs/stringprintf.cc
             google/protobuf/stubs/structurally_valid.cc
             google/protobuf/stubs/strutil.cc
             google/protobuf/stubs/time.cc
             google/protobuf/wire_format_lite.cc
        )

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