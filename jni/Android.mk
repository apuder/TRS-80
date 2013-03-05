LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  
LOCAL_MODULE := z80
LOCAL_CFLAGS := -fsigned-char  
LOCAL_SRC_FILES := native.c \
	debug.c \
	trs_chars.c \
	trs_printer.c \
	trs_disk.c \
	trs_rom1.c \
	error.c \
	trs_hard.c \
	trs_rom3.c \
	load_cmd.c \
	trs_imp_exp.c \
	trs_rom4p.c \
	load_hex.c \
	trs_interrupt.c \
	trs_uart.c \
	main.c \
	trs_io.c \
	trs_xinterface.c \
	trs_keyboard.c \
	z80.c \
	trs_cassette.c \
	trs_memory.c
include $(BUILD_SHARED_LIBRARY)