cmake_minimum_required(VERSION 3.22.1)
project(frameout_guard)

set(FRAMEOUT_CERT_SHA256 "" CACHE STRING "Expected release signing certificate SHA-256 (hex)")
set(FRAMEOUT_FINGERPRINT_MASK_HEX "a32ec5c422cb5e13e20939e9fdedb61a0773aeb5511f6cf37eb5b58e2664e86f")

string(LENGTH "${FRAMEOUT_CERT_SHA256}" FRAMEOUT_CERT_LEN)
if(NOT FRAMEOUT_CERT_LEN EQUAL 0)
    if(NOT FRAMEOUT_CERT_LEN EQUAL 64 OR NOT FRAMEOUT_CERT_SHA256 MATCHES "^[0-9a-fA-F]+$")
        message(FATAL_ERROR "FRAMEOUT_CERT_SHA256 must be empty or exactly 64 hex characters")
    endif()
endif()

function(frameout_xor_hex out_var a_hex b_hex)
    set(result "")
    foreach(i RANGE 0 31)
        math(EXPR off "${i} * 2")
        string(SUBSTRING "${a_hex}" ${off} 2 ah)
        string(SUBSTRING "${b_hex}" ${off} 2 bh)
        math(EXPR av "0x${ah}")
        math(EXPR bv "0x${bh}")
        math(EXPR xv "${av} ^ ${bv}")
        math(EXPR hi "${xv} / 16")
        math(EXPR lo "${xv} % 16")
        string(SUBSTRING "0123456789abcdef" ${hi} 1 hic)
        string(SUBSTRING "0123456789abcdef" ${lo} 1 loc)
        string(APPEND result "${hic}${loc}")
    endforeach()
    set(${out_var} "${result}" PARENT_SCOPE)
endfunction()

set(FRAMEOUT_CERT_MASKED_HEX "")
if(FRAMEOUT_CERT_SHA256)
    frameout_xor_hex(FRAMEOUT_CERT_MASKED_HEX "${FRAMEOUT_CERT_SHA256}" "${FRAMEOUT_FINGERPRINT_MASK_HEX}")
endif()

add_library(frameout_guard SHARED guard_jni.cpp guard_core.cpp)
target_compile_definitions(frameout_guard PRIVATE FRAMEOUT_CERT_MASKED_HEX_STR="${FRAMEOUT_CERT_MASKED_HEX}")
target_compile_options(frameout_guard PRIVATE
    -fvisibility=hidden -ffunction-sections -fdata-sections
    -fstack-protector-strong -Wformat -Werror=format-security
    $<$<NOT:$<CONFIG:Debug>>:-D_FORTIFY_SOURCE=2>)
set_target_properties(frameout_guard PROPERTIES LINK_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,text -Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/frameout_guard.map")

add_custom_command(TARGET frameout_guard POST_BUILD
    COMMAND python3 "${CMAKE_CURRENT_SOURCE_DIR}/../../../../scripts/inject-text-hash.py" "$<TARGET_FILE:frameout_guard>"
    COMMENT "frameout_guard: injecting .text integrity reference hash"
    VERBATIM)
