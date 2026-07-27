package org.puder.trs80

/** Character-set mappings for the emulated screen. */
object CharMapping {
    /**
     * Maps a Model 3 character code to the Unicode code point that renders it.
     *
     * Indices 0x000..0x0FF cover the Model 3 character set; 0x100..0x13F hold the
     * alternate halfwidth-Katakana bank for codes 0xC0..0xFF.
     */
    @JvmField
    val m3toUnicode = charArrayOf(
        // Special characters, highly ad-hoc.
        '\u0020', '\u00A3', '\u007C', '\u00E9', '\u00DC', '\u00C5', '\u00AC', '\u00F6',
        '\u00D8', '\u00F9', '\u00F1', '\u0060', '\u0101', '\uE00D', '\u00C4', '\u00C3',
        '\u00D1', '\u00D6', '\u00D8', '\u00D5', '\u00DF', '\u00FC', '\u00F5', '\u00E6',
        '\u00E4', '\u00E0', '\u0227', '\uE01B', '\u00C9', '\u00C6', '\u00C7', '\u02DC',

        // ASCII range.  Identity map of 20 .. 7e with special case for 7f.
        '\u0020', '\u0021', '\u0022', '\u0023', '\u0024', '\u0025', '\u0026', '\u0027',
        '\u0028', '\u0029', '\u002A', '\u002B', '\u002C', '\u002D', '\u002E', '\u002F',
        '\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037',
        '\u0038', '\u0039', '\u003A', '\u003B', '\u003C', '\u003D', '\u003E', '\u003F',
        '\u0040', '\u0041', '\u0042', '\u0043', '\u0044', '\u0045', '\u0046', '\u0047',
        '\u0048', '\u0049', '\u004A', '\u004B', '\u004C', '\u004D', '\u004E', '\u004F',
        '\u0050', '\u0051', '\u0052', '\u0053', '\u0054', '\u0055', '\u0056', '\u0057',
        '\u0058', '\u0059', '\u005A', '\u005B', '\u005C', '\u005D', '\u005E', '\u005F',
        '\u0060', '\u0061', '\u0062', '\u0063', '\u0064', '\u0065', '\u0066', '\u0067',
        '\u0068', '\u0069', '\u006A', '\u006B', '\u006C', '\u006D', '\u006E', '\u006F',
        '\u0070', '\u0071', '\u0072', '\u0073', '\u0074', '\u0075', '\u0076', '\u0077',
        '\u0078', '\u0079', '\u007A', '\u007B', '\u007C', '\u007D', '\u007E', '\u00B1',

        // Graphics characters.  Trivial map of 80 .. BF to E080 .. E0BF.
        '\uE080', '\uE081', '\uE082', '\uE083', '\uE084', '\uE085', '\uE086', '\uE087',
        '\uE088', '\uE089', '\uE08A', '\uE08B', '\uE08C', '\uE08D', '\uE08E', '\uE08F',
        '\uE090', '\uE091', '\uE092', '\uE093', '\uE094', '\uE095', '\uE096', '\uE097',
        '\uE098', '\uE099', '\uE09A', '\uE09B', '\uE09C', '\uE09D', '\uE09E', '\uE09F',
        '\uE0A0', '\uE0A1', '\uE0A2', '\uE0A3', '\uE0A4', '\uE0A5', '\uE0A6', '\uE0A7',
        '\uE0A8', '\uE0A9', '\uE0AA', '\uE0AB', '\uE0AC', '\uE0AD', '\uE0AE', '\uE0AF',
        '\uE0B0', '\uE0B1', '\uE0B2', '\uE0B3', '\uE0B4', '\uE0B5', '\uE0B6', '\uE0B7',
        '\uE0B8', '\uE0B9', '\uE0BA', '\uE0BB', '\uE0BC', '\uE0BD', '\uE0BE', '\uE0BF',

        // Special characters.  Mostly ad-hoc, but contiguous stretch for
        // the lowercase Greek letters.
        '\u2660', '\u2665', '\u2666', '\u2663', '\u263A', '\u2639', '\u2264', '\u2265',
        '\u03B1', '\u03B2', '\u03B3', '\u03B4', '\u03B5', '\u03B6', '\u03B7', '\u03B8',
        '\u03B9', '\u03BA', '\u03BC', '\u03BD', '\u03BE', '\u03BF', '\u03C0', '\u03C1',
        '\u03C2', '\u03C3', '\u03C4', '\u03C5', '\u03C6', '\u03C7', '\u03C8', '\u03C9',
        '\u2126', '\u221A', '\u00F7', '\u2211', '\u2248', '\u2206', '\u2307', '\u2260',
        '\u2301', '\uE0E9', '\u237E', '\u221E', '\u2713', '\u00A7', '\u2318', '\u00A9',
        '\u00A4', '\u00B6', '\u00A2', '\u00AE', '\uE0F4', '\uE0F5', '\uE0F6', '\u211E',
        '\u2105', '\u2642', '\u2640', '\uE0FB', '\uE0FC', '\uE0FD', '\uE0FE', '\u2302',

        // Halfwidth Katakana.  Trivial map of C1 .. FF to FF61 .. FF9F
        // with special case for C0 (Yen sign).
        '\u00A5', '\uFF61', '\uFF62', '\uFF63', '\uFF64', '\uFF65', '\uFF66', '\uFF67',
        '\uFF68', '\uFF69', '\uFF6A', '\uFF6B', '\uFF6C', '\uFF6D', '\uFF6E', '\uFF6F',
        '\uFF70', '\uFF71', '\uFF72', '\uFF73', '\uFF74', '\uFF75', '\uFF76', '\uFF77',
        '\uFF78', '\uFF79', '\uFF7A', '\uFF7B', '\uFF7C', '\uFF7D', '\uFF7E', '\uFF7F',
        '\uFF80', '\uFF81', '\uFF82', '\uFF83', '\uFF84', '\uFF85', '\uFF86', '\uFF87',
        '\uFF88', '\uFF89', '\uFF8A', '\uFF8B', '\uFF8C', '\uFF8D', '\uFF8E', '\uFF8F',
        '\uFF90', '\uFF91', '\uFF92', '\uFF93', '\uFF94', '\uFF95', '\uFF96', '\uFF97',
        '\uFF98', '\uFF99', '\uFF9A', '\uFF9B', '\uFF9C', '\uFF9D', '\uFF9E', '\uFF9F'
    )
}
