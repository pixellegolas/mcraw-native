cmake_minimum_required(VERSION 3.22)
project(mcraw_native)

set(CMAKE_CXX_STANDARD 17)
include(FetchContent)

FetchContent_Declare(
  motioncam_decoder
  GIT_REPOSITORY https://github.com/mirsadm/motioncam-decoder.git
  GIT_TAG main
)
FetchContent_MakeAvailable(motioncam_decoder)

# motioncam-decoder sources
set(MCRAW_SOURCES
  ${motioncam_decoder_SOURCE_DIR}/src/Decoder.cpp
  ${motioncam_decoder_SOURCE_DIR}/src/RawData.cpp
  ${motioncam_decoder_SOURCE_DIR}/src/RawData_Legacy.cpp
  ${motioncam_decoder_SOURCE_DIR}/src/ColorPipeline.cpp
  ${motioncam_decoder_SOURCE_DIR}/src/Debayer.cpp
  ${motioncam_decoder_SOURCE_DIR}/src/Container.cpp
)

add_library(motioncam_decoder STATIC ${MCRAW_SOURCES})
target_include_directories(motioncam_decoder PUBLIC ${motioncam_decoder_SOURCE_DIR}/src)
target_link_libraries(motioncam_decoder)

# Our JNI lib
add_library(mcraw_native SHARED native-lib.cpp)
target_include_directories(mcraw_native PRIVATE ${motioncam_decoder_SOURCE_DIR}/src)
target_link_libraries(mcraw_native motioncam_decoder android log)

# json lib needed by decoder
find_package(nlohmann_json QUIET)
if(NOT nlohmann_json_FOUND)
  FetchContent_Declare(json GIT_REPOSITORY https://github.com/nlohmann/json.git GIT_TAG v3.11.2)
  FetchContent_MakeAvailable(json)
  target_link_libraries(motioncam_decoder nlohmann_json::nlohmann_json)
  target_link_libraries(mcraw_native nlohmann_json::nlohmann_json)
endif()