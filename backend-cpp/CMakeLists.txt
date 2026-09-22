cmake_minimum_required(VERSION 3.28)
project(backend_cpp CXX)
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(OpenSSL REQUIRED)

include(FetchContent)
FetchContent_Declare(httplib GIT_REPOSITORY https://github.com/yhirose/cpp-httplib.git GIT_TAG v0.18.3)
FetchContent_Declare(json GIT_REPOSITORY https://github.com/nlohmann/json.git GIT_TAG v3.11.3)
FetchContent_MakeAvailable(httplib json)

add_executable(backend_cpp
    apps/web-api/main.cpp
    modules/orders/internal/order_service.cpp
    modules/orders/public_api/orders_api.cpp
)
target_include_directories(backend_cpp PRIVATE ${CMAKE_SOURCE_DIR})
target_link_libraries(backend_cpp PRIVATE httplib nlohmann_json::nlohmann_json OpenSSL::Crypto)
