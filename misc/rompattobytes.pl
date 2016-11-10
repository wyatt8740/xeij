#========================================================================================
#  rompattobytes.pl
#  Copyright (C) 2003-2016 Makoto Kamada
#
#  This file is part of the XEiJ (X68000 Emulator in Java).
#  You can use, modify and redistribute the XEiJ if the conditions are met.
#  Read the XEiJ License for more details.
#  http://stdkmd.com/xeij/
#========================================================================================

use strict;  #厳密な文法に従う
use warnings;  #警告を表示する
use utf8;  #UTF-8で記述する
{
  my ($name, $file) = @ARGV;
  open IN, '<', $file or die;
  binmode IN;
  my ($head, $text, $data);
  read IN, $head, 64;
  my $tlen = vec $head, 3, 32;
  my $dlen = vec $head, 4, 32;
  read IN, $text, $tlen;
  read IN, $data, $dlen;
  close IN;
  my $base = vec $head, 1, 32;
  $tlen and printf "text: 0x%08x-0x%08x (%d bytes)\n", $base, $base + $tlen - 1, $tlen;
  $tlen and printf "  public static final int %s_BASE = 0x%08x;\n", $name, $base;
  foreach my $pair (['TEXT', $text], ['DATA', $data]) {
    length $pair->[1] or next;
    my $str = join '', map {
      $_ == 8 ? '\\b' :
      $_ == 9 ? '\\t' :
      $_ == 10 ? '\\n' :
      $_ == 12 ? '\\f' :
      $_ == 13 ? '\\r' :
      $_ == 34 || $_ == 39 || $_ == 92 ? sprintf '\\%c', $_ :
      32 <= $_ && $_ <= 126 ? chr $_ :
      sprintf '\\%03o', $_;
    } unpack 'C*', $pair->[1];
    $str =~ s/(?<!\\)((?:\\\\)*\\)00([0-7])(?![0-7])/$1$2/g;
    $str =~ s/(?<!\\)((?:\\\\)*\\)0([0-7]{2})(?![0-7])/$1$2/g;
    printf "  public static final byte[] %s_%s = \"%s\".getBytes (ISO_8859_1);\n", $name, $pair->[0], $str;
  }
}
