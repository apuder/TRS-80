# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
cmake_minimum_required(VERSION 3.4.1)
add_subdirectory(src/main/c)
target_link_libraries(xtrs)
