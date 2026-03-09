cmake_minimum_required(VERSION 3.10)
project(whisper_bench C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/../src/main/jni/whisper.cpp)

# Build ggml first
add_subdirectory(${WHISPER_DIR}/ggml ${CMAKE_BINARY_DIR}/ggml)

# Build whisper library
add_library(whisper STATIC
    ${WHISPER_DIR}/src/whisper.cpp
)
target_include_directories(whisper PUBLIC
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/src
    ${WHISPER_DIR}/ggml/include
    ${WHISPER_DIR}/ggml/src
    ${WHISPER_DIR}/ggml/src/ggml-cpu
)
target_link_libraries(whisper PUBLIC ggml)
target_compile_definitions(whisper PUBLIC GGML_USE_CPU)
target_compile_options(whisper PRIVATE -O3)

# Benchmark executable
add_executable(whisper_bench bench.c)
target_link_libraries(whisper_bench whisper m)
target_include_directories(whisper_bench PRIVATE
    ${WHISPER_DIR}/include
    ${WHISPER_DIR}/ggml/include
)
target_compile_options(whisper_bench PRIVATE -O3)
