LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog
LOCAL_MODULE := xtrs
LOCAL_CFLAGS := -I$(LOCAL_PATH) -I$(LOCAL_PATH)/SDL -fsigned-char -DDISKDIR='"."' -DANDROID -D__WORDSIZE=32 -DANDROID_JAVA_SCREEN_UPDATE -g
# -DSETITIMER_FIX
LOCAL_SRC_FILES := native.c \
	blit.c \
	trs_chars.c \
	trs_printer.c \
	trs_disk.c \
	trs_rom1.c \
	trs_imp_exp.c \
	error.c \
	trs_hard.c \
	trs_rom3.c \
	load_cmd.c \
	trs_rom4p.c \
	load_hex.c \
	trs_interrupt.c \
	trs_uart.c \
	trs_state_save.c \
	main.c \
	trs_io.c \
	trs_sdl_interface.c \
	trs_sdl_keyboard.c \
	z80.c \
	trs_cassette.c \
	trs_memory.c \
	SDL/SDL.c
include $(BUILD_SHARED_LIBRARY)
