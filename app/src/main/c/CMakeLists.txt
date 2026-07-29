# Builds the TRS-80 emulator core.
#
# Two configurations come out of the same source list:
#
#   Android - a shared library, including the JNI adapter (native.c) and the
#             OpenSL ES audio backend (audio_opensl.c). Driven by Gradle's
#             externalNativeBuild.
#   iOS     - a static library of the core alone, driven from Kotlin/Native
#             through cinterop over trs80_core.h, with the audio sink supplied
#             by the host.
#
# Anything platform-specific belongs in the per-platform blocks at the bottom,
# never in TRS80_CORE_SOURCES.

cmake_minimum_required(VERSION 3.22.1)

project(trs80 C CXX)

include_directories(. SDL retrostore/include trs-io/include tcpip/include proto)

# Selects the embedded-host build: the emulator is a library driven by a host
# UI rather than a standalone SDL process. It renders into a shared character
# buffer instead of an SDL surface, and exposes trs_*_init() entry points so
# its global state can be reset in-process. Every host defines this, not just
# Android - it replaced an ANDROID macro that was never about Android.
add_compile_definitions(TRS80_EMBEDDED)

# The platform-neutral core. No JNI, no Android headers, no audio backend.
set(TRS80_CORE_SOURCES
        trs80_core.c
        blit.c
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
        trs_xray_state_save.cpp
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
)

if(ANDROID)
    # Shared library, since Gradle packages it into the APK and the JVM loads
    # it with System.loadLibrary.
    add_library(xtrs SHARED ${TRS80_CORE_SOURCES} native.c audio_opensl.c)

    find_library(log-lib log)
    find_library(OpenSLES-lib OpenSLES)
    target_link_libraries(xtrs ${log-lib} ${OpenSLES-lib})
else()
    # Static, so Kotlin/Native can link it straight into the framework. The
    # audio sink (trs80_audio.h) is the host's to implement.
    add_library(trs80core STATIC ${TRS80_CORE_SOURCES})

    # Gradle supplies these for the Android build; a standalone build has to
    # ask for them itself.
    target_compile_features(trs80core PRIVATE cxx_std_11)
    target_compile_options(trs80core PRIVATE
            $<$<COMPILE_LANGUAGE:CXX>:-frtti -fexceptions>)
endif()
