cmake_minimum_required(VERSION 3.22.1)
project(auramic_dsp LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Core DSP + C API (also built standalone on the host, see tools/).
add_library(auramic_core STATIC aura_dsp.cpp dfn.cpp polish.cpp aura_capi.cpp)
target_compile_options(auramic_core PRIVATE -O3 -fPIC -fno-finite-math-only -ffp-contract=fast)

if (ANDROID)
    add_library(auramic_dsp SHARED aura_jni.cpp)
    target_link_libraries(auramic_dsp auramic_core log)
    # Android 15+ devices may use 16 KB pages: align every LOAD segment accordingly.
    target_link_options(auramic_dsp PRIVATE -Wl,-z,max-page-size=16384)
else()
    # Host build: shared library for ctypes (python tools/tests).
    add_library(auramic_host SHARED aura_capi.cpp aura_dsp.cpp dfn.cpp polish.cpp)
    target_compile_options(auramic_host PRIVATE -O3 -fPIC -fno-finite-math-only)
    # Host JNI library: lets hosttest/ run the real Kotlin pipeline (ONNX Runtime + JNI) on a desktop JVM.
    find_package(JNI)
    if (JNI_FOUND)
        add_library(auramic_dsp SHARED aura_jni.cpp)
        target_include_directories(auramic_dsp PRIVATE ${JNI_INCLUDE_DIRS})
        target_link_libraries(auramic_dsp auramic_core)
    endif()
endif()
